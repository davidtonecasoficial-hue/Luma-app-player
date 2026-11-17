package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HistoryWithMedia
import com.example.data.model.MediaItemEntity
import com.example.ui.i18n.formatDurationMs
import com.example.ui.i18n.formatRelativeTimestamp
import com.example.ui.screens.components.ArtworkThumbnail
import com.example.ui.screens.components.ConfirmActionDialog
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle

@Composable
fun HistoryScreen(
    history: List<HistoryWithMedia>,
    mostPlayed: List<MediaItemEntity>,
    totalListeningMs: Long,
    onPlayMedia: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onOpenVideoPlayer: (MediaItemEntity) -> Unit,
    onDeleteHistoryEntry: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    onResetStats: () -> Unit,
    bottomContentPadding: Dp
) {
    val style = LocalLiquidGlassStyle.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var showResetStatsConfirm by remember { mutableStateOf(false) }
    val totalPlays = remember(mostPlayed) { mostPlayed.sumOf { it.playCount } }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.history_clear_all))
                        }
                    }
                }
            }

            // Local Listening Stats Card
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    tintColor = style.accentPrimary.copy(alpha = 0.14f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatDurationMs(totalListeningMs),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = style.accentPrimary
                                )
                                Text(
                                    text = stringResource(R.string.stats_listening_time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$totalPlays",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.stats_total_plays),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (totalListeningMs > 0L || totalPlays > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { showResetStatsConfirm = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.stats_reset))
                            }
                        }
                    }
                }
            }

            if (mostPlayed.isNotEmpty()) {
                item { SectionHeader(title = stringResource(R.string.stats_most_played)) }
                items(mostPlayed.take(5), key = { "top_${it.id}" }) { item ->
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = {
                            if (item.isVideo) onOpenVideoPlayer(item)
                            else onPlayMedia(item, mostPlayed.filter { !it.isVideo })
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkThumbnail(
                                artworkPath = item.artworkPath,
                                isVideo = item.isVideo,
                                contentDescription = item.title,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
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
                            Text(
                                text = "${item.playCount}×",
                                style = MaterialTheme.typography.labelLarge,
                                color = style.accentPrimary
                            )
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = style.accentPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.history_empty_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(history, key = { it.history.id }) { entry ->
                    val item = entry.media
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = {
                            if (item.isVideo) onOpenVideoPlayer(item)
                            else onPlayMedia(item, listOf(item))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkThumbnail(
                                artworkPath = item.artworkPath,
                                isVideo = item.isVideo,
                                contentDescription = item.title,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formatRelativeTimestamp(entry.history.playedAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteHistoryEntry(entry.history.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmActionDialog(
            title = stringResource(R.string.history_clear_all),
            message = stringResource(R.string.history_clear_confirm),
            onConfirm = onClearAllHistory,
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showResetStatsConfirm) {
        ConfirmActionDialog(
            title = stringResource(R.string.stats_reset),
            message = stringResource(R.string.stats_reset_confirm),
            onConfirm = onResetStats,
            onDismiss = { showResetStatsConfirm = false }
        )
    }
}
