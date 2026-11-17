package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentColorOption
import com.example.data.model.AppLanguage
import com.example.data.model.GlassQuality
import com.example.data.model.RepeatModeOption
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_preferences")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val GLASS_QUALITY = stringPreferencesKey("glass_quality")
        val LANGUAGE = stringPreferencesKey("language")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val ALBUM_ART_ROTATION = booleanPreferencesKey("album_art_rotation")
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val DYNAMIC_GRADIENT = booleanPreferencesKey("dynamic_gradient")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SWIPE_GESTURES = booleanPreferencesKey("swipe_gestures")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val LOUDNESS_NORM = booleanPreferencesKey("loudness_norm")
        val RESUME_LAST_SESSION = booleanPreferencesKey("resume_last_session")
        val MINIMALIST_DEFAULT = booleanPreferencesKey("minimalist_default")
        val LAST_MEDIA_ID = longPreferencesKey("last_media_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_INDEX = intPreferencesKey("eq_preset_index")
        val EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        val TOTAL_LISTENING_MS = longPreferencesKey("total_listening_ms")
        val LAST_SYNC_TS = longPreferencesKey("last_sync_ts")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            accentColor = AccentColorOption.fromKey(prefs[Keys.ACCENT_COLOR]),
            glassQuality = GlassQuality.fromKey(prefs[Keys.GLASS_QUALITY]),
            language = AppLanguage.fromTag(prefs[Keys.LANGUAGE]),
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            albumArtRotation = prefs[Keys.ALBUM_ART_ROTATION] ?: true,
            visualizerEnabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            dynamicGradientEnabled = prefs[Keys.DYNAMIC_GRADIENT] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            swipeGesturesEnabled = prefs[Keys.SWIPE_GESTURES] ?: true,
            crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: false,
            crossfadeDurationSec = prefs[Keys.CROSSFADE_DURATION] ?: 3,
            loudnessNormalizationEnabled = prefs[Keys.LOUDNESS_NORM] ?: false,
            resumeLastSession = prefs[Keys.RESUME_LAST_SESSION] ?: true,
            minimalistModeDefault = prefs[Keys.MINIMALIST_DEFAULT] ?: false,
            lastMediaId = prefs[Keys.LAST_MEDIA_ID] ?: -1L,
            lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            lastQueueIds = prefs[Keys.LAST_QUEUE_IDS]
                ?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?: emptyList(),
            repeatMode = RepeatModeOption.fromCode(prefs[Keys.REPEAT_MODE] ?: 0),
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            equalizerEnabled = prefs[Keys.EQ_ENABLED] ?: false,
            equalizerPresetIndex = prefs[Keys.EQ_PRESET_INDEX] ?: 0,
            equalizerBandLevels = prefs[Keys.EQ_BAND_LEVELS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList(),
            totalListeningMs = prefs[Keys.TOTAL_LISTENING_MS] ?: 0L,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TS] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setAccentColor(accent: AccentColorOption) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = accent.key }
    }

    suspend fun setGlassQuality(quality: GlassQuality) {
        context.dataStore.edit { it[Keys.GLASS_QUALITY] = quality.key }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.tag }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setAlbumArtRotation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALBUM_ART_ROTATION] = enabled }
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VISUALIZER_ENABLED] = enabled }
    }

    suspend fun setDynamicGradientEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_GRADIENT] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setSwipeGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_GESTURES] = enabled }
    }

    suspend fun setCrossfade(enabled: Boolean, durationSec: Int? = null) {
        context.dataStore.edit {
            it[Keys.CROSSFADE_ENABLED] = enabled
            if (durationSec != null) {
                it[Keys.CROSSFADE_DURATION] = durationSec.coerceIn(1, 10)
            }
        }
    }

    suspend fun setLoudnessNormalization(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOUDNESS_NORM] = enabled }
    }

    suspend fun setResumeLastSession(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_LAST_SESSION] = enabled }
    }

    suspend fun setMinimalistModeDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MINIMALIST_DEFAULT] = enabled }
    }

    suspend fun savePlaybackSession(
        mediaId: Long,
        positionMs: Long,
        queueIds: List<Long>
    ) {
        context.dataStore.edit {
            it[Keys.LAST_MEDIA_ID] = mediaId
            it[Keys.LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
            it[Keys.LAST_QUEUE_IDS] = queueIds.joinToString(",")
        }
    }

    suspend fun setRepeatMode(mode: RepeatModeOption) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode.code }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setEqualizerConfig(enabled: Boolean, presetIndex: Int, bandLevels: List<Int>) {
        context.dataStore.edit {
            it[Keys.EQ_ENABLED] = enabled
            it[Keys.EQ_PRESET_INDEX] = presetIndex
            it[Keys.EQ_BAND_LEVELS] = bandLevels.joinToString(",")
        }
    }

    suspend fun addListeningTime(deltaMs: Long) {
        if (deltaMs <= 0L) return
        context.dataStore.edit {
            val current = it[Keys.TOTAL_LISTENING_MS] ?: 0L
            it[Keys.TOTAL_LISTENING_MS] = current + deltaMs
        }
    }

    suspend fun resetListeningStats() {
        context.dataStore.edit { it[Keys.TOTAL_LISTENING_MS] = 0L }
    }

    suspend fun setLastSyncTimestamp(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TS] = timestampMs }
    }
}
