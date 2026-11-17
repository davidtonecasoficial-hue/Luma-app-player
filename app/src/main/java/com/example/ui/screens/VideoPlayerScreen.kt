package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.player.PlaybackUiState
import com.example.player.SelectableTrackOption
import com.example.ui.i18n.findActivity
import com.example.ui.i18n.formatDurationMs
import com.example.ui.screens.components.PlaybackSpeedBottomSheet
import com.example.ui.theme.LiquidGlassCard
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    player: Player?,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectAudioTrack: (SelectableTrackOption) -> Unit,
    onSelectSubtitleTrack: (SelectableTrackOption?) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }

    var volumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat())
    }
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.65f
        )
    }

    // Immersive fullscreen system bars + restore orientation on exit
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window?.let {
                val lp = it.attributes
                lp.screenBrightness = -1f
                it.attributes = lp
            }
        }
    }

    BackHandler { onBack() }

    LaunchedEffect(controlsVisible, playbackState.isPlaying, controlsLocked) {
        if (controlsVisible && playbackState.isPlaying && !controlsLocked) {
            delay(3500L)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.player = player
                        this.resizeMode = resizeMode
                    }
                },
                update = { pv ->
                    if (pv.player !== player) pv.player = player
                    pv.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (playbackState.isBuffering) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (controlsLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        tintColor = Color(0xAA0C1633),
                        onClick = { controlsLocked = false },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.video_unlock_controls),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Top Glass Bar
                    LiquidGlassCard(
                        shape = RoundedCornerShape(22.dp),
                        tintColor = Color(0x990D1633),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("video_back_btn")) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.close),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = playbackState.currentMedia?.title ?: stringResource(R.string.video_library_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showTracksDialog = true }) {
                                Icon(
                                    Icons.Rounded.Subtitles,
                                    contentDescription = stringResource(R.string.video_tracks),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.AspectRatio,
                                    contentDescription = stringResource(R.string.video_aspect_fit),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                isLandscape = !isLandscape
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.ScreenRotation,
                                    contentDescription = stringResource(R.string.video_rotate),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { controlsLocked = true }) {
                                Icon(
                                    Icons.Rounded.LockOpen,
                                    contentDescription = stringResource(R.string.video_lock_controls),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Center Transport Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x880D1633),
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(onClick = { onSeekBy(-10_000L) }) {
                                Icon(
                                    Icons.Rounded.Replay10,
                                    contentDescription = "-10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .size(70.dp)
                                .testTag("video_play_pause_btn")
                        ) {
                            IconButton(onClick = onTogglePlayPause) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(if (playbackState.isPlaying) R.string.pause else R.string.play),
                                    tint = Color(0xFF0A1433),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0x880D1633),
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(onClick = { onSeekBy(10_000L) }) {
                                Icon(
                                    Icons.Rounded.Forward10,
                                    contentDescription = "+10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Bottom Glass Control Deck: Seekbar + Brightness + Volume + Speed
                    LiquidGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        tintColor = Color(0xAA0D1633),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            val duration = playbackState.durationMs.coerceAtLeast(1L)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDurationMs(playbackState.positionMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                                Slider(
                                    value = playbackState.positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                                    onValueChange = { onSeekTo(it.toLong()) },
                                    valueRange = 0f..duration.toFloat(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                                Text(
                                    text = formatDurationMs(playbackState.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.BrightnessMedium,
                                    contentDescription = stringResource(R.string.video_brightness),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Slider(
                                    value = brightnessLevel,
                                    onValueChange = { level ->
                                        brightnessLevel = level
                                        activity?.window?.let { w ->
                                            val lp = w.attributes
                                            lp.screenBrightness = level.coerceIn(0.05f, 1.0f)
                                            w.attributes = lp
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = stringResource(R.string.video_volume),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Slider(
                                    value = volumeLevel,
                                    onValueChange = { level ->
                                        volumeLevel = level
                                        val target = (level * maxVolume).toInt().coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { showSpeedSheet = true }) {
                                    Icon(
                                        Icons.Rounded.Speed,
                                        contentDescription = stringResource(R.string.playback_speed),
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${playbackState.playbackSpeed}×", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSpeedSheet) {
        PlaybackSpeedBottomSheet(
            currentSpeed = playbackState.playbackSpeed,
            onSelectSpeed = onSelectSpeed,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showTracksDialog) {
        AlertDialog(
            onDismissRequest = { showTracksDialog = false },
            title = { Text(stringResource(R.string.video_tracks)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (playbackState.audioTrackOptions.isEmpty() && playbackState.subtitleTrackOptions.isEmpty()) {
                        Text(stringResource(R.string.video_no_alt_tracks))
                    } else {
                        if (playbackState.audioTrackOptions.isNotEmpty()) {
                            Text(stringResource(R.string.video_audio_tracks), style = MaterialTheme.typography.titleSmall)
                            playbackState.audioTrackOptions.forEach { opt ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectAudioTrack(opt) }
                                ) {
                                    RadioButton(selected = opt.isSelected, onClick = { onSelectAudioTrack(opt) })
                                    Text(opt.label)
                                }
                            }
                        }
                        if (playbackState.subtitleTrackOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(stringResource(R.string.video_subtitle_tracks), style = MaterialTheme.typography.titleSmall)
                            val noneSelected = playbackState.subtitleTrackOptions.none { it.isSelected }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSubtitleTrack(null) }
                            ) {
                                RadioButton(selected = noneSelected, onClick = { onSelectSubtitleTrack(null) })
                                Text(stringResource(R.string.video_subtitles_off))
                            }
                            playbackState.subtitleTrackOptions.forEach { opt ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectSubtitleTrack(opt) }
                                ) {
                                    RadioButton(selected = opt.isSelected, onClick = { onSelectSubtitleTrack(opt) })
                                    Text(opt.label)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTracksDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}
