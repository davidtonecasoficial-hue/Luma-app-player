package com.example.data.local

import android.net.Uri
import com.example.data.model.HistoryWithMedia
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SyncedFolderEntity
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val dao: MediaDao,
    val extractor: MediaMetadataExtractor,
    val syncEngine: MediaSyncEngine
) {
    val allMedia: Flow<List<MediaItemEntity>> = dao.observeAllMedia()
    val audioTracks: Flow<List<MediaItemEntity>> = dao.observeAudioTracks()
    val videoItems: Flow<List<MediaItemEntity>> = dao.observeVideoItems()
    val favorites: Flow<List<MediaItemEntity>> = dao.observeFavorites()
    val mostPlayed: Flow<List<MediaItemEntity>> = dao.observeMostPlayed()
    val playlists: Flow<List<PlaylistWithTracks>> = dao.observePlaylistsWithTracks()
    val history: Flow<List<HistoryWithMedia>> = dao.observeHistory()
    val syncedFolders: Flow<List<SyncedFolderEntity>> = dao.observeSyncedFolders()

    suspend fun getMediaById(id: Long): MediaItemEntity? = dao.getMediaById(id)

    suspend fun getAllMediaOnce(): List<MediaItemEntity> = dao.getAllMediaOnce()

    suspend fun importFiles(uris: List<Uri>): ImportResult = syncEngine.importFileUris(uris)

    suspend fun linkFolder(treeUri: Uri): SyncResult = syncEngine.linkAndSyncFolder(treeUri)

    suspend fun syncOfflineLibrary(): SyncResult = syncEngine.syncAllOfflineFoldersAndVerify()

    suspend fun removeFolder(folderId: Long) = dao.deleteSyncedFolder(folderId)

    suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean) {
        dao.setFavorite(mediaId, isFavorite)
    }

    suspend fun setMood(mediaId: Long, mood: MoodCategory) {
        dao.setMood(mediaId, mood.key)
    }

    suspend fun setAvailability(mediaId: Long, available: Boolean) {
        dao.setAvailability(mediaId, available)
    }

    suspend fun removeMediaFromLibrary(mediaId: Long) {
        dao.deleteMediaById(mediaId)
    }

    suspend fun recordPlaybackStarted(mediaId: Long, positionMs: Long = 0L) {
        val now = System.currentTimeMillis()
        dao.incrementPlayCount(mediaId, now)
        dao.insertHistory(
            PlaybackHistoryEntity(
                mediaId = mediaId,
                playedAt = now,
                positionReachedMs = positionMs
            )
        )
    }

    suspend fun saveMediaPosition(mediaId: Long, positionMs: Long) {
        dao.updateLastPosition(mediaId, positionMs.coerceAtLeast(0L))
    }

    suspend fun createPlaylist(name: String, initialTrackIds: List<Long> = emptyList()): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val now = System.currentTimeMillis()
        val id = dao.insertPlaylist(PlaylistEntity(name = trimmed, createdAt = now, updatedAt = now))
        initialTrackIds.forEachIndexed { index, trackId ->
            dao.insertPlaylistTrack(
                PlaylistTrackCrossRef(
                    playlistId = id,
                    mediaId = trackId,
                    position = index
                )
            )
        }
        return id
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            dao.renamePlaylist(playlistId, trimmed, System.currentTimeMillis())
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        val nextPos = dao.getMaxPlaylistPosition(playlistId) + 1
        dao.insertPlaylistTrack(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                mediaId = mediaId,
                position = nextPos
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        dao.removeTrackFromPlaylist(playlistId, mediaId)
    }

    suspend fun deleteHistoryEntry(historyId: Long) {
        dao.deleteHistoryEntry(historyId)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun resetPlayCounts() {
        dao.resetAllPlayCounts()
    }
}
