package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SyncState
import com.example.player.PlaybackUiState
import com.example.ui.i18n.formatRelativeTimestamp
import com.example.ui.screens.components.ArtworkThumbnail
import com.example.ui.screens.components.ConfirmActionDialog
import com.example.ui.screens.components.EmptyLibraryHero
import com.example.ui.screens.components.MediaDetailsDialog
import com.example.ui.screens.components.MediaTrackRow
import com.example.ui.screens.components.MoodPickerDialog
import com.example.ui.screens.components.PlaylistPickerDialog
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.viewmodel.LibraryTab
import java.io.File
import java.util.Calendar

@Composable
fun HomeScreen(
    allMedia: List<MediaItemEntity>,
    audioTracks: List<MediaItemEntity>,
    favorites: List<MediaItemEntity>,
    playlists: List<PlaylistWithTracks>,
    playbackState: PlaybackUiState,
    syncState: SyncState,
    lastSyncTimestamp: Long,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibraryTab: (LibraryTab) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToMood: (MoodCategory) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenVideoPlayer: (MediaItemEntity) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlaySmartMix: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistWithTrack: (String, Long) -> Unit,
    onSetMood: (MediaItemEntity, MoodCategory) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onPickVideo: () -> Unit,
    onSyncNow: () -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp
) {
    val style = LocalLiquidGlassStyle.current
    val greetingRes = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    }

    val recentlyPlayed = remember(allMedia) {
        allMedia.filter { it.lastPlayedAt > 0L }.sortedByDescending { it.lastPlayedAt }.take(6)
    }
    val recentlyAdded = remember(allMedia) {
        allMedia.sortedByDescending { it.dateAdded }.take(6)
    }

    var trackForPlaylist by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForMood by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForDetails by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackToRemove by remember { mutableStateOf<MediaItemEntity?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Greeting & Offline Sync Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(greetingRes),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.greeting_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassCard(
                            shape = CircleShape,
                            onClick = onSyncNow,
                            modifier = Modifier.testTag("home_sync_btn")
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = stringResource(R.string.action_sync_now),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        LiquidGlassCard(
                            shape = CircleShape,
                            onClick = onImportFiles,
                            modifier = Modifier.testTag("home_import_btn")
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.action_import_files),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Search Pill Bar (inspired by Image 3)
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_pill")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.home_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        val syncBadge = if (lastSyncTimestamp > 0L) {
                            formatRelativeTimestamp(lastSyncTimestamp)
                        } else {
                            stringResource(R.string.action_sync_now)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = style.accentPrimary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = syncBadge,
                                style = MaterialTheme.typography.labelMedium,
                                color = style.accentPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (allMedia.isEmpty()) {
                item {
                    EmptyLibraryHero(
                        onImportFiles = onImportFiles,
                        onImportFolder = onImportFolder,
                        onPickVideo = onPickVideo
                    )
                }
            } else {
                // Hero Featured Card: Active track or Smart Mix
                item {
                    val featured = playbackState.currentMedia ?: audioTracks.firstOrNull()
                    LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        elevation = 14.dp,
                        onClick = {
                            if (playbackState.currentMedia != null) {
                                if (playbackState.currentMedia.isVideo) {
                                    onOpenVideoPlayer(playbackState.currentMedia)
                                } else {
                                    onOpenNowPlaying()
                                }
                            } else {
                                onPlaySmartMix()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .testTag("home_hero_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val artFile = featured?.artworkPath?.let { File(it) }?.takeIf { it.exists() }
                            if (artFile != null) {
                                ArtworkThumbnail(
                                    artworkPath = artFile.absolutePath,
                                    isVideo = featured.isVideo,
                                    contentDescription = null,
                                    cornerRadius = 0.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_hero_liquid_glass),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xD9081128),
                                                Color(0x990F1E46),
                                                Color(0x4019306A)
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = style.accentGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (playbackState.currentMedia != null) {
                                            stringResource(R.string.home_featured_label)
                                        } else {
                                            stringResource(R.string.home_smart_mix)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playbackState.currentMedia?.title
                                                ?: stringResource(R.string.home_smart_mix),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = playbackState.currentMedia?.let {
                                                it.artist.ifBlank { stringResource(R.string.unknown_artist) }
                                            } ?: stringResource(R.string.home_smart_mix_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (playbackState.currentMedia != null) {
                                                    onTogglePlayPause()
                                                } else {
                                                    onPlaySmartMix()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (playbackState.isPlaying) {
                                                    Icons.Rounded.Pause
                                                } else {
                                                    Icons.Rounded.PlayArrow
                                                },
                                                contentDescription = stringResource(R.string.play),
                                                tint = Color(0xFF0A1433),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mood Collections (Circular Liquid Glass Badges inspired by Image 3)
                item {
                    SectionHeader(
                        title = stringResource(R.string.home_mood_collections),
                        actionLabel = stringResource(R.string.home_smart_mix),
                        onAction = onPlaySmartMix
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val moods = listOf(
                        Triple(MoodCategory.CHILL, stringResource(R.string.mood_chill), Icons.Rounded.SelfImprovement),
                        Triple(MoodCategory.FOCUS, stringResource(R.string.mood_focus), Icons.Rounded.Psychology),
                        Triple(MoodCategory.WORKOUT, stringResource(R.string.mood_workout), Icons.Rounded.FitnessCenter),
                        Triple(MoodCategory.ROAD_TRIP, stringResource(R.string.mood_road_trip), Icons.Rounded.DirectionsCar)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(moods) { (mood, label, icon) ->
                            val count = audioTracks.count { it.moodKey == mood.key }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onNavigateToMood(mood) }
                                    .padding(4.dp)
                            ) {
                                LiquidGlassCard(
                                    shape = CircleShape,
                                    tintColor = style.accentPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (count > 0) {
                                    Text(
                                        text = stringResource(R.string.songs_count, count),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Library Shortcuts
                item {
                    SectionHeader(title = stringResource(R.string.home_shortcuts))
                    Spacer(modifier = Modifier.height(10.dp))
                    val shortcuts = listOf(
                        Triple(LibraryTab.ALBUMS, stringResource(R.string.tab_albums), Icons.Rounded.Album),
                        Triple(LibraryTab.ARTISTS, stringResource(R.string.tab_artists), Icons.Rounded.Person),
                        Triple(LibraryTab.FOLDERS, stringResource(R.string.tab_folders), Icons.Rounded.Folder),
                        Triple(LibraryTab.VIDEOS, stringResource(R.string.tab_videos), Icons.Rounded.VideoLibrary),
                        Triple(LibraryTab.FAVORITES, stringResource(R.string.tab_favorites), Icons.Rounded.Favorite)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shortcuts) { (tab, label, icon) ->
                            LiquidGlassCard(
                                shape = RoundedCornerShape(18.dp),
                                onClick = { onNavigateToLibraryTab(tab) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = style.accentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Playlists
                if (playlists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.tab_playlists),
                            actionLabel = stringResource(R.string.home_see_all),
                            onAction = { onNavigateToLibraryTab(LibraryTab.PLAYLISTS) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(playlists.take(6), key = { it.playlist.id }) { pw ->
                                LiquidGlassCard(
                                    shape = RoundedCornerShape(20.dp),
                                    onClick = { onNavigateToPlaylist(pw.playlist.id) },
                                    modifier = Modifier.width(164.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        ArtworkThumbnail(
                                            artworkPath = pw.tracks.firstOrNull()?.artworkPath,
                                            isVideo = false,
                                            contentDescription = pw.playlist.name,
                                            cornerRadius = 14.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = pw.playlist.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(R.string.songs_count, pw.tracks.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recently Played
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(R.string.home_recently_played))
                    }
                    items(recentlyPlayed, key = { "recent_${it.id}" }) { item ->
                        MediaTrackRow(
                            item = item,
                            isCurrent = playbackState.currentMedia?.id == item.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                if (item.isVideo) onOpenVideoPlayer(item)
                                else onPlayTrack(item, audioTracks)
                            },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onPlayNext = { onPlayNext(item) },
                            onAddToQueue = { onAddToQueue(item) },
                            onAddToPlaylist = { trackForPlaylist = item },
                            onSetMood = { trackForMood = item },
                            onShowDetails = { trackForDetails = item },
                            onRemoveFromLibrary = { trackToRemove = item }
                        )
                    }
                }

                // Favorites
                if (favorites.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_favorites),
                            actionLabel = stringResource(R.string.home_see_all),
                            onAction = { onNavigateToLibraryTab(LibraryTab.FAVORITES) }
                        )
                    }
                    items(favorites.take(4), key = { "fav_${it.id}" }) { item ->
                        MediaTrackRow(
                            item = item,
                            isCurrent = playbackState.currentMedia?.id == item.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                if (item.isVideo) onOpenVideoPlayer(item)
                                else onPlayTrack(item, favorites.filter { !it.isVideo })
                            },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onPlayNext = { onPlayNext(item) },
                            onAddToQueue = { onAddToQueue(item) },
                            onAddToPlaylist = { trackForPlaylist = item },
                            onSetMood = { trackForMood = item },
                            onShowDetails = { trackForDetails = item },
                            onRemoveFromLibrary = { trackToRemove = item }
                        )
                    }
                }

                // Recently Added
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_recently_added),
                            actionLabel = stringResource(R.string.home_see_all),
                            onAction = { onNavigateToLibraryTab(LibraryTab.SONGS) }
                        )
                    }
                    items(recentlyAdded, key = { "added_${it.id}" }) { item ->
                        MediaTrackRow(
                            item = item,
                            isCurrent = playbackState.currentMedia?.id == item.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                if (item.isVideo) onOpenVideoPlayer(item)
                                else onPlayTrack(item, audioTracks)
                            },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onPlayNext = { onPlayNext(item) },
                            onAddToQueue = { onAddToQueue(item) },
                            onAddToPlaylist = { trackForPlaylist = item },
                            onSetMood = { trackForMood = item },
                            onShowDetails = { trackForDetails = item },
                            onRemoveFromLibrary = { trackToRemove = item }
                        )
                    }
                }
            }
        }
    }

    trackForPlaylist?.let { target ->
        PlaylistPickerDialog(
            playlists = playlists,
            onSelectPlaylist = { pid -> onAddTrackToPlaylist(pid, target.id) },
            onCreateNewPlaylist = { name -> onCreatePlaylistWithTrack(name, target.id) },
            onDismiss = { trackForPlaylist = null }
        )
    }
    trackForMood?.let { target ->
        MoodPickerDialog(
            currentMoodKey = target.moodKey,
            onSelectMood = { mood -> onSetMood(target, mood) },
            onDismiss = { trackForMood = null }
        )
    }
    trackForDetails?.let { target ->
        MediaDetailsDialog(item = target, onDismiss = { trackForDetails = null })
    }
    trackToRemove?.let { target ->
        ConfirmActionDialog(
            title = stringResource(R.string.remove_from_library),
            message = stringResource(R.string.remove_item_confirm, target.title),
            onConfirm = { onRemoveFromLibrary(target.id) },
            onDismiss = { trackToRemove = null }
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
