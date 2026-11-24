package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LrcDocument
import com.example.data.LrcLine
import com.example.data.LrcParser
import com.example.data.LrcSearchResult
import com.example.data.LyricsAppearanceConfig
import com.example.data.LyricsTextAlignment
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
import com.example.viewmodel.LyricsSearchUiState
import com.example.viewmodel.LyricsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Synchronized .LRC lyrics viewer with:
 * - Automatic scrolling synced to Media3 playback positionMs
 * - Manual-scroll detection + Re-sync floating pill
 * - Tap-to-seek on every timestamped LRC line
 * - Live Lyrics Appearance settings (Font Size slider + Left/Center/Right text alignment)
 * - Built-in Import .LRC file & Search Online (LRCLIB) actions
 */
@Composable
fun LyricsViewPanel(
    lyricsState: LyricsUiState,
    lyricsSearchState: LyricsSearchUiState,
    appearance: LyricsAppearanceConfig,
    positionMs: Long,
    defaultSearchQuery: String,
    onSeekToLine: (Long) -> Unit,
    onImportLrcFile: () -> Unit,
    onSearchLyrics: (String) -> Unit,
    onSelectSearchedLyrics: (LrcSearchResult) -> Unit,
    onDismissLyricsSearch: () -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetAlignment: (LyricsTextAlignment) -> Unit,
    onResetAppearance: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchDialog by remember { mutableStateOf(false) }
    var showStyleSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_view_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Lyrics Action Header (Source Badge + Style Settings + Import .LRC + Search Online)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                val badgeText = when (lyricsState) {
                    is LyricsUiState.Loading -> "SYNCING LRC..."
                    is LyricsUiState.Loaded -> {
                        val syncTag = if (lyricsState.document.isSynced) "SYNCED" else "PLAIN"
                        "$syncTag • ${lyricsState.document.sourceLabel.uppercase()}"
                    }
                    is LyricsUiState.NotFound -> "NO LRC FOUND"
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lyricsState is LyricsUiState.Loaded && lyricsState.document.isSynced) LaserMint else ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Appearance Settings Toggle (Font Size & Alignment)
                IconButton(
                    onClick = { showStyleSettings = !showStyleSettings },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showStyleSettings) LaserMint.copy(alpha = 0.2f) else ObsidianBase)
                        .border(
                            1.dp,
                            if (showStyleSettings) LaserMint else HairlineBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .testTag("lyrics_appearance_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TextFormat,
                        contentDescription = "Customize lyrics font size and alignment",
                        tint = if (showStyleSettings) LaserMint else IceWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onImportLrcFile,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBase)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .testTag("lyrics_import_lrc_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = "Import .LRC file",
                        tint = ElectricCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showSearchDialog = true
                        if (defaultSearchQuery.isNotBlank()) {
                            onSearchLyrics(defaultSearchQuery)
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBase)
                        .border(1.dp, CyberViolet.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .testTag("lyrics_search_online_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search LRC lyrics online",
                        tint = CyberViolet,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Inline Live-Preview Lyrics Appearance Customization Panel
        AnimatedVisibility(
            visible = showStyleSettings,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LyricsAppearanceSettingsCard(
                appearance = appearance,
                onSetFontSize = onSetFontSize,
                onSetAlignment = onSetAlignment,
                onReset = onResetAppearance
            )
        }

        // Lyrics Body
        when (lyricsState) {
            is LyricsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Looking up synchronized LRC lyrics...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetrySlate
                        )
                    }
                }
            }

            is LyricsUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = null,
                            tint = TelemetrySlate,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = lyricsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetrySlate,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onImportLrcFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = DeepVoid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("lyrics_empty_import_button")
                            ) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import .LRC", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    showSearchDialog = true
                                    if (defaultSearchQuery.isNotBlank()) {
                                        onSearchLyrics(defaultSearchQuery)
                                    }
                                },
                                border = BorderStroke(1.dp, CyberViolet.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("lyrics_empty_search_button")
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search LRC", color = IceWhite, maxLines = 1)
                            }
                        }
                    }
                }
            }

            is LyricsUiState.Loaded -> {
                SynchronizedLyricsList(
                    document = lyricsState.document,
                    appearance = appearance,
                    positionMs = positionMs,
                    onSeekToLine = onSeekToLine,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showSearchDialog) {
        SearchLyricsDialog(
            initialQuery = defaultSearchQuery,
            searchState = lyricsSearchState,
            onSearch = onSearchLyrics,
            onSelectResult = { result ->
                onSelectSearchedLyrics(result)
                showSearchDialog = false
            },
            onDismiss = {
                showSearchDialog = false
                onDismissLyricsSearch()
            }
        )
    }
}

