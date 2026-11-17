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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LibrarySortField
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.PlaylistWithTracks
import com.example.player.PlaybackUiState
import com.example.ui.screens.components.ArtworkThumbnail
import com.example.ui.screens.components.ConfirmActionDialog
import com.example.ui.screens.components.CreatePlaylistDialog
import com.example.ui.screens.components.EmptyLibraryHero
import com.example.ui.screens.components.MediaDetailsDialog
import com.example.ui.screens.components.MediaGridCard
import com.example.ui.screens.components.MediaTrackRow
import com.example.ui.screens.components.MoodPickerDialog
import com.example.ui.screens.components.PlaylistPickerDialog
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.viewmodel.LibraryTab

@Composable
fun LibraryScreen(
    initialTab: LibraryTab,
    audioTracks: List<MediaItemEntity>,
    videoItems: List<MediaItemEntity>,
    favorites: List<MediaItemEntity>,
    playlists: List<PlaylistWithTracks>,
    playbackState: PlaybackUiState,
    sortField: LibrarySortField,
    sortAscending: Boolean,
    isGridView: Boolean,
    onSelectSortField: (LibrarySortField) -> Unit,
    onToggleGridView: () -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onOpenVideoPlayer: (MediaItemEntity) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenGroup: (String, String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
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
    bottomContentPadding: Dp
) {
    val style = LocalLiquidGlassStyle.current
    var activeTabKey by rememberSaveable(initialTab) { mutableStateOf(initialTab.key) }
    val activeTab = LibraryTab.fromKey(activeTabKey)

    var showSortMenu by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PlaylistWithTracks?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistWithTracks?>(null) }

    var trackForPlaylist by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForMood by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForDetails by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackToRemove by remember { mutableStateOf<MediaItemEntity?>(null) }

    val albums = remember(audioTracks) {
        audioTracks.groupBy { it.album.ifBlank { "" } }
            .filterKeys { it.isNotBlank() }
            .entries.sortedBy { it.key.lowercase() }
    }
    val artists = remember(audioTracks) {
        audioTracks.groupBy { it.artist.ifBlank { "" } }
            .filterKeys { it.isNotBlank() }
            .entries.sortedBy { it.key.lowercase() }
    }
    val folders = remember(audioTracks, videoItems) {
        (audioTracks + videoItems).groupBy { it.folderName.ifBlank { "Root" } }
            .entries.sortedBy { it.key.lowercase() }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box {
                        LiquidGlassCard(shape = CircleShape, onClick = { showSortMenu = true }) {
                            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = stringResource(R.string.sort_by)
                                )
                            }
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            val sortOptions = listOf(
                                LibrarySortField.NAME to stringResource(R.string.sort_name),
                                LibrarySortField.ARTIST to stringResource(R.string.sort_artist),
                                LibrarySortField.ALBUM to stringResource(R.string.sort_album),
                                LibrarySortField.DATE_ADDED to stringResource(R.string.sort_date_added),
                                LibrarySortField.DURATION to stringResource(R.string.sort_duration)
                            )
                            sortOptions.forEach { (field, label) ->
                                val marker = if (sortField == field) (if (sortAscending) " ↑" else " ↓") else ""
                                DropdownMenuItem(
                                    text = { Text("$label$marker") },
                                    onClick = {
                                        onSelectSortField(field)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    LiquidGlassCard(shape = CircleShape, onClick = onToggleGridView) {
                        Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = stringResource(if (isGridView) R.string.view_list else R.string.view_grid)
                            )
                        }
                    }

                    Box {
                        LiquidGlassCard(
                            shape = CircleShape,
                            onClick = { showImportMenu = true },
                            modifier = Modifier.testTag("library_add_btn")
                        ) {
                            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_import_files))
                            }
                        }
                        DropdownMenu(expanded = showImportMenu, onDismissRequest = { showImportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_import_files)) },
                                leadingIcon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    onImportFiles()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_import_folder)) },
                                leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    onImportFolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_pick_video)) },
                                leadingIcon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    onPickVideo()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val tabs = listOf(
                LibraryTab.PLAYLISTS to stringResource(R.string.tab_playlists),
                LibraryTab.SONGS to stringResource(R.string.tab_songs),
                LibraryTab.ALBUMS to stringResource(R.string.tab_albums),
                LibraryTab.ARTISTS to stringResource(R.string.tab_artists),
                LibraryTab.FOLDERS to stringResource(R.string.tab_folders),
                LibraryTab.FAVORITES to stringResource(R.string.tab_favorites),
                LibraryTab.VIDEOS to stringResource(R.string.tab_videos)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs) { (tab, label) ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTabKey = tab.key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("library_tab_${tab.key}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (activeTab) {
                LibraryTab.SONGS -> {
                    MediaCollectionBody(
                        items = audioTracks,
                        queue = audioTracks,
                        isGridView = isGridView,
                        playbackState = playbackState,
                        bottomContentPadding = bottomContentPadding,
                        onItemClick = { onPlayTrack(it, audioTracks) },
                        onToggleFavorite = onToggleFavorite,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = { trackForPlaylist = it },
                        onSetMood = { trackForMood = it },
                        onShowDetails = { trackForDetails = it },
                        onRemoveFromLibrary = { trackToRemove = it },
                        onImportFiles = onImportFiles,
                        onImportFolder = onImportFolder,
                        onPickVideo = onPickVideo
                    )
                }

                LibraryTab.VIDEOS -> {
                    MediaCollectionBody(
                        items = videoItems,
                        queue = videoItems,
                        isGridView = isGridView,
                        playbackState = playbackState,
                        bottomContentPadding = bottomContentPadding,
                        onItemClick = onOpenVideoPlayer,
                        onToggleFavorite = onToggleFavorite,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = { trackForPlaylist = it },
                        onSetMood = { trackForMood = it },
                        onShowDetails = { trackForDetails = it },
                        onRemoveFromLibrary = { trackToRemove = it },
                        onImportFiles = onImportFiles,
                        onImportFolder = onImportFolder,
                        onPickVideo = onPickVideo
                    )
                }

                LibraryTab.FAVORITES -> {
                    MediaCollectionBody(
                        items = favorites,
                        queue = favorites.filter { !it.isVideo },
                        isGridView = isGridView,
                        playbackState = playbackState,
                        bottomContentPadding = bottomContentPadding,
                        onItemClick = { item ->
                            if (item.isVideo) onOpenVideoPlayer(item)
                            else onPlayTrack(item, favorites.filter { !it.isVideo })
                        },
                        onToggleFavorite = onToggleFavorite,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = { trackForPlaylist = it },
                        onSetMood = { trackForMood = it },
                        onShowDetails = { trackForDetails = it },
                        onRemoveFromLibrary = { trackToRemove = it },
                        onImportFiles = onImportFiles,
                        onImportFolder = onImportFolder,
                        onPickVideo = onPickVideo
                    )
                }

                LibraryTab.PLAYLISTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = bottomContentPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            LiquidGlassCard(
                                shape = RoundedCornerShape(22.dp),
                                onClick = { showCreatePlaylistDialog = true },
                                tintColor = style.accentPrimary.copy(alpha = 0.22f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_playlist_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = style.accentPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, tint = style.accentPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.create_playlist),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.create_playlist_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (playlists.isEmpty()) {
                            item {
                                LiquidGlassCard(shape = RoundedCornerShape(22.dp)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(stringResource(R.string.no_playlists_title), style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            stringResource(R.string.no_playlists_body),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(playlists, key = { it.playlist.id }) { pw ->
                                var menuOpen by remember { mutableStateOf(false) }
                                LiquidGlassCard(
                                    shape = RoundedCornerShape(20.dp),
                                    onClick = { onOpenPlaylist(pw.playlist.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ArtworkThumbnail(
                                            artworkPath = pw.tracks.firstOrNull()?.artworkPath,
                                            isVideo = false,
                                            contentDescription = pw.playlist.name,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pw.playlist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.songs_count, pw.tracks.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (pw.tracks.isNotEmpty()) {
                                            IconButton(onClick = { onPlayTrack(pw.tracks.first(), pw.tracks) }) {
                                                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play))
                                            }
                                        }
                                        Box {
                                            IconButton(onClick = { menuOpen = true }) {
                                                Icon(Icons.Rounded.MoreVert, contentDescription = null)
                                            }
                                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.rename_playlist)) },
                                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                                    onClick = {
                                                        menuOpen = false
                                                        playlistToRename = pw
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete_playlist), color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                    },
                                                    onClick = {
                                                        menuOpen = false
                                                        playlistToDelete = pw
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    GroupListBody(
                        entries = albums,
                        icon = Icons.Rounded.Album,
                        bottomContentPadding = bottomContentPadding,
                        onOpenGroup = { key -> onOpenGroup("album", key) }
                    )
                }

                LibraryTab.ARTISTS -> {
                    GroupListBody(
                        entries = artists,
                        icon = Icons.Rounded.Person,
                        bottomContentPadding = bottomContentPadding,
                        onOpenGroup = { key -> onOpenGroup("artist", key) }
                    )
                }

                LibraryTab.FOLDERS -> {
                    GroupListBody(
                        entries = folders,
                        icon = Icons.Rounded.Folder,
                        bottomContentPadding = bottomContentPadding,
                        onOpenGroup = { key -> onOpenGroup("folder", key) }
                    )
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    playlistToRename?.let { pw ->
        CreatePlaylistDialog(
            initialName = pw.playlist.name,
            titleText = stringResource(R.string.rename_playlist),
            confirmText = stringResource(R.string.save),
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                onRenamePlaylist(pw.playlist.id, newName)
                playlistToRename = null
            }
        )
    }

    playlistToDelete?.let { pw ->
        ConfirmActionDialog(
            title = stringResource(R.string.delete_playlist),
            message = stringResource(R.string.delete_playlist_confirm, pw.playlist.name),
            onConfirm = { onDeletePlaylist(pw.playlist.id) },
            onDismiss = { playlistToDelete = null }
        )
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
private fun MediaCollectionBody(
    items: List<MediaItemEntity>,
    queue: List<MediaItemEntity>,
    isGridView: Boolean,
    playbackState: PlaybackUiState,
    bottomContentPadding: Dp,
    onItemClick: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onSetMood: (MediaItemEntity) -> Unit,
    onShowDetails: (MediaItemEntity) -> Unit,
    onRemoveFromLibrary: (MediaItemEntity) -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    onPickVideo: () -> Unit
) {
    if (items.isEmpty()) {
        LazyColumn(contentPadding = PaddingValues(bottom = bottomContentPadding)) {
            item {
                EmptyLibraryHero(
                    onImportFiles = onImportFiles,
                    onImportFolder = onImportFolder,
                    onPickVideo = onPickVideo
                )
            }
        }
        return
    }

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.id }) { item ->
                MediaGridCard(
                    item = item,
                    isCurrent = playbackState.currentMedia?.id == item.id,
                    onClick = { onItemClick(item) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.id }) { item ->
                MediaTrackRow(
                    item = item,
                    isCurrent = playbackState.currentMedia?.id == item.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onItemClick(item) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onPlayNext = { onPlayNext(item) },
                    onAddToQueue = { onAddToQueue(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) },
                    onSetMood = { onSetMood(item) },
                    onShowDetails = { onShowDetails(item) },
                    onRemoveFromLibrary = { onRemoveFromLibrary(item) }
                )
            }
        }
    }
}

@Composable
private fun GroupListBody(
    entries: List<Map.Entry<String, List<MediaItemEntity>>>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bottomContentPadding: Dp,
    onOpenGroup: (String) -> Unit
) {
    val style = LocalLiquidGlassStyle.current
    if (entries.isEmpty()) {
        LiquidGlassCard(shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.empty_category_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.empty_category_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(entries, key = { it.key }) { (name, groupItems) ->
            LiquidGlassCard(
                shape = RoundedCornerShape(20.dp),
                onClick = { onOpenGroup(name) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firstArt = groupItems.firstOrNull { !it.artworkPath.isNullOrBlank() }?.artworkPath
                    if (firstArt != null) {
                        ArtworkThumbnail(
                            artworkPath = firstArt,
                            isVideo = false,
                            contentDescription = name,
                            modifier = Modifier.size(50.dp)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = style.accentPrimary.copy(alpha = 0.16f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = style.accentPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.items_count, groupItems.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    items: List<MediaItemEntity>,
    playlists: List<PlaylistWithTracks>,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onPlayTrack: (MediaItemEntity, List<MediaItemEntity>) -> Unit,
    onOpenVideoPlayer: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onPlayNext: (MediaItemEntity) -> Unit,
    onAddToQueue: (MediaItemEntity) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistWithTrack: (String, Long) -> Unit,
    onSetMood: (MediaItemEntity, MoodCategory) -> Unit,
    onRemoveFromLibrary: (Long) -> Unit,
    extraRemoveLabel: String? = null,
    onExtraRemove: ((MediaItemEntity) -> Unit)? = null,
    bottomContentPadding: Dp
) {
    var trackForPlaylist by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForMood by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackForDetails by remember { mutableStateOf<MediaItemEntity?>(null) }
    var trackToRemove by remember { mutableStateOf<MediaItemEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (items.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val first = items.first()
                                if (first.isVideo) onOpenVideoPlayer(first) else onPlayTrack(first, items)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.play))
                        }
                        val audioOnly = items.filter { !it.isVideo }
                        if (audioOnly.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val shuffled = audioOnly.shuffled()
                                    onPlayTrack(shuffled.first(), shuffled)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.shuffle))
                            }
                        }
                    }
                }

                items(items, key = { "detail_${it.id}" }) { item ->
                    MediaTrackRow(
                        item = item,
                        isCurrent = playbackState.currentMedia?.id == item.id,
                        isPlaying = playbackState.isPlaying,
                        onClick = {
                            if (item.isVideo) onOpenVideoPlayer(item) else onPlayTrack(item, items)
                        },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onPlayNext = { onPlayNext(item) },
                        onAddToQueue = { onAddToQueue(item) },
                        onAddToPlaylist = { trackForPlaylist = item },
                        onSetMood = { trackForMood = item },
                        onShowDetails = { trackForDetails = item },
                        onRemoveFromLibrary = { trackToRemove = item },
                        extraMenuActionLabel = extraRemoveLabel,
                        onExtraMenuAction = if (onExtraRemove != null) ({ onExtraRemove(item) }) else null
                    )
                }
            } else {
                item {
                    LiquidGlassCard(shape = RoundedCornerShape(22.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.empty_category_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.empty_category_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
