package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Generates three genuine, pleasant 48 kHz 16-bit lossless stereo PCM WAV reference tracks
 * into app-private offline storage on first launch so users can immediately audition all 10 EQ
 * bands, Bass/Treble boosters, Virtualizer, 5.1/7.1 Surround, Stereoizer, Compressor, and the
 * 60 FPS FFT visualizer even on a fresh device with no local music files yet.
 */
object DemoAudioGenerator {

    private const val SAMPLE_RATE = 48_000
    private const val CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16

    suspend fun ensureBuiltInTracks(context: Context): List<TrackEntity> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "reference_audio").apply { mkdirs() }

        val specs = listOf(
            DemoSpec(
                fileName = "neon_horizon_48k.wav",
                title = "Neon Horizon",
                artist = "PulseDSP Synth Lab",
                album = "Cyber Reference Vol. I",
                artworkResName = "img_art_neon_horizon_1795497456622",
                durationSec = 28,
                style = DemoStyle.SYNTHWAVE
            ),
            DemoSpec(
                fileName = "subterranean_pulse_48k.wav",
                title = "Subterranean Pulse",
                artist = "Sub-Bass Mastering Suite",
                album = "Low-End Calibration",
                artworkResName = "img_art_subterranean_pulse_1795497450454",
                durationSec = 26,
                style = DemoStyle.SUBBASS
            ),
            DemoSpec(
                fileName = "acoustic_glass_48k.wav",
                title = "Acoustic Glass",
                artist = "Holographic Chamber",
                album = "Spatial 7.1 Study",
                artworkResName = "img_art_acoustic_glass_1795497450521",
                durationSec = 28,
                style = DemoStyle.AMBIENT_CHIMES
            )
        )

        specs.map { spec ->
            val file = File(dir, spec.fileName)
            if (!file.exists() || file.length() < 4096L) {
                synthesizeWav(file, spec.durationSec, spec.style)
            }
            val lrcFile = File(dir, "${file.nameWithoutExtension}.lrc")
            if (!lrcFile.exists() || lrcFile.length() < 32L) {
                lrcFile.writeText(buildReferenceLrc(spec))
            }
            TrackEntity(
                uri = Uri.fromFile(file).toString(),
                title = spec.title,
                artist = spec.artist,
                album = spec.album,
                durationMs = spec.durationSec * 1000L,
                mimeType = "audio/wav",
                codecLabel = "WAV",
                sampleRateHz = SAMPLE_RATE,
                bitDepth = BITS_PER_SAMPLE,
                channelCount = CHANNELS,
                bitrateKbps = (SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE) / 1000,
                artworkResName = spec.artworkResName,
                isBuiltIn = true
            )
        }
    }

    private fun buildReferenceLrc(spec: DemoSpec): String = when (spec.style) {
        DemoStyle.SYNTHWAVE -> """
            [ti:${spec.title}]
            [ar:${spec.artist}]
            [al:${spec.album}]
            [00:00.20]Electric dawn breaks across the obsidian skyline
            [00:03.60]Analog kicks locked in at 112 beats per minute
            [00:07.20]Cyan harmonics rising through the 32-bit float stage
            [00:10.80]Detuned synth pads breathing wide in stereo space
            [00:14.40]Tap any line here to jump straight to this timestamp
            [00:18.00]Arpeggio pulses dancing between left and right channels
            [00:21.60]Crisp high-frequency shimmer floating above the bassline
            [00:25.00]Fading gently into the neon horizon...
        """.trimIndent()

        DemoStyle.SUBBASS -> """
            [ti:${spec.title}]
            [ar:${spec.artist}]
            [al:${spec.album}]
            [00:00.20]Deep subterranean fundamental at 41 Hz E1
            [00:03.80]Sidechain envelope breathing around the low-end pulse
            [00:07.50]Test your Bass Booster and 31 Hz – 62 Hz EQ faders
            [00:11.20]Harmonic overtones shimmering across the upper mids
            [00:15.00]Sub-bass steps ascending through G1 and A1 resonance
            [00:18.80]Dynamics compressor locking punch and headroom tight
            [00:22.40]Settling down to a deep 36 Hz D1 floor...
        """.trimIndent()

        DemoStyle.AMBIENT_CHIMES -> """
            [ti:${spec.title}]
            [ar:${spec.artist}]
            [al:${spec.album}]
            [00:00.20]Crystalline glass chimes suspended in midnight air
            [00:04.00]Binaural stereo pads drifting slowly across the stage
            [00:08.00]Try 5.1 or 7.1 Virtual Surround for wider room depth
            [00:12.00]Harmonic bell overtones blooming from left to right
            [00:16.00]Zero coloration — pure 48 kHz lossless PCM clarity
            [00:20.00]Ambient shimmer lingering across the acoustic chamber
            [00:24.20]Dissolving softly into silence...
        """.trimIndent()
    }

    private enum class DemoStyle { SYNTHWAVE, SUBBASS, AMBIENT_CHIMES }

    private data class DemoSpec(
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val artworkResName: String,
        val durationSec: Int,
        val style: DemoStyle
    )

    private fun synthesizeWav(target: File, durationSec: Int, style: DemoStyle) {
        val totalFrames = SAMPLE_RATE * durationSec
        val dataByteCount = totalFrames * CHANNELS * (BITS_PER_SAMPLE / 8)

        BufferedOutputStream(FileOutputStream(target), 65536).use { out ->
            out.write(buildWavHeader(dataByteCount))

            val chunkFrames = 2048
            val byteBuf = ByteBuffer.allocate(chunkFrames * CHANNELS * 2).order(ByteOrder.LITTLE_ENDIAN)

            val synthChords = arrayOf(
                doubleArrayOf(220.0, 261.63, 329.63, 440.0),   // Am
                doubleArrayOf(174.61, 220.0, 261.63, 349.23),  // F
                doubleArrayOf(130.81, 196.0, 261.63, 329.63),  // C
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)    // G
            )
            val arpNotes = doubleArrayOf(
                440.0, 523.25, 659.25, 783.99, 659.25, 523.25, 493.88, 392.0
            )
            val ambientNotes = doubleArrayOf(
                293.66, 369.99, 440.0, 554.37, 659.25, 880.0, 1174.66
            )

            var frameIndex = 0
            while (frameIndex < totalFrames) {
                val count = minOf(chunkFrames, totalFrames - frameIndex)
                byteBuf.clear()

                for (i in 0 until count) {
                    val idx = frameIndex + i
                    val t = idx.toDouble() / SAMPLE_RATE.toDouble()

                    // Smooth fade-in and fade-out at track boundaries
                    val edgeEnv = when {
                        t < 0.35 -> (t / 0.35)
                        t > durationSec - 0.8 -> ((durationSec - t) / 0.8).coerceAtLeast(0.0)
                        else -> 1.0
                    }

                    var left = 0.0
                    var right = 0.0

                    when (style) {
                        DemoStyle.SYNTHWAVE -> {
                            val bpm = 112.0
                            val beatPos = t * (bpm / 60.0)
                            val beatFrac = beatPos - beatPos.toInt()
                            val barIdx = (beatPos / 4.0).toInt() % synthChords.size
                            val chord = synthChords[barIdx]

                            // Punchy analog kick on every beat (sweeps 130Hz -> 46Hz)
                            val kickEnv = exp(-beatFrac * 9.5)
                            val kickFreq = 46.0 + 84.0 * exp(-beatFrac * 24.0)
                            val kick = sin(2.0 * PI * kickFreq * beatFrac / (bpm / 60.0)) * kickEnv * 0.42

                            // Warm detuned stereo pad chord
                            var padL = 0.0
                            var padR = 0.0
                            for (n in chord.indices) {
                                val f = chord[n]
                                padL += sin(2.0 * PI * f * t) * 0.08 + sin(2.0 * PI * (f * 2.002) * t) * 0.03
                                padR += sin(2.0 * PI * (f * 0.998) * t) * 0.08 + sin(2.0 * PI * (f * 1.999) * t) * 0.03
                            }

                            // 16th-note stereo cyber arpeggio
                            val step = (beatPos * 4.0).toInt()
                            val stepFrac = (beatPos * 4.0) - step
                            val arpFreq = arpNotes[step % arpNotes.size]
                            val arpEnv = exp(-stepFrac * 6.5)
                            val arpSig = (sin(2.0 * PI * arpFreq * t) + 0.35 * sin(2.0 * PI * arpFreq * 2.0 * t)) * arpEnv * 0.22
                            val pan = 0.5 + 0.38 * sin(step * 0.9)

                            // Crisp off-beat hi-hat shimmer (deterministic high-freq sum)
                            val hatFrac = (beatPos * 2.0) - (beatPos * 2.0).toInt()
                            val hatEnv = exp(-hatFrac * 28.0) * 0.09
                            val hat = (sin(2.0 * PI * 6840.0 * t) + sin(2.0 * PI * 9420.0 * t) + sin(2.0 * PI * 12480.0 * t)) * hatEnv

                            left = (kick + padL + arpSig * (1.0 - pan) + hat * 0.8) * edgeEnv
                            right = (kick + padR + arpSig * pan + hat * 1.1) * edgeEnv
                        }

                        DemoStyle.SUBBASS -> {
                            val bpm = 96.0
                            val beatPos = t * (bpm / 60.0)
                            val barIdx = (beatPos / 4.0).toInt() % 4
                            val rootHz = when (barIdx) {
                                0 -> 41.20 // E1
                                1 -> 49.00 // G1
                                2 -> 55.00 // A1
                                else -> 36.71 // D1
                            }
                            val subWave = (sin(2.0 * PI * rootHz * t) * 0.44 +
                                sin(2.0 * PI * (rootHz * 2.0) * t) * 0.22 +
                                sin(2.0 * PI * (rootHz * 3.0) * t) * 0.09)

                            val pulseFrac = beatPos - beatPos.toInt()
                            val sidechain = 0.35 + 0.65 * (1.0 - exp(-pulseFrac * 6.0))
                            val kickEnv = exp(-pulseFrac * 10.0)
                            val kick = sin(2.0 * PI * (48.0 + 70.0 * exp(-pulseFrac * 22.0)) * t) * kickEnv * 0.35

                            // Stereo harmonic shimmer chords on upper mids/highs
                            val chimeIdx = (beatPos * 2.0).toInt() % 4
                            val chimeFrac = (beatPos * 2.0) - (beatPos * 2.0).toInt()
                            val chimeHz = rootHz * 8.0 * (1.0 + chimeIdx * 0.25)
                            val chime = sin(2.0 * PI * chimeHz * t) * exp(-chimeFrac * 7.0) * 0.16
                            val pan = if (chimeIdx % 2 == 0) 0.2 else 0.8

                            left = (subWave * sidechain + kick + chime * (1.0 - pan)) * edgeEnv
                            right = (subWave * sidechain + kick + chime * pan) * edgeEnv
                        }

                        DemoStyle.AMBIENT_CHIMES -> {
                            // Wide binaural pad + cascading spatial crystalline chimes
                            val slowLfo = 0.5 + 0.5 * sin(2.0 * PI * 0.12 * t)
                            val padL = sin(2.0 * PI * 146.83 * t) * 0.18 +
                                sin(2.0 * PI * 220.0 * t) * 0.14 +
                                sin(2.0 * PI * 369.99 * t) * 0.09 * slowLfo
                            val padR = sin(2.0 * PI * 147.25 * t) * 0.18 +
                                sin(2.0 * PI * 220.4 * t) * 0.14 +
                                sin(2.0 * PI * 370.4 * t) * 0.09 * (1.0 - slowLfo)

                            val noteRate = 1.6
                            val noteIdx = (t * noteRate).toInt()
                            val noteFrac = (t * noteRate) - noteIdx
                            val hz = ambientNotes[noteIdx % ambientNotes.size]
                            val bellEnv = exp(-noteFrac * 3.8)
                            val bell = (sin(2.0 * PI * hz * t) +
                                0.45 * sin(2.0 * PI * hz * 2.0 * t) * exp(-noteFrac * 6.0) +
                                0.22 * sin(2.0 * PI * hz * 3.01 * t) * exp(-noteFrac * 9.0)) * bellEnv * 0.24
                            val pan = 0.15 + 0.7 * ((noteIdx * 37 % 10) / 10.0)

                            left = (padL + bell * (1.0 - pan)) * edgeEnv
                            right = (padR + bell * pan) * edgeEnv
                        }
                    }

                    val sL = (left.coerceIn(-0.98, 0.98) * 32767.0).toInt().toShort()
                    val sR = (right.coerceIn(-0.98, 0.98) * 32767.0).toInt().toShort()
                    byteBuf.putShort(sL)
                    byteBuf.putShort(sR)
                }

                out.write(byteBuf.array(), 0, count * CHANNELS * 2)
                frameIndex += count
            }
        }
    }

    private fun buildWavHeader(dataSize: Int): ByteArray {
        val totalFileSizeMinus8 = 36 + dataSize
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)
        val blockAlign = (CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()

        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalFileSizeMinus8)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray())
            putInt(dataSize)
        }.array()
    }
}
