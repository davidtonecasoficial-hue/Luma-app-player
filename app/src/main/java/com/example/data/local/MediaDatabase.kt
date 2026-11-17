package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.MediaItemEntity
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.SyncedFolderEntity

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        SyncedFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getInstance(context: Context): MediaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediaDatabase::class.java,
                    "luma_player.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
