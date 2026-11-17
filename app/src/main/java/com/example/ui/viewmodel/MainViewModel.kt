package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ImportResult
import com.example.data.local.MediaDatabase
import com.example.data.local.MediaMetadataExtractor
import com.example.data.local.MediaRepository
import com.example.data.local.MediaSyncEngine
import com.example.data.local.PreferencesRepository
import com.example.data.local.SyncResult
import com.example.data.model.AccentColorOption
import com.example.data.model.AppLanguage
import com.example.data.model.GlassQuality
import com.example.data.model.HistoryWithMedia
import com.example.data.model.LibrarySortField
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SyncState
import com.example.data.model.SyncedFolderEntity
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import com.example.player.AudioEffectsManager
import com.example.player.DeviceCodecGroup
import com.example.player.DeviceCodecInspector
import com.example.player.EqualizerUiState
import com.example.player.PlaybackController
import com.example.player.PlaybackUiState
import com.example.player.SelectableTrackOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val key: String) {
    SONGS("songs"),
    ALBUMS("albums"),
    ARTISTS("artists"),
    FOLDERS("folders"),
    PLAYLISTS("playlists"),
    FAVORITES("favorites"),
    VIDEOS("videos");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: SONGS
    }
}

enum class SearchFilter {
    ALL, SONGS, ALBUMS, ARTISTS, PLAYLISTS, VIDEOS
}

