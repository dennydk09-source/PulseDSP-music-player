package com.example.data

import org.json.JSONArray
import org.json.JSONObject

val EQ_BAND_FREQUENCIES_HZ = intArrayOf(
    31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000
)

val EQ_BAND_LABELS = listOf(
    "31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"
)

enum class ListeningMode(
    val displayName: String,
    val subtitle: String,
    val tag: String
) {
    PARTY("Party", "Sub-bass punch & wide 7.1 club energy", "PARTY"),
    AMBIENT("Ambient", "Holographic virtualizer & airy stage", "AMBIENT"),
    TRIP("Trip", "Road-noise clarity & tight vocal focus", "TRIP"),
    SLEEP("Sleep", "Warm rolled-off highs & gentle fade timer", "SLEEP"),
    CINEMA("Cinema", "Deep LFE rumble & 7.1 surround field", "CINEMA"),
    STUDIO_FLAT("Studio", "Reference flat uncolored signal path", "STUDIO"),
    AI_ADAPTIVE("AI Mode", "Online Gemini acoustic engineering", "AI"),
    CUSTOM("Custom", "User or .JSON tuned DSP curve", "CUSTOM")
}

enum class SurroundMode(val label: String, val channelsLabel: String) {
    OFF("Off", "2.0 Stereo"),
    SURROUND_5_1("5.1 Virtual", "5.1 Spatial"),
    SURROUND_7_1("7.1 Virtual", "7.1 Spatial")
}

enum class VisualizerMode(val label: String) {
    BARS("Bars"),
    RADIAL("Radial"),
    WAVEFORM("Wave")
}

enum class VisualizerPalette(val label: String, val subtitle: String) {
    CYBER_NEON("Cyber Neon", "Electric Cyan, Violet & Laser Mint"),
    SUNSET_GLOW("Sunset Glow", "Solar Gold, Neon Coral & Magenta"),
    EMERALD_MATRIX("Emerald Matrix", "Laser Mint, Teal & Studio Green"),
    MONOCHROMATIC("Monochromatic", "Pure Ice White & Precision Silver")
}

enum class ListeningGear(val label: String) {
    IEMS("In-Ear Monitors"),
    STUDIO_HEADPHONES("Studio Over-Ear"),
    BLUETOOTH_HEADPHONES("Wireless ANC"),
    CAR_AUDIO("Car Audio"),
    HOME_THEATER("Home Theater / AVR"),
    PHONE_SPEAKERS("Phone Speakers")
}

