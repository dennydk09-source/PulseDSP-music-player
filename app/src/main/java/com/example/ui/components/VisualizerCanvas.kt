package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.SurroundMode
import com.example.data.VisualizerFrame
import com.example.data.VisualizerMode
import com.example.data.VisualizerPalette
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LaserMint
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TelemetrySlate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class VisualizerPaletteColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val peakCap: Color
)

fun paletteColorsFor(palette: VisualizerPalette): VisualizerPaletteColors = when (palette) {
    VisualizerPalette.CYBER_NEON -> VisualizerPaletteColors(
        primary = ElectricCyan,
        secondary = CyberViolet,
        tertiary = LaserMint,
        peakCap = IceWhite
    )
    VisualizerPalette.SUNSET_GLOW -> VisualizerPaletteColors(
        primary = AmberPulse,
        secondary = SignalRed,
        tertiary = Color(0xFFD424FF),
        peakCap = Color(0xFFFFF4D2)
    )
    VisualizerPalette.EMERALD_MATRIX -> VisualizerPaletteColors(
        primary = LaserMint,
        secondary = Color(0xFF00D2B8),
        tertiary = Color(0xFF008F5A),
        peakCap = Color(0xFFD8FFE9)
    )
    VisualizerPalette.MONOCHROMATIC -> VisualizerPaletteColors(
        primary = IceWhite,
        secondary = TelemetrySlate,
        tertiary = MutedSlate,
        peakCap = Color.White
    )
}

/**
 * Real-time 60 FPS Compose Canvas FFT frequency visualizer supporting
 * Bars, Radial Ring (orbiting album art), and Oscilloscope Waveform modes
 * with user-selectable color palettes.
 */
