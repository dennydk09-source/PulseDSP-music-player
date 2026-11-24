package com.example.ai

import android.util.Log
import com.example.data.AudioEffectsConfig
import com.example.data.ListeningGear
import com.example.data.ListeningMode
import com.example.data.SurroundMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiEqualizerService"

class AiEqualizerService {

    private val aiEqSchema = Schema.obj(
        properties = mapOf(
            "presetName" to Schema.string(description = "Short expressive name for the acoustic profile (2-4 words)"),
            "bandGainsDb" to Schema.array(
                items = Schema.double(description = "Gain in dB between -12.0 and +12.0"),
                description = "Exactly 10 gain values in dB for 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz"
            ),
            "preampDb" to Schema.double(description = "Master preamp compensation in dB between -6.0 and +3.0"),
            "bassBoost" to Schema.double(description = "Low-shelf bass booster intensity from 0.0 to 1.0"),
            "trebleBoost" to Schema.double(description = "High-shelf treble booster intensity from 0.0 to 1.0"),
            "virtualizerLevel" to Schema.double(description = "3D spatial virtualizer intensity from 0.0 to 1.0"),
            "surroundMode" to Schema.enumeration(
                values = listOf("OFF", "SURROUND_5_1", "SURROUND_7_1"),
                description = "Virtual surround stage mode"
            ),
            "stereoWidth" to Schema.double(description = "Mid/Side stereoizer width from 0.5 to 1.9"),
            "compressorEnabled" to Schema.boolean(description = "Whether dynamics compressor should be engaged"),
            "compressorThresholdDb" to Schema.double(description = "Compressor threshold in dB from -36.0 to -6.0"),
            "compressorRatio" to Schema.double(description = "Compressor ratio from 1.5 to 8.0"),
            "compressorMakeupDb" to Schema.double(description = "Compressor makeup gain in dB from 0.0 to 6.0"),
            "rationale" to Schema.string(description = "Concise 1-2 sentence acoustic engineering explanation of the tuning")
        )
    )

    private val model by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = aiEqSchema
            temperature = 0.6f
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun generateForTrack(
        trackTitle: String,
        artist: String,
        codecLabel: String,
        sampleRateHz: Int,
        gear: ListeningGear,
        customPrompt: String?
    ): Result<AudioEffectsConfig> {
        val userInstruction = buildString {
            appendLine("You are a master audio mastering engineer configuring a 10-band graphic EQ and studio DSP FX rack.")
            appendLine("Track: \"$trackTitle\" by $artist")
            appendLine("Audio Signal: $codecLabel at ${sampleRateHz}Hz")
            appendLine("Listening Hardware: ${gear.label}")
            if (!customPrompt.isNullOrBlank()) {
                appendLine("Listener's Sound Goal: $customPrompt")
            } else {
                appendLine("Listener's Sound Goal: Optimize frequency balance, soundstage, dynamics, and musical clarity for this track and hardware.")
            }
            appendLine("Provide exactly 10 bandGainsDb values (31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz) within [-12.0, 12.0], sensible negative preampDb headroom if boosting bass heavily, and realistic FX values.")
        }

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(userInstruction)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI Equalizer returned an empty response."))
            parseStructuredResponse(rawJson)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Equalizer request timed out", e)
            Result.failure(
                IllegalStateException("AI tuning timed out after 60s. Check your internet connection and retry.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Equalizer inference failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Unable to reach online AI Equalizer. Verify internet access and try again."
                )
            )
        }
    }

    private fun parseStructuredResponse(jsonString: String): Result<AudioEffectsConfig> {
        return runCatching {
            val root = JSONObject(jsonString)
            val bandsJson = root.getJSONArray("bandGainsDb")
            val gains = (0 until 10).map { idx ->
                val safeIdx = if (idx < bandsJson.length()) idx else (bandsJson.length() - 1).coerceAtLeast(0)
                if (bandsJson.length() > 0) bandsJson.optDouble(safeIdx, 0.0).toFloat() else 0f
            }
            val surround = when (root.optString("surroundMode", "OFF").uppercase()) {
                "SURROUND_7_1" -> SurroundMode.SURROUND_7_1
                "SURROUND_5_1" -> SurroundMode.SURROUND_5_1
                else -> SurroundMode.OFF
            }
            AudioEffectsConfig(
                enabled = true,
                activeMode = ListeningMode.AI_ADAPTIVE,
                activePresetName = root.optString("presetName", "AI Studio Curve").ifBlank { "AI Studio Curve" },
                bandGainsDb = gains,
                preampDb = root.optDouble("preampDb", -1.0).toFloat(),
                bassBoost = root.optDouble("bassBoost", 0.25).toFloat(),
                trebleBoost = root.optDouble("trebleBoost", 0.2).toFloat(),
                virtualizerLevel = root.optDouble("virtualizerLevel", 0.35).toFloat(),
                surroundMode = surround,
                stereoWidth = root.optDouble("stereoWidth", 1.2).toFloat(),
                compressorEnabled = root.optBoolean("compressorEnabled", false),
                compressorThresholdDb = root.optDouble("compressorThresholdDb", -18.0).toFloat(),
                compressorRatio = root.optDouble("compressorRatio", 3.0).toFloat(),
                compressorMakeupDb = root.optDouble("compressorMakeupDb", 2.0).toFloat(),
                aiRationale = root.optString("rationale", "Custom AI-balanced acoustic curve.")
            ).clamped()
        }
    }
}
