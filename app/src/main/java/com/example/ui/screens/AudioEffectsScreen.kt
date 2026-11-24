package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AudioEffectsConfig
import com.example.data.BuiltInModeProfiles
import com.example.data.GenreEqPreset
import com.example.data.HardwareAudioDiagnostics
import com.example.data.ListeningGear
import com.example.data.ListeningMode
import com.example.data.SignalTelemetry
import com.example.data.SurroundMode
import com.example.data.VisualizerFrame
import com.example.data.VisualizerPalette
import com.example.data.local.AudioEffectPresetEntity
import com.example.ui.components.CyberPanelCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SurroundStageCanvas
import com.example.ui.components.formatCountdown
import com.example.ui.components.paletteColorsFor
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LaserMint
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RackWell
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TelemetrySlate
import com.example.viewmodel.AiEqUiState
import kotlinx.coroutines.launch

/**
 * Single unified "Audio Effects" hub containing:
 * Listening Modes (Party, Ambient, Trip, Sleep + Timer, Cinema, Studio),
 * 10-Band Graphic Equalizer + File Manager .JSON Import / Export,
 * Online AI Equalizer Studio (Firebase AI Logic),
 * Virtualizer & 5.1/7.1 Surround + Stereoizer,
 * Bass/Treble Boosters, Dynamics Compressor + live GR meter, and MP4 Digital Passthrough.
 */