sealed interface UiNotice {
    data class ImportDone(val added: Int, val duplicates: Int) : UiNotice
    data class SyncDone(val added: Int, val unavailable: Int) : UiNotice
    data class BackupExported(val tracks: Int, val playlists: Int) : UiNotice
    data class BackupImported(val playlists: Int) : UiNotice
    data class GenericError(val message: String) : UiNotice
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MediaDatabase.getInstance(application)
    private val prefsRepo = PreferencesRepository(application)
    private val extractor = MediaMetadataExtractor(application)
    private val syncEngine = MediaSyncEngine(application, db.mediaDao(), extractor, prefsRepo)
    private val repository = MediaRepository(db.mediaDao(), extractor, syncEngine)
    val playbackController = PlaybackController(application, repository, prefsRepo)

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences()
    )

    val allMedia: StateFlow<List<MediaItemEntity>> = repository.allMedia.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val audioTracks: StateFlow<List<MediaItemEntity>> = repository.audioTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val videoItems: StateFlow<List<MediaItemEntity>> = repository.videoItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites: StateFlow<List<MediaItemEntity>> = repository.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val mostPlayed: StateFlow<List<MediaItemEntity>> = repository.mostPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<PlaylistWithTracks>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history: StateFlow<List<HistoryWithMedia>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncedFolders: StateFlow<List<SyncedFolderEntity>> = repository.syncedFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playbackState: StateFlow<PlaybackUiState> = playbackController.state
    val equalizerState: StateFlow<EqualizerUiState> = AudioEffectsManager.state

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _sortField = MutableStateFlow(LibrarySortField.NAME)
    val sortField: StateFlow<LibrarySortField> = _sortField.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _notices = MutableSharedFlow<UiNotice>(extraBufferCapacity = 8)
    val notices: SharedFlow<UiNotice> = _notices.asSharedFlow()

    val sortedAudioTracks: StateFlow<List<MediaItemEntity>> = combine(
        audioTracks,
        _sortField,
        _sortAscending
    ) { tracks, field, asc ->
        sortMediaList(tracks, field, asc)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedVideoItems: StateFlow<List<MediaItemEntity>> = combine(
        videoItems,
        _sortField,
        _sortAscending
    ) { videos, field, asc ->
        sortMediaList(videos, field, asc)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceCodecs: List<DeviceCodecGroup> by lazy {
        DeviceCodecInspector.getSupportedDecoders()
    }

    init {
        playbackController.connect()
        viewModelScope.launch {
            if (repository.getAllMediaOnce().isNotEmpty()) {
                syncOfflineLibrary(silent = true)
            }
        }
    }

    private fun sortMediaList(
        items: List<MediaItemEntity>,
        field: LibrarySortField,
        asc: Boolean
    ): List<MediaItemEntity> {
        val comparator = when (field) {
            LibrarySortField.NAME -> compareBy<MediaItemEntity> { it.title.lowercase() }
            LibrarySortField.ARTIST -> compareBy<MediaItemEntity> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            LibrarySortField.ALBUM -> compareBy<MediaItemEntity> { it.album.lowercase() }.thenBy { it.title.lowercase() }
            LibrarySortField.DATE_ADDED -> compareBy<MediaItemEntity> { it.dateAdded }
            LibrarySortField.DURATION -> compareBy<MediaItemEntity> { it.durationMs }
        }
        val sorted = items.sortedWith(comparator)
        return if (asc) sorted else sorted.reversed()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun setSortField(field: LibrarySortField) {
        if (_sortField.value == field) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortField.value = field
            _sortAscending.value = true
        }
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    // Import & Offline Sync
    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing()
            try {
                val res: ImportResult = repository.importFiles(uris)
                _syncState.value = SyncState.Synced(
                    timestampMs = System.currentTimeMillis(),
                    totalItems = allMedia.value.size + res.addedCount,
                    newlyAdded = res.addedCount,
                    unavailableCount = 0
                )
                _notices.tryEmit(UiNotice.ImportDone(res.addedCount, res.duplicateCount))
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Import error")
                _notices.tryEmit(UiNotice.GenericError(e.localizedMessage ?: "Import error"))
            }
        }
    }

    fun linkAndSyncFolder(treeUri: Uri) {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing()
            try {
                val res: SyncResult = repository.linkFolder(treeUri)
                _syncState.value = SyncState.Synced(
                    timestampMs = System.currentTimeMillis(),
                    totalItems = res.totalItems,
                    newlyAdded = res.newlyAdded,
                    unavailableCount = res.unavailableCount
                )
                _notices.tryEmit(UiNotice.SyncDone(res.newlyAdded, res.unavailableCount))
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync error")
                _notices.tryEmit(UiNotice.GenericError(e.localizedMessage ?: "Sync error"))
            }
        }
    }

    fun syncOfflineLibrary(silent: Boolean = false) {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing()
            try {
                val res = repository.syncOfflineLibrary()
                _syncState.value = SyncState.Synced(
                    timestampMs = System.currentTimeMillis(),
                    totalItems = res.totalItems,
                    newlyAdded = res.newlyAdded,
                    unavailableCount = res.unavailableCount
                )
                if (!silent) {
                    _notices.tryEmit(UiNotice.SyncDone(res.newlyAdded, res.unavailableCount))
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync error")
                if (!silent) {
                    _notices.tryEmit(UiNotice.GenericError(e.localizedMessage ?: "Sync error"))
                }
            }
        }
    }

    fun removeSyncedFolder(folderId: Long) {
        viewModelScope.launch { repository.removeFolder(folderId) }
    }

    fun exportOfflineBackup(destUri: Uri) {
        viewModelScope.launch {
            try {
                val (tracks, playlistsCount) = syncEngine.exportOfflineBackup(
                    destUri,
                    preferences.value.totalListeningMs
                )
                _notices.tryEmit(UiNotice.BackupExported(tracks, playlistsCount))
            } catch (e: Exception) {
                _notices.tryEmit(UiNotice.GenericError(e.localizedMessage ?: "Export failed"))
            }
        }
    }

    fun importOfflineBackup(srcUri: Uri) {
        viewModelScope.launch {
            try {
                val count = syncEngine.importOfflineBackup(srcUri)
                _notices.tryEmit(UiNotice.BackupImported(count))
            } catch (e: Exception) {
                _notices.tryEmit(UiNotice.GenericError(e.localizedMessage ?: "Import failed"))
            }
        }
    }

    // Playback actions
    fun playItem(item: MediaItemEntity, queue: List<MediaItemEntity> = listOf(item)) {
        playbackController.playMedia(item, queue)
    }

    fun playSmartMix(seed: MediaItemEntity? = null) {
        val mix = syncEngine.buildSmartMix(audioTracks.value, seed)
        if (mix.isNotEmpty()) {
            playbackController.playMedia(mix.first(), mix)
        }
    }

    fun playMoodCollection(mood: MoodCategory) {
        val matching = audioTracks.value.filter { it.isAvailable && it.moodKey == mood.key }
        if (matching.isNotEmpty()) {
            playbackController.playMedia(matching.first(), matching)
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        val next = !item.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(item.id, next)
            playbackController.refreshCurrentMetadata(item.copy(isFavorite = next))
        }
    }

    fun setMood(item: MediaItemEntity, mood: MoodCategory) {
        viewModelScope.launch {
            repository.setMood(item.id, mood)
            playbackController.refreshCurrentMetadata(item.copy(moodKey = mood.key))
        }
    }

    fun removeMediaFromLibrary(mediaId: Long) {
        viewModelScope.launch { repository.removeMediaFromLibrary(mediaId) }
    }

    // Playlists
    fun createPlaylist(name: String, initialTrackIds: List<Long> = emptyList()) {
        viewModelScope.launch { repository.createPlaylist(name, initialTrackIds) }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, newName) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { repository.addTrackToPlaylist(playlistId, mediaId) }
    }

    fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, mediaId) }
    }

    // History & Stats
    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch { repository.deleteHistoryEntry(historyId) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun resetListeningStats() {
        viewModelScope.launch {
            repository.resetPlayCounts()
            prefsRepo.resetListeningStats()
        }
    }

    // Equalizer & Audio Effects
    fun setEqualizerEnabled(enabled: Boolean) {
        AudioEffectsManager.setEqualizerEnabled(enabled)
        viewModelScope.launch {
            val eq = equalizerState.value
            prefsRepo.setEqualizerConfig(
                enabled = enabled,
                presetIndex = eq.selectedPresetIndex,
                bandLevels = eq.bands.map { it.levelMb }
            )
        }
    }

    fun selectEqualizerPreset(index: Int) {
        AudioEffectsManager.selectPreset(index)
        viewModelScope.launch {
            val eq = equalizerState.value
            prefsRepo.setEqualizerConfig(
                enabled = eq.isEnabled,
                presetIndex = index,
                bandLevels = emptyList()
            )
        }
    }

    fun setEqualizerBand(bandIndex: Int, levelMb: Int) {
        AudioEffectsManager.setBandLevel(bandIndex, levelMb)
        viewModelScope.launch {
            val eq = equalizerState.value
            prefsRepo.setEqualizerConfig(
                enabled = eq.isEnabled,
                presetIndex = -1,
                bandLevels = eq.bands.map { it.levelMb }
            )
        }
    }

    fun setLoudnessNormalization(enabled: Boolean) {
        AudioEffectsManager.setLoudnessEnabled(enabled)
        viewModelScope.launch { prefsRepo.setLoudnessNormalization(enabled) }
    }

    // Video tracks
    fun selectAudioTrack(opt: SelectableTrackOption) = playbackController.selectAudioTrack(opt)
    fun selectSubtitleTrack(opt: SelectableTrackOption?) = playbackController.selectSubtitleTrack(opt)

    // Preferences
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setAccentColor(accent: AccentColorOption) = viewModelScope.launch { prefsRepo.setAccentColor(accent) }
    fun setGlassQuality(quality: GlassQuality) = viewModelScope.launch { prefsRepo.setGlassQuality(quality) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { prefsRepo.setLanguage(lang) }
    fun setReduceMotion(v: Boolean) = viewModelScope.launch { prefsRepo.setReduceMotion(v) }
    fun setAlbumArtRotation(v: Boolean) = viewModelScope.launch { prefsRepo.setAlbumArtRotation(v) }
    fun setVisualizerEnabled(v: Boolean) = viewModelScope.launch { prefsRepo.setVisualizerEnabled(v) }
    fun setDynamicGradientEnabled(v: Boolean) = viewModelScope.launch { prefsRepo.setDynamicGradientEnabled(v) }
    fun setHapticsEnabled(v: Boolean) = viewModelScope.launch { prefsRepo.setHapticsEnabled(v) }
    fun setSwipeGesturesEnabled(v: Boolean) = viewModelScope.launch { prefsRepo.setSwipeGesturesEnabled(v) }
    fun setCrossfade(enabled: Boolean, durationSec: Int? = null) = viewModelScope.launch {
        prefsRepo.setCrossfade(enabled, durationSec)
        playbackController.updateCrossfadeSetting(enabled, durationSec ?: preferences.value.crossfadeDurationSec)
    }
    fun setResumeLastSession(v: Boolean) = viewModelScope.launch { prefsRepo.setResumeLastSession(v) }
    fun setMinimalistModeDefault(v: Boolean) = viewModelScope.launch { prefsRepo.setMinimalistModeDefault(v) }

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }
}
