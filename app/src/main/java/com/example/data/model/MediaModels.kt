package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class MoodCategory(val key: String) {
    NONE("none"),
    CHILL("chill"),
    FOCUS("focus"),
    WORKOUT("workout"),
    ROAD_TRIP("road_trip");

    companion object {
        fun fromKey(key: String?): MoodCategory =
            entries.firstOrNull { it.key == key } ?: NONE
    }
}

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["uri"], unique = true)]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uri: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "",
    val durationMs: Long = 0L,
    val mimeType: String = "",
    val formatLabel: String = "",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val folderName: String = "",
    val folderUri: String = "",
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val moodKey: String = MoodCategory.NONE.key,
    val playCount: Int = 0,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true,
    val embeddedLyrics: String? = null,
    val artworkPath: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mediaId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val mediaId: Long,
    val position: Int
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "mediaId"
        )
    )
    val tracks: List<MediaItemEntity>
)

@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mediaId"), Index("playedAt")]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val positionReachedMs: Long = 0L
)

data class HistoryWithMedia(
    @Embedded val history: PlaybackHistoryEntity,
    @Relation(
        parentColumn = "mediaId",
        entityColumn = "id"
    )
    val media: MediaItemEntity
)

@Entity(
    tableName = "synced_folders",
    indices = [Index(value = ["folderUri"], unique = true)]
)
data class SyncedFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val folderUri: String,
    val folderName: String,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0
)

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class AccentColorOption(val key: String) {
    LIQUID_BLUE("blue"),
    ELECTRIC_VIOLET("violet"),
    CYAN_MIST("cyan"),
    CORAL_SUNSET("coral"),
    EMERALD("emerald");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: LIQUID_BLUE
    }
}

enum class GlassQuality(val key: String) {
    HIGH("high"),
    BALANCED("balanced"),
    REDUCED("reduced");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: HIGH
    }
}

enum class AppLanguage(val tag: String) {
    PORTUGUESE("pt"),
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr"),
    SYSTEM("system");

    companion object {
        fun fromTag(tag: String?) = entries.firstOrNull { it.tag == tag } ?: PORTUGUESE
    }
}

enum class LibrarySortField {
    NAME, ARTIST, ALBUM, DATE_ADDED, DURATION
}

enum class RepeatModeOption(val code: Int) {
    OFF(0), ALL(1), ONE(2);

    fun next(): RepeatModeOption = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: OFF
    }
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColorOption = AccentColorOption.LIQUID_BLUE,
    val glassQuality: GlassQuality = GlassQuality.HIGH,
    val language: AppLanguage = AppLanguage.PORTUGUESE,
    val reduceMotion: Boolean = false,
    val albumArtRotation: Boolean = true,
    val visualizerEnabled: Boolean = true,
    val dynamicGradientEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val swipeGesturesEnabled: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSec: Int = 3,
    val loudnessNormalizationEnabled: Boolean = false,
    val resumeLastSession: Boolean = true,
    val minimalistModeDefault: Boolean = false,
    val lastMediaId: Long = -1L,
    val lastPositionMs: Long = 0L,
    val lastQueueIds: List<Long> = emptyList(),
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val equalizerEnabled: Boolean = false,
    val equalizerPresetIndex: Int = 0,
    val equalizerBandLevels: List<Int> = emptyList(),
    val totalListeningMs: Long = 0L,
    val lastSyncTimestamp: Long = 0L
)

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val message: String = "") : SyncState
    data class Synced(
        val timestampMs: Long,
        val totalItems: Int,
        val newlyAdded: Int,
        val unavailableCount: Int
    ) : SyncState
    data class Error(val message: String) : SyncState
}
