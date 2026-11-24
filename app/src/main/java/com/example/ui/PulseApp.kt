package com.example.ui

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.example.R
import com.example.audio.PulseQuickPlayTileService
import com.example.data.LocalStorageScanner
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AudioEffectsScreen
import com.example.ui.screens.FileBrowserScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TelemetrySlate
import com.example.viewmodel.PulseViewModel
import kotlinx.coroutines.launch

enum class PulseTab(
    val label: String,
    val icon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Rounded.Home, "nav_tab_home"),
    QUEUE("Queue", Icons.AutoMirrored.Rounded.QueueMusic, "nav_tab_queue"),
    PLAYER("Player", Icons.Rounded.GraphicEq, "nav_tab_player"),
    EFFECTS("Effects", Icons.Rounded.Equalizer, "nav_tab_effects"),
    FILES("Files", Icons.Rounded.FolderOpen, "nav_tab_files"),
    ABOUT("About", Icons.Rounded.Info, "nav_tab_about")
}

@UnstableApi
@Composable
fun PulseApp(viewModel: PulseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by rememberSaveable { mutableStateOf(PulseTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val savedPresets by viewModel.savedPresets.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val visualizerFrame by viewModel.visualizerFrame.collectAsStateWithLifecycle()
    val signalTelemetry by viewModel.signalTelemetry.collectAsStateWithLifecycle()
    val hardwareDiagnostics by viewModel.hardwareDiagnostics.collectAsStateWithLifecycle()
    val effectsConfig by viewModel.effectsConfig.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val formatFilter by viewModel.formatFilter.collectAsStateWithLifecycle()
    val aiEqState by viewModel.aiEqState.collectAsStateWithLifecycle()
    val selectedGear by viewModel.selectedGear.collectAsStateWithLifecycle()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()
    val fileBrowserState by viewModel.fileBrowserState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val lyricsSearchState by viewModel.lyricsSearchState.collectAsStateWithLifecycle()
    val lyricsAppearance by viewModel.lyricsAppearance.collectAsStateWithLifecycle()

    // Track notification permission for Lockscreen & Status Bar Media Player (API 33+)
    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Lockscreen & Status Bar media player enabled"
                else "Allow notifications in Settings to show Lockscreen player"
            )
        }
    }

    // Request POST_NOTIFICATIONS cleanly once on Android 13+ so lockscreen & status bar controls appear
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // System back returns from secondary tabs to Home
    BackHandler(enabled = activeTab != PulseTab.HOME) {
        activeTab = PulseTab.HOME
    }

    // Play-Policy-compliant Storage Access Framework launchers
    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importAudioFiles(uris)
    }

    val mountFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.mountAndScanFolder(uri)
            activeTab = PulseTab.FILES
        }
    }

    val importEqJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importEqFromJsonUri(uri)
    }

    val exportEqJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportEqToJsonUri(uri)
    }

    val importLrcLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importLrcFromUri(uri)
    }

    val triggerImportAudio = {
        importAudioLauncher.launch(LocalStorageScanner.IMPORT_AUDIO_MIME_TYPES)
    }
    val triggerMountFolder = {
        mountFolderLauncher.launch(null)
    }
    val triggerImportEqJson = {
        importEqJsonLauncher.launch(LocalStorageScanner.IMPORT_JSON_MIME_TYPES)
    }
    val triggerImportLrcFile = {
        importLrcLauncher.launch(LocalStorageScanner.IMPORT_LRC_MIME_TYPES)
    }
    val triggerExportEqJson = {
        val safeName = effectsConfig.activePresetName
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "pulsedsp_eq" }
        exportEqJsonLauncher.launch("${safeName}.json")
    }
    val triggerPinQuickSettingsPlayTile: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            if (statusBarManager != null) {
                statusBarManager.requestAddTileService(
                    ComponentName(context, PulseQuickPlayTileService::class.java),
                    context.getString(R.string.qs_tile_play_label),
                    AndroidIcon.createWithResource(context, R.drawable.ic_qs_pulse_play),
                    ContextCompat.getMainExecutor(context)
                ) { resultCode ->
                    val feedback = when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            "PulseDSP PLAY tile added to Quick Settings!"
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            "PulseDSP PLAY tile is already enabled in Quick Settings"
                        else -> "Swipe down Quick Settings & tap Edit to pin PulseDSP Play"
                    }
                    scope.launch { snackbarHostState.showSnackbar(feedback) }
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Swipe down Quick Settings & edit tiles to add PulseDSP Play")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Swipe down Quick Settings & tap Edit to add PulseDSP Play tile")
            }
        }
        Unit
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        val isWideScreen = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    containerColor = ObsidianBase,
                    contentColor = IceWhite,
                    modifier = Modifier
                        .fillMaxHeight()
                        .border(1.dp, HairlineBorder)
                ) {
                    PulseTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { activeTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ElectricCyan,
                                selectedTextColor = ElectricCyan,
                                indicatorColor = ElectricCyan.copy(alpha = 0.16f),
                                unselectedIconColor = TelemetrySlate,
                                unselectedTextColor = TelemetrySlate
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = DeepVoid,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = ObsidianElevated,
                            contentColor = IceWhite,
                            actionColor = ElectricCyan
                        )
                    }
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val currentTrack = playbackState.currentTrack
                        AnimatedVisibility(
                            visible = currentTrack != null && activeTab != PulseTab.PLAYER,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (currentTrack != null) {
                                MiniPlayerBar(
                                    track = currentTrack,
                                    isPlaying = playbackState.isPlaying,
                                    activeModeLabel = if (effectsConfig.enabled) effectsConfig.activeMode.displayName else "Bypass",
                                    onOpenPlayer = { activeTab = PulseTab.PLAYER },
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onNext = viewModel::skipNext
                                )
                            }
                        }

                        if (!isWideScreen) {
                            CyberBottomNavigationBar(
                                activeTab = activeTab,
                                onSelectTab = { activeTab = it }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (activeTab) {
                            PulseTab.HOME -> HomeScreen(
                                tracks = tracks,
                                playlists = playlists,
                                currentTrackId = playbackState.currentTrack?.id,
                                isPlaying = playbackState.isPlaying,
                                effectsConfig = effectsConfig,
                                searchQuery = searchQuery,
                                formatFilter = formatFilter,
                                onSearchChange = viewModel::setSearchQuery,
                                onFilterChange = viewModel::setFormatFilter,
                                onPlayTrack = {
                                    viewModel.playSingleTrack(it)
                                    activeTab = PulseTab.PLAYER
                                },
                                onEnqueueTrack = viewModel::enqueueTrack,
                                onPlayAll = { shuffle ->
                                    if (tracks.isNotEmpty()) {
                                        viewModel.playTracks(tracks, 0, shuffle)
                                        activeTab = PulseTab.PLAYER
                                    }
                                },
                                onImportAudioFiles = triggerImportAudio,
                                onMountFolder = triggerMountFolder,
                                onSelectMode = viewModel::selectListeningMode,
                                onOpenAudioEffects = { activeTab = PulseTab.EFFECTS },
                                onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                onRenamePlaylist = viewModel::renamePlaylist,
                                onDeletePlaylist = viewModel::deletePlaylist,
                                onPlayPlaylist = { pl, shuffle ->
                                    if (pl.tracks.isNotEmpty()) {
                                        viewModel.playTracks(pl.tracks, 0, shuffle)
                                        activeTab = PulseTab.PLAYER
                                    }
                                },
                                onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist
                            )

                            PulseTab.QUEUE -> QueueScreen(
                                playbackState = playbackState,
                                onJumpToIndex = { idx ->
                                    viewModel.jumpToQueueIndex(idx)
                                    activeTab = PulseTab.PLAYER
                                },
                                onMoveItem = viewModel::moveQueueItem,
                                onRemoveItem = viewModel::removeFromQueue,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onClearQueue = viewModel::clearQueue,
                                onSaveQueueAsPlaylist = viewModel::saveQueueAsPlaylist
                            )

                            PulseTab.PLAYER -> PlayerScreen(
                                playbackState = playbackState,
                                visualizerFrame = visualizerFrame,
                                signalTelemetry = signalTelemetry,
                                effectsConfig = effectsConfig,
                                sleepTimerRemainingSec = sleepTimerRemainingSec,
                                lyricsState = lyricsState,
                                lyricsSearchState = lyricsSearchState,
                                lyricsAppearance = lyricsAppearance,
                                onSelectVisualizerMode = viewModel::setVisualizerMode,
                                onSeek = viewModel::seekTo,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onPrevious = viewModel::skipPrevious,
                                onNext = viewModel::skipNext,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onCycleRepeat = viewModel::cycleRepeatMode,
                                onOpenAudioEffects = { activeTab = PulseTab.EFFECTS },
                                onOpenQueue = { activeTab = PulseTab.QUEUE },
                                onImportLrcFile = triggerImportLrcFile,
                                onSearchLyrics = viewModel::searchLyricsOnline,
                                onSelectSearchedLyrics = viewModel::selectSearchedLyrics,
                                onDismissLyricsSearch = viewModel::dismissLyricsSearch,
                                onSetLyricsFontSize = viewModel::setLyricsFontSize,
                                onSetLyricsAlignment = viewModel::setLyricsAlignment,
                                onResetLyricsAppearance = viewModel::resetLyricsAppearance
                            )

                            PulseTab.EFFECTS -> AudioEffectsScreen(
                                config = effectsConfig,
                                savedPresets = savedPresets,
                                visualizerFrame = visualizerFrame,
                                signalTelemetry = signalTelemetry,
                                hardwareDiagnostics = hardwareDiagnostics,
                                aiEqState = aiEqState,
                                selectedGear = selectedGear,
                                sleepTimerRemainingSec = sleepTimerRemainingSec,
                                onToggleMasterEnabled = viewModel::setEffectsEnabled,
                                onSelectMode = viewModel::selectListeningMode,
                                onSetSleepTimer = viewModel::startSleepTimer,
                                onSetBandGain = viewModel::setBandGain,
                                onSetPreampDb = viewModel::setPreampDb,
                                onResetFlat = viewModel::resetEqFlat,
                                onImportEqJson = triggerImportEqJson,
                                onExportEqJson = triggerExportEqJson,
                                onSavePreset = viewModel::saveCurrentPreset,
                                onApplySavedPreset = viewModel::applySavedPreset,
                                onDeleteSavedPreset = viewModel::deleteSavedPreset,
                                onSelectGear = viewModel::selectGear,
                                onGenerateAiEq = viewModel::generateAiEqualizer,
                                onToggleAiAbCompare = viewModel::toggleAiAbCompare,
                                onSetVirtualizer = viewModel::setVirtualizerLevel,
                                onSetSurroundMode = viewModel::setSurroundMode,
                                onSetStereoWidth = viewModel::setStereoWidth,
                                onSetBassBoost = viewModel::setBassBoost,
                                onSetTrebleBoost = viewModel::setTrebleBoost,
                                onSetCompressorEnabled = viewModel::setCompressorEnabled,
                                onSetCompressorParams = viewModel::setCompressorParams,
                                onSetDigitalPassthrough = viewModel::setDigitalPassthroughEnabled,
                                visualizerPalette = playbackState.visualizerPalette,
                                onSelectVisualizerPalette = viewModel::setVisualizerPalette,
                                hapticBeatSyncEnabled = playbackState.hapticBeatSyncEnabled,
                                hapticIntensity = playbackState.hapticIntensity,
                                hasVibratorHardware = playbackState.hasVibratorHardware,
                                hasAmplitudeControl = playbackState.hasAmplitudeControl,
                                hapticPulseStrength = playbackState.hapticPulseStrength,
                                onToggleHapticBeatSync = viewModel::setHapticBeatSyncEnabled,
                                onSetHapticIntensity = viewModel::setHapticIntensity,
                                onApplyGenrePreset = viewModel::applyGenreEqPreset
                            )

                            PulseTab.FILES -> FileBrowserScreen(
                                browserState = fileBrowserState,
                                onMountFolder = triggerMountFolder,
                                onImportFiles = triggerImportAudio,
                                onImportEqJson = triggerImportEqJson,
                                onSelectRoot = { uri, label -> viewModel.openBrowserDirectory(uri, label, resetStack = true) },
                                onNavigateUp = viewModel::navigateBrowserUp,
                                onOpenDirectory = { uri, label -> viewModel.openBrowserDirectory(uri, label, resetStack = false) },
                                onPlayEntry = { entry ->
                                    viewModel.playOrImportFileEntry(entry, enqueueOnly = false)
                                    activeTab = PulseTab.PLAYER
                                },
                                onEnqueueEntry = { entry -> viewModel.playOrImportFileEntry(entry, enqueueOnly = true) },
                                onApplyJsonEqEntry = { uri ->
                                    viewModel.importEqFromJsonUri(uri)
                                    activeTab = PulseTab.EFFECTS
                                },
                                onApplyLrcEntry = { uri ->
                                    viewModel.importLrcFromUri(uri)
                                    activeTab = PulseTab.PLAYER
                                }
                            )

                            PulseTab.ABOUT -> AboutScreen(
                                hardwareDiagnostics = hardwareDiagnostics,
                                trackCount = tracks.size,
                                playlistCount = playlists.size,
                                presetCount = savedPresets.size,
                                lockscreenNotificationGranted = notificationGranted,
                                onEnableLockscreenNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onEnableQuickSettingsPlayTile = triggerPinQuickSettingsPlayTile,
                                onExportEqJson = triggerExportEqJson,
                                onImportEqJson = triggerImportEqJson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberBottomNavigationBar(
    activeTab: PulseTab,
    onSelectTab: (PulseTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianBase.copy(alpha = 0.98f))
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(HairlineBorder, ElectricCyan.copy(alpha = 0.45f), CyberViolet.copy(alpha = 0.45f), HairlineBorder)
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) ElectricCyan.copy(alpha = 0.14f) else DeepVoid.copy(alpha = 0f))
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 4.dp)
                        .testTag(tab.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) ElectricCyan else TelemetrySlate,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ElectricCyan else TelemetrySlate,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
