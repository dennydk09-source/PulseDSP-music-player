package com.example.audio

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi

/**
 * Android Quick Settings Panel Play/Pause Tile for PulseDSP.
 * Lets users start, pause, or resume background high-resolution audio directly
 * from the Quick Settings shade, including from the lockscreen.
 */
@UnstableApi
class PulseQuickPlayTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, PulsePlaybackService::class.java).apply {
            action = PulsePlaybackService.ACTION_TOGGLE_PLAY_BACKGROUND
        }
        ContextCompat.startForegroundService(this, intent)

        // Optimistically toggle visual tile state immediately; service callback syncs exact state
        val willPlay = !PulseAudioEngine.isPlayingNow.value
        applyTileVisual(
            isPlaying = willPlay,
            trackTitle = PulseAudioEngine.currentTrackTitle.value
        )
    }

    private fun refreshTileState() {
        applyTileVisual(
            isPlaying = PulseAudioEngine.isPlayingNow.value,
            trackTitle = PulseAudioEngine.currentTrackTitle.value
        )
    }

    private fun applyTileVisual(isPlaying: Boolean, trackTitle: String) {
        val tile = qsTile ?: return
        tile.label = if (isPlaying) "Pause PulseDSP" else "Play PulseDSP"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                isPlaying && trackTitle.isNotBlank() -> trackTitle
                isPlaying -> "Playing Offline"
                trackTitle.isNotBlank() -> "Paused • $trackTitle"
                else -> "Tap to Play"
            }
        }
        tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