enum class GenreEqPreset(
    val displayName: String,
    val tagline: String,
    val bandGainsDb: List<Float>,
    val preampDb: Float
) {
    FLAT(
        displayName = "Flat (Reference)",
        tagline = "Neutral uncolored studio curve",
        bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        preampDb = 0f
    ),
    ROCK(
        displayName = "Rock",
        tagline = "Punchy guitars, tight drums & crisp snare",
        bandGainsDb = listOf(4.5f, 3.5f, 1.5f, -1.0f, -1.5f, 0.5f, 2.5f, 4.0f, 4.5f, 3.5f),
        preampDb = -1.0f
    ),
    JAZZ(
        displayName = "Jazz",
        tagline = "Warm upright bass & silky brass shimmer",
        bandGainsDb = listOf(3.0f, 2.5f, 1.5f, 2.0f, -1.0f, -1.0f, 0.5f, 2.0f, 3.5f, 4.0f),
        preampDb = -0.5f
    ),
    CLASSICAL(
        displayName = "Classical",
        tagline = "Lush hall depth & airy orchestral strings",
        bandGainsDb = listOf(3.5f, 2.5f, 1.0f, 0.5f, -0.5f, -0.5f, 0.5f, 2.5f, 3.5f, 4.5f),
        preampDb = -0.5f
    ),
    ELECTRONIC(
        displayName = "Electronic",
        tagline = "Deep sub-bass drop & sparkling synth highs",
        bandGainsDb = listOf(6.0f, 5.0f, 2.5f, 0.0f, -1.5f, 1.0f, 2.5f, 4.0f, 5.0f, 5.5f),
        preampDb = -1.5f
    ),
    POP(
        displayName = "Pop",
        tagline = "Forward vocals & energetic polished beat",
        bandGainsDb = listOf(-1.0f, 1.5f, 3.5f, 4.0f, 2.5f, 0.0f, -1.0f, 1.5f, 2.5f, 3.0f),
        preampDb = -1.0f
    ),
    HIP_HOP(
        displayName = "Hip-Hop",
        tagline = "Heavy 808 sub extension & crisp hi-hats",
        bandGainsDb = listOf(6.5f, 5.5f, 3.0f, 1.0f, -1.0f, -0.5f, 1.5f, 2.0f, 3.0f, 3.5f),
        preampDb = -1.5f
    ),
    ACOUSTIC(
        displayName = "Acoustic",
        tagline = "Natural wood warmth & intimate string detail",
        bandGainsDb = listOf(2.5f, 2.0f, 1.5f, 0.5f, 1.5f, 2.0f, 2.5f, 3.0f, 2.5f, 1.5f),
        preampDb = -0.5f
    ),
    VOCAL(
        displayName = "Vocal Boost",
        tagline = "Focused midrange presence & clarity",
        bandGainsDb = listOf(-2.0f, -1.5f, 0.0f, 2.0f, 4.0f, 4.5f, 3.5f, 2.0f, 0.5f, -0.5f),
        preampDb = -1.0f
    ),
    METAL(
        displayName = "Metal",
        tagline = "Scooped mids, heavy chug & razor edge",
        bandGainsDb = listOf(5.0f, 4.0f, 1.0f, -2.5f, -3.0f, 0.0f, 3.0f, 4.5f, 4.0f, 3.0f),
        preampDb = -1.5f
    )
}

data class AudioEffectsConfig(
    val enabled: Boolean = true,
    val activeMode: ListeningMode = ListeningMode.STUDIO_FLAT,
    val activePresetName: String = "Studio Flat",
    val bandGainsDb: List<Float> = List(10) { 0f },
    val preampDb: Float = 0f,
    val bassBoost: Float = 0f,          // 0f..1f (maps up to +10 dB low shelf)
    val trebleBoost: Float = 0f,        // 0f..1f (maps up to +9 dB high shelf)
    val virtualizerLevel: Float = 0f,   // 0f..1f (spatial headstage & room depth)
    val surroundMode: SurroundMode = SurroundMode.OFF,
    val stereoWidth: Float = 1.0f,      // 0f (Mono) .. 1.0f (Natural) .. 2.0f (Ultra-Wide)
    val compressorEnabled: Boolean = false,
    val compressorThresholdDb: Float = -18f, // -40f..0f
    val compressorRatio: Float = 3.5f,       // 1f..12f
    val compressorMakeupDb: Float = 2f,      // 0f..12f
    val digitalPassthroughEnabled: Boolean = false,
    val aiRationale: String = ""
) {
    fun clamped(): AudioEffectsConfig = copy(
        bandGainsDb = (0 until 10).map { i ->
            bandGainsDb.getOrElse(i) { 0f }.coerceIn(-12f, 12f)
        },
        preampDb = preampDb.coerceIn(-12f, 12f),
        bassBoost = bassBoost.coerceIn(0f, 1f),
        trebleBoost = trebleBoost.coerceIn(0f, 1f),
        virtualizerLevel = virtualizerLevel.coerceIn(0f, 1f),
        stereoWidth = stereoWidth.coerceIn(0f, 2f),
        compressorThresholdDb = compressorThresholdDb.coerceIn(-40f, 0f),
        compressorRatio = compressorRatio.coerceIn(1f, 12f),
        compressorMakeupDb = compressorMakeupDb.coerceIn(0f, 12f)
    )
}

