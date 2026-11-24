package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.data.AudioEffectsConfig
import com.example.data.LrcSearchResult
import com.example.data.LyricsAppearanceConfig
import com.example.data.LyricsTextAlignment
import com.example.data.SignalTelemetry
import com.example.data.VisualizerFrame
import com.example.data.VisualizerMode
import com.example.ui.components.CodecBadge
import com.example.ui.components.LyricsViewPanel
import com.example.ui.components.RealTimeVisualizer
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatCountdown
import com.example.ui.components.formatDuration
import com.example.ui.components.paletteColorsFor
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
import com.example.ui.theme.TelemetrySlate
import com.example.viewmodel.LyricsSearchUiState
import com.example.viewmodel.LyricsUiState
import com.example.viewmodel.PlaybackUiState

enum class PlayerStageTab(val label: String, val icon: ImageVector) {
    COVER("Cover", Icons.Rounded.Album),
    VISUALIZER("Visualizer", Icons.Rounded.GraphicEq),
    LYRICS("Lyrics", Icons.Rounded.Lyrics)
}

@Composable
fun PlayerScreen(
    playbackState: PlaybackUiState,
    visualizerFrame: VisualizerFrame,
    signalTelemetry: SignalTelemetry,
    effectsConfig: AudioEffectsConfig,
    sleepTimerRemainingSec: Int,
    lyricsState: LyricsUiState,
    lyricsSearchState: LyricsSearchUiState,
    lyricsAppearance: LyricsAppearanceConfig,
    onSelectVisualizerMode: (VisualizerMode) -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenAudioEffects: () -> Unit,
    onOpenQueue: () -> Unit,
    onImportLrcFile: () -> Unit,
    onSearchLyrics: (String) -> Unit,
    onSelectSearchedLyrics: (LrcSearchResult) -> Unit,
    onDismissLyricsSearch: () -> Unit,
    onSetLyricsFontSize: (Float) -> Unit,
    onSetLyricsAlignment: (LyricsTextAlignment) -> Unit,
    onResetLyricsAppearance: () -> Unit
) {
    val track = playbackState.currentTrack
    val scrollState = rememberScrollState()
    var stageTab by rememberSaveable { mutableStateOf(PlayerStageTab.COVER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("player_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hi-Res Signal Path Telemetry Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurface)
                .border(
                    1.dp,
                    if (signalTelemetry.passthroughActive) LaserMint.copy(alpha = 0.6f) else HairlineBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = signalTelemetry.formattedBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = LaserMint
                    )
                    if (sleepTimerRemainingSec > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Bedtime,
                                contentDescription = "Sleep timer active",
                                tint = CyberViolet,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatCountdown(sleepTimerRemainingSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberViolet
                            )
                        }
                    }
                }
                Text(
                    text = signalTelemetry.passthroughStatusNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = TelemetrySlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Primary Stage Selector: Audio Coverpage | Live FFT Visualizer | Synchronized LRC Lyrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlayerStageTab.entries.forEach { tab ->
                val selected = stageTab == tab
                val accent = when (tab) {
                    PlayerStageTab.COVER -> ElectricCyan
                    PlayerStageTab.VISUALIZER -> LaserMint
                    PlayerStageTab.LYRICS -> CyberViolet
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) accent.copy(alpha = 0.75f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { stageTab = tab }
                        .padding(horizontal = 8.dp, vertical = 9.dp)
                        .testTag("player_stage_tab_${tab.name.lowercase()}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) accent else TelemetrySlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) accent else TelemetrySlate,
                        maxLines = 1
                    )
                }
            }
        }

        // Hero Interactive Stage (Coverpage / Live 60 FPS FFT Visualizer / Synchronized LRC Lyrics)
        val paletteColors = paletteColorsFor(playbackState.visualizerPalette)
        val bassGlowAlpha = (0.18f + visualizerFrame.bassEnergy * 0.28f).coerceIn(0.18f, 0.52f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(292.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianElevated.copy(alpha = 0.75f), DeepVoid)
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            paletteColors.primary.copy(alpha = 0.45f),
                            paletteColors.secondary.copy(alpha = 0.4f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(14.dp)
        ) {
            when (stageTab) {
                PlayerStageTab.COVER -> {
                    // Prominent Audio Coverpage with Reactive Neon Halo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("player_coverpage_stage"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(258.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            paletteColors.primary.copy(alpha = bassGlowAlpha),
                                            paletteColors.secondary.copy(alpha = bassGlowAlpha * 0.55f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.92f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                paletteColors.primary.copy(alpha = 0.7f),
                                                paletteColors.secondary.copy(alpha = 0.65f)
                                            )
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                            ) {
                                TrackArtwork(
                                    artworkResName = track?.artworkResName,
                                    contentDescription = track?.title ?: stringResource(R.string.cd_album_art),
                                    trackUri = track?.uri,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Floating Bottom Glass Overlay on Coverpage
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, DeepVoid.copy(alpha = 0.86f))
                                            )
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = track?.album?.ifBlank { "Single" } ?: "PulseDSP Audio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IceWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CodecBadge(codecLabel = track?.codecLabel ?: signalTelemetry.codecLabel)
                                }
                            }
                        }
                    }
                }

                PlayerStageTab.VISUALIZER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("realtime_fft_visualizer_stage"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compact Header with Track Cover Mini + Bars | Radial | Wave Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                TrackArtwork(
                                    artworkResName = track?.artworkResName,
                                    contentDescription = track?.title ?: stringResource(R.string.cd_album_art),
                                    trackUri = track?.uri,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text(
                                        text = "LIVE FFT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = if (playbackState.isPlaying) "60 FPS PCM" else "STANDBY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TelemetrySlate
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBase)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                VisualizerMode.entries.forEach { mode ->
                                    val selected = playbackState.visualizerMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (selected) ElectricCyan.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { onSelectVisualizerMode(mode) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("visualizer_mode_${mode.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) ElectricCyan else TelemetrySlate
                                        )
                                    }
                                }
                            }
                        }

                        RealTimeVisualizer(
                            frame = visualizerFrame,
                            mode = playbackState.visualizerMode,
                            palette = playbackState.visualizerPalette,
                            artworkResName = track?.artworkResName,
                            trackTitle = track?.title ?: "No track",
                            trackUri = track?.uri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                PlayerStageTab.LYRICS -> {
                    val defaultQuery = track?.let {
                        val artistPart = it.artist.takeUnless { a -> a.equals("Local Audio", ignoreCase = true) }.orEmpty()
                        "${it.title} $artistPart".trim()
                    }.orEmpty()

                    LyricsViewPanel(
                        lyricsState = lyricsState,
                        lyricsSearchState = lyricsSearchState,
                        appearance = lyricsAppearance,
                        positionMs = playbackState.positionMs,
                        defaultSearchQuery = defaultQuery,
                        onSeekToLine = onSeek,
                        onImportLrcFile = onImportLrcFile,
                        onSearchLyrics = onSearchLyrics,
                        onSelectSearchedLyrics = onSelectSearchedLyrics,
                        onDismissLyricsSearch = onDismissLyricsSearch,
                        onSetFontSize = onSetLyricsFontSize,
                        onSetAlignment = onSetLyricsAlignment,
                        onResetAppearance = onResetLyricsAppearance,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Track Title & Metadata (with contextual mini cover art when viewing Lyrics)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (stageTab == PlayerStageTab.LYRICS && track != null) {
                TrackArtwork(
                    artworkResName = track.artworkResName,
                    contentDescription = track.title,
                    trackUri = track.uri,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable { stageTab = PlayerStageTab.COVER }
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(
                modifier = if (stageTab == PlayerStageTab.LYRICS) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                horizontalAlignment = if (stageTab == PlayerStageTab.LYRICS) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = track?.title ?: "Select a Track",
                    style = MaterialTheme.typography.headlineSmall,
                    color = IceWhite,
                    textAlign = if (stageTab == PlayerStageTab.LYRICS) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.let { "${it.artist} • ${it.album}" } ?: "Import local audio or play a reference track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TelemetrySlate,
                    textAlign = if (stageTab == PlayerStageTab.LYRICS) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Scrubbable Progress Bar
        val safeDuration = playbackState.durationMs.coerceAtLeast(1000L)
        val fraction = (playbackState.positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = fraction,
                onValueChange = { frac -> onSeek((frac * safeDuration).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = RackWell
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seek_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playbackState.positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Text(
                    text = formatDuration(playbackState.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = TelemetrySlate
                )
            }
        }

        // Primary Transport Controls (Shuffle | Prev | Play/Pause | Next | Repeat)
        val shuffleAccent = ElectricCyan
        val shuffleTint by animateColorAsState(
            targetValue = if (playbackState.shuffleEnabled) shuffleAccent else TelemetrySlate,
            animationSpec = tween(200),
            label = "shuffleTint"
        )
        val shuffleContainer by animateColorAsState(
            targetValue = if (playbackState.shuffleEnabled) shuffleAccent.copy(alpha = 0.16f) else ObsidianSurface,
            animationSpec = tween(200),
            label = "shuffleContainer"
        )
        val shuffleBorder by animateColorAsState(
            targetValue = if (playbackState.shuffleEnabled) shuffleAccent.copy(alpha = 0.7f) else HairlineBorder,
            animationSpec = tween(200),
            label = "shuffleBorder"
        )

        val repeatActive = playbackState.repeatMode != Player.REPEAT_MODE_OFF
        val repeatAccent = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) CyberViolet else ElectricCyan
        val repeatTint by animateColorAsState(
            targetValue = if (repeatActive) repeatAccent else TelemetrySlate,
            animationSpec = tween(200),
            label = "repeatTint"
        )
        val repeatContainer by animateColorAsState(
            targetValue = if (repeatActive) repeatAccent.copy(alpha = 0.16f) else ObsidianSurface,
            animationSpec = tween(200),
            label = "repeatContainer"
        )
        val repeatBorder by animateColorAsState(
            targetValue = if (repeatActive) repeatAccent.copy(alpha = 0.7f) else HairlineBorder,
            animationSpec = tween(200),
            label = "repeatBorder"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Mode Control
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(shuffleContainer)
                    .border(1.dp, shuffleBorder, RoundedCornerShape(15.dp))
                    .clickable(onClick = onToggleShuffle)
                    .testTag("player_shuffle_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(
                            if (playbackState.shuffleEnabled) R.string.cd_shuffle_on else R.string.cd_shuffle_off
                        ),
                        tint = shuffleTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(if (playbackState.shuffleEnabled) shuffleAccent else Color.Transparent)
                    )
                }
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("player_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_track),
                    tint = IceWhite,
                    modifier = Modifier.size(34.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricCyan, CyberViolet))
                    )
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = DeepVoid,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_track),
                    tint = IceWhite,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Repeat Mode Control (Off -> All -> One)
            val repeatIcon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) {
                Icons.Rounded.RepeatOne
            } else {
                Icons.Rounded.Repeat
            }
            val repeatContentDesc = when (playbackState.repeatMode) {
                Player.REPEAT_MODE_ALL -> stringResource(R.string.cd_repeat_all)
                Player.REPEAT_MODE_ONE -> stringResource(R.string.cd_repeat_one)
                else -> stringResource(R.string.cd_repeat_off)
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(repeatContainer)
                    .border(1.dp, repeatBorder, RoundedCornerShape(15.dp))
                    .clickable(onClick = onCycleRepeat)
                    .testTag("player_repeat_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = repeatContentDesc,
                        tint = repeatTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(if (repeatActive) repeatAccent else Color.Transparent)
                    )
                }
            }
        }

        // Quick Action Pills (Audio Effects & Queue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenAudioEffects)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("player_audio_effects_shortcut"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (effectsConfig.enabled) "Effects: ${effectsConfig.activeMode.displayName}" else "Audio Effects (Off)",
                    style = MaterialTheme.typography.labelLarge,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, CyberViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenQueue)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("player_queue_shortcut"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Queue (${playbackState.queue.size})", style = MaterialTheme.typography.labelLarge, color = IceWhite)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
