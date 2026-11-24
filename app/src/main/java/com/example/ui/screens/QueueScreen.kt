package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.CodecBadge
import com.example.ui.components.CyberPanelCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDuration
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TelemetrySlate
import com.example.viewmodel.PlaybackUiState

@Composable
fun QueueScreen(
    playbackState: PlaybackUiState,
    onJumpToIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("queue_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeaderRow(
                title = "Playback Queue",
                subtitle = "${playbackState.queue.size} tracks queued • Offline ready"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("queue_shuffle_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (playbackState.shuffleEnabled) ElectricCyan else HairlineBorder
                    )
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) ElectricCyan else TelemetrySlate,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (playbackState.shuffleEnabled) "Shuffle On" else "Shuffle",
                        color = if (playbackState.shuffleEnabled) ElectricCyan else IceWhite
                    )
                }

                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    enabled = playbackState.queue.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("queue_save_playlist_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = CyberViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save List", color = IceWhite)
                }

                IconButton(
                    onClick = onClearQueue,
                    enabled = playbackState.queue.isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                        .testTag("queue_clear_button")
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear queue", tint = SignalRed)
                }
            }
        }

        if (playbackState.queue.isEmpty()) {
            item {
                CyberPanelCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = TelemetrySlate,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("Queue is empty", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                        Text(
                            "Play any track or folder from Home or Files to populate your Up Next queue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetrySlate
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = playbackState.queue,
                key = { index, track -> "${index}_${track.id}" }
            ) { index, track ->
                val isCurrent = index == playbackState.currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCurrent) ElectricCyan.copy(alpha = 0.14f) else ObsidianSurface)
                        .border(
                            1.dp,
                            if (isCurrent) ElectricCyan else HairlineBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onJumpToIndex(index) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("queue_item_$index"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d", index + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrent) ElectricCyan else TelemetrySlate,
                        modifier = Modifier.width(26.dp)
                    )
                    TrackArtwork(
                        artworkResName = track.artworkResName,
                        contentDescription = track.title,
                        trackUri = track.uri,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrent) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = "Now playing",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCurrent) ElectricCyan else IceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CodecBadge(codecLabel = track.codecLabel)
                            Text(
                                text = formatDuration(track.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = TelemetrySlate
                            )
                        }
                    }

                    IconButton(
                        onClick = { onMoveItem(index, index - 1) },
                        enabled = index > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = "Move up",
                            tint = if (index > 0) IceWhite else TelemetrySlate.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onMoveItem(index, index + 1) },
                        enabled = index < playbackState.queue.lastIndex,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowDownward,
                            contentDescription = "Move down",
                            tint = if (index < playbackState.queue.lastIndex) IceWhite else TelemetrySlate.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onRemoveItem(index) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove from queue",
                            tint = SignalRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }

    if (showSaveDialog) {
        var playlistName by remember { mutableStateOf("Queue Session") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = ObsidianElevated,
            title = { Text("Save Queue as Playlist", color = IceWhite) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveQueueAsPlaylist(playlistName)
                        showSaveDialog = false
                    },
                    enabled = playlistName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepVoid)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}
