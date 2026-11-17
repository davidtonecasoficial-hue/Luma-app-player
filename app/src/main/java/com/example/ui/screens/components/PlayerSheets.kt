package com.example.ui.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.player.EqualizerUiState
import com.example.player.SleepTimerMode
import com.example.ui.i18n.formatDurationMs
import com.example.ui.theme.LocalLiquidGlassStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<MediaItemEntity>,
    currentIndex: Int,
    onPlayAt: (MediaItemEntity) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearUpcoming: () -> Unit,
    onSaveAsPlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalLiquidGlassStyle.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSavePlaylist by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.titleLarge)
                Row {
                    if (queue.isNotEmpty()) {
                        IconButton(onClick = { showSavePlaylist = true }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = stringResource(R.string.save_queue_as_playlist)
                            )
                        }
                    }
                    if (queue.size > 1) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_queue)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (queue.isEmpty()) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queue, key = { idx, item -> "q_${idx}_${item.id}" }) { index, item ->
                        val isCurrent = index == currentIndex
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) {
                                style.accentPrimary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onPlayAt(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkThumbnail(
                                    artworkPath = item.artworkPath,
                                    isVideo = item.isVideo,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCurrent) style.accentPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist.ifBlank { stringResource(R.string.unknown_artist) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveItem(index, index - 1) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ArrowUpward,
                                            contentDescription = stringResource(R.string.move_up),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (index < queue.lastIndex) {
                                    IconButton(
                                        onClick = { onMoveItem(index, index + 1) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ArrowDownward,
                                            contentDescription = stringResource(R.string.move_down),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveItem(index) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmActionDialog(
            title = stringResource(R.string.clear_queue),
            message = stringResource(R.string.clear_queue_confirm),
            onConfirm = onClearUpcoming,
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showSavePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showSavePlaylist = false },
            onConfirm = { name ->
                onSaveAsPlaylist(name)
                showSavePlaylist = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    eqState: EqualizerUiState,
    onToggleEq: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onToggleLoudness: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.eq_title), style = MaterialTheme.typography.titleLarge)
                }
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onToggleEq,
                    enabled = eqState.isAvailable
                )
            }

            if (!eqState.isAvailable) {
                Text(
                    text = stringResource(R.string.eq_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (eqState.presets.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(eqState.presets) { idx, name ->
                            FilterChip(
                                selected = eqState.selectedPresetIndex == idx,
                                onClick = { onSelectPreset(idx) },
                                label = { Text(name) },
                                enabled = eqState.isEnabled
                            )
                        }
                    }
                }

                eqState.bands.forEach { band ->
                    val freqText = if (band.centerFreqHz >= 1000) {
                        "${band.centerFreqHz / 1000} kHz"
                    } else {
                        "${band.centerFreqHz} Hz"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(56.dp)
                        )
                        Slider(
                            value = band.levelMb.toFloat(),
                            onValueChange = { onBandLevelChange(band.index, it.toInt()) },
                            valueRange = eqState.minLevelMb.toFloat()..eqState.maxLevelMb.toFloat(),
                            enabled = eqState.isEnabled,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${band.levelMb / 100} dB",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(46.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.eq_loudness_norm), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.eq_loudness_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = eqState.loudnessEnabled,
                    onCheckedChange = onToggleLoudness
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    mode: SleepTimerMode,
    remainingMs: Long,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.titleLarge)
            }

            if (mode == SleepTimerMode.COUNTDOWN) {
                Text(
                    text = stringResource(R.string.sleep_timer_active, formatDurationMs(remainingMs)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (mode == SleepTimerMode.END_OF_TRACK) {
                Text(
                    text = stringResource(R.string.sleep_timer_active_end),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val options = listOf(10, 15, 30, 45, 60)
            options.forEach { mins ->
                OutlinedButton(
                    onClick = {
                        onSelectMinutes(mins)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.sleep_timer_minutes, mins))
                }
            }

            Button(
                onClick = {
                    onSelectEndOfTrack()
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sleep_timer_end_of_track))
            }

            if (mode != SleepTimerMode.OFF) {
                TextButton(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.sleep_timer_off))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedBottomSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.titleLarge)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(speeds) { _, speed ->
                    FilterChip(
                        selected = kotlin.math.abs(currentSpeed - speed) < 0.05f,
                        onClick = {
                            onSelectSpeed(speed)
                            onDismiss()
                        },
                        label = { Text("${speed}×") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    item: MediaItemEntity,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lyrics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.lyrics), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = item.embeddedLyrics?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.lyrics_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