@Composable
fun RealTimeVisualizer(
    frame: VisualizerFrame,
    mode: VisualizerMode,
    palette: VisualizerPalette = VisualizerPalette.CYBER_NEON,
    artworkResName: String?,
    trackTitle: String,
    modifier: Modifier = Modifier,
    trackUri: String? = null
) {
    val colors = paletteColorsFor(palette)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            VisualizerMode.BARS -> SpectrumBarsCanvas(
                frame = frame,
                colors = colors,
                modifier = Modifier.fillMaxSize()
            )

            VisualizerMode.RADIAL -> {
                RadialRingCanvas(
                    frame = frame,
                    colors = colors,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(colors.primary, colors.secondary, colors.tertiary, colors.primary)
                            ),
                            shape = CircleShape
                        )
                        .background(DeepVoid)
                ) {
                    TrackArtwork(
                        artworkResName = artworkResName,
                        contentDescription = trackTitle,
                        trackUri = trackUri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            VisualizerMode.WAVEFORM -> WaveformOscilloscopeCanvas(
                frame = frame,
                colors = colors,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SpectrumBarsCanvas(
    frame: VisualizerFrame,
    colors: VisualizerPaletteColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val gridLines = 4
        for (g in 1..gridLines) {
            val y = h * (g.toFloat() / (gridLines + 1))
            drawLine(
                color = HairlineBorder.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        val bands = frame.spectrumBands
        val peaks = frame.peakHoldBands
        val count = bands.size
        val slotW = w / count
        val barW = (slotW * 0.68f).coerceAtLeast(3f)
        val gap = (slotW - barW) * 0.5f
        val baseY = h * 0.86f
        val maxBarH = h * 0.78f

        val barBrush = Brush.verticalGradient(
            colors = listOf(colors.primary, colors.secondary, colors.tertiary.copy(alpha = 0.75f)),
            startY = baseY - maxBarH,
            endY = baseY
        )

        for (i in 0 until count) {
            val mag = bands[i].coerceIn(0f, 1f)
            val peak = peaks[i].coerceIn(0f, 1f)
            val x = i * slotW + gap
            val barH = (mag * maxBarH).coerceAtLeast(4f)
            val topY = baseY - barH

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, topY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW * 0.45f, barW * 0.45f)
            )

            val reflH = (barH * 0.16f).coerceAtMost(h - baseY - 2f).coerceAtLeast(1f)
            drawRoundRect(
                color = colors.primary.copy(alpha = 0.16f),
                topLeft = Offset(x, baseY + 3f),
                size = Size(barW, reflH),
                cornerRadius = CornerRadius(2f, 2f)
            )

            if (peak > 0.03f) {
                val peakY = (baseY - peak * maxBarH - 5f).coerceAtLeast(4f)
                drawRoundRect(
                    color = colors.peakCap,
                    topLeft = Offset(x, peakY),
                    size = Size(barW, 3.5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
private fun RadialRingCanvas(
    frame: VisualizerFrame,
    colors: VisualizerPaletteColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val minDim = min(size.width, size.height)
        val innerR = minDim * 0.30f
        val maxSpoke = minDim * 0.18f

        val haloR = innerR + maxSpoke * (0.45f + frame.bassEnergy * 0.65f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.24f + frame.bassEnergy * 0.18f),
                    colors.secondary.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = haloR.coerceAtLeast(10f)
            ),
            radius = haloR,
            center = Offset(cx, cy)
        )

        drawCircle(
            color = HairlineBorder.copy(alpha = 0.5f),
            radius = innerR + 4f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )

        val totalSpokes = 64
        val bands = frame.spectrumBands
        for (i in 0 until totalSpokes) {
            val bandIdx = if (i < 32) i else (63 - i)
            val mag = bands[bandIdx].coerceIn(0f, 1f)
            val angle = (2.0 * PI * i / totalSpokes) - PI / 2.0
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            val startR = innerR + 6f
            val endR = startR + (mag * maxSpoke).coerceAtLeast(5f)

            val t = bandIdx / 31f
            val spokeColor = when {
                t < 0.45f -> colors.primary
                t < 0.8f -> colors.secondary
                else -> colors.tertiary
            }

            drawLine(
                color = spokeColor,
                start = Offset(cx + cosA * startR, cy + sinA * startR),
                end = Offset(cx + cosA * endR, cy + sinA * endR),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun WaveformOscilloscopeCanvas(
    frame: VisualizerFrame,
    colors: VisualizerPaletteColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h * 0.5f

        drawLine(
            color = HairlineBorder.copy(alpha = 0.55f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.5f
        )
        for (d in 1..3) {
            val x = w * (d / 4f)
            drawLine(
                color = HairlineBorder.copy(alpha = 0.25f),
                start = Offset(x, 8f),
                end = Offset(x, h - 8f),
                strokeWidth = 1f
            )
        }

        val samples = frame.waveform
        val count = samples.size
        if (count < 2) return@Canvas

        val path = Path()
        val stepX = w / (count - 1)
        val amp = h * 0.42f

        for (i in 0 until count) {
            val x = i * stepX
            val y = midY - samples[i].coerceIn(-1f, 1f) * amp
            if (i == 0) path.moveTo(x, y) else {
                val prevX = (i - 1) * stepX
                val prevY = midY - samples[i - 1].coerceIn(-1f, 1f) * amp
                val ctrlX = (prevX + x) * 0.5f
                path.quadraticTo(prevX, prevY, ctrlX, (prevY + y) * 0.5f)
            }
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                listOf(
                    colors.secondary.copy(alpha = 0.45f),
                    colors.primary.copy(alpha = 0.55f),
                    colors.tertiary.copy(alpha = 0.45f)
                )
            ),
            style = Stroke(width = 11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(colors.secondary, colors.primary, colors.peakCap, colors.tertiary)),
            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Interactive visual radar for Virtualizer & 5.1 / 7.1 Surround speaker simulation.
 */
@Composable
fun SurroundStageCanvas(
    surroundMode: SurroundMode,
    virtualizerLevel: Float,
    stereoWidth: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val radius = min(size.width, size.height) * 0.40f

        drawCircle(
            color = HairlineBorder.copy(alpha = 0.45f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = HairlineBorder.copy(alpha = 0.3f),
            radius = radius * 0.6f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )

        val fieldRadius = radius * (0.45f + virtualizerLevel * 0.52f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ElectricCyan.copy(alpha = 0.22f + virtualizerLevel * 0.2f),
                    CyberViolet.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = fieldRadius.coerceAtLeast(8f)
            ),
            radius = fieldRadius,
            center = Offset(cx, cy)
        )

        val spreadHalfW = radius * (0.25f + (stereoWidth / 2f) * 0.72f)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(CyberViolet, ElectricCyan, CyberViolet),
                startX = cx - spreadHalfW,
                endX = cx + spreadHalfW
            ),
            start = Offset(cx - spreadHalfW, cy),
            end = Offset(cx + spreadHalfW, cy),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        drawCircle(color = ObsidianBase, radius = 10f, center = Offset(cx, cy))
        drawCircle(color = IceWhite, radius = 10f, center = Offset(cx, cy), style = Stroke(width = 2.5f))

        val anglesDeg = when (surroundMode) {
            SurroundMode.OFF -> listOf(-35f, 35f)
            SurroundMode.SURROUND_5_1 -> listOf(-35f, 0f, 35f, -125f, 125f)
            SurroundMode.SURROUND_7_1 -> listOf(-35f, 0f, 35f, -90f, 90f, -140f, 140f)
        }
        anglesDeg.forEach { deg ->
            val rad = Math.toRadians((deg - 90f).toDouble())
            val sx = cx + cos(rad).toFloat() * radius
            val sy = cy + sin(rad).toFloat() * radius
            val nodeColor = if (deg == 0f) LaserMint else if (kotlin.math.abs(deg) > 60f) CyberViolet else ElectricCyan
            drawCircle(color = nodeColor.copy(alpha = 0.35f), radius = 11f, center = Offset(sx, sy))
            drawCircle(color = nodeColor, radius = 5.5f, center = Offset(sx, sy))
        }
    }
}
