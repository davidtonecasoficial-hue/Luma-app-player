package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.config.AppConfig
import com.example.data.model.BackupPlaylist
import com.example.data.model.BackupTrackMetadata
import com.example.data.model.MediaItemEntity
import com.example.data.model.OfflineBackupSnapshot
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.SyncedFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class ImportResult(
    val addedCount: Int,
    val duplicateCount: Int
)

data class SyncResult(
    val totalItems: Int,
    val newlyAdded: Int,
    val unavailableCount: Int
)

class MediaSyncEngine(
    private val context: Context,
    private val dao: MediaDao,
    private val extractor: MediaMetadataExtractor,
    private val prefsRepo: PreferencesRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun importFileUris(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var added = 0
        var duplicates = 0
        for (uri in uris) {
            takePersistablePermissionQuietly(uri)
            val existing = dao.getMediaByUri(uri.toString())
            if (existing != null) {
                if (!existing.isAvailable) {
                    dao.setAvailability(existing.id, true)
                }
                duplicates++
                continue
            }
            val item = extractor.extractMediaItem(uri)
            if (item != null) {
                dao.insertMedia(item)
                added++
            }
        }
        prefsRepo.setLastSyncTimestamp(System.currentTimeMillis())
        ImportResult(addedCount = added, duplicateCount = duplicates)
    }

    suspend fun linkAndSyncFolder(treeUri: Uri): SyncResult = withContext(Dispatchers.IO) {
        takePersistablePermissionQuietly(treeUri)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        val folderName = rootDoc?.name ?: treeUri.lastPathSegment ?: "Folder"
        var added = 0
        var scannedCount = 0

        if (rootDoc != null && rootDoc.exists() && rootDoc.isDirectory) {
            val mediaFiles = mutableListOf<DocumentFile>()
            collectMediaDocuments(rootDoc, mediaFiles, depth = 0)
            scannedCount = mediaFiles.size
            for (doc in mediaFiles) {
                val uriStr = doc.uri.toString()
                val existing = dao.getMediaByUri(uriStr)
                if (existing != null) {
                    if (!existing.isAvailable) dao.setAvailability(existing.id, true)
                    continue
                }
                val item = extractor.extractMediaItem(
                    uri = doc.uri,
                    folderName = doc.parentFile?.name ?: folderName,
                    folderUri = treeUri.toString()
                )
                if (item != null) {
                    dao.insertMedia(item)
                    added++
                }
            }
        }

        dao.upsertSyncedFolder(
            SyncedFolderEntity(
                folderUri = treeUri.toString(),
                folderName = folderName,
                lastSyncedAt = System.currentTimeMillis(),
                itemCount = scannedCount
            )
        )
        val syncSummary = reconcileAllOfflineMedia()
        SyncResult(
            totalItems = syncSummary.totalItems,
            newlyAdded = added + syncSummary.newlyAdded,
            unavailableCount = syncSummary.unavailableCount
        )
    }

    suspend fun syncAllOfflineFoldersAndVerify(): SyncResult = withContext(Dispatchers.IO) {
        var added = 0
        val folders = dao.getSyncedFoldersOnce()
        for (folder in folders) {
            val treeUri = Uri.parse(folder.folderUri)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null && rootDoc.exists() && rootDoc.isDirectory) {
                val mediaFiles = mutableListOf<DocumentFile>()
                collectMediaDocuments(rootDoc, mediaFiles, depth = 0)
                for (doc in mediaFiles) {
                    val uriStr = doc.uri.toString()
                    val existing = dao.getMediaByUri(uriStr)
                    if (existing == null) {
                        val item = extractor.extractMediaItem(
                            uri = doc.uri,
                            folderName = doc.parentFile?.name ?: folder.folderName,
                            folderUri = folder.folderUri
                        )
                        if (item != null) {
                            dao.insertMedia(item)
                            added++
                        }
                    } else if (!existing.isAvailable) {
                        dao.setAvailability(existing.id, true)
                    }
                }
                dao.upsertSyncedFolder(
                    folder.copy(
                        lastSyncedAt = System.currentTimeMillis(),
                        itemCount = mediaFiles.size
                    )
                )
            }
        }
        val reconciled = reconcileAllOfflineMedia()
        SyncResult(
            totalItems = reconciled.totalItems,
            newlyAdded = added,
            unavailableCount = reconciled.unavailableCount
        )
    }

    private suspend fun reconcileAllOfflineMedia(): SyncResult {
        val all = dao.getAllMediaOnce()
        var unavailable = 0
        for (item in all) {
            val accessible = extractor.isUriAccessible(item.uri)
            if (accessible != item.isAvailable) {
                dao.setAvailability(item.id, accessible)
            }
            if (!accessible) unavailable++
        }
        prefsRepo.setLastSyncTimestamp(System.currentTimeMillis())
        return SyncResult(
            totalItems = all.size,
            newlyAdded = 0,
            unavailableCount = unavailable
        )
    }

    private fun collectMediaDocuments(
        dir: DocumentFile,
        out: MutableList<DocumentFile>,
        depth: Int
    ) {
        if (depth > 4 || out.size >= 1500) return
        val children = dir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                collectMediaDocuments(child, out, depth + 1)
            } else if (child.isFile) {
                val name = child.name.orEmpty()
                val ext = name.substringAfterLast('.', "").lowercase()
                val type = child.type.orEmpty()
                if (type.startsWith("audio/") ||
                    type.startsWith("video/") ||
                    ext in AppConfig.SUPPORTED_AUDIO_EXTENSIONS ||
                    ext in AppConfig.SUPPORTED_VIDEO_EXTENSIONS
                ) {
                    out.add(child)
                }
            }
        }
    }

    private fun takePersistablePermissionQuietly(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Some providers (e.g., Photo Picker) grant temporary or non-persistable URIs
        }
    }

    suspend fun exportOfflineBackup(destUri: Uri, totalListeningMs: Long): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            val allMedia = dao.getAllMediaOnce()
            val playlists = dao.getPlaylistsWithTracksOnce()

            val snapshot = OfflineBackupSnapshot(
                exportedAt = System.currentTimeMillis(),
                totalListeningMs = totalListeningMs,
                tracks = allMedia.map {
                    BackupTrackMetadata(
                        uri = it.uri,
                        displayName = it.displayName,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        isVideo = it.isVideo,
                        isFavorite = it.isFavorite,
                        moodKey = it.moodKey,
                        playCount = it.playCount,
                        lastPositionMs = it.lastPositionMs,
                        lastPlayedAt = it.lastPlayedAt
                    )
                },
                playlists = playlists.map { pw ->
                    BackupPlaylist(
                        name = pw.playlist.name,
                        createdAt = pw.playlist.createdAt,
                        trackUris = pw.tracks.map { it.uri }
                    )
                }
            )
            val payload = json.encodeToString(snapshot)
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            }
            allMedia.size to playlists.size
        }

    suspend fun importOfflineBackup(srcUri: Uri): Int = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(srcUri)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
        } ?: return@withContext 0

        val snapshot = json.decodeFromString<OfflineBackupSnapshot>(raw)
        val allExisting = dao.getAllMediaOnce()
        val byUri = allExisting.associateBy { it.uri }
        val byDisplayName = allExisting.associateBy { it.displayName.lowercase() }

        for (backupTrack in snapshot.tracks) {
            val match = byUri[backupTrack.uri] ?: byDisplayName[backupTrack.displayName.lowercase()]
            if (match != null) {
                dao.updateMedia(
                    match.copy(
                        isFavorite = match.isFavorite || backupTrack.isFavorite,
                        moodKey = if (match.moodKey == "none") backupTrack.moodKey else match.moodKey,
                        playCount = maxOf(match.playCount, backupTrack.playCount),
                        lastPositionMs = maxOf(match.lastPositionMs, backupTrack.lastPositionMs),
                        lastPlayedAt = maxOf(match.lastPlayedAt, backupTrack.lastPlayedAt)
                    )
                )
            }
        }

        var syncedPlaylists = 0
        for (bp in snapshot.playlists) {
            val existingPlaylist = dao.getPlaylistByName(bp.name)
            val playlistId = existingPlaylist?.id ?: dao.insertPlaylist(
                PlaylistEntity(name = bp.name, createdAt = bp.createdAt)
            )
            var pos = dao.getMaxPlaylistPosition(playlistId) + 1
            for (uri in bp.trackUris) {
                val track = dao.getMediaByUri(uri)
                if (track != null) {
                    dao.insertPlaylistTrack(
                        PlaylistTrackCrossRef(
                            playlistId = playlistId,
                            mediaId = track.id,
                            position = pos++
                        )
                    )
                }
            }
            syncedPlaylists++
        }
        prefsRepo.setLastSyncTimestamp(System.currentTimeMillis())
        syncedPlaylists
    }

    fun buildSmartMix(allAudio: List<MediaItemEntity>, seed: MediaItemEntity? = null): List<MediaItemEntity> {
        val available = allAudio.filter { it.isAvailable && !it.isVideo }
        if (available.isEmpty()) return emptyList()
        val target = seed ?: available.maxByOrNull { it.playCount } ?: available.first()
        val scored = available.sortedByDescending { item ->
            var score = 0
            if (item.id == target.id) score += 100
            if (target.artist.isNotBlank() && item.artist.equals(target.artist, ignoreCase = true)) score += 45
            if (target.album.isNotBlank() && item.album.equals(target.album, ignoreCase = true)) score += 30
            if (target.genre.isNotBlank() && item.genre.equals(target.genre, ignoreCase = true)) score += 35
            if (target.moodKey != "none" && item.moodKey == target.moodKey) score += 25
            if (item.isFavorite) score += 15
            score + (item.playCount.coerceAtMost(10))
        }
        return scored.take(25)
    }
}
