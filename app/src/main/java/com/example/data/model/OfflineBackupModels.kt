package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OfflineBackupSnapshot(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val totalListeningMs: Long = 0L,
    val tracks: List<BackupTrackMetadata> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList()
)

@Serializable
data class BackupTrackMetadata(
    val uri: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val isVideo: Boolean,
    val isFavorite: Boolean,
    val moodKey: String,
    val playCount: Int,
    val lastPositionMs: Long,
    val lastPlayedAt: Long
)

@Serializable
data class BackupPlaylist(
    val name: String,
    val createdAt: Long,
    val trackUris: List<String>
)
