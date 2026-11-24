package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorMixCyanContainer = androidx.compose.ui.graphics.Color(0xFF082E3A)
private val ColorMixVioletContainer = androidx.compose.ui.graphics.Color(0xFF281242)
private val ColorMixMintContainer = androidx.compose.ui.graphics.Color(0xFF0A3327)

private val NeonCyberDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepVoid,
    primaryContainer = ColorMixCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = CyberViolet,
    onSecondary = IceWhite,
    secondaryContainer = ColorMixVioletContainer,
    onSecondaryContainer = IceWhite,
    tertiary = LaserMint,
    onTertiary = DeepVoid,
    tertiaryContainer = ColorMixMintContainer,
    onTertiaryContainer = LaserMint,
    background = DeepVoid,
    onBackground = IceWhite,
    surface = ObsidianBase,
    onSurface = IceWhite,
    surfaceVariant = RackWell,
    onSurfaceVariant = TelemetrySlate,
    surfaceContainerLowest = DeepVoid,
    surfaceContainerLow = ObsidianBase,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = RackWell,
    outline = HairlineBorder,
    outlineVariant = MutedSlate,
    error = SignalRed,
    onError = IceWhite
)

val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulseDspTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonCyberDarkColorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content
    )
}
