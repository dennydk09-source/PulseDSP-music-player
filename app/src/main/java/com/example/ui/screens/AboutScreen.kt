package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.HardwareAudioDiagnostics
import com.example.ui.components.CodecBadge
import com.example.ui.components.CyberPanelCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LaserMint
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.TelemetrySlate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    hardwareDiagnostics: HardwareAudioDiagnostics,
    trackCount: Int,
    playlistCount: Int,
    presetCount: Int,
    lockscreenNotificationGranted: Boolean,
    onEnableLockscreenNotification: () -> Unit,
    onEnableQuickSettingsPlayTile: () -> Unit,
    onExportEqJson: () -> Unit,
    onImportEqJson: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Card
        item {
            CyberPanelCard(borderColor = ElectricCyan.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pulsedsp_logo_1795497442908),
                        contentDescription = "PulseDSP logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("PulseDSP", style = MaterialTheme.typography.headlineSmall, color = IceWhite)
                        Text("v1.0 • 32-bit Float Audiophile Engine", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                        Text(
                            "Offline high-res music player with unified Audio Effects, real-time 60 FPS FFT visualizer, background play, and online Gemini AI tuning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetrySlate
                        )
                    }
                }
            }
        }

        // Lockscreen Player, Status Bar Controls & Quick Settings PLAY Button Card
        item {
            CyberPanelCard(borderColor = LaserMint.copy(alpha = 0.6f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderRow(
                        title = "Lockscreen, Status Bar & Quick Settings",
                        subtitle = "Background play & 1-tap PLAY tile on your phone shade"
                    )
                    DiagnosticRow("Background Playback Service", true)
                    DiagnosticRow("Lockscreen & Status Bar Media Player", lockscreenNotificationGranted)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = onEnableQuickSettingsPlayTile,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = com.example.ui.theme.DeepVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("enable_qs_play_tile_button")
                        ) {
                            Text("Enable Quick Settings PLAY")
                        }
                    }

                    if (!lockscreenNotificationGranted) {
                        OutlinedButton(
                            onClick = onEnableLockscreenNotification,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LaserMint.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("enable_lockscreen_notif_button")
                        ) {
                            Text("Allow Lockscreen & Status Bar Player Notification", color = IceWhite)
                        }
                    }
                }
            }
        }

        // Offline Library Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChipCard("TRACKS", "$trackCount", ElectricCyan, Modifier.weight(1f))
                StatChipCard("PLAYLISTS", "$playlistCount", CyberViolet, Modifier.weight(1f))
                StatChipCard("EQ PRESETS", "$presetCount", LaserMint, Modifier.weight(1f))
            }
        }

        // Hardware Passthrough & Audio Capabilities Diagnostics
        item {
            CyberPanelCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderRow(
                        title = "Hardware Audio Capabilities",
                        subtitle = hardwareDiagnostics.outputDeviceSummary
                    )
                    DiagnosticRow("32-bit Float PCM Pipeline", hardwareDiagnostics.supportsPcmFloat)
                    DiagnosticRow("Dolby Digital Plus (E-AC-3) Passthrough", hardwareDiagnostics.supportsEac3Passthrough)
                    DiagnosticRow("Dolby Atmos (E-AC-3 JOC) Passthrough", hardwareDiagnostics.supportsEac3JocPassthrough)
                    DiagnosticRow("Dolby AC-4 Passthrough", hardwareDiagnostics.supportsAc4Passthrough)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Hardware Output Channels", style = MaterialTheme.typography.bodyMedium, color = TelemetrySlate)
                        Text("${hardwareDiagnostics.maxChannelCount} ch", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                    }
                }
            }
        }

        // Supported High-Resolution Audio Formats
        item {
            CyberPanelCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeaderRow(
                        title = "Supported Audio Formats",
                        subtitle = "Media3 ExoPlayer + MP4 Digital Passthrough"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("FLAC", "WAV", "MP3", "OGG", "OPUS", "M4A", "AAC", "EAC3", "AC4", "MP4").forEach { fmt ->
                            CodecBadge(codecLabel = fmt)
                        }
                    }
                }
            }
        }

        // Real-Time DSP Architecture Summary
        item {
            CyberPanelCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionHeaderRow(
                        title = "DSP Signal Chain",
                        subtitle = "Active in Unified Audio Effects"
                    )
                    val chainSteps = listOf(
                        "1. Master Preamp & Headroom Compensation",
                        "2. 10-Band Peaking Graphic EQ (31 Hz – 16 kHz, ±12 dB)",
                        "3. Analog Low-Shelf Bass & High-Shelf Treble Boosters",
                        "4. Mid/Side Stereoizer (0% Mono – 200% Ultra-Wide)",
                        "5. Virtualizer & 5.1 / 7.1 HRTF Surround Spatializer",
                        "6. Soft-Knee Dynamics Compressor with live GR meter",
                        "7. Transparent Soft-Clip Limiter + 512-pt Radix-2 FFT"
                    )
                    chainSteps.forEach { step ->
                        Text(step, style = MaterialTheme.typography.bodySmall, color = TelemetrySlate)
                    }
                }
            }
        }

        // .JSON EQ File Schema Guide & Quick Import/Export
        item {
            CyberPanelCard(borderColor = CyberViolet.copy(alpha = 0.5f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DataObject, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(".JSON Equalizer Files", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                    }
                    Text(
                        "Share or edit EQ profiles in any file manager. Tap 'Import' on the Equalizer or open any .json file from the Files tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TelemetrySlate
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBase)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "{\n  \"name\": \"Midnight Bass\",\n  \"bandsDb\": [5.5, 4.0, 2.0, 0, -1, 0, 1.5, 3, 4, 3.5],\n  \"fx\": { \"bassBoost\": 0.5, \"surroundMode\": \"SURROUND_7_1\" }\n}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LaserMint
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onImportEqJson,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import .JSON", color = IceWhite)
                        }
                        OutlinedButton(
                            onClick = onExportEqJson,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberViolet.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export .JSON", color = IceWhite)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(86.dp)) }
    }
}

@Composable
private fun StatChipCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBase)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TelemetrySlate)
    }
}

@Composable
private fun DiagnosticRow(label: String, supported: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = IceWhite, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = if (supported) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (supported) LaserMint else TelemetrySlate,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (supported) "SUPPORTED" else "PCM FALLBACK",
                style = MaterialTheme.typography.labelSmall,
                color = if (supported) LaserMint else TelemetrySlate
            )
        }
    }
}