object BuiltInModeProfiles {
    fun forMode(mode: ListeningMode): AudioEffectsConfig = when (mode) {
        ListeningMode.PARTY -> AudioEffectsConfig(
            enabled = true,
            activeMode = ListeningMode.PARTY,
            activePresetName = "Party Mode",
            bandGainsDb = listOf(6.5f, 5.5f, 3.5f, 0.5f, -1.0f, 0.5f, 2.5f, 4.0f, 5.0f, 4.5f),
            preampDb = -1.5f,
            bassBoost = 0.65f,
            trebleBoost = 0.45f,
            virtualizerLevel = 0.55f,
            surroundMode = SurroundMode.SURROUND_7_1,
            stereoWidth = 1.45f,
            compressorEnabled = true,
            compressorThresholdDb = -16f,
            compressorRatio = 4.5f,
            compressorMakeupDb = 3.0f
        )

        ListeningMode.AMBIENT -> AudioEffectsConfig(
            enabled = true,
            activeMode = ListeningMode.AMBIENT,
            activePresetName = "Ambient Mode",
            bandGainsDb = listOf(1.5f, 2.0f, 1.0f, -0.5f, 0.0f, 1.0f, 2.5f, 4.0f, 5.5f, 5.0f),
            preampDb = 0f,
            bassBoost = 0.2f,
            trebleBoost = 0.5f,
            virtualizerLevel = 0.85f,
            surroundMode = SurroundMode.SURROUND_5_1,
            stereoWidth = 1.75f,
            compressorEnabled = false,
            compressorThresholdDb = -22f,
            compressorRatio = 2.0f,
            compressorMakeupDb = 1.0f
        )

        ListeningMode.TRIP -> AudioEffectsConfig(
            enabled = true,
            activeMode = ListeningMode.TRIP,
            activePresetName = "Trip Mode",
            bandGainsDb = listOf(3.0f, 4.0f, 2.5f, 1.0f, 2.0f, 3.5f, 3.0f, 2.5f, 2.0f, 1.5f),
            preampDb = -0.5f,
            bassBoost = 0.35f,
            trebleBoost = 0.3f,
            virtualizerLevel = 0.35f,
            surroundMode = SurroundMode.SURROUND_5_1,
            stereoWidth = 1.25f,
            compressorEnabled = true,
            compressorThresholdDb = -20f,
            compressorRatio = 3.5f,
            compressorMakeupDb = 3.5f
        )

        ListeningMode.SLEEP -> AudioEffectsConfig(
            enabled = true,
            activeMode = ListeningMode.SLEEP,
            activePresetName = "Sleep Mode",
            bandGainsDb = listOf(2.0f, 2.5f, 1.5f, 0.5f, -0.5f, -1.5f, -3.0f, -5.0f, -6.5f, -7.5f),
            preampDb = -2.0f,
            bassBoost = 0.15f,
            trebleBoost = 0.0f,
            virtualizerLevel = 0.4f,
            surroundMode = SurroundMode.OFF,
            stereoWidth = 1.15f,
            compressorEnabled = true,
            compressorThresholdDb = -26f,
            compressorRatio = 4.0f,
            compressorMakeupDb = 1.5f
        )

        ListeningMode.CINEMA -> AudioEffectsConfig(
            enabled = true,
            activeMode = ListeningMode.CINEMA,
            activePresetName = "Cinema 7.1",
            bandGainsDb = listOf(7.0f, 5.0f, 2.0f, -1.0f, 1.5f, 3.5f, 3.0f, 2.0f, 3.5f, 4.0f),
            preampDb = -1.5f,
            bassBoost = 0.6f,
            trebleBoost = 0.35f,
            virtualizerLevel = 0.8f,
            surroundMode = SurroundMode.SURROUND_7_1,
            stereoWidth = 1.6f,
            compressorEnabled = true,
            compressorThresholdDb = -18f,
            compressorRatio = 3.0f,
            compressorMakeupDb = 2.5f
        )

        ListeningMode.STUDIO_FLAT,
        ListeningMode.AI_ADAPTIVE,
        ListeningMode.CUSTOM -> AudioEffectsConfig(
            enabled = true,
            activeMode = ListeningMode.STUDIO_FLAT,
            activePresetName = "Studio Flat"
        )
    }

