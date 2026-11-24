package com.example.audio

import android.content.Context
import android.media.AudioFormat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities
import com.example.data.AudioEffectsConfig
import com.example.data.HardwareAudioDiagnostics
import com.example.data.SignalTelemetry
import com.example.data.VisualizerFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared singleton audio bus connecting Media3's PulsePlaybackService, AudiophileDspAudioProcessor,
 * hardware digital passthrough diagnostics, and Jetpack Compose UI.
 */
@UnstableApi
object PulseAudioEngine {

    private val _visualizerFrame = MutableStateFlow(VisualizerFrame())
    val visualizerFrame: StateFlow<VisualizerFrame> = _visualizerFrame.asStateFlow()

    private val _signalTelemetry = MutableStateFlow(SignalTelemetry())
    val signalTelemetry: StateFlow<SignalTelemetry> = _signalTelemetry.asStateFlow()

    private val _hardwareDiagnostics = MutableStateFlow(HardwareAudioDiagnostics())
    val hardwareDiagnostics: StateFlow<HardwareAudioDiagnostics> = _hardwareDiagnostics.asStateFlow()

    private val _effectsConfig = MutableStateFlow(AudioEffectsConfig())
    val effectsConfig: StateFlow<AudioEffectsConfig> = _effectsConfig.asStateFlow()

    private val _isPlayingNow = MutableStateFlow(false)
    val isPlayingNow: StateFlow<Boolean> = _isPlayingNow.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    fun updatePlaybackBanner(isPlaying: Boolean, trackTitle: String) {
        _isPlayingNow.value = isPlaying
        if (trackTitle.isNotBlank()) {
            _currentTrackTitle.value = trackTitle
        }
    }

    val dspProcessor: AudiophileDspAudioProcessor = AudiophileDspAudioProcessor { frame ->
        _visualizerFrame.value = frame
    }

    fun applyEffectsConfig(config: AudioEffectsConfig) {
        val safe = config.clamped()
        _effectsConfig.value = safe
        dspProcessor.updateConfig(safe)
        refreshPassthroughNote()
    }

    fun setSleepFadeGain(gain: Float) {
        dspProcessor.sleepFadeGain = gain.coerceIn(0f, 1f)
    }

    fun clearVisualizer() {
        dspProcessor.emitSilenceFrame()
    }

    fun updateSignalTelemetry(telemetry: SignalTelemetry) {
        val diag = _hardwareDiagnostics.value
        val cfg = _effectsConfig.value
        val isSurroundBitstream = telemetry.codecLabel in setOf("EAC3", "AC4", "MP4") ||
            telemetry.mimeType.contains("eac3", ignoreCase = true) ||
            telemetry.mimeType.contains("ac4", ignoreCase = true)

        val hwSupportedForTrack = when {
            telemetry.mimeType.contains("ac4", ignoreCase = true) || telemetry.codecLabel == "AC4" ->
                diag.supportsAc4Passthrough
            telemetry.mimeType.contains("eac3", ignoreCase = true) || telemetry.codecLabel == "EAC3" ->
                diag.supportsEac3Passthrough || diag.supportsEac3JocPassthrough
            else -> false
        }

        val passthroughActive = cfg.digitalPassthroughEnabled && hwSupportedForTrack
        val note = when {
            passthroughActive -> "Direct Digital Bitstream Passthrough Active (DSP Bypassed)"
            cfg.digitalPassthroughEnabled && isSurroundBitstream ->
                "Digital Passthrough Requested • Output is ${diag.outputDeviceSummary} (Decoded to Hi-Res PCM)"
            cfg.digitalPassthroughEnabled ->
                "Passthrough Armed for E-AC-3 / AC-4 MP4 • PCM DSP Bypassed"
            cfg.enabled -> "32-bit Float Studio DSP Active • ${cfg.activeMode.displayName}"
            else -> "Bit-Perfect Direct PCM (Audio Effects Off)"
        }

        _signalTelemetry.value = telemetry.copy(
            passthroughActive = passthroughActive,
            passthroughStatusNote = note
        )
    }

    private fun refreshPassthroughNote() {
        updateSignalTelemetry(_signalTelemetry.value)
    }

    fun refreshHardwareCapabilities(context: Context) {
        val caps = runCatching {
            AudioCapabilities.getCapabilities(
                context,
                AudioAttributes.DEFAULT,
                null
            )
        }.getOrNull()

        if (caps == null) {
            _hardwareDiagnostics.value = HardwareAudioDiagnostics()
            return
        }

        val eac3 = caps.supportsEncoding(AudioFormat.ENCODING_E_AC3)
        val eac3Joc = caps.supportsEncoding(AudioFormat.ENCODING_E_AC3_JOC)
        val ac4 = caps.supportsEncoding(AudioFormat.ENCODING_AC4)
        val pcmFloat = caps.supportsEncoding(C.ENCODING_PCM_FLOAT)
        val maxCh = caps.maxChannelCount.coerceAtLeast(2)

        val summary = when {
            ac4 || eac3Joc -> "HDMI / AVR Multichannel (${maxCh}ch Dolby Ready)"
            eac3 -> "Digital Surround Output (${maxCh}ch E-AC-3 Ready)"
            maxCh > 2 -> "Multichannel PCM Output (${maxCh}ch)"
            else -> "Stereo DAC / Headphones (2.0ch PCM)"
        }

        _hardwareDiagnostics.value = HardwareAudioDiagnostics(
            supportsEac3Passthrough = eac3,
            supportsEac3JocPassthrough = eac3Joc,
            supportsAc4Passthrough = ac4,
            supportsPcmFloat = pcmFloat,
            maxChannelCount = maxCh,
            outputDeviceSummary = summary
        )
        refreshPassthroughNote()
    }
}
