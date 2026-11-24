package com.example.audio

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.DemoAudioGenerator
import com.example.data.LocalStorageScanner
import com.example.data.SignalTelemetry
import com.example.data.local.DspPreferencesStore
import com.example.data.local.PulseDatabase
import com.example.data.local.TrackEntity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Foreground Media3 MediaSessionService powering:
 * - Uninterrupted background audio playback (even with screen off or app swiped)
 * - Status Bar & Lockscreen Media Player showing the next 2 upcoming songs + Shuffle & Repeat icons
 * - Quick Settings Panel Play/Pause Tile toggle (including cold-start playback)
 * - Custom 32-bit float studio DSP + MP4 E-AC-3/AC-4 digital audio passthrough
 */
@UnstableApi
class PulsePlaybackService : MediaSessionService() {

    companion object {
        const val ACTION_TOGGLE_PLAY_BACKGROUND = "com.example.action.TOGGLE_PLAY_BACKGROUND"
        private const val ACTION_CUSTOM_TOGGLE_SHUFFLE = "com.example.action.CUSTOM_TOGGLE_SHUFFLE"
        private const val ACTION_CUSTOM_CYCLE_REPEAT = "com.example.action.CUSTOM_CYCLE_REPEAT"
        private const val NOTIFICATION_CHANNEL_ID = "pulsedsp_playback_channel"

        private val COMMAND_TOGGLE_SHUFFLE = SessionCommand(ACTION_CUSTOM_TOGGLE_SHUFFLE, Bundle.EMPTY)
        private val COMMAND_CYCLE_REPEAT = SessionCommand(ACTION_CUSTOM_CYCLE_REPEAT, Bundle.EMPTY)
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var sessionPlayer: UpNextMetadataPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        PulseAudioEngine.refreshHardwareCapabilities(this)

        // Status bar & lockscreen media notification provider
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_qs_pulse_play)
        setMediaNotificationProvider(notificationProvider)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(PulseAudioEngine.dspProcessor))
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val wrappedPlayer = UpNextMetadataPlayer(player)

        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                        val format = group.getTrackFormat(0)
                        val mime = format.sampleMimeType ?: "audio/unknown"
                        val codec = LocalStorageScanner.inferCodecLabel(
                            format.containerMimeType ?: mime,
                            mime
                        )
                        val bitDepth = when (format.pcmEncoding) {
                            C.ENCODING_PCM_FLOAT -> 32
                            C.ENCODING_PCM_24BIT -> 24
                            C.ENCODING_PCM_32BIT -> 32
                            else -> if (codec == "FLAC") 24 else 16
                        }
                        PulseAudioEngine.updateSignalTelemetry(
                            SignalTelemetry(
                                codecLabel = codec,
                                mimeType = mime,
                                sampleRateHz = format.sampleRate.takeIf { it > 0 } ?: 48_000,
                                bitDepth = bitDepth,
                                channelCount = format.channelCount.takeIf { it > 0 } ?: 2,
                                bitrateKbps = (format.bitrate.takeIf { it > 0 } ?: 0) / 1000
                            )
                        )
                        break
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    PulseAudioEngine.clearVisualizer()
                }
                val title = player.mediaMetadata.title?.toString().orEmpty()
                PulseAudioEngine.updatePlaybackBanner(isPlaying, title)
                notifyQuickSettingsTile()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val title = mediaItem?.mediaMetadata?.title?.toString().orEmpty()
                PulseAudioEngine.updatePlaybackBanner(player.isPlaying, title)
                notifyQuickSettingsTile()
                wrappedPlayer.notifyUpNextMetadataChanged()
                updateNotificationCustomButtons()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                wrappedPlayer.notifyUpNextMetadataChanged()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                wrappedPlayer.notifyUpNextMetadataChanged()
                updateNotificationCustomButtons()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                wrappedPlayer.notifyUpNextMetadataChanged()
                updateNotificationCustomButtons()
            }
        })

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(COMMAND_TOGGLE_SHUFFLE)
                    .add(COMMAND_CYCLE_REPEAT)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                val p = exoPlayer ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                when (customCommand.customAction) {
                    ACTION_CUSTOM_TOGGLE_SHUFFLE -> {
                        val nextShuffle = !p.shuffleModeEnabled
                        p.shuffleModeEnabled = nextShuffle
                        serviceScope.launch {
                            DspPreferencesStore(applicationContext).saveShuffleEnabled(nextShuffle)
                        }
                        updateNotificationCustomButtons()
                    }
                    ACTION_CUSTOM_CYCLE_REPEAT -> {
                        val nextRepeat = when (p.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        p.repeatMode = nextRepeat
                        serviceScope.launch {
                            DspPreferencesStore(applicationContext).saveRepeatMode(nextRepeat)
                        }
                        updateNotificationCustomButtons()
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        exoPlayer = player
        sessionPlayer = wrappedPlayer
        mediaSession = MediaSession.Builder(this, wrappedPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .setCustomLayout(buildNotificationCommandButtons(player))
            .build()
    }

    private fun buildNotificationCommandButtons(player: Player): List<CommandButton> {
        val shuffleIcon = if (player.shuffleModeEnabled) {
            R.drawable.ic_notif_shuffle_on
        } else {
            R.drawable.ic_notif_shuffle_off
        }
        val shuffleLabel = if (player.shuffleModeEnabled) "Shuffle On" else "Shuffle Off"

        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_notif_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.ic_notif_repeat_all
            else -> R.drawable.ic_notif_repeat_off
        }
        val repeatLabel = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> "Repeat One"
            Player.REPEAT_MODE_ALL -> "Repeat All"
            else -> "Repeat Off"
        }

        val shuffleBtn = CommandButton.Builder()
            .setDisplayName(shuffleLabel)
            .setIconResId(shuffleIcon)
            .setSessionCommand(COMMAND_TOGGLE_SHUFFLE)
            .build()

        val repeatBtn = CommandButton.Builder()
            .setDisplayName(repeatLabel)
            .setIconResId(repeatIcon)
            .setSessionCommand(COMMAND_CYCLE_REPEAT)
            .build()

        return listOf(shuffleBtn, repeatBtn)
    }

    private fun updateNotificationCustomButtons() {
        val session = mediaSession ?: return
        val player = exoPlayer ?: return
        session.setCustomLayout(buildNotificationCommandButtons(player))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_PLAY_BACKGROUND) {
            handleQuickSettingsToggle()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleQuickSettingsToggle() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            return
        }
        if (player.mediaItemCount > 0) {
            player.play()
            return
        }
        // Cold-start playback from Quick Settings tile: load offline Room tracks automatically
        serviceScope.launch {
            val dao = PulseDatabase.getInstance(applicationContext).pulseDao()
            var offlineTracks = withContext(Dispatchers.IO) {
                dao.observeAllTracks().first()
            }
            if (offlineTracks.isEmpty()) {
                val builtIn = DemoAudioGenerator.ensureBuiltInTracks(applicationContext)
                withContext(Dispatchers.IO) { dao.insertTracks(builtIn) }
                offlineTracks = builtIn
            }
            if (offlineTracks.isNotEmpty()) {
                val prefsStore = DspPreferencesStore(applicationContext)
                val savedShuffle = prefsStore.shuffleEnabledFlow.first()
                val savedRepeat = prefsStore.repeatModeFlow.first()
                val items = offlineTracks.map { buildMediaItemWithArt(this@PulsePlaybackService, it) }
                player.shuffleModeEnabled = savedShuffle
                player.repeatMode = savedRepeat
                player.setMediaItems(items, 0, 0L)
                player.prepare()
                player.play()
                updateNotificationCustomButtons()
            }
        }
    }

    private fun notifyQuickSettingsTile() {
        runCatching {
            TileService.requestListeningState(
                this,
                ComponentName(this, PulseQuickPlayTileService::class.java)
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing seamlessly in background if playback is active; stop only when idle/paused
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        PulseAudioEngine.updatePlaybackBanner(false, "")
        PulseAudioEngine.clearVisualizer()
        notifyQuickSettingsTile()
        mediaSession?.run {
            exoPlayer?.release()
            release()
        }
        mediaSession = null
        sessionPlayer = null
        exoPlayer = null
        super.onDestroy()
    }
}

/**
 * ForwardingPlayer wrapper that enriches the MediaSession's MediaMetadata with the next 2
 * upcoming song titles (honoring live Media3 Shuffle and Repeat timeline order) so the
 * Android Status Bar & Lockscreen background player displays Up Next directly.
 */
@UnstableApi
private class UpNextMetadataPlayer(player: Player) : ForwardingPlayer(player) {

    private val registeredListeners = mutableSetOf<Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        registeredListeners.add(listener)
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        registeredListeners.remove(listener)
        super.removeListener(listener)
    }

    fun notifyUpNextMetadataChanged() {
        val enriched = getMediaMetadata()
        registeredListeners.toList().forEach { listener ->
            listener.onMediaMetadataChanged(enriched)
        }
    }

    override fun getMediaMetadata(): MediaMetadata {
        val base = super.getMediaMetadata()
        val upcoming = resolveNextTwoTitles()
        if (upcoming.isEmpty()) return base

        val upNextText = when (upcoming.size) {
            1 -> "Next: 1. ${upcoming[0]}"
            else -> "Next: 1. ${upcoming[0]} • 2. ${upcoming[1]}"
        }

        val baseArtist = base.artist?.toString().orEmpty()
        val combinedSubtitle = if (baseArtist.isNotBlank()) {
            "$baseArtist  •  $upNextText"
        } else {
            upNextText
        }

        return base.buildUpon()
            .setArtist(combinedSubtitle)
            .setSubtitle(upNextText)
            .setDescription(upNextText)
            .build()
    }

    private fun resolveNextTwoTitles(): List<String> {
        val tl = currentTimeline
        if (tl.isEmpty || mediaItemCount <= 1) return emptyList()
        val currentIdx = currentMediaItemIndex
        if (currentIdx !in 0 until mediaItemCount) return emptyList()

        // Use REPEAT_MODE_ALL when peeking upcoming tracks if repeat is REPEAT_MODE_ONE
        // so the user still sees what follows in the queue order
        val navRepeatMode = if (repeatMode == Player.REPEAT_MODE_ONE) {
            Player.REPEAT_MODE_ALL
        } else {
            repeatMode
        }

        val result = mutableListOf<String>()
        val firstIdx = tl.getNextWindowIndex(currentIdx, navRepeatMode, shuffleModeEnabled)
        if (firstIdx != C.INDEX_UNSET && firstIdx in 0 until mediaItemCount && firstIdx != currentIdx) {
            val firstTitle = getMediaItemAt(firstIdx).mediaMetadata.title?.toString()
            if (!firstTitle.isNullOrBlank()) result.add(firstTitle)

            val secondIdx = tl.getNextWindowIndex(firstIdx, navRepeatMode, shuffleModeEnabled)
            if (secondIdx != C.INDEX_UNSET && secondIdx in 0 until mediaItemCount &&
                secondIdx != firstIdx && secondIdx != currentIdx
            ) {
                val secondTitle = getMediaItemAt(secondIdx).mediaMetadata.title?.toString()
                if (!secondTitle.isNullOrBlank()) result.add(secondTitle)
            }
        }
        return result
    }
}