    val curatedModes: List<ListeningMode> = listOf(
        ListeningMode.PARTY,
        ListeningMode.AMBIENT,
        ListeningMode.TRIP,
        ListeningMode.SLEEP,
        ListeningMode.CINEMA,
        ListeningMode.STUDIO_FLAT
    )
}

data class VisualizerFrame(
    val spectrumBands: FloatArray = FloatArray(32),
    val peakHoldBands: FloatArray = FloatArray(32),
    val waveform: FloatArray = FloatArray(64),
    val bassEnergy: Float = 0f,
    val gainReductionDb: Float = 0f,
    val isActive: Boolean = false
)

data class SignalTelemetry(
    val codecLabel: String = "PCM",
    val mimeType: String = "audio/wav",
    val sampleRateHz: Int = 48000,
    val bitDepth: Int = 16,
    val channelCount: Int = 2,
    val bitrateKbps: Int = 1536,
    val passthroughActive: Boolean = false,
    val passthroughStatusNote: String = "32-bit Float DSP Active"
) {
    val formattedBadge: String
        get() {
            val srKhz = if (sampleRateHz % 1000 == 0) {
                "${sampleRateHz / 1000} kHz"
            } else {
                String.format("%.1f kHz", sampleRateHz / 1000f)
            }
            val ch = when (channelCount) {
                1 -> "1.0"
                2 -> "2.0"
                6 -> "5.1"
                8 -> "7.1"
                else -> "${channelCount}ch"
            }
            return "$codecLabel • ${bitDepth}-BIT • $srKhz • $ch"
        }
}

data class HardwareAudioDiagnostics(
    val supportsEac3Passthrough: Boolean = false,
    val supportsEac3JocPassthrough: Boolean = false,
    val supportsAc4Passthrough: Boolean = false,
    val supportsPcmFloat: Boolean = true,
    val maxChannelCount: Int = 2,
    val outputDeviceSummary: String = "Standard Stereo Output"
)

/**
 * Human-readable .json EQ & Audio Effects file codec so users can export, share, edit,
 * and import EQ profiles directly via Android's File Manager.
 */
object EqJsonCodec {
    fun toJsonString(config: AudioEffectsConfig, presetName: String = config.activePresetName): String {
        val root = JSONObject()
        root.put("format", "PulseDSP-AudioEffects-v1")
        root.put("name", presetName.ifBlank { "Custom EQ" })
        root.put("mode", config.activeMode.name)
        root.put("enabled", config.enabled)
        root.put("preampDb", config.preampDb.toDouble())

        val bandsArray = JSONArray()
        val namedBands = JSONObject()
        EQ_BAND_FREQUENCIES_HZ.forEachIndexed { idx, hz ->
            val gain = config.bandGainsDb.getOrElse(idx) { 0f }
            val rounded = Math.round(gain * 10.0) / 10.0
            bandsArray.put(rounded)
            namedBands.put("${hz}Hz", rounded)
        }
        root.put("bandsDb", bandsArray)
        root.put("bandsByFrequency", namedBands)

        val fx = JSONObject().apply {
            put("bassBoost", (Math.round(config.bassBoost * 100.0) / 100.0))
            put("trebleBoost", (Math.round(config.trebleBoost * 100.0) / 100.0))
            put("virtualizerLevel", (Math.round(config.virtualizerLevel * 100.0) / 100.0))
            put("surroundMode", config.surroundMode.name)
            put("stereoWidth", (Math.round(config.stereoWidth * 100.0) / 100.0))
            put("compressorEnabled", config.compressorEnabled)
            put("compressorThresholdDb", config.compressorThresholdDb.toDouble())
            put("compressorRatio", config.compressorRatio.toDouble())
            put("compressorMakeupDb", config.compressorMakeupDb.toDouble())
            put("digitalPassthroughEnabled", config.digitalPassthroughEnabled)
        }
        root.put("fx", fx)
        if (config.aiRationale.isNotBlank()) {
            root.put("notes", config.aiRationale)
        }
        return root.toString(2)
    }