@Composable
fun AudioEffectsScreen(
    config: AudioEffectsConfig,
    savedPresets: List<AudioEffectPresetEntity>,
    visualizerFrame: VisualizerFrame,
    signalTelemetry: SignalTelemetry,
    hardwareDiagnostics: HardwareAudioDiagnostics,
    aiEqState: AiEqUiState,
    selectedGear: ListeningGear,
    sleepTimerRemainingSec: Int,
    onToggleMasterEnabled: (Boolean) -> Unit,
    onSelectMode: (ListeningMode) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSetBandGain: (Int, Float) -> Unit,
    onSetPreampDb: (Float) -> Unit,
    onResetFlat: () -> Unit,
    onImportEqJson: () -> Unit,
    onExportEqJson: () -> Unit,
    onSavePreset: (String) -> Unit,
    onApplySavedPreset: (AudioEffectPresetEntity) -> Unit,
    onDeleteSavedPreset: (Long) -> Unit,
    onSelectGear: (ListeningGear) -> Unit,
    onGenerateAiEq: (String?) -> Unit,
    onToggleAiAbCompare: () -> Unit,
    onSetVirtualizer: (Float) -> Unit,
    onSetSurroundMode: (SurroundMode) -> Unit,
    onSetStereoWidth: (Float) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetTrebleBoost: (Float) -> Unit,
    onSetCompressorEnabled: (Boolean) -> Unit,
    onSetCompressorParams: (Float, Float, Float) -> Unit,
    onSetDigitalPassthrough: (Boolean) -> Unit,
    visualizerPalette: VisualizerPalette = VisualizerPalette.CYBER_NEON,
    onSelectVisualizerPalette: (VisualizerPalette) -> Unit = {},
    hapticBeatSyncEnabled: Boolean = false,
    hapticIntensity: Float = 0.7f,
    hasVibratorHardware: Boolean = true,
    hasAmplitudeControl: Boolean = false,
    hapticPulseStrength: Float = 0f,
    onToggleHapticBeatSync: (Boolean) -> Unit = {},
    onSetHapticIntensity: (Float) -> Unit = {},
    onApplyGenrePreset: (GenreEqPreset) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var genreMenuExpanded by remember { mutableStateOf(false) }
    var aiPromptText by remember { mutableStateOf("") }

    val dspInteractive = config.enabled && !config.digitalPassthroughEnabled

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_effects_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0: Unified Master Header + Section Jump Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(ObsidianElevated, ObsidianSurface))
                        )
                        .border(
                            1.dp,
                            if (config.enabled) ElectricCyan.copy(alpha = 0.6f) else HairlineBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UNIFIED AUDIO EFFECTS", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                            Text(
                                text = if (!config.enabled) "Bypassed (Bit-Perfect)" else config.activePresetName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = IceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = signalTelemetry.passthroughStatusNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = TelemetrySlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = config.enabled,
                            onCheckedChange = onToggleMasterEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepVoid,
                                checkedTrackColor = ElectricCyan,
                                uncheckedTrackColor = RackWell
                            ),
                            modifier = Modifier.testTag("effects_master_switch")
                        )
                    }
                }

                // Quick Jump Chips within Single Audio Effects Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPillButton("Modes", false, ElectricCyan, { scope.launch { listState.animateScrollToItem(1) } })
                    QuickPillButton("10-Band EQ", false, ElectricCyan, { scope.launch { listState.animateScrollToItem(2) } })
                    QuickPillButton("AI Equalizer", false, CyberViolet, { scope.launch { listState.animateScrollToItem(3) } })
                    QuickPillButton("Surround & Virt.", false, LaserMint, { scope.launch { listState.animateScrollToItem(4) } })
                    QuickPillButton("FX & Passthrough", false, AmberPulse, { scope.launch { listState.animateScrollToItem(5) } })
                    QuickPillButton("Visualizer", false, ElectricCyan, { scope.launch { listState.animateScrollToItem(6) } })
                }
            }
        }

        // 1: Listening Modes (Party, Ambient, Trip, Sleep + Timer, Cinema, Studio)
        item {
            CyberPanelCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeaderRow(
                        title = "Listening Modes",
                        subtitle = "One-tap acoustic scene profiles"
                    )

                    val rows = BuiltInModeProfiles.curatedModes.chunked(2)
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { mode ->
                                val active = config.enabled && config.activeMode == mode
                                ModeTileCard(
                                    mode = mode,
                                    active = active,
                                    onClick = { onSelectMode(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Inline Sleep Timer whenever Sleep Mode is selected OR timer is running
                    if (config.activeMode == ListeningMode.SLEEP || sleepTimerRemainingSec > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberViolet.copy(alpha = 0.12f))
                                .border(1.dp, CyberViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sleep Fade-Out Timer", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                }
                                Text(
                                    text = if (sleepTimerRemainingSec > 0) formatCountdown(sleepTimerRemainingSec) else "OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sleepTimerRemainingSec > 0) ElectricCyan else TelemetrySlate
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (mins, label) ->
                                    val selected = if (mins == 0) sleepTimerRemainingSec == 0 else (sleepTimerRemainingSec > 0 && ((sleepTimerRemainingSec + 59) / 60) in (mins - 2)..mins)
                                    QuickPillButton(
                                        text = label,
                                        selected = selected,
                                        accent = CyberViolet,
                                        onClick = { onSetSleepTimer(mins) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2: 10-Band Graphic Equalizer + Direct "Import" (.JSON) / Export / Save Controls
        item {
            CyberPanelCard(borderColor = if (dspInteractive) ElectricCyan.copy(alpha = 0.45f) else HairlineBorder) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeaderRow(
                        title = "10-Band Graphic Equalizer",
                        subtitle = "31 Hz – 16 kHz • ±12 dB • .JSON File Support"
                    )

                    // Prominent Equalizer Action Row featuring the requested 'Import' button for .json files
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onImportEqJson,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = DeepVoid
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("eq_import_json_button")
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }

                        OutlinedButton(
                            onClick = onExportEqJson,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eq_export_json_button")
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = IceWhite)
                        }

                        OutlinedButton(
                            onClick = { showSavePresetDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberViolet.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("eq_save_preset_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = IceWhite)
                        }

                        IconButton(
                            onClick = onResetFlat,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBase)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                .testTag("eq_reset_flat_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset Flat", tint = TelemetrySlate, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Genre Equalization Presets Dropdown Menu & Quick-Apply Chips
                    val matchedGenre = remember(config.activePresetName) {
                        GenreEqPreset.entries.firstOrNull { preset ->
                            config.activePresetName.equals("${preset.displayName} EQ", ignoreCase = true) ||
                                (preset == GenreEqPreset.FLAT && config.activePresetName.equals("Studio Flat", ignoreCase = true))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianBase)
                                    .border(
                                        1.dp,
                                        if (matchedGenre != null && matchedGenre != GenreEqPreset.FLAT) ElectricCyan.copy(alpha = 0.65f) else HairlineBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = dspInteractive) { genreMenuExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("eq_genre_menu_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "GENRE EQ PRESET",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TelemetrySlate
                                        )
                                        Text(
                                            text = matchedGenre?.displayName ?: "Select Genre (Rock, Jazz, Classical, Electronic...)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = IceWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Open genre EQ presets menu",
                                    tint = ElectricCyan
                                )
                            }

                            DropdownMenu(
                                expanded = genreMenuExpanded,
                                onDismissRequest = { genreMenuExpanded = false },
                                containerColor = ObsidianElevated,
                                modifier = Modifier
                                    .fillMaxWidth(0.84f)
                                    .testTag("eq_genre_dropdown_menu")
                            ) {
                                GenreEqPreset.entries.forEach { preset ->
                                    val isSelected = matchedGenre == preset
                                    DropdownMenuItem(
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(
                                                    text = preset.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (isSelected) ElectricCyan else IceWhite
                                                )
                                                Text(
                                                    text = preset.tagline,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TelemetrySlate,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Active genre preset",
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            genreMenuExpanded = false
                                            onApplyGenrePreset(preset)
                                        },
                                        modifier = Modifier.testTag("eq_genre_option_${preset.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Quick 1-Tap Genre Presets Strip
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(
                                listOf(
                                    GenreEqPreset.ROCK,
                                    GenreEqPreset.JAZZ,
                                    GenreEqPreset.CLASSICAL,
                                    GenreEqPreset.ELECTRONIC,
                                    GenreEqPreset.POP,
                                    GenreEqPreset.HIP_HOP
                                )
                            ) { preset ->
                                QuickPillButton(
                                    text = preset.displayName,
                                    selected = matchedGenre == preset,
                                    accent = ElectricCyan,
                                    onClick = { if (dspInteractive) onApplyGenrePreset(preset) },
                                    modifier = Modifier.testTag("eq_genre_chip_${preset.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Live Interpolated Frequency Curve Preview
                    EqCurvePreviewCanvas(
                        bandGainsDb = config.bandGainsDb,
                        enabled = dspInteractive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianBase)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    // 10-Band Vertical Faders Rack
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (bandIdx in 0 until 10) {
                            VerticalEqBandSlider(
                                bandIndex = bandIdx,
                                gainDb = config.bandGainsDb.getOrElse(bandIdx) { 0f },
                                enabled = dspInteractive,
                                onGainChange = { onSetBandGain(bandIdx, it) }
                            )
                        }
                    }

                    LabeledFxSlider(
                        title = "Master Preamp",
                        valueLabel = String.format("%+.1f dB", config.preampDb),
                        value = config.preampDb,
                        valueRange = -12f..12f,
                        enabled = dspInteractive,
                        accentColor = ElectricCyan,
                        testTagName = "eq_preamp_slider",
                        onValueChange = onSetPreampDb
                    )

                    // Saved Offline & Imported .JSON Presets Strip
                    if (savedPresets.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SAVED OFFLINE & .JSON PRESETS", style = MaterialTheme.typography.labelSmall, color = LaserMint)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(savedPresets, key = { it.id }) { preset ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(RackWell)
                                            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                            .clickable { onApplySavedPreset(preset) }
                                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(preset.name, style = MaterialTheme.typography.labelMedium, color = IceWhite)
                                        IconButton(
                                            onClick = { onDeleteSavedPreset(preset.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Delete preset", tint = TelemetrySlate, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3: Online AI Equalizer Studio (Firebase AI Logic - Internet)
        item {
            CyberPanelCard(borderColor = CyberViolet.copy(alpha = 0.65f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("AI-Powered Equalizer", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                            Text("Online acoustic tuning via Gemini AI", style = MaterialTheme.typography.bodySmall, color = TelemetrySlate)
                        }
                    }

                    // Listening Hardware Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ListeningGear.entries) { gear ->
                            QuickPillButton(
                                text = gear.label,
                                selected = selectedGear == gear,
                                accent = CyberViolet,
                                onClick = { onSelectGear(gear) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = aiPromptText,
                        onValueChange = { aiPromptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_eq_prompt_input"),
                        placeholder = { Text("Optional: e.g. 'Deep warm club bass with holographic 7.1 vocals'", color = TelemetrySlate) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberViolet,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = ObsidianBase,
                            unfocusedContainerColor = ObsidianBase,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        )
                    )

                    Button(
                        onClick = { onGenerateAiEq(aiPromptText.takeIf { it.isNotBlank() }) },
                        enabled = aiEqState !is AiEqUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet, contentColor = IceWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_eq_generate_button")
                    ) {
                        if (aiEqState is AiEqUiState.Loading) {
                            CircularProgressIndicator(
                                color = IceWhite,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Engineering AI Sound Curve...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (aiPromptText.isBlank()) "Auto-Tune Active Track Online" else "Generate Custom AI EQ")
                        }
                    }

                    when (aiEqState) {
                        is AiEqUiState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberViolet.copy(alpha = 0.14f))
                                    .border(1.dp, CyberViolet.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${aiEqState.config.activePresetName} ${if (aiEqState.comparingPrevious) "(A: Previous)" else "(B: AI Active)"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = aiEqState.config.aiRationale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IceWhite
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onToggleAiAbCompare,
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Rounded.CompareArrows, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("A/B Compare", color = IceWhite)
                                    }
                                    Button(
                                        onClick = { onSavePreset(aiEqState.config.activePresetName) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LaserMint, contentColor = DeepVoid),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save Offline")
                                    }
                                }
                            }
                        }

                        is AiEqUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SignalRed.copy(alpha = 0.14f))
                                    .border(1.dp, SignalRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiEqState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IceWhite
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }

        // 4: Virtualizer, 5.1/7.1 Surround Simulation & Stereoizer
        item {
            CyberPanelCard(borderColor = LaserMint.copy(alpha = 0.45f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeaderRow(
                        title = "Virtualizer, Surround & Stereoizer",
                        subtitle = "Spatial headstage & 5.1 / 7.1 HRTF matrix"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SurroundStageCanvas(
                            surroundMode = config.surroundMode,
                            virtualizerLevel = config.virtualizerLevel,
                            stereoWidth = config.stereoWidth,
                            modifier = Modifier
                                .size(116.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBase)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("SURROUND SIMULATION", style = MaterialTheme.typography.labelSmall, color = LaserMint)
                            SurroundMode.entries.forEach { sMode ->
                                QuickPillButton(
                                    text = "${sMode.label} (${sMode.channelsLabel})",
                                    selected = config.surroundMode == sMode,
                                    accent = LaserMint,
                                    onClick = { onSetSurroundMode(sMode) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("surround_mode_${sMode.name.lowercase()}")
                                )
                            }
                        }
                    }

                    LabeledFxSlider(
                        title = "3D Virtualizer Depth",
                        valueLabel = "${(config.virtualizerLevel * 100).toInt()}%",
                        value = config.virtualizerLevel,
                        valueRange = 0f..1f,
                        enabled = dspInteractive,
                        accentColor = LaserMint,
                        testTagName = "virtualizer_slider",
                        onValueChange = onSetVirtualizer
                    )

                    LabeledFxSlider(
                        title = "Stereoizer (M/S Width)",
                        valueLabel = "${(config.stereoWidth * 100).toInt()}%",
                        value = config.stereoWidth,
                        valueRange = 0f..2f,
                        enabled = dspInteractive,
                        accentColor = ElectricCyan,
                        testTagName = "stereoizer_slider",
                        onValueChange = onSetStereoWidth
                    )
                }
            }
        }

        // 5: Modular FX Rack (Bass & Treble Boosters, Dynamics Compressor, MP4 Digital Passthrough)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Bass & Treble Boosters
                CyberPanelCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeaderRow(
                            title = "Bass & Treble Boosters",
                            subtitle = "Analog low-shelf (85 Hz) & air high-shelf (9.5 kHz)"
                        )
                        LabeledFxSlider(
                            title = "Bass Booster",
                            valueLabel = String.format("+%.1f dB", config.bassBoost * 10f),
                            value = config.bassBoost,
                            valueRange = 0f..1f,
                            enabled = dspInteractive,
                            accentColor = ElectricCyan,
                            testTagName = "bass_booster_slider",
                            onValueChange = onSetBassBoost
                        )
                        LabeledFxSlider(
                            title = "Treble Booster",
                            valueLabel = String.format("+%.1f dB", config.trebleBoost * 9f),
                            value = config.trebleBoost,
                            valueRange = 0f..1f,
                            enabled = dspInteractive,
                            accentColor = CyberViolet,
                            testTagName = "treble_booster_slider",
                            onValueChange = onSetTrebleBoost
                        )
                    }
                }

                // Dynamics Compressor
                CyberPanelCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dynamics Compressor", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                                Text("Soft-knee mastering leveler & punch", style = MaterialTheme.typography.bodySmall, color = TelemetrySlate)
                            }
                            Switch(
                                checked = config.compressorEnabled,
                                onCheckedChange = onSetCompressorEnabled,
                                enabled = dspInteractive,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DeepVoid,
                                    checkedTrackColor = AmberPulse,
                                    uncheckedTrackColor = RackWell
                                ),
                                modifier = Modifier.testTag("compressor_switch")
                            )
                        }

                        CompressorGainReductionMeter(gainReductionDb = visualizerFrame.gainReductionDb)

                        val compControlsEnabled = dspInteractive && config.compressorEnabled
                        LabeledFxSlider(
                            title = "Threshold",
                            valueLabel = String.format("%.0f dB", config.compressorThresholdDb),
                            value = config.compressorThresholdDb,
                            valueRange = -40f..0f,
                            enabled = compControlsEnabled,
                            accentColor = AmberPulse,
                            testTagName = "compressor_threshold_slider",
                            onValueChange = { onSetCompressorParams(it, config.compressorRatio, config.compressorMakeupDb) }
                        )
                        LabeledFxSlider(
                            title = "Ratio",
                            valueLabel = String.format("%.1f : 1", config.compressorRatio),
                            value = config.compressorRatio,
                            valueRange = 1f..12f,
                            enabled = compControlsEnabled,
                            accentColor = AmberPulse,
                            testTagName = "compressor_ratio_slider",
                            onValueChange = { onSetCompressorParams(config.compressorThresholdDb, it, config.compressorMakeupDb) }
                        )
                        LabeledFxSlider(
                            title = "Makeup Gain",
                            valueLabel = String.format("+%.1f dB", config.compressorMakeupDb),
                            value = config.compressorMakeupDb,
                            valueRange = 0f..12f,
                            enabled = compControlsEnabled,
                            accentColor = LaserMint,
                            testTagName = "compressor_makeup_slider",
                            onValueChange = { onSetCompressorParams(config.compressorThresholdDb, config.compressorRatio, it) }
                        )
                    }
                }

                // MP4 / E-AC-3 / AC-4 Digital Audio Passthrough
                CyberPanelCard(
                    borderColor = if (config.digitalPassthroughEnabled) LaserMint.copy(alpha = 0.7f) else HairlineBorder
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = LaserMint, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("MP4 Digital Passthrough", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                                    Text("Direct E-AC-3 / AC-4 bitstream routing", style = MaterialTheme.typography.bodySmall, color = TelemetrySlate)
                                }
                            }
                            Switch(
                                checked = config.digitalPassthroughEnabled,
                                onCheckedChange = onSetDigitalPassthrough,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DeepVoid,
                                    checkedTrackColor = LaserMint,
                                    uncheckedTrackColor = RackWell
                                ),
                                modifier = Modifier.testTag("passthrough_switch")
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBase)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "OUTPUT ROUTE: ${hardwareDiagnostics.outputDeviceSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            Text(
                                text = "E-AC-3: ${if (hardwareDiagnostics.supportsEac3Passthrough) "HW PASSTHROUGH" else "DECODE PCM"}  •  AC-4: ${if (hardwareDiagnostics.supportsAc4Passthrough) "HW PASSTHROUGH" else "DECODE PCM"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TelemetrySlate
                            )
                        }
                    }
                }
            }
        }

        // 6: FFT Visualizer Color Palette Selector
        item {
            CyberPanelCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeaderRow(
                        title = "Visualizer Color Palette",
                        subtitle = "Theme the real-time FFT spectrum, radial ring & waveform"
                    )
                    val paletteRows = VisualizerPalette.entries.chunked(2)
                    paletteRows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { pal ->
                                val selected = visualizerPalette == pal
                                val palColors = paletteColorsFor(pal)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) palColors.primary.copy(alpha = 0.16f) else ObsidianBase)
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) palColors.primary else HairlineBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectVisualizerPalette(pal) }
                                        .padding(12.dp)
                                        .testTag("visualizer_palette_${pal.name.lowercase()}"),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(palColors.primary, palColors.secondary, palColors.tertiary)
                                                )
                                            )
                                    )
                                    Text(
                                        text = pal.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) palColors.primary else IceWhite
                                    )
                                    Text(
                                        text = pal.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TelemetrySlate,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Beat-Synced Haptic Motor Link
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianBase)
                            .border(
                                width = 1.dp,
                                color = if (hapticBeatSyncEnabled) CyberViolet.copy(alpha = 0.75f) else HairlineBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (hapticBeatSyncEnabled) {
                                                CyberViolet.copy(alpha = 0.22f + hapticPulseStrength * 0.45f)
                                            } else {
                                                RackWell
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Vibration,
                                        contentDescription = "Beat-synced haptics",
                                        tint = if (hapticBeatSyncEnabled) ElectricCyan else TelemetrySlate,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Beat-Synced Haptic Motor",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IceWhite
                                    )
                                    Text(
                                        text = when {
                                            !hasVibratorHardware -> "No vibration motor detected on device"
                                            hapticBeatSyncEnabled && hasAmplitudeControl ->
                                                "Linked to FFT bass peaks • Variable amplitude"
                                            hapticBeatSyncEnabled ->
                                                "Linked to FFT bass peaks • Pulse active"
                                            else -> "Sync device vibration to visualizer beats"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TelemetrySlate
                                    )
                                }
                            }
                            Switch(
                                checked = hapticBeatSyncEnabled,
                                onCheckedChange = onToggleHapticBeatSync,
                                enabled = hasVibratorHardware,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DeepVoid,
                                    checkedTrackColor = CyberViolet,
                                    uncheckedTrackColor = RackWell
                                ),
                                modifier = Modifier.testTag("haptic_beat_sync_switch")
                            )
                        }

                        LabeledFxSlider(
                            title = "Haptic Beat Intensity",
                            valueLabel = "${(hapticIntensity * 100).toInt()}%",
                            value = hapticIntensity,
                            valueRange = 0.1f..1f,
                            enabled = hasVibratorHardware && hapticBeatSyncEnabled,
                            accentColor = CyberViolet,
                            testTagName = "haptic_intensity_slider",
                            onValueChange = onSetHapticIntensity
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(86.dp)) }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf(config.activePresetName) }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = ObsidianElevated,
            title = { Text("Save Offline Audio Effects Preset", color = IceWhite) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("save_preset_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePreset(presetName)
                        showSavePresetDialog = false
                    },
                    enabled = presetName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepVoid)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ModeTileCard(
    mode: ListeningMode,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (mode) {
        ListeningMode.PARTY -> Icons.Rounded.Celebration
        ListeningMode.AMBIENT -> Icons.Rounded.Spa
        ListeningMode.TRIP -> Icons.Rounded.DirectionsCar
        ListeningMode.SLEEP -> Icons.Rounded.Bedtime
        ListeningMode.CINEMA -> Icons.Rounded.Movie
        else -> Icons.Rounded.Tune
    }
    val accent = when (mode) {
        ListeningMode.PARTY -> ElectricCyan
        ListeningMode.AMBIENT -> LaserMint
        ListeningMode.TRIP -> AmberPulse
        ListeningMode.SLEEP -> CyberViolet
        ListeningMode.CINEMA -> ElectricCyan
        else -> IceWhite
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) accent.copy(alpha = 0.16f) else ObsidianBase)
            .border(1.dp, if (active) accent else HairlineBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("effects_mode_${mode.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (active) accent else TelemetrySlate, modifier = Modifier.size(20.dp))
            if (active) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = "Active", tint = accent, modifier = Modifier.size(16.dp))
            }
        }
        Text(mode.displayName, style = MaterialTheme.typography.titleSmall, color = if (active) accent else IceWhite)
        Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = TelemetrySlate, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