@Composable
private fun LyricsAppearanceSettingsCard(
    appearance: LyricsAppearanceConfig,
    onSetFontSize: (Float) -> Unit,
    onSetAlignment: (LyricsTextAlignment) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBase)
            .border(1.dp, LaserMint.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("lyrics_appearance_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LYRICS READABILITY",
                style = MaterialTheme.typography.labelSmall,
                color = LaserMint
            )
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("lyrics_appearance_reset_button")
            )
        }

        // Font Size Row (- | Slider | + | sp readout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { onSetFontSize((appearance.fontSizeSp - 1f).coerceAtLeast(12f)) },
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                    .testTag("lyrics_font_decrease_button")
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease font size", tint = IceWhite, modifier = Modifier.size(16.dp))
            }

            Slider(
                value = appearance.fontSizeSp,
                onValueChange = onSetFontSize,
                valueRange = 12f..26f,
                colors = SliderDefaults.colors(
                    thumbColor = LaserMint,
                    activeTrackColor = LaserMint,
                    inactiveTrackColor = RackWell
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("lyrics_font_size_slider")
            )

            IconButton(
                onClick = { onSetFontSize((appearance.fontSizeSp + 1f).coerceAtMost(26f)) },
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                    .testTag("lyrics_font_increase_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase font size", tint = IceWhite, modifier = Modifier.size(16.dp))
            }

            Text(
                text = "${appearance.fontSizeSp.roundToInt()}sp",
                style = MaterialTheme.typography.labelSmall,
                color = LaserMint,
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.End
            )
        }

        // Text Alignment Selector (Left | Center | Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LyricsTextAlignment.entries.forEach { align ->
                val selected = appearance.alignment == align
                val alignIcon = when (align) {
                    LyricsTextAlignment.LEFT -> Icons.AutoMirrored.Rounded.FormatAlignLeft
                    LyricsTextAlignment.CENTER -> Icons.Rounded.FormatAlignCenter
                    LyricsTextAlignment.RIGHT -> Icons.AutoMirrored.Rounded.FormatAlignRight
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ElectricCyan.copy(alpha = 0.18f) else ObsidianSurface)
                        .border(
                            1.dp,
                            if (selected) ElectricCyan else HairlineBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSetAlignment(align) }
                        .padding(vertical = 7.dp)
                        .testTag("lyrics_align_${align.name.lowercase()}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = alignIcon,
                        contentDescription = "Align lyrics ${align.label}",
                        tint = if (selected) ElectricCyan else TelemetrySlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = align.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ElectricCyan else TelemetrySlate
                    )
                }
            }
        }
    }
}

