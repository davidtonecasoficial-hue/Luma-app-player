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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.PlaylistWithTracks
import com.example.player.PlaybackUiState
import com.example.ui.screens.components.ConfirmActionDialog
import com.example.ui.screens.components.MediaDetailsDialog
import com.example.ui.screens.components.MediaTrackRow
import com.example.ui.screens.components.MoodPickerDialog
import com.example.ui.screens.components.PlaylistPickerDialog
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.viewmodel.SearchFilter

@Composable
fun SearchScreen(
    query: String,
    activeFilter: SearchFilter,
    allMedia: List<MediaItemEntity>,
    playlists: List<PlaylistWithTracks>,
    playbackState: PlaybackUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onOpenVideoPlayer: (MediaItemEntity) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenGroup: (String, String) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistWithTrack: (String, Long) -> Unit,
    onSetMood: (MediaItemEntity, MoodCategory) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit,
    bottomContentPadding: Dp
) {
    val style = LocalLiquidGlassStyle.current
    val trimmed = query.trim()

    val matchingSongs = remember(allMedia, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else allMedia.filter {
            !it.isVideo && (
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.artist.contains(trimmed, ignoreCase = true) ||
                    it.album.contains(trimmed, ignoreCase = true) ||
                    it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.folderName.contains(trimmed, ignoreCase = true)
                )
        }
    }

    val matchingVideos = remember(allMedia, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else allMedia.filter {
            it.isVideo && (
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.folderName.contains(trimmed, ignoreCase = true)
                )
        }
    }

    val matchingAlbums = remember(allMedia, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else allMedia.filter { !it.isVideo && it.album.isNotBlank() }
            .map { it.album }
            .distinct()
            .filter { it.contains(trimmed, ignoreCase = true) }
    }

    val matchingArtists = remember(allMedia, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else allMedia.filter { !it.isVideo && it.artist.isNotBlank() }
            .map { it.artist }
            .distinct()
            .filter { it.contains(trimmed, ignoreCase = true) }
    }

    val matchingPlaylists = remember(playlists, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else playlists.filter { it.playlist.name.contains(trimmed, ignoreCase = true) }
    }

    val showSongs = activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.SONGS
    val showVideos = activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.VIDEOS
    val showAlbums = activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.ALBUMS
    val showArtists = activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.ARTISTS
    val showPlaylists = activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.PLAYLISTS

    val hasResults = (showSongs && matchingSongs.isNotEmpty()) ||
        (showVideos && matchingVideos.isNotEmpty()) ||
        (showAlbums && matchingAlbums.isNotEmpty()) ||
        (showArtists && matchingArtists.isNotEmpty()) ||
        (showPlaylists && matchingPlaylists.isNotEmpty())

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.search_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LiquidGlassCard(shape = RoundedCornerShape(22.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cancel)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )
                }
            }

            item {
                val filterChips = listOf(
                    SearchFilter.ALL to stringResource(R.string.filter_all),
                    SearchFilter.SONGS to stringResource(R.string.tab_songs),
                    SearchFilter.ALBUMS to stringResource(R.string.tab_albums),
                    SearchFilter.ARTISTS to stringResource(R.string.tab_artists),
                    SearchFilter.PLAYLISTS to stringResource(R.string.tab_playlists),
                    SearchFilter.VIDEOS to stringResource(R.string.tab_videos)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterChips) { (filter, label) ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (trimmed.isEmpty()) {
                item {
                    LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = style.accentPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.search_idle_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_idle_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (!hasResults) {
                item {
                    LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.search_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.search_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                if (showSongs && matchingSongs.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.tab_songs)) }
                    items(matchingSongs, key = { "ss_${it.id}" }) { item ->
                        MediaTrackRow(
                            item = item,
                            isCurrent = playbackState.currentMedia?.id == item.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = { onPlayTrack(item, matchingSongs) },
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

                if (showVideos && matchingVideos.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.tab_videos)) }
                    items(matchingVideos, key = { "sv_${it.id}" }) { item ->
                        MediaTrackRow(
                            item = item,
                            isCurrent = playbackState.currentMedia?.id == item.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = { onOpenVideoPlayer(item) },
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

                if (showAlbums && matchingAlbums.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.tab_albums)) }
                    items(matchingAlbums, key = { "sal_$it" }) { album ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onOpenGroup("album", album) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = style.accentPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = album, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }

                if (showArtists && matchingArtists.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.tab_artists)) }
                    items(matchingArtists, key = { "sar_$it" }) { artist ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onOpenGroup("artist", artist) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = style.accentPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = artist, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }

                if (showPlaylists && matchingPlaylists.isNotEmpty()) {
                    item { SectionHeader(title = stringResource(R.string.tab_playlists)) }
                    items(matchingPlaylists, key = { "spl_${it.playlist.id}" }) { pw ->
                        LiquidGlassCard(
                            shape = RoundedCornerShape(18.dp),
                            onClick = { onOpenPlaylist(pw.playlist.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = style.accentPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = pw.playlist.name, style = MaterialTheme.typography.titleSmall)
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