    fun fromJsonString(rawJson: String, fallbackName: String = "Imported JSON EQ"): Result<AudioEffectsConfig> {
        return runCatching {
            val trimmed = rawJson.trim()
            val root = JSONObject(trimmed)
            val name = root.optString("name", "").ifBlank {
                root.optString("presetName", fallbackName)
            }

            // Support bandsDb array, bands array, gains array, or bandsByFrequency object
            val gains = MutableList(10) { 0f }
            val array = root.optJSONArray("bandsDb")
                ?: root.optJSONArray("bands")
                ?: root.optJSONArray("gains")
                ?: root.optJSONArray("eq")

            if (array != null && array.length() > 0) {
                for (i in 0 until 10) {
                    val idx = if (i < array.length()) i else array.length() - 1
                    gains[i] = array.optDouble(idx, 0.0).toFloat()
                }
            } else {
                val freqObj = root.optJSONObject("bandsByFrequency") ?: root
                EQ_BAND_FREQUENCIES_HZ.forEachIndexed { i, hz ->
                    val label = EQ_BAND_LABELS[i]
                    val value = when {
                        freqObj.has("${hz}Hz") -> freqObj.optDouble("${hz}Hz", 0.0)
                        freqObj.has("$hz") -> freqObj.optDouble("$hz", 0.0)
                        freqObj.has(label) -> freqObj.optDouble(label, 0.0)
                        else -> 0.0
                    }
                    gains[i] = value.toFloat()
                }
            }

            val fx = root.optJSONObject("fx") ?: root
            val surroundStr = fx.optString("surroundMode", SurroundMode.OFF.name).uppercase()
            val surround = when {
                surroundStr.contains("7") -> SurroundMode.SURROUND_7_1
                surroundStr.contains("5") -> SurroundMode.SURROUND_5_1
                else -> SurroundMode.OFF
            }
            val modeStr = root.optString("mode", ListeningMode.CUSTOM.name).uppercase()
            val mode = ListeningMode.entries.firstOrNull { it.name == modeStr } ?: ListeningMode.CUSTOM

            AudioEffectsConfig(
                enabled = root.optBoolean("enabled", true),
                activeMode = mode,
                activePresetName = name,
                bandGainsDb = gains,
                preampDb = root.optDouble("preampDb", fx.optDouble("preamp", 0.0)).toFloat(),
                bassBoost = fx.optDouble("bassBoost", 0.0).toFloat(),
                trebleBoost = fx.optDouble("trebleBoost", 0.0).toFloat(),
                virtualizerLevel = fx.optDouble("virtualizerLevel", fx.optDouble("virtualizer", 0.0)).toFloat(),
                surroundMode = surround,
                stereoWidth = fx.optDouble("stereoWidth", fx.optDouble("stereoizer", 1.0)).toFloat(),
                compressorEnabled = fx.optBoolean("compressorEnabled", fx.optBoolean("compressor", false)),
                compressorThresholdDb = fx.optDouble("compressorThresholdDb", -18.0).toFloat(),
                compressorRatio = fx.optDouble("compressorRatio", 3.5).toFloat(),
                compressorMakeupDb = fx.optDouble("compressorMakeupDb", 2.0).toFloat(),
                digitalPassthroughEnabled = fx.optBoolean("digitalPassthroughEnabled", false),
                aiRationale = root.optString("notes", root.optString("rationale", "Imported from JSON file"))
            ).clamped()
        }
    }
}
