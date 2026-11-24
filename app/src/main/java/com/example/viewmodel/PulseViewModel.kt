package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.ai.AiEqualizerService
import com.example.audio.BeatHapticsEngine
import com.example.audio.PulseAudioEngine
import com.example.audio.PulsePlaybackService
import com.example.data.AudioEffectsConfig
import com.example.data.BuiltInModeProfiles
import com.example.data.DemoAudioGenerator
import com.example.data.EqJsonCodec
import com.example.data.GenreEqPreset
import com.example.data.HardwareAudioDiagnostics
import com.example.data.ListeningGear
import com.example.data.ListeningMode
import com.example.data.LocalFileEntry
import com.example.data.LocalStorageScanner
import com.example.data.LrcDocument
import com.example.data.LrcSearchResult
import com.example.data.LyricsAppearanceConfig
import com.example.data.LyricsRepository
import com.example.data.LyricsTextAlignment
import com.example.data.SignalTelemetry
import com.example.data.SurroundMode
import com.example.data.VisualizerFrame
import com.example.data.VisualizerMode
import com.example.data.VisualizerPalette
import com.example.data.local.AudioEffectPresetEntity
import com.example.data.local.DspPreferencesStore
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.PulseDatabase
import com.example.data.local.TrackEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "PulseViewModel"

enum class LibraryFormatFilter(val label: String) {
    ALL("All"),
    LOSSLESS("Lossless"),
    SURROUND_MP4("Surround / MP4"),
    COMPRESSED("Compressed")
}

data class PlaybackUiState(
    val queue: List<TrackEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val visualizerMode: VisualizerMode = VisualizerMode.BARS,
    val visualizerPalette: VisualizerPalette = VisualizerPalette.CYBER_NEON,
    val hapticBeatSyncEnabled: Boolean = false,
    val hapticIntensity: Float = 0.7f,
    val hasVibratorHardware: Boolean = true,
    val hasAmplitudeControl: Boolean = false,
    val hapticPulseStrength: Float = 0f
) {
    val currentTrack: TrackEntity?
        get() = queue.getOrNull(currentIndex)
}

sealed interface AiEqUiState {
    data object Idle : AiEqUiState
    data object Loading : AiEqUiState
    data class Success(
        val config: AudioEffectsConfig,
        val previousConfig: AudioEffectsConfig,
        val comparingPrevious: Boolean = false
    ) : AiEqUiState
    data class Error(val message: String) : AiEqUiState
}

data class FileBrowserUiState(
    val mountedRoots: List<Uri> = emptyList(),
    val navStack: List<Pair<String, Uri>> = emptyList(),
    val entries: List<LocalFileEntry> = emptyList(),
    val isScanning: Boolean = false
)

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data class Loaded(val document: LrcDocument) : LyricsUiState
    data class NotFound(val message: String) : LyricsUiState
}

sealed interface LyricsSearchUiState {
    data object Idle : LyricsSearchUiState
    data object Searching : LyricsSearchUiState
    data class Results(val items: List<LrcSearchResult>) : LyricsSearchUiState
    data class Error(val message: String) : LyricsSearchUiState
}