@Composable
private fun SynchronizedLyricsList(
    document: LrcDocument,
    appearance: LyricsAppearanceConfig,
    positionMs: Long,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val activeIndex = remember(document.lines, positionMs) {
        if (document.isSynced) LrcParser.findActiveLineIndex(document.lines, positionMs) else -1
    }

    // Track whether the user is manually scrolling so auto-scroll doesn't fight their finger
    var userScrollingPaused by remember(document) { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrollingPaused = true
        } else if (userScrollingPaused) {
            delay(3_500L)
            userScrollingPaused = false
        }
    }

    // Automatic scrolling synchronized to the active Media3 playback timestamp
    LaunchedEffect(activeIndex, userScrollingPaused) {
        if (document.isSynced && activeIndex >= 0 && !userScrollingPaused) {
            val targetIdx = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(index = targetIdx, scrollOffset = 0)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("lyrics_lazy_column"),
            contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(
                items = document.lines,
                key = { idx, line -> "${idx}_${line.timeMs}" }
            ) { index, line ->
                val isActive = document.isSynced && index == activeIndex
                val isPast = document.isSynced && activeIndex > index
                LyricLineRow(
                    line = line,
                    appearance = appearance,
                    isSynced = document.isSynced,
                    isActive = isActive,
                    isPast = isPast,
                    onClick = {
                        if (document.isSynced && line.timeMs >= 0L) {
                            userScrollingPaused = false
                            onSeekToLine(line.timeMs)
                        }
                    },
                    modifier = Modifier.testTag("lyric_line_$index")
                )
            }
        }

        // Floating Re-sync button whenever user scrolled away from current synced line
        AnimatedVisibility(
            visible = document.isSynced && userScrollingPaused && activeIndex >= 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElectricCyan)
                    .clickable {
                        userScrollingPaused = false
                        scope.launch {
                            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("lyrics_resync_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Re-sync lyrics to playback",
                    tint = DeepVoid,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Re-sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepVoid,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LyricLineRow(
    line: LrcLine,
    appearance: LyricsAppearanceConfig,
    isSynced: Boolean,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> IceWhite
            isPast -> TelemetrySlate.copy(alpha = 0.65f)
            else -> TelemetrySlate.copy(alpha = 0.88f)
        },
        animationSpec = tween(220),
        label = "lyricTextColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) ElectricCyan.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(220),
        label = "lyricBgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) ElectricCyan.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(220),
        label = "lyricBorderColor"
    )

    val effectiveSizeSp = (if (isActive) appearance.fontSizeSp + 2f else appearance.fontSizeSp).coerceIn(12f, 28f)
    val effectiveLineHeightSp = (effectiveSizeSp * 1.38f)
    val lyricTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = effectiveSizeSp.sp,
        lineHeight = effectiveLineHeightSp.sp,
        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
    )

    val boxModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(bgColor)
        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
        .clickable(enabled = isSynced && line.timeMs >= 0L, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 8.dp)

    when (appearance.alignment) {
        LyricsTextAlignment.LEFT -> {
            Row(
                modifier = boxModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(if (isActive) 24.dp else 14.dp)
                        .clip(CircleShape)
                        .background(if (isActive) ElectricCyan else HairlineBorder.copy(alpha = 0.4f))
                )
                if (isSynced && line.timeMs >= 0L) {
                    Text(
                        text = LrcParser.formatTimestamp(line.timeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) ElectricCyan else TelemetrySlate.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = line.text,
                    style = lyricTextStyle,
                    color = textColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LyricsTextAlignment.CENTER -> {
            Column(
                modifier = boxModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isSynced && line.timeMs >= 0L) {
                    Text(
                        text = LrcParser.formatTimestamp(line.timeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) ElectricCyan else TelemetrySlate.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = line.text,
                    style = lyricTextStyle,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LyricsTextAlignment.RIGHT -> {
            Row(
                modifier = boxModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = line.text,
                    style = lyricTextStyle,
                    color = textColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                if (isSynced && line.timeMs >= 0L) {
                    Text(
                        text = LrcParser.formatTimestamp(line.timeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) ElectricCyan else TelemetrySlate.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(if (isActive) 24.dp else 14.dp)
                        .clip(CircleShape)
                        .background(if (isActive) ElectricCyan else HairlineBorder.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun SearchLyricsDialog(
    initialQuery: String,
    searchState: LyricsSearchUiState,
    onSearch: (String) -> Unit,
    onSelectResult: (LrcSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianElevated,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Search LRC Lyrics Online", color = IceWhite)
                Text(
                    "Powered by LRCLIB • Tap a match to sync & cache offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetrySlate
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Song title & artist", color = TelemetrySlate) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = ObsidianBase,
                            unfocusedContainerColor = ObsidianBase,
                            focusedTextColor = IceWhite,
                            unfocusedTextColor = IceWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lyrics_search_input")
                    )
                    IconButton(
                        onClick = { onSearch(query) },
                        enabled = query.isNotBlank() && searchState !is LyricsSearchUiState.Searching,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan)
                            .testTag("lyrics_search_submit_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = DeepVoid)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                ) {
                    when (searchState) {
                        is LyricsSearchUiState.Idle -> {
                            Text(
                                text = "Enter a song title and artist to search for synchronized .LRC files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TelemetrySlate,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        is LyricsSearchUiState.Searching -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = ElectricCyan, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                                Text("Searching LRCLIB...", style = MaterialTheme.typography.bodySmall, color = TelemetrySlate)
                            }
                        }

                        is LyricsSearchUiState.Error -> {
                            Text(
                                text = searchState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = SignalRed,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        is LyricsSearchUiState.Results -> {
                            if (searchState.items.isEmpty()) {
                                Text(
                                    text = "No matching lyrics found. Try refining the song title or importing a local .lrc file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TelemetrySlate,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(searchState.items, key = { it.id }) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ObsidianSurface)
                                                .border(
                                                    1.dp,
                                                    if (item.hasSynced) LaserMint.copy(alpha = 0.5f) else HairlineBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { onSelectResult(item) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.trackName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = IceWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(item.artistName)
                                                        if (item.durationSec > 0) {
                                                            append(" • ")
                                                            append(formatDuration(item.durationSec * 1000L))
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TelemetrySlate,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        (if (item.hasSynced) LaserMint else AmberPulse).copy(alpha = 0.16f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (item.hasSynced) "SYNCED" else "PLAIN",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (item.hasSynced) LaserMint else AmberPulse
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricCyan)
            }
        }
    )
}
