package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.HistoryWithMedia
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.SyncedFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun observeAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeAudioTracks(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 ORDER BY dateAdded DESC")
    fun observeVideoItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT 12")
    fun observeMostPlayed(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaOnce(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getMediaByUri(uri: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(item: MediaItemEntity): Long

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("UPDATE media_items SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE media_items SET moodKey = :moodKey WHERE id = :id")
    suspend fun setMood(id: Long, moodKey: String)

    @Query("UPDATE media_items SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailability(id: Long, available: Boolean)

    @Query("UPDATE media_items SET lastPositionMs = :positionMs WHERE id = :id")
    suspend fun updateLastPosition(id: Long, positionMs: Long)

    @Query("UPDATE media_items SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long)

    @Query("UPDATE media_items SET playCount = 0")
    suspend fun resetAllPlayCounts()

    // Playlists
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylistsWithTracks(): Flow<List<PlaylistWithTracks>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    suspend fun getPlaylistsWithTracksOnce(): List<PlaylistWithTracks>

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName, updatedAt = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String, updatedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPlaylistPosition(playlistId: Long): Int

    // History
    @Transaction
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<HistoryWithMedia>>

    @Insert
    suspend fun insertHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE id = :historyId")
    suspend fun deleteHistoryEntry(historyId: Long)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    // Synced Folders
    @Query("SELECT * FROM synced_folders ORDER BY folderName COLLATE NOCASE ASC")
    fun observeSyncedFolders(): Flow<List<SyncedFolderEntity>>

    @Query("SELECT * FROM synced_folders")
    suspend fun getSyncedFoldersOnce(): List<SyncedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncedFolder(folder: SyncedFolderEntity): Long

    @Query("DELETE FROM synced_folders WHERE id = :folderId")
    suspend fun deleteSyncedFolder(folderId: Long)
}
