package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.data.EQ_BAND_LABELS
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LaserMint
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.RackWell
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TelemetrySlate

@Composable
fun EqCurvePreviewCanvas(
    bandGainsDb: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h * 0.5f

        // Zero dB center reference line & ±6 dB guides
        drawLine(
            color = HairlineBorder.copy(alpha = 0.7f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.5f
        )
        drawLine(
            color = HairlineBorder.copy(alpha = 0.3f),
            start = Offset(0f, h * 0.25f),
            end = Offset(w, h * 0.25f),
            strokeWidth = 1f
        )
        drawLine(
            color = HairlineBorder.copy(alpha = 0.3f),
            start = Offset(0f, h * 0.75f),
            end = Offset(w, h * 0.75f),
            strokeWidth = 1f
        )

        val count = bandGainsDb.size.coerceAtLeast(2)
        val stepX = w / (count - 1)
        val points = List(count) { i ->
            val gain = bandGainsDb.getOrElse(i) { 0f }.coerceIn(-12f, 12f)
            val norm = gain / 12f
            Offset(i * stepX, midY - norm * (h * 0.42f))
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val midX = (p0.x + p1.x) * 0.5f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(points.last().x, midY)
            lineTo(points.first().x, midY)
            close()
        }

        val strokeColor = if (enabled) ElectricCyan else TelemetrySlate
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(strokeColor.copy(alpha = 0.28f), CyberViolet.copy(alpha = 0.08f))
            )
        )
        drawPath(
            path = curvePath,
            brush = Brush.horizontalGradient(
                if (enabled) listOf(LaserMint, ElectricCyan, CyberViolet)
                else listOf(TelemetrySlate, TelemetrySlate)
            ),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEach { pt ->
            drawCircle(color = strokeColor, radius = 4f, center = pt)
        }
    }
}

@Composable
fun VerticalEqBandSlider(
    bandIndex: Int,
    gainDb: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = EQ_BAND_LABELS.getOrElse(bandIndex) { "" }
    Column(
        modifier = modifier.width(34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = String.format("%+.0f", gainDb),
            style = MaterialTheme.typography.labelSmall,
            color = if (!enabled) TelemetrySlate else if (gainDb > 0.4f) ElectricCyan else if (gainDb < -0.4f) CyberViolet else IceWhite
        )

        Box(
            modifier = Modifier
                .height(144.dp)
                .width(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = gainDb,
                onValueChange = onGainChange,
                valueRange = -12f..12f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = if (enabled) ElectricCyan else TelemetrySlate,
                    activeTrackColor = if (enabled) ElectricCyan else TelemetrySlate,
                    inactiveTrackColor = RackWell
                ),
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxHeight
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(-placeable.width / 2 + placeable.height / 2, -placeable.height / 2 + placeable.width / 2)
                        }
                    }
                    .width(140.dp)
                    .height(34.dp)
                    .testTag("eq_band_slider_$bandIndex")
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TelemetrySlate
        )
    }
}

@Composable
fun LabeledFxSlider(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color = ElectricCyan,
    testTagName: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = IceWhite)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) accentColor else TelemetrySlate
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = RackWell
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTagName)
        )
    }
}

@Composable
fun CompressorGainReductionMeter(
    gainReductionDb: Float,
    modifier: Modifier = Modifier
) {
    val segments = 16
    val litCount = ((gainReductionDb / 18f).coerceIn(0f, 1f) * segments).toInt()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBase)
            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("GAIN REDUCTION (GR)", style = MaterialTheme.typography.labelSmall, color = TelemetrySlate)
            Text(
                text = String.format("-%.1f dB", gainReductionDb),
                style = MaterialTheme.typography.labelSmall,
                color = if (gainReductionDb > 8f) SignalRed else LaserMint
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until segments) {
                val active = i < litCount
                val segColor = when {
                    i >= 12 -> SignalRed
                    i >= 8 -> CyberViolet
                    else -> LaserMint
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (active) segColor else RackWell)
                )
            }
        }
    }
}

@Composable
fun QuickPillButton(
    text: String,
    selected: Boolean,
    accent: Color = ElectricCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else ObsidianBase)
            .border(1.dp, if (selected) accent else HairlineBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else IceWhite
        )
    }
}
