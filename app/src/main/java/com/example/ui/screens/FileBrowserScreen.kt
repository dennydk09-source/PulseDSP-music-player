package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FileEntryKind
import com.example.data.LocalFileEntry
import com.example.ui.components.CodecBadge
import com.example.ui.components.CyberPanelCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LaserMint
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TelemetrySlate
import com.example.viewmodel.FileBrowserUiState

@Composable
fun FileBrowserScreen(
    browserState: FileBrowserUiState,
    onMountFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onImportEqJson: () -> Unit,
    onSelectRoot: (Uri, String) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenDirectory: (Uri, String) -> Unit,
    onPlayEntry: (LocalFileEntry) -> Unit,
    onEnqueueEntry: (LocalFileEntry) -> Unit,
    onApplyJsonEqEntry: (Uri) -> Unit,
    onApplyLrcEntry: (Uri) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("file_browser_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeaderRow(
                title = "Local File Browser",
                subtitle = "FLAC, WAV, MP3, OGG, M4A, AAC, EAC3, AC4, MP4 & .JSON EQ"
            )
        }

        // Action Bar: Mount Folder, Import Audio, Import .JSON EQ
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMountFolder,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepVoid),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("files_mount_folder_button")
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Folder")
                    }
                    OutlinedButton(
                        onClick = onImportFiles,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("files_import_audio_button")
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Audio", color = IceWhite)
                    }
                }
                OutlinedButton(
                    onClick = onImportEqJson,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberViolet.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("files_import_eq_json_button")
                ) {
                    Icon(Icons.Rounded.DataObject, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import .JSON Equalizer File from File Manager", color = IceWhite)
                }
            }
        }

        // Mounted Storage Roots
        if (browserState.mountedRoots.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(browserState.mountedRoots) { rootUri ->
                        val label = rootUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Folder"
                        QuickPillButton(
                            text = label,
                            selected = browserState.navStack.firstOrNull()?.second == rootUri,
                            accent = LaserMint,
                            onClick = { onSelectRoot(rootUri, label) }
                        )
                    }
                }
            }
        }

        // Breadcrumb Navigation Bar
        if (browserState.navStack.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateUp,
                        enabled = browserState.navStack.size > 1,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Parent folder",
                            tint = if (browserState.navStack.size > 1) ElectricCyan else TelemetrySlate.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        text = browserState.navStack.joinToString(" / ") { it.first },
                        style = MaterialTheme.typography.labelMedium,
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (browserState.isScanning) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Directory Listing or Empty State
        if (browserState.mountedRoots.isEmpty()) {
            item {
                CyberPanelCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(44.dp))
                        Text("Browse Local Storage", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                        Text(
                            "Tap 'Open Folder' to mount your Music, Downloads, or EQ presets folder using Android's secure Storage Access Framework. You can play FLAC, WAV, EAC3, AC4, MP4 files or tap any .json file to load an EQ profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetrySlate
                        )
                    }
                }
            }
        } else if (browserState.entries.isEmpty() && !browserState.isScanning) {
            item {
                CyberPanelCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("No audio or .json files in this folder", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                        Text("Try opening a subfolder or picking another music directory.", style = MaterialTheme.typography.bodySmall, color = TelemetrySlate)
                    }
                }
            }
        } else {
            items(browserState.entries, key = { it.uri.toString() }) { entry ->
                FileEntryRow(
                    entry = entry,
                    onClick = {
                        when (entry.kind) {
                            FileEntryKind.DIRECTORY -> onOpenDirectory(entry.uri, entry.name)
                            FileEntryKind.AUDIO -> onPlayEntry(entry)
                            FileEntryKind.EQ_JSON -> onApplyJsonEqEntry(entry.uri)
                            FileEntryKind.LYRICS_LRC -> onApplyLrcEntry(entry.uri)
                        }
                    },
                    onEnqueue = { onEnqueueEntry(entry) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(86.dp)) }
    }
}

@Composable
private fun FileEntryRow(
    entry: LocalFileEntry,
    onClick: () -> Unit,
    onEnqueue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianBase),
            contentAlignment = Alignment.Center
        ) {
            when (entry.kind) {
                FileEntryKind.DIRECTORY -> Icon(Icons.Rounded.Folder, contentDescription = "Folder", tint = AmberPulse)
                FileEntryKind.AUDIO -> Icon(Icons.Rounded.AudioFile, contentDescription = "Audio file", tint = ElectricCyan)
                FileEntryKind.EQ_JSON -> Icon(Icons.Rounded.DataObject, contentDescription = "EQ JSON", tint = CyberViolet)
                FileEntryKind.LYRICS_LRC -> Icon(Icons.Rounded.Lyrics, contentDescription = "LRC lyrics file", tint = LaserMint)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                color = IceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CodecBadge(codecLabel = entry.codecLabel)
                if (entry.sizeBytes > 0L) {
                    Text(
                        text = formatFileSize(entry.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = TelemetrySlate
                    )
                } else if (entry.kind == FileEntryKind.DIRECTORY) {
                    Text("Tap to browse", style = MaterialTheme.typography.labelSmall, color = TelemetrySlate)
                }
            }
        }
        if (entry.kind == FileEntryKind.AUDIO) {
            IconButton(onClick = onEnqueue, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Enqueue", tint = TelemetrySlate)
            }
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = ElectricCyan)
            }
        } else if (entry.kind == FileEntryKind.EQ_JSON) {
            Text("LOAD EQ", style = MaterialTheme.typography.labelMedium, color = CyberViolet)
        } else if (entry.kind == FileEntryKind.LYRICS_LRC) {
            Text("LOAD LRC", style = MaterialTheme.typography.labelMedium, color = LaserMint)
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
