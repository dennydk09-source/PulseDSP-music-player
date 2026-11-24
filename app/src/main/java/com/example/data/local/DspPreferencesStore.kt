package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import com.example.data.AudioEffectsConfig
import com.example.data.EqJsonCodec
import com.example.data.LyricsAppearanceConfig
import com.example.data.LyricsTextAlignment
import com.example.data.VisualizerMode
import com.example.data.VisualizerPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dspDataStore by preferencesDataStore(name = "pulsedsp_prefs")

class DspPreferencesStore(private val context: Context) {

    private object Keys {
        val ACTIVE_EFFECTS_JSON = stringPreferencesKey("active_effects_json")
        val VISUALIZER_MODE = stringPreferencesKey("visualizer_mode")
        val VISUALIZER_PALETTE = stringPreferencesKey("visualizer_palette")
        val HAPTIC_BEAT_SYNC_ENABLED = booleanPreferencesKey("haptic_beat_sync_enabled")
        val HAPTIC_INTENSITY = floatPreferencesKey("haptic_intensity")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val LYRICS_FONT_SIZE_SP = floatPreferencesKey("lyrics_font_size_sp")
        val LYRICS_TEXT_ALIGNMENT = stringPreferencesKey("lyrics_text_alignment")
        val MOUNTED_FOLDERS = stringSetPreferencesKey("mounted_folders")
    }

    val activeEffectsFlow: Flow<AudioEffectsConfig> = context.dspDataStore.data.map { prefs ->
        val raw = prefs[Keys.ACTIVE_EFFECTS_JSON]
        if (raw.isNullOrBlank()) {
            AudioEffectsConfig()
        } else {
            EqJsonCodec.fromJsonString(raw).getOrDefault(AudioEffectsConfig())
        }
    }

    val visualizerModeFlow: Flow<VisualizerMode> = context.dspDataStore.data.map { prefs ->
        val name = prefs[Keys.VISUALIZER_MODE]
        VisualizerMode.entries.firstOrNull { it.name == name } ?: VisualizerMode.BARS
    }

    val visualizerPaletteFlow: Flow<VisualizerPalette> = context.dspDataStore.data.map { prefs ->
        val name = prefs[Keys.VISUALIZER_PALETTE]
        VisualizerPalette.entries.firstOrNull { it.name == name } ?: VisualizerPalette.CYBER_NEON
    }

    val hapticBeatSyncEnabledFlow: Flow<Boolean> = context.dspDataStore.data.map { prefs ->
        prefs[Keys.HAPTIC_BEAT_SYNC_ENABLED] ?: false
    }

    val hapticIntensityFlow: Flow<Float> = context.dspDataStore.data.map { prefs ->
        (prefs[Keys.HAPTIC_INTENSITY] ?: 0.7f).coerceIn(0.1f, 1f)
    }

    val shuffleEnabledFlow: Flow<Boolean> = context.dspDataStore.data.map { prefs ->
        prefs[Keys.SHUFFLE_ENABLED] ?: false
    }

    val repeatModeFlow: Flow<Int> = context.dspDataStore.data.map { prefs ->
        when (prefs[Keys.REPEAT_MODE]) {
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    val lyricsAppearanceFlow: Flow<LyricsAppearanceConfig> = context.dspDataStore.data.map { prefs ->
        val sizeSp = (prefs[Keys.LYRICS_FONT_SIZE_SP] ?: 15f).coerceIn(12f, 26f)
        val alignName = prefs[Keys.LYRICS_TEXT_ALIGNMENT]
        val alignment = LyricsTextAlignment.entries.firstOrNull { it.name == alignName }
            ?: LyricsTextAlignment.LEFT
        LyricsAppearanceConfig(fontSizeSp = sizeSp, alignment = alignment)
    }

    val mountedFoldersFlow: Flow<Set<String>> = context.dspDataStore.data.map { prefs ->
        prefs[Keys.MOUNTED_FOLDERS] ?: emptySet()
    }

    suspend fun saveActiveEffects(config: AudioEffectsConfig) {
        val json = EqJsonCodec.toJsonString(config, config.activePresetName)
        context.dspDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_EFFECTS_JSON] = json
        }
    }

    suspend fun saveVisualizerMode(mode: VisualizerMode) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.VISUALIZER_MODE] = mode.name
        }
    }

    suspend fun saveVisualizerPalette(palette: VisualizerPalette) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.VISUALIZER_PALETTE] = palette.name
        }
    }

    suspend fun saveHapticBeatSyncEnabled(enabled: Boolean) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.HAPTIC_BEAT_SYNC_ENABLED] = enabled
        }
    }

    suspend fun saveHapticIntensity(intensity: Float) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.HAPTIC_INTENSITY] = intensity.coerceIn(0.1f, 1f)
        }
    }

    suspend fun saveShuffleEnabled(enabled: Boolean) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun saveRepeatMode(mode: Int) {
        val safeMode = when (mode) {
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        context.dspDataStore.edit { prefs ->
            prefs[Keys.REPEAT_MODE] = safeMode
        }
    }

    suspend fun saveLyricsFontSize(sizeSp: Float) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.LYRICS_FONT_SIZE_SP] = sizeSp.coerceIn(12f, 26f)
        }
    }

    suspend fun saveLyricsAlignment(alignment: LyricsTextAlignment) {
        context.dspDataStore.edit { prefs ->
            prefs[Keys.LYRICS_TEXT_ALIGNMENT] = alignment.name
        }
    }

    suspend fun addMountedFolder(treeUri: String) {
        context.dspDataStore.edit { prefs ->
            val current = prefs[Keys.MOUNTED_FOLDERS] ?: emptySet()
            prefs[Keys.MOUNTED_FOLDERS] = current + treeUri
        }
    }

    suspend fun removeMountedFolder(treeUri: String) {
        context.dspDataStore.edit { prefs ->
            val current = prefs[Keys.MOUNTED_FOLDERS] ?: emptySet()
            prefs[Keys.MOUNTED_FOLDERS] = current - treeUri
        }
    }
}
