package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.documentfile.provider.DocumentFile
import com.example.config.AppConfig
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class MediaMetadataExtractor(private val context: Context) {

    private val artworkDir: File by lazy {
        File(context.cacheDir, "artwork").apply { mkdirs() }
    }

    fun isUriAccessible(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun extractMediaItem(
        uri: Uri,
        folderName: String = "",
        folderUri: String = ""
    ): MediaItemEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = ""
        var sizeBytes = 0L

        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: ""
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {
            // Fall back to DocumentFile below
        }

        if (displayName.isBlank()) {
            displayName = DocumentFile.fromSingleUri(context, uri)?.name
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: "Media_${abs(uri.toString().hashCode())}"
        }

        val mimeType = resolver.getType(uri).orEmpty()
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val isVideo = mimeType.startsWith("video/") ||
            (!mimeType.startsWith("audio/") && ext in AppConfig.SUPPORTED_VIDEO_EXTENSIONS)

        val retriever = MediaMetadataRetriever()
        var title = ""
        var artist = ""
        var album = ""
        var genre = ""
        var durationMs = 0L
        var bitrateKbps = 0
        var sampleRateHz = 0
        var width = 0
        var height = 0
        var artworkPath: String? = null
        var hasVideoTrack = isVideo

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().trim()
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty().trim()
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            bitrateKbps = if (bitrateRaw > 0) bitrateRaw / 1000 else 0
            sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val videoFlag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            if (videoFlag == "yes" && (mimeType.startsWith("video/") || ext in AppConfig.SUPPORTED_VIDEO_EXTENSIONS)) {
                hasVideoTrack = true
            }

            if (!hasVideoTrack) {
                val picBytes = retriever.embeddedPicture
                if (picBytes != null && picBytes.isNotEmpty()) {
                    artworkPath = saveArtworkBytes(uri.toString(), picBytes)
                }
            } else {
                val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                if (frame != null) {
                    artworkPath = saveBitmapArtwork(uri.toString(), frame)
                    frame.recycle()
                }
            }
        } catch (_: Exception) {
            // File may still be playable via Media3 even if MediaMetadataRetriever fails on exotic container
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val cleanTitle = title.ifBlank { displayName.substringBeforeLast('.') }
        val cleanArtist = artist.ifBlank { "" }
        val cleanAlbum = album.ifBlank { "" }
        val formatLabel = deriveFormatLabel(ext, mimeType)
        val inferredMood = inferMoodFromMetadata(genre, cleanTitle, cleanAlbum)

        MediaItemEntity(
            uri = uri.toString(),
            displayName = displayName,
            title = cleanTitle,
            artist = cleanArtist,
            album = cleanAlbum,
            genre = genre,
            durationMs = durationMs,
            mimeType = mimeType,
            formatLabel = formatLabel,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            folderName = folderName,
            folderUri = folderUri,
            isVideo = hasVideoTrack,
            moodKey = inferredMood.key,
            isAvailable = true,
            artworkPath = artworkPath
        )
    }

    private fun saveArtworkBytes(key: String, bytes: ByteArray): String? {
        return try {
            val file = File(artworkDir, "art_${abs(key.hashCode())}.jpg")
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val scaled = scaleDownBitmap(decoded, 512)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBitmapArtwork(key: String, bitmap: Bitmap): String? {
        return try {
            val file = File(artworkDir, "vid_${abs(key.hashCode())}.jpg")
            val scaled = scaleDownBitmap(bitmap, 480)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownBitmap(src: Bitmap, maxEdge: Int): Bitmap {
        val maxDim = maxOf(src.width, src.height)
        if (maxDim <= maxEdge) return src
        val ratio = maxEdge.toFloat() / maxDim.toFloat()
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun deriveFormatLabel(ext: String, mimeType: String): String {
        if (ext.isNotBlank()) return ext.uppercase()
        return when {
            mimeType.contains("mpeg") -> "MP3"
            mimeType.contains("mp4") -> "MP4"
            mimeType.contains("aac") -> "AAC"
            mimeType.contains("flac") -> "FLAC"
            mimeType.contains("ogg") -> "OGG"
            mimeType.contains("opus") -> "OPUS"
            mimeType.contains("wav") -> "WAV"
            mimeType.contains("webm") -> "WEBM"
            mimeType.contains("matroska") -> "MKV"
            mimeType.contains("quicktime") -> "MOV"
            else -> mimeType.substringAfter('/').uppercase().ifBlank { "MEDIA" }
        }
    }

    private fun inferMoodFromMetadata(genre: String, title: String, album: String): MoodCategory {
        val text = "$genre $title $album".lowercase()
        return when {
            listOf("chill", "lofi", "lo-fi", "ambient", "acoustic", "jazz", "soul").any { it in text } ->
                MoodCategory.CHILL
            listOf("focus", "study", "classical", "piano", "instrumental", "synth").any { it in text } ->
                MoodCategory.FOCUS
            listOf("workout", "gym", "rock", "edm", "dance", "hip", "trap", "fitness").any { it in text } ->
                MoodCategory.WORKOUT
            listOf("road", "drive", "pop", "indie", "highway", "summer").any { it in text } ->
                MoodCategory.ROAD_TRIP
            else -> MoodCategory.NONE
        }
    }

    suspend fun extractDominantColors(artworkPath: String?): Pair<Color, Color>? = withContext(Dispatchers.IO) {
        if (artworkPath.isNullOrBlank()) return@withContext null
        val file = File(artworkPath)
        if (!file.exists()) return@withContext null
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bitmap = BitmapFactory.decodeFile(artworkPath, opts) ?: return@withContext null
        try {
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0
            val w = bitmap.width
            val h = bitmap.height
            for (y in 0 until h step 2) {
                for (x in 0 until w step 2) {
                    val px = bitmap.getPixel(x, y)
                    rSum += android.graphics.Color.red(px)
                    gSum += android.graphics.Color.green(px)
                    bSum += android.graphics.Color.blue(px)
                    count++
                }
            }
            if (count == 0) return@withContext null
            val avgR = (rSum / count).toInt().coerceIn(20, 225)
            val avgG = (gSum / count).toInt().coerceIn(20, 225)
            val avgB = (bSum / count).toInt().coerceIn(30, 235)
            val primary = Color(avgR, avgG, avgB)
            val secondary = Color(
                (avgR * 0.55f).toInt().coerceIn(10, 180),
                (avgG * 0.55f).toInt().coerceIn(10, 180),
                (avgB * 0.85f).toInt().coerceIn(24, 210)
            )
            primary to secondary
        } finally {
            bitmap.recycle()
        }
    }
}
