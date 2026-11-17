package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MediaDatabase
import com.example.data.local.MediaMetadataExtractor
import com.example.data.local.MediaSyncEngine
import com.example.data.local.PreferencesRepository
import com.example.data.model.BackupPlaylist
import com.example.data.model.BackupTrackMetadata
import com.example.data.model.MediaItemEntity
import com.example.data.model.MoodCategory
import com.example.data.model.OfflineBackupSnapshot
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.RepeatModeOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LumaLibraryAndSyncTest {

    private lateinit var db: MediaDatabase
    private lateinit var syncEngine: MediaSyncEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        syncEngine = MediaSyncEngine(
            context = context,
            dao = db.mediaDao(),
            extractor = MediaMetadataExtractor(context),
            prefsRepo = PreferencesRepository(context)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `room persistence stores favorites playlists and availability`() = runTest {
        val dao = db.mediaDao()
        val id1 = dao.insertMedia(
            MediaItemEntity(
                uri = "content://local/audio/1",
                displayName = "track1.flac",
                title = "Midnight Glass",
                artist = "Luma Synth",
                album = "Refractions",
                durationMs = 215_000L,
                formatLabel = "FLAC",
                moodKey = MoodCategory.CHILL.key
            )
        )
        val id2 = dao.insertMedia(
            MediaItemEntity(
                uri = "content://local/audio/2",
                displayName = "track2.mp3",
                title = "Ocean Echo",
                artist = "Luma Synth",
                album = "Refractions",
                durationMs = 180_000L,
                formatLabel = "MP3",
                moodKey = MoodCategory.FOCUS.key
            )
        )

        dao.setFavorite(id1, true)
        val favs = dao.observeFavorites().first()
        assertEquals(1, favs.size)
        assertEquals("Midnight Glass", favs.first().title)

        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Night Drive"))
        dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, id1, 0))
        dao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, id2, 1))

        val playlists = dao.observePlaylistsWithTracks().first()
        assertEquals(1, playlists.size)
        assertEquals(2, playlists.first().tracks.size)

        dao.setAvailability(id2, false)
        val unavailable = dao.getMediaById(id2)
        assertFalse(unavailable?.isAvailable ?: true)
    }

    @Test
    fun `smart mix prioritizes matching artist and mood`() {
        val seed = MediaItemEntity(
            id = 1,
            uri = "u1",
            displayName = "a.mp3",
            title = "A",
            artist = "Artist One",
            album = "Al1",
            moodKey = MoodCategory.CHILL.key
        )
        val related = MediaItemEntity(
            id = 2,
            uri = "u2",
            displayName = "b.mp3",
            title = "B",
            artist = "Artist One",
            album = "Al2",
            moodKey = MoodCategory.CHILL.key
        )
        val unrelated = MediaItemEntity(
            id = 3,
            uri = "u3",
            displayName = "c.mp3",
            title = "C",
            artist = "Other",
            album = "Other",
            moodKey = MoodCategory.WORKOUT.key
        )

        val mix = syncEngine.buildSmartMix(listOf(unrelated, related, seed), seed)
        assertEquals(listOf(1L, 2L, 3L), mix.map { it.id })
    }

    @Test
    fun `offline backup snapshot round-trips via kotlinx serialization`() {
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
        val snapshot = OfflineBackupSnapshot(
            exportedAt = 1_700_000_000_000L,
            totalListeningMs = 95_000L,
            tracks = listOf(
                BackupTrackMetadata(
                    uri = "content://local/1",
                    displayName = "song.opus",
                    title = "Song",
                    artist = "Artist",
                    album = "Album",
                    isVideo = false,
                    isFavorite = true,
                    moodKey = MoodCategory.FOCUS.key,
                    playCount = 4,
                    lastPositionMs = 12_000L,
                    lastPlayedAt = 1_700_000_000_000L
                )
            ),
            playlists = listOf(
                BackupPlaylist(name = "Offline Mix", createdAt = 1_700_000_000_000L, trackUris = listOf("content://local/1"))
            )
        )
        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<OfflineBackupSnapshot>(encoded)
        assertEquals(snapshot.tracks.size, decoded.tracks.size)
        assertTrue(decoded.tracks.first().isFavorite)
        assertEquals("Offline Mix", decoded.playlists.first().name)
    }

    @Test
    fun `repeat mode cycles through all three states`() {
        assertEquals(RepeatModeOption.ALL, RepeatModeOption.OFF.next())
        assertEquals(RepeatModeOption.ONE, RepeatModeOption.ALL.next())
        assertEquals(RepeatModeOption.OFF, RepeatModeOption.ONE.next())
    }
}
