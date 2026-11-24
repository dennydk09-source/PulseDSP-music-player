package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LaserMint
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TelemetrySlate

private object CoverArtCache {
    private val bitmapCache = object : LruCache<String, Bitmap>(24) {}
    private val missingUris = mutableSetOf<String>()

    suspend fun loadEmbeddedArt(context: Context, trackUri: String): Bitmap? {
        bitmapCache.get(trackUri)?.let { return it }
        synchronized(missingUris) {
            if (trackUri in missingUris) return null
        }
        return withContext(Dispatchers.IO) {
            val decoded = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(trackUri))
                    val bytes = retriever.embeddedPicture
                    if (bytes != null && bytes.isNotEmpty()) {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        var sampleSize = 1
                        while (opts.outWidth / sampleSize > 640 || opts.outHeight / sampleSize > 640) {
                            sampleSize *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                    } else null
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrNull()

            if (decoded != null) {
                bitmapCache.put(trackUri, decoded)
            } else {
                synchronized(missingUris) { missingUris.add(trackUri) }
            }
            decoded
        }
    }
}

@Composable
fun TrackArtwork(
    artworkResName: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    trackUri: String? = null
) {
    val context = LocalContext.current
    val embeddedBitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = trackUri,
        key2 = artworkResName
    ) {
        value = if (!trackUri.isNullOrBlank() && artworkResName == null) {
            CoverArtCache.loadEmbeddedArt(context, trackUri)?.asImageBitmap()
        } else {
            null
        }
    }

    val drawableId = when (artworkResName) {
        "img_art_neon_horizon_1795497456622" -> R.drawable.img_art_neon_horizon_1795497456622
        "img_art_subterranean_pulse_1795497450454" -> R.drawable.img_art_subterranean_pulse_1795497450454
        "img_art_acoustic_glass_1795497450521" -> R.drawable.img_art_acoustic_glass_1795497450521
        else -> null
    }

    when {
        embeddedBitmap != null -> {
            Image(
                bitmap = embeddedBitmap!!,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        drawableId != null -> {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        else -> {
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0E223A), Color(0xFF1F1238), Color(0xFF0A1424))
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = contentDescription,
                    tint = ElectricCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun CodecBadge(
    codecLabel: String,
    modifier: Modifier = Modifier
) {
    val accent = when (codecLabel.uppercase()) {
        "FLAC", "WAV" -> LaserMint
        "EAC3", "AC4", "MP4" -> CyberViolet
        "OPUS", "OGG" -> AmberPulse
        else -> ElectricCyan
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = codecLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

@Composable
fun MiniPlayerBar(
    track: TrackEntity,
    isPlaying: Boolean,
    activeModeLabel: String,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(ElectricCyan.copy(alpha = 0.55f), CyberViolet.copy(alpha = 0.45f))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onOpenPlayer)
            .testTag("mini_player_bar"),
        color = ObsidianElevated.copy(alpha = 0.96f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(
                artworkResName = track.artworkResName,
                contentDescription = track.title,
                trackUri = track.uri,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CodecBadge(codecLabel = track.codecLabel)
                    Text(
                        text = activeModeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetrySlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mini_play_pause_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = DeepVoid,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mini_next_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_track),
                    tint = IceWhite
                )
            }
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = IceWhite
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TelemetrySlate
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun CyberPanelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HairlineBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface
    ) {
        content()
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d", mins, secs)
}

fun formatCountdown(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format("%02d:%02d", mins, secs)
}
