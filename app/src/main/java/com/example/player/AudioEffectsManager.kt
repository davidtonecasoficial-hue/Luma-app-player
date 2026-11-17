package com.example.player

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqualizerBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int
)

data class EqualizerUiState(
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = false,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val presets: List<String> = emptyList(),
    val selectedPresetIndex: Int = 0,
    val bands: List<EqualizerBandInfo> = emptyList(),
    val loudnessEnabled: Boolean = false
)

object AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    @Synchronized
    fun attachToAudioSession(
        audioSessionId: Int,
        eqEnabled: Boolean,
        presetIndex: Int,
        savedBandLevels: List<Int>,
        loudnessEnabled: Boolean
    ) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null) {
            setEqualizerEnabled(eqEnabled)
            setLoudnessEnabled(loudnessEnabled)
            return
        }

        releaseEffects()
        currentSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val numBands = eq.numberOfBands.toInt().coerceAtLeast(0)
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range.getOrNull(1)?.toInt() ?: 1500

            val presetNames = (0 until eq.numberOfPresets.toInt()).map { idx ->
                eq.getPresetName(idx.toShort())
            }

            if (savedBandLevels.size == numBands) {
                savedBandLevels.forEachIndexed { i, level ->
                    eq.setBandLevel(i.toShort(), level.coerceIn(minMb, maxMb).toShort())
                }
            } else if (presetIndex in presetNames.indices) {
                eq.usePreset(presetIndex.toShort())
            }

            eq.enabled = eqEnabled

            val bands = (0 until numBands).map { i ->
                EqualizerBandInfo(
                    index = i,
                    centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                    levelMb = eq.getBandLevel(i.toShort()).toInt()
                )
            }

            _state.update {
                it.copy(
                    isAvailable = true,
                    isEnabled = eqEnabled,
                    minLevelMb = minMb,
                    maxLevelMb = maxMb,
                    presets = presetNames,
                    selectedPresetIndex = if (savedBandLevels.size == numBands) -1 else presetIndex,
                    bands = bands
                )
            }
        } catch (_: Exception) {
            _state.update { it.copy(isAvailable = false, bands = emptyList(), presets = emptyList()) }
        }

        try {
            val le = LoudnessEnhancer(audioSessionId)
            le.setTargetGain(if (loudnessEnabled) 600 else 0)
            le.enabled = loudnessEnabled
            loudnessEnhancer = le
            _state.update { it.copy(loudnessEnabled = loudnessEnabled) }
        } catch (_: Exception) {
            _state.update { it.copy(loudnessEnabled = false) }
        }
    }

    @Synchronized
    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            _state.update { it.copy(isEnabled = enabled) }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun selectPreset(presetIndex: Int) {
        val eq = equalizer ?: return
        try {
            if (presetIndex in 0 until eq.numberOfPresets.toInt()) {
                eq.usePreset(presetIndex.toShort())
                val bands = (0 until eq.numberOfBands.toInt()).map { i ->
                    EqualizerBandInfo(
                        index = i,
                        centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                        levelMb = eq.getBandLevel(i.toShort()).toInt()
                    )
                }
                _state.update {
                    it.copy(selectedPresetIndex = presetIndex, bands = bands)
                }
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val eq = equalizer ?: return
        try {
            val min = _state.value.minLevelMb
            val max = _state.value.maxLevelMb
            val clamped = levelMb.coerceIn(min, max)
            eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
            val updatedBands = _state.value.bands.map {
                if (it.index == bandIndex) it.copy(levelMb = clamped) else it
            }
            _state.update {
                it.copy(selectedPresetIndex = -1, bands = updatedBands)
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun setLoudnessEnabled(enabled: Boolean) {
        try {
            loudnessEnhancer?.setTargetGain(if (enabled) 600 else 0)
            loudnessEnhancer?.enabled = enabled
            _state.update { it.copy(loudnessEnabled = enabled) }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        loudnessEnhancer = null
        currentSessionId = 0
    }
}
