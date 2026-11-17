package com.example.ui.screens.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.PlaylistWithTracks
import com.example.player.PlaybackUiState
import com.example.ui.i18n.formatDurationMs
import com.example.ui.i18n.formatFileSize
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle
import java.io.File

@Composable
fun ArtworkThumbnail(
    artworkPath: String?,
    isVideo: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    val fileExists = remember(artworkPath) {
        !artworkPath.isNullOrBlank() && File(artworkPath).exists()
    }
    val fallbackRes = if (isVideo) R.drawable.img_default_video_thumb else R.drawable.img_default_album_art

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (fileExists) {
            AsyncImage(
                model = File(artworkPath!!),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = fallbackRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun LiquidGlassMiniPlayer(
    playbackState: PlaybackUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = playbackState.currentMedia ?: return
    val style = LocalLiquidGlassStyle.current
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("mini_player_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = 14.dp,
        tintColor = if (style.isDark) {
            (playbackState.dynamicPrimaryColor ?: Color(0xFF1C3570))
        } else {
            Color(0xFFFFFFFF)
        },
        onClick = onExpand
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkThumbnail(
                    artworkPath = item.artworkPath,
                    isVideo = item.isVideo,
                    contentDescription = item.title,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
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
                IconButton(
                    onClick = { onToggleFavorite(item) },
                    modifier = Modifier.testTag("mini_player_fav_btn")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = if (item.isFavorite) style.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = style.accentPrimary,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mini_player_play_pause")
                ) {
                    IconButton(onClick = onTogglePlayPause) {
                        AnimatedContent(
                            targetState = playbackState.isPlaying,
                            transitionSpec = {
                                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                            },
                            label = "miniPlayPause"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("mini_player_dismiss_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.dismiss_mini_player),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape),
                color = style.accentPrimary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun MediaTrackRow(
    item: MediaItemEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSetMood: () -> Unit,
    onShowDetails: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    extraMenuActionLabel: String? = null,
    onExtraMenuAction: (() -> Unit)? = null
) {
    val style = LocalLiquidGlassStyle.current
    var menuExpanded by remember { mutableStateOf(false) }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_row_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = if (isCurrent) 10.dp else 4.dp,
        tintColor = if (isCurrent) style.accentPrimary.copy(alpha = 0.25f) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                ArtworkThumbnail(
                    artworkPath = item.artworkPath,
                    isVideo = item.isVideo,
                    contentDescription = item.title,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(52.dp)
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Equalizer else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCurrent) style.accentPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!item.isAvailable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = stringResource(R.string.unavailable_badge),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val subParts = buildList {
                    if (item.isVideo) {
                        if (item.width > 0 && item.height > 0) add("${item.width}x${item.height}")
                    } else {
                        add(item.artist.ifBlank { stringResource(R.string.unknown_artist) })
                    }
                    if (item.formatLabel.isNotBlank()) add(item.formatLabel)
                    if (item.durationMs > 0L) add(formatDurationMs(item.durationMs))
                }
                Text(
                    text = subParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (item.isFavorite) style.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.track_details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.play_next)) },
                        leadingIcon = { Icon(Icons.Rounded.QueuePlayNext, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_queue)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.set_mood)) },
                        leadingIcon = { Icon(Icons.Rounded.Mood, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSetMood()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.track_details)) },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShowDetails()
                        }
                    )
                    if (extraMenuActionLabel != null && onExtraMenuAction != null) {
                        DropdownMenuItem(
                            text = { Text(extraMenuActionLabel) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onExtraMenuAction()
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.remove_from_library),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemoveFromLibrary()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaGridCard(
    item: MediaItemEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalLiquidGlassStyle.current
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tintColor = if (isCurrent) style.accentPrimary.copy(alpha = 0.25f) else null,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                ArtworkThumbnail(
                    artworkPath = item.artworkPath,
                    isVideo = item.isVideo,
                    contentDescription = item.title,
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.formatLabel.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = item.formatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) style.accentPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.isVideo) {
                    formatDurationMs(item.durationMs)
                } else {
                    item.artist.ifBlank { stringResource(R.string.unknown_artist) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyLibraryHero(
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalLiquidGlassStyle.current
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_library),
                contentDescription = stringResource(R.string.home_empty_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onImportFiles,
                colors = ButtonDefaults.buttonColors(containerColor = style.accentPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_import_files_btn")
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_import_files))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onImportFolder,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_sync_folder_btn")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_import_folder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onPickVideo,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_pick_video_btn")
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_pick_video),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    titleText: String = stringResource(R.string.create_playlist),
    confirmText: String = stringResource(R.string.create),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_playlist_btn")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistWithTracks>,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onConfirm = { newName ->
                onCreateNewPlaylist(newName)
                showCreate = false
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedButton(
                        onClick = { showCreate = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.create_playlist))
                    }
                }
                items(playlists, key = { it.playlist.id }) { pw ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectPlaylist(pw.playlist.id)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pw.playlist.name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.songs_count, pw.tracks.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun MoodPickerDialog(
    currentMoodKey: String,
    onSelectMood: (MoodCategory) -> Unit,
    onDismiss: () -> Unit
) {
    val moods = listOf(
        MoodCategory.CHILL to stringResource(R.string.mood_chill),
        MoodCategory.FOCUS to stringResource(R.string.mood_focus),
        MoodCategory.WORKOUT to stringResource(R.string.mood_workout),
        MoodCategory.ROAD_TRIP to stringResource(R.string.mood_road_trip),
        MoodCategory.NONE to stringResource(R.string.mood_none)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_mood)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                moods.forEach { (mood, label) ->
                    val selected = mood.key == currentMoodKey
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMood(mood)
                                onDismiss()
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun MediaDetailsDialog(
    item: MediaItemEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(stringResource(R.string.format_label), item.formatLabel.ifBlank { "—" })
                DetailRow(stringResource(R.string.mime_type_label), item.mimeType.ifBlank { "—" })
                DetailRow(stringResource(R.string.duration_label), formatDurationMs(item.durationMs))
                DetailRow(stringResource(R.string.size_label), formatFileSize(item.sizeBytes))
                if (item.bitrateKbps > 0) {
                    DetailRow(stringResource(R.string.bitrate_label), "${item.bitrateKbps} kbps")
                }
                if (item.sampleRateHz > 0) {
                    DetailRow(stringResource(R.string.sample_rate_label), "${item.sampleRateHz} Hz")
                }
                if (item.isVideo && item.width > 0 && item.height > 0) {
                    DetailRow(stringResource(R.string.resolution_label), "${item.width} × ${item.height}")
                }
                if (item.folderName.isNotBlank()) {
                    DetailRow(stringResource(R.string.folder_label), item.folderName)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.delete),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
