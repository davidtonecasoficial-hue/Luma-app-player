package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.RepeatModeOption
import com.example.data.model.UserPreferences
import com.example.player.EqualizerUiState
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerMode
import com.example.ui.i18n.formatDurationMs
import com.example.ui.i18n.formatRemainingMs
import com.example.ui.screens.components.ArtworkThumbnail
import com.example.ui.screens.components.EqualizerBottomSheet
import com.example.ui.screens.components.LyricsBottomSheet
import com.example.ui.screens.components.MediaDetailsDialog
import com.example.ui.screens.components.PlaybackSpeedBottomSheet
import com.example.ui.screens.components.PlaylistPickerDialog
import com.example.ui.screens.components.QueueBottomSheet
import com.example.ui.screens.components.SleepTimerBottomSheet
import com.example.ui.theme.InteractiveWaveformScrubber
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    eqState: EqualizerUiState,
    preferences: UserPreferences,
    playlists: List<PlaylistWithTracks>,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayFromQueue: (MediaItemEntity) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearUpcomingQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistWithTrack: (String, Long) -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onSelectEqPreset: (Int) -> Unit,
    onEqBandChange: (Int, Int) -> Unit,
    onToggleLoudness: (Boolean) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSelectSpeed: (Float) -> Unit
) {
    val item = playbackState.currentMedia
    val style = LocalLiquidGlassStyle.current

    var minimalistMode by rememberSaveable(preferences.minimalistModeDefault) {
        mutableStateOf(preferences.minimalistModeDefault)
    }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    val dynamicPrimary = if (preferences.dynamicGradientEnabled) playbackState.dynamicPrimaryColor else null
    val dynamicSecondary = if (preferences.dynamicGradientEnabled) playbackState.dynamicSecondaryColor else null

    val infiniteTransition = rememberInfiniteTransition(label = "coverSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val shouldRotate = preferences.albumArtRotation && playbackState.isPlaying && !style.reduceMotion

    LiquidGlassBackground(
        dynamicPrimary = dynamicPrimary,
        dynamicSecondary = dynamicSecondary
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (item == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.queue_empty), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.close))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("now_playing_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.close),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (item.album.isNotBlank() && !minimalistMode) {
                                Text(
                                    text = item.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row {
                            IconButton(onClick = { minimalistMode = !minimalistMode }) {
                                Icon(
                                    imageVector = if (minimalistMode) Icons.Rounded.FullscreenExit else Icons.Rounded.VisibilityOff,
                                    contentDescription = stringResource(R.string.minimalist_mode)
                                )
                            }
                            if (!minimalistMode) {
                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_to_playlist)) },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showPlaylistPicker = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.playback_speed) + " (${playbackState.playbackSpeed}×)") },
                                            leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showSpeedSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.lyrics)) },
                                            leadingIcon = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showLyricsSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.track_details)) },
                                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showDetailsDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Album Art with optional subtle rotation and swipe gesture
                    var dragAccumulated by remember { mutableFloatStateOf(0f) }
                    val swipeModifier = if (preferences.swipeGesturesEnabled) {
                        Modifier.pointerInput(item.id) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulated = 0f },
                                onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount },
                                onDragEnd = {
                                    if (dragAccumulated < -90f) onSkipNext()
                                    else if (dragAccumulated > 90f) onSkipPrevious()
                                    dragAccumulated = 0f
                                }
                            )
                        }
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1f)
                            .then(swipeModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkThumbnail(
                            artworkPath = item.artworkPath,
                            isVideo = item.isVideo,
                            contentDescription = item.title,
                            cornerRadius = if (shouldRotate) 180.dp else 30.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(24.dp, RoundedCornerShape(if (shouldRotate) 180.dp else 30.dp))
                                .graphicsLayer {
                                    if (shouldRotate) {
                                        rotationZ = spinAngle
                                    }
                                }
                        )
                    }

                    // Title + Artist + Favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.artist.ifBlank { stringResource(R.string.unknown_artist) },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!minimalistMode) {
                            IconButton(
                                onClick = { onToggleFavorite(item) },
                                modifier = Modifier.testTag("now_playing_fav_btn")
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = stringResource(R.string.favorite),
                                    tint = if (item.isFavorite) style.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Waveform / Progress Scrubber + Timestamps
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val duration = playbackState.durationMs.coerceAtLeast(1L)
                        val progress = (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        InteractiveWaveformScrubber(
                            progress = progress,
                            isPlaying = playbackState.isPlaying,
                            seedKey = item.id,
                            showWaveform = preferences.visualizerEnabled && !minimalistMode,
                            onSeekFraction = { frac ->
                                onSeekTo((frac * duration).toLong())
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDurationMs(playbackState.positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatRemainingMs(playbackState.positionMs, playbackState.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Main Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!minimalistMode) {
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.testTag("now_playing_shuffle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = stringResource(R.string.shuffle),
                                    tint = if (playbackState.shuffleEnabled) {
                                        style.accentPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("now_playing_prev_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.previous),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = style.accentPrimary,
                            shadowElevation = 14.dp,
                            modifier = Modifier
                                .size(74.dp)
                                .testTag("now_playing_play_pause_btn")
                        ) {
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AnimatedContent(
                                    targetState = playbackState.isPlaying,
                                    transitionSpec = {
                                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                                    },
                                    label = "npPlayPause"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("now_playing_next_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.next),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        if (!minimalistMode) {
                            IconButton(
                                onClick = onCycleRepeat,
                                modifier = Modifier.testTag("now_playing_repeat_btn")
                            ) {
                                val (repIcon, repDesc) = when (playbackState.repeatMode) {
                                    RepeatModeOption.OFF -> Icons.Rounded.Repeat to R.string.repeat_off
                                    RepeatModeOption.ALL -> Icons.Rounded.Repeat to R.string.repeat_all
                                    RepeatModeOption.ONE -> Icons.Rounded.RepeatOne to R.string.repeat_one
                                }
                                Icon(
                                    imageVector = repIcon,
                                    contentDescription = stringResource(repDesc),
                                    tint = if (playbackState.repeatMode != RepeatModeOption.OFF) {
                                        style.accentPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Liquid Glass Action Dock (inspired by Image 3)
                    if (!minimalistMode) {
                        LiquidGlassCard(
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showQueueSheet = true },
                                    modifier = Modifier.testTag("now_playing_queue_btn")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = stringResource(R.string.queue)
                                    )
                                }
                                IconButton(
                                    onClick = { showEqSheet = true },
                                    modifier = Modifier.testTag("now_playing_eq_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.Equalizer,
                                        contentDescription = stringResource(R.string.equalizer),
                                        tint = if (eqState.isEnabled) style.accentPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { showTimerSheet = true },
                                    modifier = Modifier.testTag("now_playing_timer_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.Timer,
                                        contentDescription = stringResource(R.string.sleep_timer),
                                        tint = if (playbackState.sleepTimerMode != SleepTimerMode.OFF) {
                                            style.accentPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { showSpeedSheet = true },
                                    modifier = Modifier.testTag("now_playing_speed_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.Speed,
                                        contentDescription = stringResource(R.string.playback_speed)
                                    )
                                }
                                IconButton(
                                    onClick = { showLyricsSheet = true },
                                    modifier = Modifier.testTag("now_playing_lyrics_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.Lyrics,
                                        contentDescription = stringResource(R.string.lyrics)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentIndex,
            onPlayAt = onPlayFromQueue,
            onMoveItem = onMoveQueueItem,
            onRemoveItem = onRemoveQueueItem,
            onClearUpcoming = onClearUpcomingQueue,
            onSaveAsPlaylist = onSaveQueueAsPlaylist,
            onDismiss = { showQueueSheet = false }
        )
    }
    if (showEqSheet) {
        EqualizerBottomSheet(
            eqState = eqState,
            onToggleEq = onToggleEq,
            onSelectPreset = onSelectEqPreset,
            onBandLevelChange = onEqBandChange,
            onToggleLoudness = onToggleLoudness,
            onDismiss = { showEqSheet = false }
        )
    }
    if (showTimerSheet) {
        SleepTimerBottomSheet(
            mode = playbackState.sleepTimerMode,
            remainingMs = playbackState.sleepTimerRemainingMs,
            onSelectMinutes = onSetSleepTimerMinutes,
            onSelectEndOfTrack = onSetSleepTimerEndOfTrack,
            onCancelTimer = onCancelSleepTimer,
            onDismiss = { showTimerSheet = false }
        )
    }
    if (showSpeedSheet) {
        PlaybackSpeedBottomSheet(
            currentSpeed = playbackState.playbackSpeed,
            onSelectSpeed = onSelectSpeed,
            onDismiss = { showSpeedSheet = false }
        )
    }
    if (showLyricsSheet && item != null) {
        LyricsBottomSheet(item = item, onDismiss = { showLyricsSheet = false })
    }
    if (showDetailsDialog && item != null) {
        MediaDetailsDialog(item = item, onDismiss = { showDetailsDialog = false })
    }
    if (showPlaylistPicker && item != null) {
        PlaylistPickerDialog(
            playlists = playlists,
            onSelectPlaylist = { pid -> onAddTrackToPlaylist(pid, item.id) },
            onCreateNewPlaylist = { name -> onCreatePlaylistWithTrack(name, item.id) },
            onDismiss = { showPlaylistPicker = false }
        )
    }
}
