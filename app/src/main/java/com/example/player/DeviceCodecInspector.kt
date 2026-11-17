package com.example.player

import android.media.MediaCodecList

data class DeviceCodecGroup(
    val mimeType: String,
    val friendlyName: String,
    val isVideo: Boolean,
    val codecNames: List<String>
)

object DeviceCodecInspector {
    fun getSupportedDecoders(): List<DeviceCodecGroup> {
        return try {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val grouped = linkedMapOf<String, MutableList<String>>()
            for (info in list.codecInfos) {
                if (info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.startsWith("audio/") || type.startsWith("video/")) {
                        grouped.getOrPut(type) { mutableListOf() }.add(info.name)
                    }
                }
            }
            grouped.entries
                .map { (mime, names) ->
                    DeviceCodecGroup(
                        mimeType = mime,
                        friendlyName = friendlyLabelForMime(mime),
                        isVideo = mime.startsWith("video/"),
                        codecNames = names.distinct().take(3)
                    )
                }
                .sortedWith(compareBy<DeviceCodecGroup> { it.isVideo }.thenBy { it.friendlyName })
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun friendlyLabelForMime(mime: String): String = when (mime.lowercase()) {
        "audio/mpeg" -> "MP3 (MPEG-1/2 Audio Layer III)"
        "audio/mp4a-latm" -> "AAC / M4A"
        "audio/flac" -> "FLAC (Free Lossless Audio)"
        "audio/vorbis" -> "OGG / Vorbis"
        "audio/opus" -> "Opus"
        "audio/raw" -> "WAV / PCM Raw"
        "audio/alac" -> "ALAC (Apple Lossless)"
        "audio/3gpp", "audio/amr-wb" -> "AMR Audio"
        "audio/ac3", "audio/eac3" -> "Dolby AC-3 / E-AC-3"
        "video/avc" -> "H.264 / AVC (MP4, MKV, MOV)"
        "video/hevc" -> "H.265 / HEVC"
        "video/x-vnd.on2.vp8" -> "VP8 (WebM)"
        "video/x-vnd.on2.vp9" -> "VP9 (WebM, MKV)"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4 Visual"
        "video/3gpp" -> "H.263 / 3GP"
        else -> mime
    }
}
