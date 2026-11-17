package com.example.config

object AppConfig {
    const val APP_NAME = "Luma Player"
    const val APP_VERSION = "1.0.0"

    val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "aiff", "aif", "alac", "wma", "amr", "3gp"
    )

    val SUPPORTED_VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "mov", "m4v", "3gp", "avi", "ts", "flv"
    )

    val IMPORT_AUDIO_VIDEO_MIME_TYPES = arrayOf(
        "audio/*",
        "video/*",
        "application/ogg",
        "application/x-flac"
    )
}
