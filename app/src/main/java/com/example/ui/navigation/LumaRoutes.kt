package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object SearchRoute

@Serializable
data class LibraryRoute(val initialTab: String = "songs")

@Serializable
object HistoryRoute

@Serializable
object SettingsRoute

@Serializable
object NowPlayingRoute

@Serializable
object VideoPlayerRoute

@Serializable
data class PlaylistDetailRoute(val playlistId: Long)

@Serializable
data class GroupDetailRoute(
    val groupType: String, // "album", "artist", "folder", "mood"
    val groupKey: String
)