fun buildMediaItemWithArt(context: Context, track: TrackEntity): MediaItem {
    val metaBuilder = MediaMetadata.Builder()
        .setTitle(track.title)
        .setArtist(track.artist)
        .setAlbumTitle(track.album)

    val embeddedArtBytes = runCatching {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(track.uri))
            retriever.embeddedPicture
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    if (embeddedArtBytes != null && embeddedArtBytes.isNotEmpty()) {
        runCatching {
            val bmp = BitmapFactory.decodeByteArray(embeddedArtBytes, 0, embeddedArtBytes.size)
            if (bmp != null) {
                val scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true)
                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                metaBuilder.setArtworkData(stream.toByteArray(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }
    } else {
        val artResId = when (track.artworkResName) {
            "img_art_neon_horizon_1795497456622" -> R.drawable.img_art_neon_horizon_1795497456622
            "img_art_subterranean_pulse_1795497450454" -> R.drawable.img_art_subterranean_pulse_1795497450454
            "img_art_acoustic_glass_1795497450521" -> R.drawable.img_art_acoustic_glass_1795497450521
            else -> R.drawable.ic_pulsedsp_logo_1795497442908
        }

        runCatching {
            val bmp = BitmapFactory.decodeResource(context.resources, artResId)
            if (bmp != null) {
                val scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true)
                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, stream)
                metaBuilder.setArtworkData(stream.toByteArray(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        }
    }

    return MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(Uri.parse(track.uri))
        .setMediaMetadata(metaBuilder.build())
        .build()
}