@UnstableApi
class PulseViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = PulseDatabase.getInstance(appContext).pulseDao()
    private val prefsStore = DspPreferencesStore(appContext)
    private val aiService = AiEqualizerService()
    private val hapticsEngine = BeatHapticsEngine(appContext)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    val tracks: StateFlow<List<TrackEntity>> = dao.observeAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistWithTracks>> = dao.observePlaylistsWithTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPresets: StateFlow<List<AudioEffectPresetEntity>> = dao.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visualizerFrame: StateFlow<VisualizerFrame> = PulseAudioEngine.visualizerFrame
    val signalTelemetry: StateFlow<SignalTelemetry> = PulseAudioEngine.signalTelemetry
    val hardwareDiagnostics: StateFlow<HardwareAudioDiagnostics> = PulseAudioEngine.hardwareDiagnostics
    val effectsConfig: StateFlow<AudioEffectsConfig> = PulseAudioEngine.effectsConfig

    private val _playbackState = MutableStateFlow(
        PlaybackUiState(
            hasVibratorHardware = hapticsEngine.hasHardwareVibrator,
            hasAmplitudeControl = hapticsEngine.hasAmplitudeControl
        )
    )
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _formatFilter = MutableStateFlow(LibraryFormatFilter.ALL)
    val formatFilter: StateFlow<LibraryFormatFilter> = _formatFilter.asStateFlow()

    private val _aiEqState = MutableStateFlow<AiEqUiState>(AiEqUiState.Idle)
    val aiEqState: StateFlow<AiEqUiState> = _aiEqState.asStateFlow()

    private val _selectedGear = MutableStateFlow(ListeningGear.IEMS)
    val selectedGear: StateFlow<ListeningGear> = _selectedGear.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow(0)
    val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()

    private val _fileBrowserState = MutableStateFlow(FileBrowserUiState())
    val fileBrowserState: StateFlow<FileBrowserUiState> = _fileBrowserState.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.NotFound("Play a track to load synchronized .LRC lyrics"))
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _lyricsSearchState = MutableStateFlow<LyricsSearchUiState>(LyricsSearchUiState.Idle)
    val lyricsSearchState: StateFlow<LyricsSearchUiState> = _lyricsSearchState.asStateFlow()

    private val _lyricsAppearance = MutableStateFlow(LyricsAppearanceConfig())
    val lyricsAppearance: StateFlow<LyricsAppearanceConfig> = _lyricsAppearance.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var sleepTimerJob: Job? = null
    private var persistEffectsJob: Job? = null
    private var lyricsLoadJob: Job? = null
    private var lastLyricsTrackUri: String? = null

    init {
        PulseAudioEngine.refreshHardwareCapabilities(appContext)
        bootstrapOfflineLibrary()
        restorePreferences()
        connectMediaController()
        startPositionTicker()
        observeVisualizerForBeatHaptics()
    }

    private fun observeVisualizerForBeatHaptics() {
        viewModelScope.launch {
            visualizerFrame.collect { frame ->
                val state = _playbackState.value
                val pulse = hapticsEngine.processFrame(
                    frame = frame,
                    isPlaying = state.isPlaying,
                    enabled = state.hapticBeatSyncEnabled,
                    intensity = state.hapticIntensity
                )
                if (pulse > 0f) {
                    _playbackState.update { it.copy(hapticPulseStrength = pulse) }
                } else if (state.hapticPulseStrength > 0.02f) {
                    _playbackState.update { it.copy(hapticPulseStrength = it.hapticPulseStrength * 0.72f) }
                }
            }
        }
    }

    private fun bootstrapOfflineLibrary() {
        viewModelScope.launch {
            val builtIn = DemoAudioGenerator.ensureBuiltInTracks(appContext)
            dao.insertTracks(builtIn)

            // Seed one starter playlist on very first launch if none exist
            val existingPlaylists = dao.observePlaylistsWithTracks().first()
            if (existingPlaylists.isEmpty()) {
                val all = dao.observeAllTracks().first()
                if (all.isNotEmpty()) {
                    val plId = dao.insertPlaylist(
                        PlaylistEntity(
                            name = "Hi-Res Reference Mix",
                            description = "Lossless 48 kHz calibration tracks for DSP & surround testing"
                        )
                    )
                    all.forEachIndexed { idx, track ->
                        dao.insertPlaylistTrackCrossRef(PlaylistTrackCrossRef(plId, track.id, idx))
                    }
                }
            }

            // Pre-populate queue with library tracks if queue is empty (paused, ready to play)
            val currentTracks = dao.observeAllTracks().first()
            if (_playbackState.value.queue.isEmpty() && currentTracks.isNotEmpty()) {
                _playbackState.update {
                    it.copy(
                        queue = currentTracks,
                        currentIndex = 0,
                        durationMs = currentTracks.first().durationMs
                    )
                }
                syncTelemetryFromTrack(currentTracks.first())
                loadLyricsForTrack(currentTracks.first())
            }
        }
    }

    private fun restorePreferences() {
        viewModelScope.launch {
            val savedEffects = prefsStore.activeEffectsFlow.first()
            PulseAudioEngine.applyEffectsConfig(savedEffects)
        }
        viewModelScope.launch {
            prefsStore.visualizerModeFlow.collect { mode ->
                _playbackState.update { it.copy(visualizerMode = mode) }
            }
        }
        viewModelScope.launch {
            prefsStore.visualizerPaletteFlow.collect { palette ->
                _playbackState.update { it.copy(visualizerPalette = palette) }
            }
        }
        viewModelScope.launch {
            prefsStore.hapticBeatSyncEnabledFlow.collect { enabled ->
                _playbackState.update { it.copy(hapticBeatSyncEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefsStore.hapticIntensityFlow.collect { intensity ->
                _playbackState.update { it.copy(hapticIntensity = intensity) }
            }
        }
        viewModelScope.launch {
            prefsStore.lyricsAppearanceFlow.collect { appearance ->
                _lyricsAppearance.value = appearance
            }
        }
        viewModelScope.launch {
            val savedShuffle = prefsStore.shuffleEnabledFlow.first()
            val savedRepeat = prefsStore.repeatModeFlow.first()
            val ctrl = mediaController
            if (ctrl != null && ctrl.mediaItemCount > 0) {
                // Active session already running in service: reflect live Media3 state
                _playbackState.update {
                    it.copy(
                        shuffleEnabled = ctrl.shuffleModeEnabled,
                        repeatMode = ctrl.repeatMode
                    )
                }
            } else {
                _playbackState.update {
                    it.copy(
                        shuffleEnabled = savedShuffle,
                        repeatMode = savedRepeat
                    )
                }
                ctrl?.shuffleModeEnabled = savedShuffle
                ctrl?.repeatMode = savedRepeat
            }
        }
        viewModelScope.launch {
            prefsStore.mountedFoldersFlow.collect { uriStrings ->
                val uris = uriStrings.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
                _fileBrowserState.update { it.copy(mountedRoots = uris) }
                if (uris.isNotEmpty() && _fileBrowserState.value.navStack.isEmpty()) {
                    openBrowserDirectory(uris.first(), LocalStorageScanner.resolveDisplayName(appContext, uris.first()) ?: "Music Folder", resetStack = true)
                }
            }
        }
    }

    private fun connectMediaController() {
        val token = SessionToken(appContext, ComponentName(appContext, PulsePlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                controller.addListener(playerListener)
                syncStateFromController(controller)
            }.onFailure {
                Log.e(TAG, "Failed to connect MediaController", it)
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val ctrl = mediaController ?: return
            val idx = ctrl.currentMediaItemIndex
            _playbackState.update { state ->
                val safeIdx = idx.coerceIn(0, (state.queue.size - 1).coerceAtLeast(0))
                val track = state.queue.getOrNull(safeIdx)
                if (track != null) {
                    syncTelemetryFromTrack(track)
                    loadLyricsForTrack(track)
                }
                state.copy(
                    currentIndex = if (state.queue.isEmpty()) -1 else safeIdx,
                    durationMs = ctrl.duration.takeIf { it > 0 } ?: track?.durationMs ?: 0L
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val ctrl = mediaController ?: return
            if (playbackState == Player.STATE_READY) {
                _playbackState.update {
                    it.copy(durationMs = ctrl.duration.takeIf { d -> d > 0 } ?: it.durationMs)
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
            viewModelScope.launch { prefsStore.saveShuffleEnabled(shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode) }
            viewModelScope.launch { prefsStore.saveRepeatMode(repeatMode) }
        }
    }

    private fun syncStateFromController(controller: MediaController) {
        val currentState = _playbackState.value
        if (controller.mediaItemCount > 0) {
            _playbackState.update {
                it.copy(
                    isPlaying = controller.isPlaying,
                    positionMs = controller.currentPosition.coerceAtLeast(0L),
                    durationMs = controller.duration.takeIf { d -> d > 0 } ?: it.durationMs,
                    shuffleEnabled = controller.shuffleModeEnabled,
                    repeatMode = controller.repeatMode
                )
            }
        } else {
            controller.shuffleModeEnabled = currentState.shuffleEnabled
            controller.repeatMode = currentState.repeatMode
            _playbackState.update {
                it.copy(
                    isPlaying = controller.isPlaying,
                    positionMs = controller.currentPosition.coerceAtLeast(0L),
                    durationMs = controller.duration.takeIf { d -> d > 0 } ?: it.durationMs
                )
            }
        }
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (isActive) {
                val ctrl = mediaController
                if (ctrl != null && ctrl.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                            durationMs = ctrl.duration.takeIf { d -> d > 0 } ?: it.durationMs
                        )
                    }
                }
                delay(150L)
            }
        }
    }

    // --- Playback & Queue Controls ---

    fun playTracks(trackList: List<TrackEntity>, startIndex: Int = 0, shuffle: Boolean? = null) {
        if (trackList.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, trackList.lastIndex)
        val effectiveShuffle = shuffle ?: _playbackState.value.shuffleEnabled
        val effectiveRepeat = _playbackState.value.repeatMode
        _playbackState.update {
            it.copy(
                queue = trackList,
                currentIndex = safeIdx,
                positionMs = 0L,
                durationMs = trackList[safeIdx].durationMs,
                shuffleEnabled = effectiveShuffle,
                repeatMode = effectiveRepeat
            )
        }
        if (shuffle != null) {
            viewModelScope.launch { prefsStore.saveShuffleEnabled(effectiveShuffle) }
        }
        syncTelemetryFromTrack(trackList[safeIdx])
        loadLyricsForTrack(trackList[safeIdx])

        val ctrl = mediaController ?: return
        val items = trackList.map { it.toMediaItem() }
        ctrl.shuffleModeEnabled = effectiveShuffle
        ctrl.repeatMode = effectiveRepeat
        ctrl.setMediaItems(items, safeIdx, 0L)
        ctrl.prepare()
        ctrl.play()
    }

    fun playSingleTrack(track: TrackEntity) {
        val all = tracks.value
        val idx = all.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            playTracks(all, idx)
        } else {
            playTracks(listOf(track), 0)
        }
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        val state = _playbackState.value
        if (ctrl.mediaItemCount == 0 && state.queue.isNotEmpty()) {
            playTracks(state.queue, state.currentIndex.coerceAtLeast(0), state.shuffleEnabled)
            return
        }
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _playbackState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun skipNext() {
        val ctrl = mediaController ?: return
        val state = _playbackState.value
        if (ctrl.mediaItemCount == 0 && state.queue.isNotEmpty()) {
            val nextIdx = (state.currentIndex + 1) % state.queue.size
            playTracks(state.queue, nextIdx)
            return
        }
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
        } else if (state.queue.isNotEmpty()) {
            ctrl.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val ctrl = mediaController ?: return
        val state = _playbackState.value
        if (ctrl.mediaItemCount == 0 && state.queue.isNotEmpty()) {
            val prevIdx = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.lastIndex
            playTracks(state.queue, prevIdx)
            return
        }
        if (ctrl.currentPosition > 3000L) {
            ctrl.seekTo(0L)
        } else if (ctrl.hasPreviousMediaItem()) {
            ctrl.seekToPreviousMediaItem()
        } else {
            ctrl.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val ctrl = mediaController
        val next = !(ctrl?.shuffleModeEnabled ?: _playbackState.value.shuffleEnabled)
        _playbackState.update { it.copy(shuffleEnabled = next) }
        ctrl?.shuffleModeEnabled = next
        viewModelScope.launch { prefsStore.saveShuffleEnabled(next) }
        emitMessage(if (next) "Shuffle: On" else "Shuffle: Off")
    }

    fun cycleRepeatMode() {
        val ctrl = mediaController
        val current = ctrl?.repeatMode ?: _playbackState.value.repeatMode
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _playbackState.update { it.copy(repeatMode = next) }
        ctrl?.repeatMode = next
        viewModelScope.launch { prefsStore.saveRepeatMode(next) }
        val label = when (next) {
            Player.REPEAT_MODE_ALL -> "Repeat: All"
            Player.REPEAT_MODE_ONE -> "Repeat: One"
            else -> "Repeat: Off"
        }
        emitMessage(label)
    }

    fun jumpToQueueIndex(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        val ctrl = mediaController
        if (ctrl == null || ctrl.mediaItemCount != state.queue.size) {
            playTracks(state.queue, index, state.shuffleEnabled)
        } else {
            ctrl.seekTo(index, 0L)
            ctrl.play()
        }
    }

    fun enqueueTrack(track: TrackEntity) {
        val currentQueue = _playbackState.value.queue
        val updated = currentQueue + track
        _playbackState.update { it.copy(queue = updated) }
        val ctrl = mediaController
        if (ctrl != null && ctrl.mediaItemCount > 0) {
            ctrl.addMediaItem(track.toMediaItem())
        }
        emitMessage("Added \"${track.title}\" to Queue")
    }

    fun removeFromQueue(index: Int) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        val updated = state.queue.toMutableList().apply { removeAt(index) }
        val newIdx = when {
            updated.isEmpty() -> -1
            index < state.currentIndex -> state.currentIndex - 1
            index == state.currentIndex -> index.coerceAtMost(updated.lastIndex)
            else -> state.currentIndex
        }
        _playbackState.update { it.copy(queue = updated, currentIndex = newIdx) }
        mediaController?.let { ctrl ->
            if (index < ctrl.mediaItemCount) {
                ctrl.removeMediaItem(index)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val state = _playbackState.value
        if (fromIndex !in state.queue.indices || toIndex !in state.queue.indices || fromIndex == toIndex) return
        val updated = state.queue.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)

        val newCurrent = when (state.currentIndex) {
            fromIndex -> toIndex
            in (fromIndex + 1)..toIndex -> state.currentIndex - 1
            in toIndex until fromIndex -> state.currentIndex + 1
            else -> state.currentIndex
        }
        _playbackState.update { it.copy(queue = updated, currentIndex = newCurrent) }
        mediaController?.let { ctrl ->
            if (fromIndex < ctrl.mediaItemCount && toIndex < ctrl.mediaItemCount) {
                ctrl.moveMediaItem(fromIndex, toIndex)
            }
        }
    }

    fun clearQueue() {
        mediaController?.clearMediaItems()
        _playbackState.update { it.copy(queue = emptyList(), currentIndex = -1, isPlaying = false, positionMs = 0L) }
        PulseAudioEngine.clearVisualizer()
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        _playbackState.update { it.copy(visualizerMode = mode) }
        viewModelScope.launch { prefsStore.saveVisualizerMode(mode) }
    }

    fun setVisualizerPalette(palette: VisualizerPalette) {
        _playbackState.update { it.copy(visualizerPalette = palette) }
        viewModelScope.launch { prefsStore.saveVisualizerPalette(palette) }
        emitMessage("Visualizer palette: ${palette.label}")
    }

    fun setHapticBeatSyncEnabled(enabled: Boolean) {
        if (enabled && !hapticsEngine.hasHardwareVibrator) {
            emitMessage("No haptic vibrator motor detected on this device")
            return
        }
        _playbackState.update {
            it.copy(
                hapticBeatSyncEnabled = enabled,
                hapticPulseStrength = if (enabled) 0.7f else 0f
            )
        }
        if (enabled) {
            hapticsEngine.triggerConfirmationPulse(_playbackState.value.hapticIntensity)
            emitMessage("Beat-synced haptic feedback enabled")
        } else {
            hapticsEngine.cancel()
            emitMessage("Beat-synced haptics disabled")
        }
        viewModelScope.launch { prefsStore.saveHapticBeatSyncEnabled(enabled) }
    }

    fun setHapticIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.1f, 1f)
        _playbackState.update { it.copy(hapticIntensity = clamped) }
        viewModelScope.launch { prefsStore.saveHapticIntensity(clamped) }
    }

    // --- Unified Audio Effects Controls ---

    fun setEffectsEnabled(enabled: Boolean) {
        updateEffects { it.copy(enabled = enabled) }
    }

    fun selectListeningMode(mode: ListeningMode) {
        val currentPassthrough = effectsConfig.value.digitalPassthroughEnabled
        val profile = BuiltInModeProfiles.forMode(mode)
            .copy(digitalPassthroughEnabled = currentPassthrough)
        applyAndPersistEffects(profile)
        if (mode != ListeningMode.SLEEP && _sleepTimerRemainingSec.value > 0) {
            cancelSleepTimer()
        }
        emitMessage("${mode.displayName} Mode active")
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        updateEffects { cfg ->
            val updatedBands = cfg.bandGainsDb.toMutableList()
            if (bandIndex in updatedBands.indices) {
                updatedBands[bandIndex] = gainDb.coerceIn(-12f, 12f)
            }
            cfg.copy(
                activeMode = ListeningMode.CUSTOM,
                activePresetName = "Custom EQ",
                bandGainsDb = updatedBands
            )
        }
    }

    fun resetEqFlat() {
        updateEffects { cfg ->
            cfg.copy(
                bandGainsDb = List(10) { 0f },
                preampDb = 0f,
                activeMode = ListeningMode.STUDIO_FLAT,
                activePresetName = "Studio Flat"
            )
        }
    }

    fun applyGenreEqPreset(preset: GenreEqPreset) {
        updateEffects { cfg ->
            cfg.copy(
                enabled = true,
                bandGainsDb = preset.bandGainsDb,
                preampDb = preset.preampDb,
                activeMode = if (preset == GenreEqPreset.FLAT) ListeningMode.STUDIO_FLAT else ListeningMode.CUSTOM,
                activePresetName = if (preset == GenreEqPreset.FLAT) "Studio Flat" else "${preset.displayName} EQ"
            )
        }
        emitMessage("Applied ${preset.displayName} EQ preset")
    }

    // --- Synchronized .LRC Lyrics Resolution, Manual Import & Online Search ---

    fun loadLyricsForTrack(track: TrackEntity, forceReload: Boolean = false) {
        if (!forceReload && lastLyricsTrackUri == track.uri && _lyricsState.value is LyricsUiState.Loaded) {
            return
        }
        lastLyricsTrackUri = track.uri
        lyricsLoadJob?.cancel()
        lyricsLoadJob = viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            val doc = LyricsRepository.resolveLyricsForTrack(
                context = appContext,
                track = track,
                mountedFolders = _fileBrowserState.value.mountedRoots
            )
            _lyricsState.value = if (doc != null && doc.lines.isNotEmpty()) {
                LyricsUiState.Loaded(doc)
            } else {
                LyricsUiState.NotFound(
                    "No automatic .LRC lyrics found for \"${track.title}\". Import a local .lrc file or search online."
                )
            }
        }
    }

    fun importLrcFromUri(uri: Uri) {
        val track = _playbackState.value.currentTrack
        if (track == null) {
            emitMessage("Select a track first to attach .LRC lyrics")
            return
        }
        viewModelScope.launch {
            val fileName = LocalStorageScanner.resolveDisplayName(appContext, uri) ?: "Imported .LRC"
            LocalStorageScanner.readTextFromUri(appContext, uri)
                .onSuccess { raw ->
                    val doc = LyricsRepository.saveCustomLrcForTrack(
                        context = appContext,
                        trackUri = track.uri,
                        rawLrc = raw,
                        sourceLabel = fileName
                    )
                    if (doc.lines.isNotEmpty()) {
                        lastLyricsTrackUri = track.uri
                        _lyricsState.value = LyricsUiState.Loaded(doc)
                        val syncNote = if (doc.isSynced) "synchronized" else "plain"
                        emitMessage("Loaded $syncNote lyrics: $fileName")
                    } else {
                        emitMessage("The selected .LRC file contained no readable lyric lines")
                    }
                }
                .onFailure { err ->
                    emitMessage("Unable to read .LRC file: ${err.localizedMessage ?: "Access denied"}")
                }
        }
    }

    fun searchLyricsOnline(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            _lyricsSearchState.value = LyricsSearchUiState.Searching
            LyricsRepository.searchOnline(cleaned)
                .onSuccess { items ->
                    _lyricsSearchState.value = LyricsSearchUiState.Results(items)
                }
                .onFailure { err ->
                    _lyricsSearchState.value = LyricsSearchUiState.Error(
                        err.localizedMessage ?: "Lyrics search failed. Check internet connection."
                    )
                }
        }
    }

    fun selectSearchedLyrics(result: LrcSearchResult) {
        val track = _playbackState.value.currentTrack ?: return
        val rawText = result.bestRawText
        if (rawText.isBlank()) {
            emitMessage("Selected entry has no lyrics text")
            return
        }
        viewModelScope.launch {
            val sourceLabel = if (result.hasSynced) "LRCLIB Synced" else "LRCLIB Plain"
            val doc = LyricsRepository.saveCustomLrcForTrack(
                context = appContext,
                trackUri = track.uri,
                rawLrc = rawText,
                sourceLabel = sourceLabel
            )
            if (doc.lines.isNotEmpty()) {
                lastLyricsTrackUri = track.uri
                _lyricsState.value = LyricsUiState.Loaded(doc)
                emitMessage("Applied lyrics: ${result.trackName}")
            }
            _lyricsSearchState.value = LyricsSearchUiState.Idle
        }
    }

    fun dismissLyricsSearch() {
        _lyricsSearchState.value = LyricsSearchUiState.Idle
    }

    fun setLyricsFontSize(sizeSp: Float) {
        val clamped = sizeSp.coerceIn(12f, 26f)
        _lyricsAppearance.update { it.copy(fontSizeSp = clamped) }
        viewModelScope.launch { prefsStore.saveLyricsFontSize(clamped) }
    }

    fun setLyricsAlignment(alignment: LyricsTextAlignment) {
        _lyricsAppearance.update { it.copy(alignment = alignment) }
        viewModelScope.launch { prefsStore.saveLyricsAlignment(alignment) }
    }

    fun resetLyricsAppearance() {
        val default = LyricsAppearanceConfig()
        _lyricsAppearance.value = default
        viewModelScope.launch {
            prefsStore.saveLyricsFontSize(default.fontSizeSp)
            prefsStore.saveLyricsAlignment(default.alignment)
        }
    }

    fun setPreampDb(preampDb: Float) {
        updateEffects { it.copy(preampDb = preampDb.coerceIn(-12f, 12f)) }
    }

    fun setBassBoost(level: Float) {
        updateEffects { it.copy(bassBoost = level.coerceIn(0f, 1f), activeMode = ListeningMode.CUSTOM) }
    }

    fun setTrebleBoost(level: Float) {
        updateEffects { it.copy(trebleBoost = level.coerceIn(0f, 1f), activeMode = ListeningMode.CUSTOM) }
    }

    fun setVirtualizerLevel(level: Float) {
        updateEffects { it.copy(virtualizerLevel = level.coerceIn(0f, 1f), activeMode = ListeningMode.CUSTOM) }
    }

    fun setSurroundMode(mode: SurroundMode) {
        updateEffects { it.copy(surroundMode = mode, activeMode = ListeningMode.CUSTOM) }
    }

    fun setStereoWidth(width: Float) {
        updateEffects { it.copy(stereoWidth = width.coerceIn(0f, 2f), activeMode = ListeningMode.CUSTOM) }
    }

    fun setCompressorEnabled(enabled: Boolean) {
        updateEffects { it.copy(compressorEnabled = enabled) }
    }

    fun setCompressorParams(thresholdDb: Float, ratio: Float, makeupDb: Float) {
        updateEffects {
            it.copy(
                compressorThresholdDb = thresholdDb.coerceIn(-40f, 0f),
                compressorRatio = ratio.coerceIn(1f, 12f),
                compressorMakeupDb = makeupDb.coerceIn(0f, 12f)
            )
        }
    }

    fun setDigitalPassthroughEnabled(enabled: Boolean) {
        PulseAudioEngine.refreshHardwareCapabilities(appContext)
        updateEffects { it.copy(digitalPassthroughEnabled = enabled) }
        val diag = hardwareDiagnostics.value
        val msg = if (enabled) {
            if (diag.supportsEac3Passthrough || diag.supportsAc4Passthrough) {
                "Digital Passthrough enabled for E-AC-3 / AC-4 hardware"
            } else {
                "Passthrough armed (current stereo output falls back to hi-res PCM)"
            }
        } else {
            "Studio 32-bit Float DSP restored"
        }
        emitMessage(msg)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        PulseAudioEngine.setSleepFadeGain(1f)
        _sleepTimerRemainingSec.value = minutes * 60
        emitMessage("Sleep timer set for $minutes min (with gentle fade-out)")

        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemainingSec.value > 0) {
                delay(1000L)
                val remaining = _sleepTimerRemainingSec.value - 1
                _sleepTimerRemainingSec.value = remaining
                if (remaining in 1..15) {
                    PulseAudioEngine.setSleepFadeGain(remaining / 15f)
                }
            }
            mediaController?.pause()
            PulseAudioEngine.setSleepFadeGain(1f)
            emitMessage("Sleep timer finished — playback paused")
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSec.value = 0
        PulseAudioEngine.setSleepFadeGain(1f)
    }

    private fun updateEffects(transform: (AudioEffectsConfig) -> AudioEffectsConfig) {
        val next = transform(effectsConfig.value).clamped()
        applyAndPersistEffects(next)
    }

    private fun applyAndPersistEffects(config: AudioEffectsConfig) {
        PulseAudioEngine.applyEffectsConfig(config)
        persistEffectsJob?.cancel()
        persistEffectsJob = viewModelScope.launch {
            delay(250L)
            prefsStore.saveActiveEffects(config)
        }
    }

    // --- Saved Offline Presets & File Manager .JSON Import/Export ---

    fun saveCurrentPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = effectsConfig.value.copy(activePresetName = trimmed)
            dao.insertPreset(AudioEffectPresetEntity.fromConfig(trimmed, current))
            applyAndPersistEffects(current)
            emitMessage("Saved offline preset \"$trimmed\"")
        }
    }

    fun applySavedPreset(preset: AudioEffectPresetEntity) {
        applyAndPersistEffects(preset.toConfig())
        emitMessage("Loaded preset \"${preset.name}\"")
    }

    fun deleteSavedPreset(presetId: Long) {
        viewModelScope.launch {
            dao.deletePresetById(presetId)
            emitMessage("Deleted preset")
        }
    }

    fun exportEqToJsonUri(uri: Uri) {
        viewModelScope.launch {
            val cfg = effectsConfig.value
            val json = EqJsonCodec.toJsonString(cfg, cfg.activePresetName)
            LocalStorageScanner.writeTextToUri(appContext, uri, json)
                .onSuccess {
                    val fileName = LocalStorageScanner.resolveDisplayName(appContext, uri) ?: "eq_preset.json"
                    emitMessage("Exported EQ file: $fileName")
                }
                .onFailure {
                    emitMessage("Export failed: ${it.localizedMessage ?: "Unable to write file"}")
                }
        }
    }

    fun importEqFromJsonUri(uri: Uri) {
        viewModelScope.launch {
            val fileName = LocalStorageScanner.resolveDisplayName(appContext, uri)
                ?.substringBeforeLast('.') ?: "JSON EQ"
            val textResult = LocalStorageScanner.readTextFromUri(appContext, uri)
            textResult.onFailure {
                emitMessage("Cannot read .json file: ${it.localizedMessage ?: "Access denied"}")
                return@launch
            }
            EqJsonCodec.fromJsonString(textResult.getOrThrow(), fallbackName = fileName)
                .onSuccess { imported ->
                    applyAndPersistEffects(imported)
                    dao.insertPreset(AudioEffectPresetEntity.fromConfig(imported.activePresetName, imported))
                    emitMessage("Applied & saved .JSON EQ: ${imported.activePresetName}")
                }
                .onFailure {
                    emitMessage("Invalid EQ JSON format: ${it.localizedMessage ?: "Parse error"}")
                }
        }
    }

    // --- Online AI Equalizer (Firebase AI Logic) ---

    fun selectGear(gear: ListeningGear) {
        _selectedGear.value = gear
    }

    fun generateAiEqualizer(customPrompt: String?) {
        val currentTrack = _playbackState.value.currentTrack
        val previous = effectsConfig.value
        viewModelScope.launch {
            _aiEqState.value = AiEqUiState.Loading
            val result = aiService.generateForTrack(
                trackTitle = currentTrack?.title ?: "High-Resolution Stereo Reference",
                artist = currentTrack?.artist ?: "Audiophile Library",
                codecLabel = currentTrack?.codecLabel ?: "FLAC",
                sampleRateHz = currentTrack?.sampleRateHz ?: 48_000,
                gear = _selectedGear.value,
                customPrompt = customPrompt
            )
            result.onSuccess { aiConfig ->
                val merged = aiConfig.copy(digitalPassthroughEnabled = previous.digitalPassthroughEnabled)
                applyAndPersistEffects(merged)
                _aiEqState.value = AiEqUiState.Success(
                    config = merged,
                    previousConfig = previous,
                    comparingPrevious = false
                )
                emitMessage("AI Equalizer applied: ${merged.activePresetName}")
            }.onFailure { err ->
                _aiEqState.value = AiEqUiState.Error(
                    err.localizedMessage ?: "Online AI Equalizer failed."
                )
            }
        }
    }

    fun toggleAiAbCompare() {
        val state = _aiEqState.value as? AiEqUiState.Success ?: return
        val nextCompare = !state.comparingPrevious
        applyAndPersistEffects(if (nextCompare) state.previousConfig else state.config)
        _aiEqState.value = state.copy(comparingPrevious = nextCompare)
    }

    fun dismissAiState() {
        _aiEqState.value = AiEqUiState.Idle
    }

    // --- Local Storage & File Browser ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFormatFilter(filter: LibraryFormatFilter) {
        _formatFilter.value = filter
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var imported = 0
            for (uri in uris) {
                runCatching {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val track = LocalStorageScanner.extractTrackMetadata(appContext, uri)
                val id = dao.insertTrack(track)
                if (id != -1L) imported++
            }
            emitMessage(
                if (imported > 0) "Imported $imported high-res audio file(s)"
                else "Selected file(s) already in library"
            )
        }
    }

    fun mountAndScanFolder(treeUri: Uri) {
        viewModelScope.launch {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            prefsStore.addMountedFolder(treeUri.toString())
            val label = LocalStorageScanner.resolveDisplayName(appContext, treeUri) ?: "Storage Folder"
            openBrowserDirectory(treeUri, label, resetStack = true)

            _fileBrowserState.update { it.copy(isScanning = true) }
            val foundTracks = LocalStorageScanner.scanFolderRecursively(appContext, treeUri)
            if (foundTracks.isNotEmpty()) {
                dao.insertTracks(foundTracks)
                emitMessage("Indexed ${foundTracks.size} audio file(s) from $label")
            } else {
                emitMessage("Mounted folder \"$label\" (browse subfolders or .json EQ files)")
            }
            _fileBrowserState.update { it.copy(isScanning = false) }
        }
    }

    fun openBrowserDirectory(uri: Uri, label: String, resetStack: Boolean = false) {
        viewModelScope.launch {
            _fileBrowserState.update { it.copy(isScanning = true) }
            val entries = LocalStorageScanner.listDirectory(appContext, uri)
            _fileBrowserState.update { state ->
                val newStack = if (resetStack) {
                    listOf(label to uri)
                } else {
                    state.navStack + (label to uri)
                }
                state.copy(navStack = newStack, entries = entries, isScanning = false)
            }
        }
    }

    fun navigateBrowserUp() {
        val stack = _fileBrowserState.value.navStack
        if (stack.size <= 1) return
        val parentStack = stack.dropLast(1)
        val target = parentStack.last()
        viewModelScope.launch {
            _fileBrowserState.update { it.copy(isScanning = true) }
            val entries = LocalStorageScanner.listDirectory(appContext, target.second)
            _fileBrowserState.update {
                it.copy(navStack = parentStack, entries = entries, isScanning = false)
            }
        }
    }

    fun playOrImportFileEntry(entry: LocalFileEntry, enqueueOnly: Boolean = false) {
        viewModelScope.launch {
            val existing = dao.findTrackByUri(entry.uri.toString())
            val track = existing ?: run {
                val extracted = LocalStorageScanner.extractTrackMetadata(appContext, entry.uri, entry.name)
                val insertedId = dao.insertTrack(extracted)
                if (insertedId > 0) extracted.copy(id = insertedId) else extracted
            }
            if (enqueueOnly) {
                enqueueTrack(track)
            } else {
                playSingleTrack(track)
            }
        }
    }

    // --- Custom Offline Playlists ---

    fun createPlaylist(name: String, description: String = "", initialTrackIds: List<Long> = emptyList()) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val playlistId = dao.insertPlaylist(
                PlaylistEntity(name = trimmed, description = description.trim())
            )
            initialTrackIds.forEachIndexed { idx, trackId ->
                dao.insertPlaylistTrackCrossRef(PlaylistTrackCrossRef(playlistId, trackId, idx))
            }
            emitMessage("Created playlist \"$trimmed\"")
        }
    }

    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.updatePlaylist(playlist.copy(name = trimmed, updatedAt = System.currentTimeMillis()))
            emitMessage("Renamed playlist to \"$trimmed\"")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            dao.deletePlaylistById(playlistId)
            emitMessage("Deleted playlist")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            val pos = dao.getPlaylistTrackCount(playlistId)
            dao.insertPlaylistTrackCrossRef(PlaylistTrackCrossRef(playlistId, trackId, pos))
            emitMessage("Added track to playlist")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            dao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun saveQueueAsPlaylist(name: String) {
        val ids = _playbackState.value.queue.map { it.id }.filter { it > 0 }
        createPlaylist(name, "Saved from active playback queue", ids)
    }

    private fun syncTelemetryFromTrack(track: TrackEntity) {
        PulseAudioEngine.updateSignalTelemetry(
            SignalTelemetry(
                codecLabel = track.codecLabel,
                mimeType = track.mimeType,
                sampleRateHz = track.sampleRateHz,
                bitDepth = track.bitDepth,
                channelCount = track.channelCount,
                bitrateKbps = track.bitrateKbps
            )
        )
    }

    private fun emitMessage(text: String) {
        _messages.tryEmit(text)
    }

    private fun TrackEntity.toMediaItem(): MediaItem {
        return com.example.audio.buildMediaItemWithArt(appContext, this)
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        hapticsEngine.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
