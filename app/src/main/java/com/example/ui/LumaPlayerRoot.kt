package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.config.AppConfig
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.ui.i18n.ProvideAppLocale
import com.example.ui.i18n.findActivity
import com.example.ui.navigation.GroupDetailRoute
import com.example.ui.navigation.HistoryRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.NowPlayingRoute
import com.example.ui.navigation.PlaylistDetailRoute
import com.example.ui.navigation.SearchRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.VideoPlayerRoute
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.components.LiquidGlassMiniPlayer
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LiquidGlassFloatingNavBar
import com.example.ui.theme.LiquidGlassNavRail
import com.example.ui.theme.LumaPlayerTheme
import com.example.ui.theme.NavTabItem
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiNotice

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LumaPlayerRoot(viewModel: MainViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    ProvideAppLocale(language = prefs.language) {
        LumaPlayerTheme(
            themeMode = prefs.themeMode,
            accentColor = prefs.accentColor,
            glassQuality = prefs.glassQuality,
            reduceMotion = prefs.reduceMotion,
            hapticsEnabled = prefs.hapticsEnabled
        ) {
            val context = LocalContext.current
            val activity = remember(context) { context.findActivity() }
            val widthSizeClass = if (activity != null) {
                calculateWindowSizeClass(activity).widthSizeClass
            } else {
                WindowWidthSizeClass.Compact
            }
            val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact

            val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
            val audioTracks by viewModel.sortedAudioTracks.collectAsStateWithLifecycle()
            val videoItems by viewModel.sortedVideoItems.collectAsStateWithLifecycle()
            val favorites by viewModel.favorites.collectAsStateWithLifecycle()
            val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
            val history by viewModel.history.collectAsStateWithLifecycle()
            val syncedFolders by viewModel.syncedFolders.collectAsStateWithLifecycle()
            val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
            val eqState by viewModel.equalizerState.collectAsStateWithLifecycle()
            val syncState by viewModel.syncState.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
            val sortField by viewModel.sortField.collectAsStateWithLifecycle()
            val sortAscending by viewModel.sortAscending.collectAsStateWithLifecycle()
            val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDest = backStackEntry?.destination
            val snackbarHostState = remember { SnackbarHostState() }

            var askedNotificationPermission by remember { mutableStateOf(false) }
            var showNotificationRationale by remember { mutableStateOf(false) }

            val notifLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            fun maybeRequestNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotificationPermission) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        askedNotificationPermission = true
                        showNotificationRationale = true
                    }
                }
            }

            // SAF & Photo Picker Launchers (zero broad storage permissions)
            val importFilesLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (uris.isNotEmpty()) viewModel.importFiles(uris)
            }

            val importFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) viewModel.linkAndSyncFolder(uri)
            }

            val pickVideoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) viewModel.importFiles(listOf(uri))
            }

            val exportBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) viewModel.exportOfflineBackup(uri)
            }

            val importBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) viewModel.importOfflineBackup(uri)
            }

            LaunchedEffect(Unit) {
                viewModel.notices.collect { notice ->
                    val msg = when (notice) {
                        is UiNotice.ImportDone -> context.getString(
                            R.string.import_result_message,
                            notice.added,
                            notice.duplicates
                        )
                        is UiNotice.SyncDone -> context.getString(
                            R.string.sync_result_message,
                            notice.added,
                            notice.unavailable
                        )
                        is UiNotice.BackupExported -> context.getString(
                            R.string.backup_exported,
                            notice.tracks,
                            notice.playlists
                        )
                        is UiNotice.BackupImported -> context.getString(
                            R.string.backup_imported,
                            notice.playlists
                        )
                        is UiNotice.GenericError -> notice.message
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }

            val isImmersivePlayer = currentDest?.hasRoute<NowPlayingRoute>() == true ||
                currentDest?.hasRoute<VideoPlayerRoute>() == true

            val activeTabKey = when {
                currentDest?.hasRoute<SearchRoute>() == true -> "search"
                currentDest?.hasRoute<LibraryRoute>() == true ||
                    currentDest?.hasRoute<PlaylistDetailRoute>() == true ||
                    currentDest?.hasRoute<GroupDetailRoute>() == true -> "library"
                currentDest?.hasRoute<HistoryRoute>() == true -> "history"
                currentDest?.hasRoute<SettingsRoute>() == true -> "settings"
                else -> "home"
            }

            val tabs = listOf(
                NavTabItem(
                    key = "home",
                    label = stringResource(R.string.nav_home),
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    testTag = "nav_home"
                ),
                NavTabItem(
                    key = "search",
                    label = stringResource(R.string.nav_search),
                    selectedIcon = Icons.Rounded.Search,
                    unselectedIcon = Icons.Outlined.Search,
                    testTag = "nav_search"
                ),
                NavTabItem(
                    key = "library",
                    label = stringResource(R.string.nav_library),
                    selectedIcon = Icons.Rounded.LibraryMusic,
                    unselectedIcon = Icons.Outlined.LibraryMusic,
                    testTag = "nav_library"
                ),
                NavTabItem(
                    key = "history",
                    label = stringResource(R.string.nav_history),
                    selectedIcon = Icons.Rounded.History,
                    unselectedIcon = Icons.Outlined.History,
                    testTag = "nav_history"
                ),
                NavTabItem(
                    key = "settings",
                    label = stringResource(R.string.nav_settings),
                    selectedIcon = Icons.Rounded.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    testTag = "nav_settings"
                )
            )

            fun navigateToTab(tab: NavTabItem) {
                val route: Any = when (tab.key) {
                    "search" -> SearchRoute
                    "library" -> LibraryRoute()
                    "history" -> HistoryRoute
                    "settings" -> SettingsRoute
                    else -> HomeRoute
                }
                navController.navigate(route) {
                    popUpTo(HomeRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            fun playAudioAndNotify(item: MediaItemEntity, queue: List<MediaItemEntity>) {
                maybeRequestNotificationPermission()
                viewModel.playItem(item, queue)
            }

            fun openVideo(item: MediaItemEntity) {
                viewModel.playItem(item, listOf(item))
                navController.navigate(VideoPlayerRoute)
            }

            val miniVisible = playbackState.isMiniPlayerVisible &&
                playbackState.currentMedia != null &&
                !isImmersivePlayer

            val bottomContentPadding = when {
                useNavRail && miniVisible -> 110.dp
                useNavRail -> 28.dp
                miniVisible -> 196.dp
                else -> 110.dp
            }

            LiquidGlassBackground(
                dynamicPrimary = if (prefs.dynamicGradientEnabled) playbackState.dynamicPrimaryColor else null,
                dynamicSecondary = if (prefs.dynamicGradientEnabled) playbackState.dynamicSecondaryColor else null
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail && !isImmersivePlayer) {
                        LiquidGlassNavRail(
                            tabs = tabs,
                            selectedKey = activeTabKey,
                            onSelect = ::navigateToTab,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute,
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() }
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    allMedia = allMedia,
                                    audioTracks = audioTracks,
                                    favorites = favorites,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    syncState = syncState,
                                    lastSyncTimestamp = prefs.lastSyncTimestamp,
                                    onNavigateToSearch = {
                                        navController.navigate(SearchRoute) {
                                            popUpTo(HomeRoute) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToLibraryTab = { tab ->
                                        navController.navigate(LibraryRoute(tab.key)) {
                                            popUpTo(HomeRoute) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToPlaylist = { pid ->
                                        navController.navigate(PlaylistDetailRoute(pid))
                                    },
                                    onNavigateToMood = { mood ->
                                        navController.navigate(GroupDetailRoute("mood", mood.key))
                                    },
                                    onOpenNowPlaying = { navController.navigate(NowPlayingRoute) },
                                    onOpenVideoPlayer = ::openVideo,
                                    onPlayTrack = ::playAudioAndNotify,
                                    onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                                    onPlaySmartMix = {
                                        maybeRequestNotificationPermission()
                                        viewModel.playSmartMix()
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackController::playNext,
                                    onAddToQueue = viewModel.playbackController::addToQueue,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onCreatePlaylistWithTrack = { name, id ->
                                        viewModel.createPlaylist(name, listOf(id))
                                    },
                                    onSetMood = viewModel::setMood,
                                    onRemoveFromLibrary = viewModel::removeMediaFromLibrary,
                                    onImportFiles = { importFilesLauncher.launch(AppConfig.IMPORT_AUDIO_VIDEO_MIME_TYPES) },
                                    onImportFolder = { importFolderLauncher.launch(null) },
                                    onPickVideo = {
                                        pickVideoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    onSyncNow = { viewModel.syncOfflineLibrary(silent = false) },
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<SearchRoute> {
                                SearchScreen(
                                    query = searchQuery,
                                    activeFilter = searchFilter,
                                    allMedia = allMedia,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onQueryChange = viewModel::setSearchQuery,
                                    onFilterChange = viewModel::setSearchFilter,
                                    onPlayTrack = ::playAudioAndNotify,
                                    onOpenVideoPlayer = ::openVideo,
                                    onOpenPlaylist = { pid -> navController.navigate(PlaylistDetailRoute(pid)) },
                                    onOpenGroup = { type, key -> navController.navigate(GroupDetailRoute(type, key)) },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackController::playNext,
                                    onAddToQueue = viewModel.playbackController::addToQueue,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onCreatePlaylistWithTrack = { name, id ->
                                        viewModel.createPlaylist(name, listOf(id))
                                    },
                                    onSetMood = viewModel::setMood,
                                    onRemoveFromLibrary = viewModel::removeMediaFromLibrary,
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<LibraryRoute> { entry ->
                                val route = entry.toRoute<LibraryRoute>()
                                LibraryScreen(
                                    initialTab = LibraryTab.fromKey(route.initialTab),
                                    audioTracks = audioTracks,
                                    videoItems = videoItems,
                                    favorites = favorites,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    sortField = sortField,
                                    sortAscending = sortAscending,
                                    isGridView = isGridView,
                                    onSelectSortField = viewModel::setSortField,
                                    onToggleGridView = viewModel::toggleGridView,
                                    onPlayTrack = ::playAudioAndNotify,
                                    onOpenVideoPlayer = ::openVideo,
                                    onOpenPlaylist = { pid -> navController.navigate(PlaylistDetailRoute(pid)) },
                                    onOpenGroup = { type, key -> navController.navigate(GroupDetailRoute(type, key)) },
                                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                    onRenamePlaylist = viewModel::renamePlaylist,
                                    onDeletePlaylist = viewModel::deletePlaylist,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackController::playNext,
                                    onAddToQueue = viewModel.playbackController::addToQueue,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onCreatePlaylistWithTrack = { name, id ->
                                        viewModel.createPlaylist(name, listOf(id))
                                    },
                                    onSetMood = viewModel::setMood,
                                    onRemoveFromLibrary = viewModel::removeMediaFromLibrary,
                                    onImportFiles = { importFilesLauncher.launch(AppConfig.IMPORT_AUDIO_VIDEO_MIME_TYPES) },
                                    onImportFolder = { importFolderLauncher.launch(null) },
                                    onPickVideo = {
                                        pickVideoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<PlaylistDetailRoute> { entry ->
                                val route = entry.toRoute<PlaylistDetailRoute>()
                                val pw = playlists.firstOrNull { it.playlist.id == route.playlistId }
                                val tracks = pw?.tracks ?: emptyList()
                                CollectionDetailScreen(
                                    title = pw?.playlist?.name ?: stringResource(R.string.tab_playlists),
                                    subtitle = stringResource(R.string.songs_count, tracks.size),
                                    items = tracks,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onPlayTrack = ::playAudioAndNotify,
                                    onOpenVideoPlayer = ::openVideo,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackController::playNext,
                                    onAddToQueue = viewModel.playbackController::addToQueue,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onCreatePlaylistWithTrack = { name, id ->
                                        viewModel.createPlaylist(name, listOf(id))
                                    },
                                    onSetMood = viewModel::setMood,
                                    onRemoveFromLibrary = viewModel::removeMediaFromLibrary,
                                    extraRemoveLabel = stringResource(R.string.remove_from_playlist),
                                    onExtraRemove = { item ->
                                        viewModel.removeTrackFromPlaylist(route.playlistId, item.id)
                                    },
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<GroupDetailRoute> { entry ->
                                val route = entry.toRoute<GroupDetailRoute>()
                                val groupItems = remember(allMedia, route) {
                                    when (route.groupType) {
                                        "album" -> allMedia.filter { !it.isVideo && it.album == route.groupKey }
                                        "artist" -> allMedia.filter { !it.isVideo && it.artist == route.groupKey }
                                        "folder" -> allMedia.filter { (it.folderName.ifBlank { "Root" }) == route.groupKey }
                                        "mood" -> allMedia.filter { !it.isVideo && it.moodKey == route.groupKey }
                                        else -> emptyList()
                                    }
                                }
                                val displayTitle = if (route.groupType == "mood") {
                                    when (MoodCategory.fromKey(route.groupKey)) {
                                        MoodCategory.CHILL -> stringResource(R.string.mood_chill)
                                        MoodCategory.FOCUS -> stringResource(R.string.mood_focus)
                                        MoodCategory.WORKOUT -> stringResource(R.string.mood_workout)
                                        MoodCategory.ROAD_TRIP -> stringResource(R.string.mood_road_trip)
                                        MoodCategory.NONE -> route.groupKey
                                    }
                                } else {
                                    route.groupKey
                                }
                                CollectionDetailScreen(
                                    title = displayTitle,
                                    subtitle = stringResource(R.string.items_count, groupItems.size),
                                    items = groupItems,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onPlayTrack = ::playAudioAndNotify,
                                    onOpenVideoPlayer = ::openVideo,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackController::playNext,
                                    onAddToQueue = viewModel.playbackController::addToQueue,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onCreatePlaylistWithTrack = { name, id ->
                                        viewModel.createPlaylist(name, listOf(id))
                                    },
                                    onSetMood = viewModel::setMood,
                                    onRemoveFromLibrary = viewModel::removeMediaFromLibrary,
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<HistoryRoute> {
                                HistoryScreen(
                                    history = history,
                                    mostPlayed = mostPlayed,
                                    totalListeningMs = prefs.totalListeningMs,
                                    onPlayMedia = ::playAudioAndNotify,
                                    onOpenVideoPlayer = ::openVideo,
                                    onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                                    onClearAllHistory = viewModel::clearHistory,
                                    onResetStats = viewModel::resetListeningStats,
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    preferences = prefs,
                                    syncedFolders = syncedFolders,
                                    syncState = syncState,
                                    totalMediaCount = allMedia.size,
                                    deviceCodecs = viewModel.deviceCodecs,
                                    onSetThemeMode = viewModel::setThemeMode,
                                    onSetAccentColor = viewModel::setAccentColor,
                                    onSetGlassQuality = viewModel::setGlassQuality,
                                    onSetLanguage = viewModel::setLanguage,
                                    onSetReduceMotion = viewModel::setReduceMotion,
                                    onSetAlbumRotation = viewModel::setAlbumArtRotation,
                                    onSetVisualizer = viewModel::setVisualizerEnabled,
                                    onSetDynamicGradient = viewModel::setDynamicGradientEnabled,
                                    onSetHaptics = viewModel::setHapticsEnabled,
                                    onSetSwipeGestures = viewModel::setSwipeGesturesEnabled,
                                    onSetCrossfade = viewModel::setCrossfade,
                                    onSetResumeSession = viewModel::setResumeLastSession,
                                    onSetMinimalistDefault = viewModel::setMinimalistModeDefault,
                                    onLinkFolder = { importFolderLauncher.launch(null) },
                                    onRemoveFolder = viewModel::removeSyncedFolder,
                                    onSyncNow = { viewModel.syncOfflineLibrary(silent = false) },
                                    onExportBackup = {
                                        exportBackupLauncher.launch("luma_player_offline_sync.json")
                                    },
                                    onImportBackup = {
                                        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                    },
                                    bottomContentPadding = bottomContentPadding
                                )
                            }

                            composable<NowPlayingRoute>(
                                enterTransition = { slideInVertically { it / 2 } + fadeIn() },
                                exitTransition = { slideOutVertically { it / 2 } + fadeOut() }
                            ) {
                                NowPlayingScreen(
                                    playbackState = playbackState,
                                    eqState = eqState,
                                    preferences = prefs,
                                    playlists = playlists,
                                    onBack = { navController.popBackStack() },
                                    onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                                    onSkipPrevious = viewModel.playbackController::skipPrevious,
                                    onSkipNext = viewModel.playbackController::skipNext,
                                    onSeekTo = viewModel.playbackController::seekTo,
                                    onToggleShuffle = viewModel.playbackController::toggleShuffle,
                                    onCycleRepeat = viewModel.playbackController::cycleRepeatMode,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayFromQueue = { item ->
                                        viewModel.playbackController.playMedia(item, playbackState.queue)
                                    },
                                    onMoveQueueItem = viewModel.playbackController::moveQueueItem,
                                    onRemoveQueueItem = viewModel.playbackController::removeQueueItem,
                                    onClearUpcomingQueue = viewModel.playbackController::clearUpcomingQueue,
                                    onSaveQueueAsPlaylist = { name ->
                                        viewModel.createPlaylist(name, playbackState.queue.map { it.id })
                                    },
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onCreatePlaylistWithTrack = { name, id ->
                                        viewModel.createPlaylist(name, listOf(id))
                                    },
                                    onToggleEq = viewModel::setEqualizerEnabled,
                                    onSelectEqPreset = viewModel::selectEqualizerPreset,
                                    onEqBandChange = viewModel::setEqualizerBand,
                                    onToggleLoudness = viewModel::setLoudnessNormalization,
                                    onSetSleepTimerMinutes = viewModel.playbackController::setSleepTimerMinutes,
                                    onSetSleepTimerEndOfTrack = viewModel.playbackController::setSleepTimerEndOfTrack,
                                    onCancelSleepTimer = viewModel.playbackController::cancelSleepTimer,
                                    onSelectSpeed = viewModel.playbackController::setPlaybackSpeed
                                )
                            }

                            composable<VideoPlayerRoute> {
                                VideoPlayerScreen(
                                    player = viewModel.playbackController.playerOrNull,
                                    playbackState = playbackState,
                                    onBack = {
                                        viewModel.playbackController.playerOrNull?.pause()
                                        navController.popBackStack()
                                    },
                                    onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                                    onSeekTo = viewModel.playbackController::seekTo,
                                    onSeekBy = viewModel.playbackController::seekBy,
                                    onSelectSpeed = viewModel.playbackController::setPlaybackSpeed,
                                    onSelectAudioTrack = viewModel::selectAudioTrack,
                                    onSelectSubtitleTrack = viewModel::selectSubtitleTrack
                                )
                            }
                        }

                        // Snackbar host at top safe area so it never collides with bottom nav/mini player
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        )

                        // Bottom Overlay: Mini-Player + Floating Liquid Glass Nav Bar
                        if (!isImmersivePlayer) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                AnimatedVisibility(
                                    visible = miniVisible,
                                    enter = slideInVertically { it } + fadeIn(),
                                    exit = slideOutVertically { it } + fadeOut()
                                ) {
                                    LiquidGlassMiniPlayer(
                                        playbackState = playbackState,
                                        onExpand = { navController.navigate(NowPlayingRoute) },
                                        onTogglePlayPause = viewModel.playbackController::togglePlayPause,
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onDismiss = viewModel.playbackController::dismissMiniPlayer
                                    )
                                }

                                if (miniVisible) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                if (!useNavRail) {
                                    LiquidGlassFloatingNavBar(
                                        tabs = tabs,
                                        selectedKey = activeTabKey,
                                        onSelect = ::navigateToTab
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Recoverable Playback Error Dialog
            playbackState.errorEvent?.let { err ->
                val message = when {
                    err.isUnavailable -> stringResource(R.string.error_file_unavailable)
                    err.isUnsupportedOrDamaged -> stringResource(R.string.error_unsupported_format, err.detail)
                    else -> stringResource(R.string.error_playback_generic, err.title, err.detail)
                }
                AlertDialog(
                    onDismissRequest = viewModel.playbackController::clearError,
                    title = { Text(err.title) },
                    text = { Text(message) },
                    confirmButton = {
                        Button(onClick = viewModel.playbackController::clearError) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }

            // Polite Notification Permission Rationale (Android 13+)
            if (showNotificationRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                AlertDialog(
                    onDismissRequest = { showNotificationRationale = false },
                    title = { Text(AppConfig.APP_NAME) },
                    text = { Text(stringResource(R.string.notif_permission_rationale)) },
                    confirmButton = {
                        Button(onClick = {
                            showNotificationRationale = false
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text(stringResource(R.string.allow))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationRationale = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}
