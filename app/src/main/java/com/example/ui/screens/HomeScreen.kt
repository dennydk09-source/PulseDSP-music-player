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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AudioEffectsConfig
import com.example.data.BuiltInModeProfiles
import com.example.data.ListeningMode
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.TrackEntity
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
import com.example.ui.theme.LaserMint
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RackWell
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TelemetrySlate
import com.example.viewmodel.LibraryFormatFilter

@Composable
fun HomeScreen(
    tracks: List<TrackEntity>,
    playlists: List<PlaylistWithTracks>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    effectsConfig: AudioEffectsConfig,
    searchQuery: String,
    formatFilter: LibraryFormatFilter,
    onSearchChange: (String) -> Unit,
    onFilterChange: (LibraryFormatFilter) -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onEnqueueTrack: (TrackEntity) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onImportAudioFiles: () -> Unit,
    onMountFolder: () -> Unit,
    onSelectMode: (ListeningMode) -> Unit,
    onOpenAudioEffects: () -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onRenamePlaylist: (PlaylistEntity, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (PlaylistWithTracks, Boolean) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onRemoveTrackFromPlaylist: (Long, Long) -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistWithTracks?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }

    val filteredTracks = remember(tracks, searchQuery, formatFilter) {
        tracks.filter { track ->
            val matchesQuery = searchQuery.isBlank() ||
                track.title.contains(searchQuery, ignoreCase = true) ||
                track.artist.contains(searchQuery, ignoreCase = true) ||
                track.codecLabel.contains(searchQuery, ignoreCase = true)
            val matchesFormat = when (formatFilter) {
                LibraryFormatFilter.ALL -> true
                LibraryFormatFilter.LOSSLESS -> track.codecLabel in setOf("FLAC", "WAV")
                LibraryFormatFilter.SURROUND_MP4 -> track.codecLabel in setOf("EAC3", "AC4", "MP4")
                LibraryFormatFilter.COMPRESSED -> track.codecLabel in setOf("MP3", "OGG", "OPUS", "M4A", "AAC")
            }
            matchesQuery && matchesFormat
        }
    }

    // Keep detail dialog fresh when playlist tracks update
    val liveDetailPlaylist = selectedPlaylistForDetail?.let { sel ->
        playlists.firstOrNull { it.playlist.id == sel.playlist.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Brand Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ObsidianElevated, ObsidianSurface)
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(ElectricCyan.copy(alpha = 0.55f), CyberViolet.copy(alpha = 0.45f))
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PULSEDSP",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Offline Hi-Res Studio",
                                style = MaterialTheme.typography.headlineMedium,
                                color = IceWhite
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberViolet.copy(alpha = 0.18f))
                                .border(1.dp, CyberViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenAudioEffects)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("home_effects_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Equalizer,
                                    contentDescription = stringResource(R.string.cd_open_audio_effects),
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (effectsConfig.enabled) effectsConfig.activeMode.tag else "DSP OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IceWhite
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onImportAudioFiles,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_import_audio_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = DeepVoid
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.import_audio_files))
                        }
                        OutlinedButton(
                            onClick = onMountFolder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_mount_folder_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Rounded.CreateNewFolder,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.mount_music_folder), color = IceWhite)
                        }
                    }
                }
            }
        }

        // Quick Listening Modes Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderRow(
                    title = "Listening Modes",
                    subtitle = "Tap to switch Audio Effects instantly",
                    trailing = {
                        TextButton(onClick = onOpenAudioEffects) {
                            Text("All Effects", color = ElectricCyan)
                        }
                    }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BuiltInModeProfiles.curatedModes) { mode ->
                        val active = effectsConfig.enabled && effectsConfig.activeMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) ElectricCyan.copy(alpha = 0.18f) else ObsidianSurface)
                                .border(
                                    1.dp,
                                    if (active) ElectricCyan else HairlineBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("quick_mode_${mode.name.lowercase()}")
                        ) {
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) ElectricCyan else IceWhite
                                )
                                Text(
                                    text = mode.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) LaserMint else TelemetrySlate
                                )
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberViolet.copy(alpha = 0.22f))
                                .border(1.dp, CyberViolet, RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenAudioEffects)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("quick_mode_ai")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("AI Equalizer", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                    Text("ONLINE", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Offline Playlists
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderRow(
                    title = "Custom Playlists",
                    subtitle = "${playlists.size} offline playlists",
                    trailing = {
                        OutlinedButton(
                            onClick = { showCreatePlaylistDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("create_playlist_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.create_playlist), color = IceWhite)
                        }
                    }
                )

                if (playlists.isEmpty()) {
                    CyberPanelCard {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistPlay,
                                contentDescription = null,
                                tint = TelemetrySlate,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Create your first offline playlist to organize high-res tracks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TelemetrySlate
                            )
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(playlists, key = { it.playlist.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .width(204.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(ObsidianSurface)
                                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                                    .clickable { selectedPlaylistForDetail = item }
                                    .padding(14.dp)
                                    .testTag("playlist_card_${item.playlist.id}")
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CyberViolet.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.PlaylistPlay,
                                                contentDescription = null,
                                                tint = ElectricCyan
                                            )
                                        }
                                        IconButton(
                                            onClick = { onPlayPlaylist(item, false) },
                                            enabled = item.tracks.isNotEmpty()
                                        ) {
                                            Icon(
                                                Icons.Rounded.PlayArrow,
                                                contentDescription = "Play playlist",
                                                tint = if (item.tracks.isNotEmpty()) ElectricCyan else TelemetrySlate
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IceWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.tracks.size} tracks • Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TelemetrySlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Audio Library Search & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeaderRow(
                    title = "Local Audio Library",
                    subtitle = "${tracks.size} tracks ready offline",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onPlayAll(false) },
                                modifier = Modifier.testTag("play_all_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play all", tint = ElectricCyan)
                            }
                            IconButton(
                                onClick = { onPlayAll(true) },
                                modifier = Modifier.testTag("shuffle_all_button")
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle all", tint = CyberViolet)
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                    placeholder = { Text("Search FLAC, WAV, EAC3, AC4, artist...", color = TelemetrySlate) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ElectricCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = TelemetrySlate)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    )
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibraryFormatFilter.entries) { filter ->
                        val selected = formatFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = ObsidianSurface,
                                labelColor = TelemetrySlate
                            )
                        )
                    }
                }
            }
        }

        if (filteredTracks.isEmpty()) {
            item {
                CyberPanelCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = TelemetrySlate,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("No matching tracks", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                        Text(
                            "Import local FLAC, WAV, MP3, OGG, M4A, AAC, EAC3, AC4 or MP4 files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelemetrySlate
                        )
                    }
                }
            }
        } else {
            items(filteredTracks, key = { it.id }) { track ->
                val active = track.id == currentTrackId
                TrackRowCard(
                    track = track,
                    isActive = active,
                    isPlaying = active && isPlaying,
                    onClick = { onPlayTrack(track) },
                    onEnqueue = { onEnqueueTrack(track) },
                    onAddToPlaylist = { trackToAddToPlaylist = track }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (liveDetailPlaylist != null) {
        PlaylistDetailDialog(
            playlistWithTracks = liveDetailPlaylist,
            allTracks = tracks,
            onDismiss = { selectedPlaylistForDetail = null },
            onPlayAll = { shuffle ->
                onPlayPlaylist(liveDetailPlaylist, shuffle)
                selectedPlaylistForDetail = null
            },
            onRenameClick = { playlistToRename = liveDetailPlaylist.playlist },
            onDeleteClick = {
                onDeletePlaylist(liveDetailPlaylist.playlist.id)
                selectedPlaylistForDetail = null
            },
            onAddTrack = { trackId -> onAddTrackToPlaylist(liveDetailPlaylist.playlist.id, trackId) },
            onRemoveTrack = { trackId -> onRemoveTrackFromPlaylist(liveDetailPlaylist.playlist.id, trackId) }
        )
    }

    if (playlistToRename != null) {
        val target = playlistToRename!!
        var renameText by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = ObsidianElevated,
            title = { Text("Rename Playlist", color = IceWhite) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Playlist Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenamePlaylist(target, renameText)
                        playlistToRename = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) { Text("Cancel") }
            }
        )
    }

    if (trackToAddToPlaylist != null) {
        val track = trackToAddToPlaylist!!
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            containerColor = ObsidianElevated,
            title = { Text("Add to Playlist", color = IceWhite) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Create a playlist first.", color = TelemetrySlate)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlists.forEach { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RackWell)
                                    .clickable {
                                        onAddTrackToPlaylist(pl.playlist.id, track.id)
                                        trackToAddToPlaylist = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pl.playlist.name, color = IceWhite, style = MaterialTheme.typography.titleSmall)
                                Text("${pl.tracks.size} tracks", color = TelemetrySlate, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    trackToAddToPlaylist = null
                    showCreatePlaylistDialog = true
                }) { Text("New Playlist", color = ElectricCyan) }
            },
            dismissButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun TrackRowCard(
    track: TrackEntity,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onEnqueue: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) ElectricCyan.copy(alpha = 0.12f) else ObsidianSurface)
            .border(
                width = 1.dp,
                color = if (isActive) ElectricCyan.copy(alpha = 0.7f) else HairlineBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            TrackArtwork(
                artworkResName = track.artworkResName,
                contentDescription = track.title,
                trackUri = track.uri,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepVoid.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Playing", tint = ElectricCyan)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isActive) ElectricCyan else IceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TelemetrySlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CodecBadge(codecLabel = track.codecLabel)
                Text(
                    text = "${track.sampleRateHz / 1000}kHz • ${formatDuration(track.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetrySlate
                )
            }
        }
        IconButton(onClick = onEnqueue, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Add to queue",
                tint = TelemetrySlate
            )
        }
        IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = stringResource(R.string.cd_add_to_playlist),
                tint = ElectricCyan
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianElevated,
        title = { Text("New Offline Playlist", color = IceWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("playlist_name_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, desc) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepVoid),
                modifier = Modifier.testTag("confirm_create_playlist_button")
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PlaylistDetailDialog(
    playlistWithTracks: PlaylistWithTracks,
    allTracks: List<TrackEntity>,
    onDismiss: () -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddTrack: (Long) -> Unit,
    onRemoveTrack: (Long) -> Unit
) {
    val existingIds = remember(playlistWithTracks.tracks) {
        playlistWithTracks.tracks.map { it.id }.toSet()
    }
    val addableTracks = remember(allTracks, existingIds) {
        allTracks.filter { it.id !in existingIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianElevated,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playlistWithTracks.playlist.name,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRenameClick) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = ElectricCyan)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = SignalRed)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onPlayAll(false) },
                        enabled = playlistWithTracks.tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepVoid),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick = { onPlayAll(true) },
                        enabled = playlistWithTracks.tracks.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shuffle", color = IceWhite)
                    }
                }

                Text("Tracks (${playlistWithTracks.tracks.size})", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                if (playlistWithTracks.tracks.isEmpty()) {
                    Text("No tracks in this playlist yet.", color = TelemetrySlate, style = MaterialTheme.typography.bodySmall)
                } else {
                    playlistWithTracks.tracks.take(6).forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RackWell)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.title, color = IceWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onRemoveTrack(t.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove track", tint = SignalRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (addableTracks.isNotEmpty()) {
                    Text("Quick Add from Library", style = MaterialTheme.typography.labelMedium, color = LaserMint)
                    addableTracks.take(4).forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianSurface)
                                .clickable { onAddTrack(t.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.title, color = TelemetrySlate, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.Add, contentDescription = "Add", tint = LaserMint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = ElectricCyan) }
        }
    )
}
