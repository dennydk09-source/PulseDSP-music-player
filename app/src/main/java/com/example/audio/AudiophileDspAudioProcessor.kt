package com.example.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.data.AudioEffectsConfig
import com.example.data.EQ_BAND_FREQUENCIES_HZ
import com.example.data.SurroundMode
import com.example.data.VisualizerFrame
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Custom Media3 BaseAudioProcessor implementing a real-time 32-bit float PCM studio DSP chain:
 * Preamp -> 10-Band Peaking EQ -> Bass/Treble Shelf Boosters -> Mid/Side Stereoizer ->
 * Virtualizer & 5.1/7.1 Virtual Surround Matrix -> Dynamics Compressor -> Sleep Fade ->
 * Soft Limiter -> Live 512-pt Radix-2 FFT & Oscilloscope Analyzer.
 */
@UnstableApi
class AudiophileDspAudioProcessor(
    private val onVisualizerFrame: (VisualizerFrame) -> Unit
) : BaseAudioProcessor() {

    private class BiquadFilter {
        var b0 = 1f; var b1 = 0f; var b2 = 0f
        var a1 = 0f; var a2 = 0f
        var z1L = 0f; var z2L = 0f
        var z1R = 0f; var z2R = 0f

        fun reset() {
            z1L = 0f; z2L = 0f; z1R = 0f; z2R = 0f
        }

        fun setPeaking(sampleRate: Float, freqHz: Float, q: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f || freqHz >= sampleRate * 0.48f) {
                setBypass()
                return
            }
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freqHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)

            val b0d = 1.0 + alpha * a
            val b1d = -2.0 * cosW0
            val b2d = 1.0 - alpha * a
            val a0d = 1.0 + alpha / a
            val a1d = -2.0 * cosW0
            val a2d = 1.0 - alpha / a

            b0 = (b0d / a0d).toFloat()
            b1 = (b1d / a0d).toFloat()
            b2 = (b2d / a0d).toFloat()
            a1 = (a1d / a0d).toFloat()
            a2 = (a2d / a0d).toFloat()
        }

        fun setLowShelf(sampleRate: Float, freqHz: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f) {
                setBypass()
                return
            }
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freqHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val sqrtA = sqrt(a)
            val alpha = sinW0 / 2.0 * sqrt(2.0)

            val b0d = a * ((a + 1.0) - (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
            val b1d = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
            val b2d = a * ((a + 1.0) - (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
            val a0d = (a + 1.0) + (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha
            val a1d = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
            val a2d = (a + 1.0) + (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha

            b0 = (b0d / a0d).toFloat()
            b1 = (b1d / a0d).toFloat()
            b2 = (b2d / a0d).toFloat()
            a1 = (a1d / a0d).toFloat()
            a2 = (a2d / a0d).toFloat()
        }

        fun setHighShelf(sampleRate: Float, freqHz: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f || freqHz >= sampleRate * 0.48f) {
                setBypass()
                return
            }
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freqHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val sqrtA = sqrt(a)
            val alpha = sinW0 / 2.0 * sqrt(2.0)

            val b0d = a * ((a + 1.0) + (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
            val b1d = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
            val b2d = a * ((a + 1.0) + (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
            val a0d = (a + 1.0) - (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha
            val a1d = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
            val a2d = (a + 1.0) - (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha

            b0 = (b0d / a0d).toFloat()
            b1 = (b1d / a0d).toFloat()
            b2 = (b2d / a0d).toFloat()
            a1 = (a1d / a0d).toFloat()
            a2 = (a2d / a0d).toFloat()
        }

        fun setBypass() {
            b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
        }

        fun processL(x: Float): Float {
            val y = b0 * x + z1L
            z1L = b1 * x - a1 * y + z2L
            z2L = b2 * x - a2 * y
            return y
        }

        fun processR(x: Float): Float {
            val y = b0 * x + z1R
            z1R = b1 * x - a1 * y + z2R
            z2R = b2 * x - a2 * y
            return y
        }
    }

    private val configRef = AtomicReference(AudioEffectsConfig())
    @Volatile private var configDirty = true
    @Volatile var sleepFadeGain: Float = 1f

    private var currentSampleRate = 48_000f
    private var activeConfig = AudioEffectsConfig()

    // Filter bank: 10-band EQ + Bass Shelf + Treble Shelf
    private val eqFilters = Array(10) { BiquadFilter() }
    private val bassShelf = BiquadFilter()
    private val trebleShelf = BiquadFilter()

    // Surround / Virtualizer multi-tap delay line (power-of-2 mask)
    private val delaySize = 4096
    private val delayMask = delaySize - 1
    private val delayLineL = FloatArray(delaySize)
    private val delayLineR = FloatArray(delaySize)
    private var delayWritePos = 0

    // Compressor envelope state
    private var compEnvelope = 0f
    private var currentGainReductionDb = 0f

    // Real-time 512-point FFT analyzer state (zero-alloc on audio hot path)
    private val fftSize = 512
    private val fftInput = FloatArray(fftSize)
    private val fftReal = FloatArray(fftSize)
    private val fftImag = FloatArray(fftSize)
    private val hannWindow = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }
    private var fftWriteIdx = 0
    private val smoothedBands = FloatArray(32)
    private val peakHoldBands = FloatArray(32)
    private val waveformOut = FloatArray(64)
    private var lastEmitNanos = 0L

    fun updateConfig(newConfig: AudioEffectsConfig) {
        configRef.set(newConfig.clamped())
        configDirty = true
    }

    fun emitSilenceFrame() {
        smoothedBands.fill(0f)
        peakHoldBands.fill(0f)
        waveformOut.fill(0f)
        onVisualizerFrame(VisualizerFrame(isActive = false))
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        if (inputAudioFormat.channelCount < 1) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        currentSampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        configDirty = true
        return inputAudioFormat
    }

    override fun onFlush() {
        eqFilters.forEach { it.reset() }
        bassShelf.reset()
        trebleShelf.reset()
        delayLineL.fill(0f)
        delayLineR.fill(0f)
        delayWritePos = 0
        compEnvelope = 0f
        currentGainReductionDb = 0f
        fftWriteIdx = 0
    }

    override fun onReset() {
        onFlush()
        emitSilenceFrame()
    }

    private fun refreshCoefficientsIfNeeded() {
        if (!configDirty) return
        configDirty = false
        val cfg = configRef.get()
        activeConfig = cfg
        for (i in 0 until 10) {
            eqFilters[i].setPeaking(
                sampleRate = currentSampleRate,
                freqHz = EQ_BAND_FREQUENCIES_HZ[i].toFloat(),
                q = 1.41f,
                gainDb = cfg.bandGainsDb.getOrElse(i) { 0f }
            )
        }
        bassShelf.setLowShelf(currentSampleRate, 85f, cfg.bassBoost * 10f)
        trebleShelf.setHighShelf(currentSampleRate, 9500f, cfg.trebleBoost * 9f)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        refreshCoefficientsIfNeeded()
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        val channels = inputAudioFormat.channelCount
        val isFloatPcm = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerSample = if (isFloatPcm) 4 else 2
        val frameCount = remaining / (bytesPerSample * channels)

        val cfg = activeConfig
        val dspActive = cfg.enabled && !cfg.digitalPassthroughEnabled
        val preampLin = if (dspActive) 10.0.pow(cfg.preampDb / 20.0).toFloat() else 1f
        val stereoWidth = cfg.stereoWidth
        val virtualizer = cfg.virtualizerLevel
        val surroundMode = cfg.surroundMode

        // Delay taps in samples for 5.1 & 7.1 spatial surround simulation
        val tap1 = (currentSampleRate * 0.009f).toInt().coerceIn(32, 1024)
        val tap2 = (currentSampleRate * 0.017f).toInt().coerceIn(64, 2048)
        val tap3 = (currentSampleRate * 0.026f).toInt().coerceIn(96, 3072)

        // Compressor constants
        val compEnabled = dspActive && cfg.compressorEnabled
        val compThreshDb = cfg.compressorThresholdDb
        val compRatio = cfg.compressorRatio.coerceAtLeast(1f)
        val compMakeupLin = 10.0.pow(cfg.compressorMakeupDb / 20.0).toFloat()
        val attackCoeff = exp(-1.0 / (0.005 * currentSampleRate)).toFloat()
        val releaseCoeff = exp(-1.0 / (0.110 * currentSampleRate)).toFloat()
        val fadeGain = sleepFadeGain.coerceIn(0f, 1f)

        for (f in 0 until frameCount) {
            var left: Float
            var right: Float

            if (channels == 1) {
                val s = readSample(inputBuffer, isFloatPcm)
                left = s
                right = s
            } else {
                left = readSample(inputBuffer, isFloatPcm)
                right = readSample(inputBuffer, isFloatPcm)
                // Pass through extra channels unmodified after reading
                for (c in 2 until channels) {
                    val extra = readSample(inputBuffer, isFloatPcm)
                    // Fold center/surround energy gently into visualization only; write below
                    if (c == 2) {
                        left += extra * 0.15f
                        right += extra * 0.15f
                    }
                }
            }

            if (dspActive) {
                left *= preampLin
                right *= preampLin

                // 10-Band Peaking Graphic EQ
                for (b in 0 until 10) {
                    left = eqFilters[b].processL(left)
                    right = eqFilters[b].processR(right)
                }

                // Dedicated Bass & Treble Shelf Boosters
                left = bassShelf.processL(left)
                right = bassShelf.processR(right)
                left = trebleShelf.processL(left)
                right = trebleShelf.processR(right)

                // Mid/Side Stereoizer
                if (abs(stereoWidth - 1f) > 0.01f) {
                    val mid = (left + right) * 0.5f
                    val side = (left - right) * 0.5f * stereoWidth
                    left = mid + side
                    right = mid - side
                }

                // Virtualizer & 5.1 / 7.1 Virtual Surround Matrix
                val spatialStrength = when (surroundMode) {
                    SurroundMode.OFF -> virtualizer * 0.45f
                    SurroundMode.SURROUND_5_1 -> (0.35f + virtualizer * 0.55f).coerceAtMost(0.95f)
                    SurroundMode.SURROUND_7_1 -> (0.50f + virtualizer * 0.50f).coerceAtMost(1.0f)
                }
                delayLineL[delayWritePos] = left
                delayLineR[delayWritePos] = right

                if (spatialStrength > 0.01f) {
                    val d1L = delayLineL[(delayWritePos - tap1) and delayMask]
                    val d1R = delayLineR[(delayWritePos - tap1) and delayMask]
                    val d2L = delayLineL[(delayWritePos - tap2) and delayMask]
                    val d2R = delayLineR[(delayWritePos - tap2) and delayMask]

                    var surL = (d1R * 0.24f - d2L * 0.16f)
                    var surR = (d1L * 0.24f - d2R * 0.16f)

                    if (surroundMode == SurroundMode.SURROUND_7_1) {
                        val d3L = delayLineL[(delayWritePos - tap3) and delayMask]
                        val d3R = delayLineR[(delayWritePos - tap3) and delayMask]
                        surL += (d3L * 0.14f - d3R * 0.12f)
                        surR += (d3R * 0.14f - d3L * 0.12f)
                    }

                    left += surL * spatialStrength
                    right += surR * spatialStrength
                }
                delayWritePos = (delayWritePos + 1) and delayMask

                // Dynamics Compressor
                if (compEnabled) {
                    val peak = max(abs(left), abs(right))
                    compEnvelope = if (peak > compEnvelope) {
                        attackCoeff * compEnvelope + (1f - attackCoeff) * peak
                    } else {
                        releaseCoeff * compEnvelope + (1f - releaseCoeff) * peak
                    }
                    val envDb = 20f * log10(compEnvelope.coerceAtLeast(1e-6f))
                    val overshoot = envDb - compThreshDb
                    if (overshoot > 0f) {
                        val grDb = overshoot * (1f - 1f / compRatio)
                        currentGainReductionDb = grDb
                        val grLin = 10.0.pow(-grDb / 20.0).toFloat() * compMakeupLin
                        left *= grLin
                        right *= grLin
                    } else {
                        currentGainReductionDb *= 0.995f
                        left *= compMakeupLin
                        right *= compMakeupLin
                    }
                } else {
                    currentGainReductionDb = 0f
                }

                // Soft-clip studio output limiter
                if (abs(left) > 0.92f) left = tanh(left.toDouble()).toFloat()
                if (abs(right) > 0.92f) right = tanh(right.toDouble()).toFloat()
            } else {
                currentGainReductionDb = 0f
            }

            left *= fadeGain
            right *= fadeGain

            // Feed live FFT ring buffer with post-DSP audio
            val mono = (left + right) * 0.5f
            fftInput[fftWriteIdx] = mono
            fftWriteIdx++
            if (fftWriteIdx >= fftSize) {
                fftWriteIdx = 0
                maybeComputeAndEmitVisualizer()
            }

            if (channels == 1) {
                writeSample(outputBuffer, left, isFloatPcm)
            } else {
                writeSample(outputBuffer, left, isFloatPcm)
                writeSample(outputBuffer, right, isFloatPcm)
                for (c in 2 until channels) {
                    writeSample(outputBuffer, mono * 0.5f, isFloatPcm)
                }
            }
        }

        outputBuffer.flip()
    }

    private fun readSample(buffer: ByteBuffer, isFloatPcm: Boolean): Float {
        return if (isFloatPcm) {
            buffer.float
        } else {
            buffer.short.toFloat() / 32768f
        }
    }

    private fun writeSample(buffer: ByteBuffer, sample: Float, isFloatPcm: Boolean) {
        val clamped = sample.coerceIn(-1f, 1f)
        if (isFloatPcm) {
            buffer.putFloat(clamped)
        } else {
            buffer.putShort((clamped * 32767f).toInt().toShort())
        }
    }

    private fun maybeComputeAndEmitVisualizer() {
        val now = System.nanoTime()
        // Cap emission to ~60 FPS (16.5ms interval) for silky UI rendering
        if (now - lastEmitNanos < 16_500_000L) return
        lastEmitNanos = now

        for (i in 0 until fftSize) {
            fftReal[i] = fftInput[i] * hannWindow[i]
            fftImag[i] = 0f
        }
        computeRadix2Fft(fftReal, fftImag)

        // Downsample waveform to 64 crisp oscilloscope points
        val stride = fftSize / 64
        for (i in 0 until 64) {
            waveformOut[i] = fftInput[i * stride].coerceIn(-1f, 1f)
        }

        // Map 256 FFT bins logarithmically across 32 visual spectrum bars (30Hz..16kHz)
        val half = fftSize / 2
        val minBin = 1f
        val maxBin = (half - 1).toFloat()
        val logRatio = ln(maxBin / minBin)
        var bassSum = 0f

        for (band in 0 until 32) {
            val startBin = (minBin * exp(logRatio * band / 32f)).toInt().coerceIn(1, half - 1)
            val endBin = (minBin * exp(logRatio * (band + 1) / 32f)).toInt().coerceIn(startBin, half - 1)

            var peakMag = 0f
            for (k in startBin..endBin) {
                val re = fftReal[k]
                val im = fftImag[k]
                val mag = sqrt(re * re + im * im) / (fftSize * 0.25f)
                if (mag > peakMag) peakMag = mag
            }

            // Convert to perceptually boosted 0..1 bar height with slight treble tilt
            val db = 20f * log10(peakMag.coerceAtLeast(1e-5f))
            val tilt = band * 0.35f
            val normalized = ((db + 54f + tilt) / 52f).coerceIn(0f, 1f)

            val prev = smoothedBands[band]
            val smooth = if (normalized > prev) {
                prev * 0.30f + normalized * 0.70f
            } else {
                prev * 0.78f + normalized * 0.22f
            }
            smoothedBands[band] = smooth

            val prevPeak = peakHoldBands[band]
            peakHoldBands[band] = if (smooth >= prevPeak) {
                smooth
            } else {
                (prevPeak - 0.016f).coerceAtLeast(smooth)
            }

            if (band < 6) bassSum += smooth
        }

        onVisualizerFrame(
            VisualizerFrame(
                spectrumBands = smoothedBands.copyOf(),
                peakHoldBands = peakHoldBands.copyOf(),
                waveform = waveformOut.copyOf(),
                bassEnergy = (bassSum / 6f).coerceIn(0f, 1f),
                gainReductionDb = currentGainReductionDb.coerceIn(0f, 24f),
                isActive = true
            )
        )
    }

    private fun computeRadix2Fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wLenRe = cos(ang).toFloat()
            val wLenIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wRe = 1f
                var wIm = 0f
                val halfLen = len shr 1
                for (k in 0 until halfLen) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + halfLen] * wRe - im[i + k + halfLen] * wIm
                    val vIm = re[i + k + halfLen] * wIm + im[i + k + halfLen] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + halfLen] = uRe - vRe
                    im[i + k + halfLen] = uIm - vIm
                    val nextWRe = wRe * wLenRe - wIm * wLenIm
                    wIm = wRe * wLenIm + wIm * wLenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
