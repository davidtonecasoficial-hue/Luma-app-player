package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.local.MediaRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.MediaItemEntity
import com.example.data.model.RepeatModeOption
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class SleepTimerMode {
    OFF, COUNTDOWN, END_OF_TRACK
}

data class SelectableTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class PlaybackUiState(
    val currentMedia: MediaItemEntity? = null,
    val queue: List<MediaItemEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val playbackSpeed: Float = 1.0f,
    val isMiniPlayerVisible: Boolean = false,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF,
    val sleepTimerRemainingMs: Long = 0L,
    val dynamicPrimaryColor: Color? = null,
    val dynamicSecondaryColor: Color? = null,
    val audioTrackOptions: List<SelectableTrackOption> = emptyList(),
    val subtitleTrackOptions: List<SelectableTrackOption> = emptyList(),
    val errorEvent: PlaybackErrorEvent? = null
)

data class PlaybackErrorEvent(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val detail: String,
    val isUnsupportedOrDamaged: Boolean = false,
    val isUnavailable: Boolean = false
)

class PlaybackController(
    private val context: Context,
    private val repository: MediaRepository,
    private val prefsRepo: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastRecordedMediaId: Long = -1L
    private var lastSaveTimestamp: Long = 0L
    private var hasRestoredSession: Boolean = false
    private var crossfadeEnabled: Boolean = false
    private var crossfadeDurationSec: Int = 3

    val playerOrNull: Player?
        get() = mediaController

    fun connect() {
        if (controllerFuture != null) return
        if (android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                setupPlayerListener(controller)
                scope.launch { applySavedPreferencesAndRestoreSession(controller) }
            } catch (_: Exception) {
                // Service binding failed transiently
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun applySavedPreferencesAndRestoreSession(controller: MediaController) {
        val prefs = prefsRepo.preferencesFlow.first()
        crossfadeEnabled = prefs.crossfadeEnabled
        crossfadeDurationSec = prefs.crossfadeDurationSec

        controller.shuffleModeEnabled = prefs.shuffleEnabled
        controller.repeatMode = toMedia3RepeatMode(prefs.repeatMode)
        controller.setPlaybackSpeed(prefs.playbackSpeed)

        _state.update {
            it.copy(
                shuffleEnabled = prefs.shuffleEnabled,
                repeatMode = prefs.repeatMode,
                playbackSpeed = prefs.playbackSpeed
            )
        }

        if (!hasRestoredSession && prefs.resumeLastSession && prefs.lastMediaId > 0L) {
            hasRestoredSession = true
            val allMedia = repository.getAllMediaOnce()
            val byId = allMedia.associateBy { it.id }
            val restoredQueue = prefs.lastQueueIds.mapNotNull { byId[it] }.ifEmpty {
                listOfNotNull(byId[prefs.lastMediaId])
            }
            val startItem = byId[prefs.lastMediaId]
            if (startItem != null && !startItem.isVideo && repository.extractor.isUriAccessible(startItem.uri)) {
                val startIdx = restoredQueue.indexOfFirst { it.id == startItem.id }.coerceAtLeast(0)
                val mediaItems = restoredQueue.map { it.toMedia3Item() }
                controller.setMediaItems(mediaItems, startIdx, prefs.lastPositionMs)
                controller.prepare()
                controller.pause()
                lastRecordedMediaId = startItem.id
                updateDynamicColors(startItem)
                _state.update {
                    it.copy(
                        currentMedia = startItem,
                        queue = restoredQueue,
                        currentIndex = startIdx,
                        positionMs = prefs.lastPositionMs,
                        durationMs = startItem.durationMs,
                        isMiniPlayerVisible = true
                    )
                }
            }
        }
    }

    fun updateCrossfadeSetting(enabled: Boolean, durationSec: Int) {
        crossfadeEnabled = enabled
        crossfadeDurationSec = durationSec
        if (!enabled) {
            mediaController?.volume = 1f
        }
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                    persistCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val dur = controller.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                    ?: _state.value.currentMedia?.durationMs
                    ?: 0L
                _state.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = dur
                    )
                }
                if (playbackState == Player.STATE_ENDED &&
                    _state.value.sleepTimerMode == SleepTimerMode.END_OF_TRACK
                ) {
                    cancelSleepTimer()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (_state.value.sleepTimerMode == SleepTimerMode.END_OF_TRACK &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) {
                    controller.pause()
                    cancelSleepTimer()
                }

                val idx = controller.currentMediaItemIndex
                val queue = _state.value.queue
                val matched = queue.getOrNull(idx)
                    ?: mediaItem?.mediaId?.toLongOrNull()?.let { id -> queue.firstOrNull { it.id == id } }

                if (matched != null) {
                    updateDynamicColors(matched)
                    _state.update {
                        it.copy(
                            currentMedia = matched,
                            currentIndex = idx,
                            positionMs = 0L,
                            durationMs = controller.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET }
                                ?: matched.durationMs,
                            isMiniPlayerVisible = !matched.isVideo
                        )
                    }
                    if (matched.id != lastRecordedMediaId) {
                        lastRecordedMediaId = matched.id
                        scope.launch {
                            repository.recordPlaybackStarted(matched.id, 0L)
                            prefsRepo.savePlaybackSession(matched.id, 0L, queue.map { it.id })
                        }
                    }
                }
                if (crossfadeEnabled) {
                    scope.launch { rampVolumeIn(controller) }
                } else {
                    controller.volume = 1f
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractTrackOptions(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                val current = _state.value.currentMedia
                val isSourceError = error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE

                val isFormatError = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED

                if (current != null && isSourceError) {
                    val stillAccessible = repository.extractor.isUriAccessible(current.uri)
                    if (!stillAccessible) {
                        scope.launch { repository.setAvailability(current.id, false) }
                    }
                }

                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorEvent = PlaybackErrorEvent(
                            title = current?.title ?: "Media",
                            detail = error.localizedMessage ?: error.errorCodeName,
                            isUnsupportedOrDamaged = isFormatError,
                            isUnavailable = isSourceError
                        )
                    )
                }
            }
        })
    }

    private fun extractTrackOptions(tracks: Tracks) {
        val audioOpts = mutableListOf<SelectableTrackOption>()
        val subOpts = mutableListOf<SelectableTrackOption>()
        tracks.groups.forEachIndexed { groupIdx, group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    val lang = fmt.language ?: "und"
                    val label = fmt.label ?: "Audio ${audioOpts.size + 1} (${lang.uppercase()})"
                    audioOpts.add(
                        SelectableTrackOption(
                            groupIndex = groupIdx,
                            trackIndex = i,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    val lang = fmt.language ?: "und"
                    val label = fmt.label ?: "Subtitle ${subOpts.size + 1} (${lang.uppercase()})"
                    subOpts.add(
                        SelectableTrackOption(
                            groupIndex = groupIdx,
                            trackIndex = i,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            }
        }
        _state.update {
            it.copy(
                audioTrackOptions = audioOpts,
                subtitleTrackOptions = subOpts
            )
        }
    }

    fun selectAudioTrack(option: SelectableTrackOption) {
        val controller = mediaController ?: return
        val group = controller.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        val params = controller.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
        controller.trackSelectionParameters = params
    }

    fun selectSubtitleTrack(option: SelectableTrackOption?) {
        val controller = mediaController ?: return
        val builder = controller.trackSelectionParameters.buildUpon()
        if (option == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val group = controller.currentTracks.groups.getOrNull(option.groupIndex) ?: return
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
        }
        controller.trackSelectionParameters = builder.build()
    }

    fun playMedia(item: MediaItemEntity, queue: List<MediaItemEntity> = listOf(item)) {
        val controller = mediaController ?: return
        if (!repository.extractor.isUriAccessible(item.uri)) {
            scope.launch { repository.setAvailability(item.id, false) }
            _state.update {
                it.copy(
                    errorEvent = PlaybackErrorEvent(
                        title = item.title,
                        detail = item.displayName,
                        isUnavailable = true
                    )
                )
            }
            return
        }

        val validQueue = queue.filter { it.isAvailable }.ifEmpty { listOf(item) }
        val startIndex = validQueue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        val startPosition = if (item.isVideo && item.lastPositionMs > 2000L &&
            (item.durationMs <= 0L || item.lastPositionMs < item.durationMs - 5000L)
        ) {
            item.lastPositionMs
        } else {
            0L
        }

        val mediaItems = validQueue.map { it.toMedia3Item() }
        controller.volume = 1f
        controller.setMediaItems(mediaItems, startIndex, startPosition)
        controller.prepare()
        controller.play()

        lastRecordedMediaId = item.id
        updateDynamicColors(item)

        _state.update {
            it.copy(
                currentMedia = item,
                queue = validQueue,
                currentIndex = startIndex,
                positionMs = startPosition,
                durationMs = item.durationMs,
                isMiniPlayerVisible = !item.isVideo
            )
        }

        scope.launch {
            repository.recordPlaybackStarted(item.id, startPosition)
            prefsRepo.savePlaybackSession(item.id, startPosition, validQueue.map { it.id })
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0)
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
        persistCurrentPosition()
    }

    fun seekBy(deltaMs: Long) {
        val controller = mediaController ?: return
        val target = (controller.currentPosition + deltaMs).coerceIn(
            0L,
            controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        )
        seekTo(target)
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
            controller.play()
        } else if (_state.value.queue.isNotEmpty() && _state.value.repeatMode == RepeatModeOption.ALL) {
            controller.seekTo(0, 0L)
            controller.play()
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
            controller.play()
        } else {
            seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val next = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = next
        _state.update { it.copy(shuffleEnabled = next) }
        scope.launch { prefsRepo.setShuffleEnabled(next) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = _state.value.repeatMode.next()
        controller.repeatMode = toMedia3RepeatMode(nextMode)
        _state.update { it.copy(repeatMode = nextMode) }
        scope.launch { prefsRepo.setRepeatMode(nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        val clamped = speed.coerceIn(0.5f, 2.0f)
        controller.setPlaybackSpeed(clamped)
        _state.update { it.copy(playbackSpeed = clamped) }
        scope.launch { prefsRepo.setPlaybackSpeed(clamped) }
    }

    fun playNext(item: MediaItemEntity) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playMedia(item, listOf(item))
            return
        }
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(insertIndex, item)
        controller.addMediaItem(insertIndex, item.toMedia3Item())
        _state.update { it.copy(queue = currentQueue) }
        saveQueueSnapshot(currentQueue)
    }

    fun addToQueue(item: MediaItemEntity) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playMedia(item, listOf(item))
            return
        }
        currentQueue.add(item)
        controller.addMediaItem(item.toMedia3Item())
        _state.update { it.copy(queue = currentQueue) }
        saveQueueSnapshot(currentQueue)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val moved = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, moved)
        controller.moveMediaItem(fromIndex, toIndex)
        _state.update {
            it.copy(
                queue = currentQueue,
                currentIndex = controller.currentMediaItemIndex
            )
        }
        saveQueueSnapshot(currentQueue)
    }

    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        val currentQueue = _state.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        if (currentQueue.size == 1) {
            clearQueueCompletely()
            return
        }
        currentQueue.removeAt(index)
        controller.removeMediaItem(index)
        _state.update {
            it.copy(
                queue = currentQueue,
                currentIndex = controller.currentMediaItemIndex,
                currentMedia = currentQueue.getOrNull(controller.currentMediaItemIndex)
            )
        }
        saveQueueSnapshot(currentQueue)
    }

    fun clearUpcomingQueue() {
        val controller = mediaController ?: return
        val currentIdx = controller.currentMediaItemIndex
        val current = _state.value.queue.getOrNull(currentIdx) ?: return
        if (currentIdx + 1 < controller.mediaItemCount) {
            controller.removeMediaItems(currentIdx + 1, controller.mediaItemCount)
        }
        if (currentIdx > 0) {
            controller.removeMediaItems(0, currentIdx)
        }
        val singleQueue = listOf(current)
        _state.update { it.copy(queue = singleQueue, currentIndex = 0) }
        saveQueueSnapshot(singleQueue)
    }

    private fun clearQueueCompletely() {
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()
        _state.update {
            it.copy(
                currentMedia = null,
                queue = emptyList(),
                currentIndex = -1,
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L,
                isMiniPlayerVisible = false
            )
        }
    }

    fun dismissMiniPlayer() {
        mediaController?.pause()
        _state.update { it.copy(isMiniPlayerVisible = false) }
    }

    fun showMiniPlayerIfActive() {
        val current = _state.value.currentMedia
        if (current != null && !current.isVideo) {
            _state.update { it.copy(isMiniPlayerVisible = true) }
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val totalMs = minutes * 60_000L
        _state.update {
            it.copy(
                sleepTimerMode = SleepTimerMode.COUNTDOWN,
                sleepTimerRemainingMs = totalMs
            )
        }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1000L
                _state.update { it.copy(sleepTimerRemainingMs = remaining.coerceAtLeast(0L)) }
            }
            if (isActive) {
                mediaController?.pause()
                _state.update {
                    it.copy(sleepTimerMode = SleepTimerMode.OFF, sleepTimerRemainingMs = 0L)
                }
            }
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _state.update {
            it.copy(sleepTimerMode = SleepTimerMode.END_OF_TRACK, sleepTimerRemainingMs = 0L)
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _state.update {
            it.copy(sleepTimerMode = SleepTimerMode.OFF, sleepTimerRemainingMs = 0L)
        }
    }

    fun refreshCurrentMetadata(updatedItem: MediaItemEntity) {
        val current = _state.value.currentMedia
        val updatedQueue = _state.value.queue.map {
            if (it.id == updatedItem.id) updatedItem else it
        }
        _state.update {
            it.copy(
                currentMedia = if (current?.id == updatedItem.id) updatedItem else current,
                queue = updatedQueue
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorEvent = null) }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                val controller = mediaController ?: break
                val pos = controller.currentPosition.coerceAtLeast(0L)
                val dur = controller.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                    ?: _state.value.durationMs
                _state.update { it.copy(positionMs = pos, durationMs = dur) }

                if (crossfadeEnabled && dur > 8000L && _state.value.repeatMode != RepeatModeOption.ONE) {
                    val remaining = dur - pos
                    val fadeMs = crossfadeDurationSec * 1000L
                    if (remaining in 1..fadeMs) {
                        controller.volume = (remaining.toFloat() / fadeMs.toFloat()).coerceIn(0.1f, 1f)
                    } else if (controller.volume < 0.99f && pos > fadeMs) {
                        controller.volume = 1f
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastSaveTimestamp >= 4000L) {
                    lastSaveTimestamp = now
                    persistCurrentPosition()
                    if (controller.isPlaying) {
                        prefsRepo.addListeningTime(4000L)
                    }
                }
                delay(400L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private suspend fun rampVolumeIn(controller: MediaController) {
        val steps = 8
        val stepDelay = (crossfadeDurationSec * 500L) / steps
        for (i in 1..steps) {
            controller.volume = (i.toFloat() / steps.toFloat()).coerceIn(0.1f, 1f)
            delay(stepDelay.coerceAtLeast(30L))
        }
        controller.volume = 1f
    }

    private fun persistCurrentPosition() {
        val current = _state.value.currentMedia ?: return
        val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
        val queueIds = _state.value.queue.map { it.id }
        scope.launch {
            repository.saveMediaPosition(current.id, pos)
            prefsRepo.savePlaybackSession(current.id, pos, queueIds)
        }
    }

    private fun saveQueueSnapshot(queue: List<MediaItemEntity>) {
        val current = _state.value.currentMedia ?: return
        scope.launch {
            prefsRepo.savePlaybackSession(current.id, _state.value.positionMs, queue.map { it.id })
        }
    }

    private fun updateDynamicColors(item: MediaItemEntity) {
        scope.launch {
            val colors = repository.extractor.extractDominantColors(item.artworkPath)
            _state.update {
                it.copy(
                    dynamicPrimaryColor = colors?.first,
                    dynamicSecondaryColor = colors?.second
                )
            }
        }
    }

    private fun MediaItemEntity.toMedia3Item(): MediaItem {
        val artUri = artworkPath?.let { path ->
            val f = File(path)
            if (f.exists()) Uri.fromFile(f) else null
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.ifBlank { null })
            .setAlbumTitle(album.ifBlank { null })
            .setArtworkUri(artUri)
            .setExtras(Bundle().apply { putBoolean("isVideo", isVideo) })
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun toMedia3RepeatMode(mode: RepeatModeOption): Int = when (mode) {
        RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
        RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
        RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
    }

    fun release() {
        stopPositionTicker()
        sleepTimerJob?.cancel()
        persistCurrentPosition()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}
