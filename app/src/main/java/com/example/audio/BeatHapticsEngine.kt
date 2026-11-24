package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.VisualizerFrame

/**
 * Real-time beat-onset detector driving the Android hardware haptic motor directly from
 * the live FFT Visualizer's bass energy and spectral intensity.
 */
class BeatHapticsEngine(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val hasHardwareVibrator: Boolean = runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)
    val hasAmplitudeControl: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { vibrator?.hasAmplitudeControl() == true }.getOrDefault(false)
    } else {
        false
    }

    private var avgBassEnergy = 0f
    private var lastTriggerMs = 0L

    /**
     * Evaluates the latest FFT VisualizerFrame and fires a crisp tactile beat pulse
     * when bass/kick energy spikes above the adaptive rolling average.
     *
     * @return Normalized pulse strength (0f if no beat triggered, 0.2f..1f if triggered).
     */
    fun processFrame(
        frame: VisualizerFrame,
        isPlaying: Boolean,
        enabled: Boolean,
        intensity: Float
    ): Float {
        if (!enabled || !isPlaying || !frame.isActive || !hasHardwareVibrator) {
            avgBassEnergy *= 0.85f
            return 0f
        }

        val bass = frame.bassEnergy.coerceIn(0f, 1f)
        // Compute overall upper-band energy for percussive transients
        var highSum = 0f
        for (i in 6 until minOf(16, frame.spectrumBands.size)) {
            highSum += frame.spectrumBands[i]
        }
        val midEnergy = (highSum / 10f).coerceIn(0f, 1f)
        val combinedEnergy = (bass * 0.78f + midEnergy * 0.22f).coerceIn(0f, 1f)

        val baseline = avgBassEnergy
        avgBassEnergy = baseline * 0.86f + combinedEnergy * 0.14f

        val nowMs = System.currentTimeMillis()
        val cooldownMs = 125L
        val isBeatSpike = combinedEnergy > 0.22f &&
            (combinedEnergy > baseline * 1.24f || combinedEnergy > 0.72f)

        if (isBeatSpike && (nowMs - lastTriggerMs) >= cooldownMs) {
            lastTriggerMs = nowMs
            val scaledStrength = (combinedEnergy * intensity.coerceIn(0.1f, 1f)).coerceIn(0.15f, 1f)
            firePulse(scaledStrength)
            return scaledStrength
        }

        return 0f
    }

    fun triggerConfirmationPulse(intensity: Float) {
        if (!hasHardwareVibrator) return
        firePulse((0.55f * intensity.coerceIn(0.2f, 1f)).coerceIn(0.2f, 1f))
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
        avgBassEnergy = 0f
    }

    private fun firePulse(strength: Float) {
        val vib = vibrator ?: return
        val durationMs = (16L + (strength * 28L).toLong()).coerceIn(16L, 46L)
        val amplitude = (40 + (strength * 215f).toInt()).coerceIn(40, 255)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val effect = if (hasAmplitudeControl) {
                    VibrationEffect.createOneShot(durationMs, amplitude)
                } else {
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_MEDIA)
                    .build()
                vib.vibrate(effect, attrs)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (hasAmplitudeControl) {
                    VibrationEffect.createOneShot(durationMs, amplitude)
                } else {
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }
}
