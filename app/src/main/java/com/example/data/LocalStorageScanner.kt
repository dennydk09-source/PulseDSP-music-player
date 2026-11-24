package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class FileEntryKind { DIRECTORY, AUDIO, EQ_JSON, LYRICS_LRC }

data class LocalFileEntry(
    val uri: Uri,
    val name: String,
    val kind: FileEntryKind,
    val codecLabel: String,
    val sizeBytes: Long,
    val lastModified: Long
)

/**
 * Play-Policy-compliant local storage browser and metadata scanner backed by Android's
 * Storage Access Framework (DocumentFile + persistable URI permissions).
 * Supports FLAC, WAV, MP3, OGG/OPUS, M4A, AAC, EAC3, AC4, MP4, plus .JSON EQ preset files.
 */
object LocalStorageScanner {

    val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "flac", "wav", "mp3", "ogg", "oga", "opus",
        "m4a", "aac", "eac3", "ec3", "ac4", "mp4", "m4b"
    )

    val IMPORT_AUDIO_MIME_TYPES = arrayOf(
        "audio/*",
        "video/mp4",
        "application/ogg",
        "application/octet-stream"
    )

    val IMPORT_JSON_MIME_TYPES = arrayOf(
        "application/json",
        "text/plain",
        "application/octet-stream"
    )

    val IMPORT_LRC_MIME_TYPES = arrayOf(
        "text/plain",
        "application/octet-stream",
        "*/*"
    )

    fun isAudioFileName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_AUDIO_EXTENSIONS
    }

    fun isJsonFileName(name: String): Boolean {
        return name.lowercase().endsWith(".json")
    }

    fun isLrcFileName(name: String): Boolean {
        return name.lowercase().endsWith(".lrc")
    }

    fun inferCodecLabel(name: String, mimeType: String?): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = mimeType.orEmpty().lowercase()
        return when {
            ext == "eac3" || ext == "ec3" || mime.contains("eac3") -> "EAC3"
            ext == "ac4" || mime.contains("ac4") -> "AC4"
            ext == "flac" || mime.contains("flac") -> "FLAC"
            ext == "wav" || mime.contains("wav") || mime.contains("raw") -> "WAV"
            ext == "opus" || mime.contains("opus") -> "OPUS"
            ext == "ogg" || ext == "oga" || mime.contains("ogg") || mime.contains("vorbis") -> "OGG"
            ext == "m4a" || ext == "m4b" -> "M4A"
            ext == "aac" || mime.contains("mp4a") || mime.contains("aac") -> "AAC"
            ext == "mp4" || mime.contains("mp4") -> "MP4"
            ext == "mp3" || mime.contains("mpeg") -> "MP3"
            else -> ext.uppercase().ifBlank { "AUDIO" }
        }
    }

    suspend fun listDirectory(context: Context, directoryUri: Uri): List<LocalFileEntry> =
        withContext(Dispatchers.IO) {
            val docDir = DocumentFile.fromTreeUri(context, directoryUri)
                ?: DocumentFile.fromSingleUri(context, directoryUri)
                ?: return@withContext emptyList()

            docDir.listFiles()
                .mapNotNull { doc ->
                    val name = doc.name ?: return@mapNotNull null
                    when {
                        doc.isDirectory -> LocalFileEntry(
                            uri = doc.uri,
                            name = name,
                            kind = FileEntryKind.DIRECTORY,
                            codecLabel = "DIR",
                            sizeBytes = 0L,
                            lastModified = doc.lastModified()
                        )
                        isJsonFileName(name) -> LocalFileEntry(
                            uri = doc.uri,
                            name = name,
                            kind = FileEntryKind.EQ_JSON,
                            codecLabel = "JSON",
                            sizeBytes = doc.length(),
                            lastModified = doc.lastModified()
                        )
                        isLrcFileName(name) -> LocalFileEntry(
                            uri = doc.uri,
                            name = name,
                            kind = FileEntryKind.LYRICS_LRC,
                            codecLabel = "LRC",
                            sizeBytes = doc.length(),
                            lastModified = doc.lastModified()
                        )
                        isAudioFileName(name) || doc.type?.startsWith("audio/") == true -> LocalFileEntry(
                            uri = doc.uri,
                            name = name,
                            kind = FileEntryKind.AUDIO,
                            codecLabel = inferCodecLabel(name, doc.type),
                            sizeBytes = doc.length(),
                            lastModified = doc.lastModified()
                        )
                        else -> null
                    }
                }
                .sortedWith(
                    compareBy<LocalFileEntry> {
                        when (it.kind) {
                            FileEntryKind.DIRECTORY -> 0
                            FileEntryKind.AUDIO -> 1
                            FileEntryKind.EQ_JSON -> 2
                            FileEntryKind.LYRICS_LRC -> 3
                        }
                    }.thenBy { it.name.lowercase() }
                )
        }

    suspend fun scanFolderRecursively(
        context: Context,
        treeUri: Uri,
        maxFiles: Int = 200
    ): List<TrackEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val found = mutableListOf<DocumentFile>()
        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 4 || found.size >= maxFiles) return
            for (child in dir.listFiles()) {
                if (found.size >= maxFiles) break
                if (child.isDirectory) {
                    walk(child, depth + 1)
                } else {
                    val name = child.name.orEmpty()
                    if (isAudioFileName(name) || child.type?.startsWith("audio/") == true) {
                        found.add(child)
                    }
                }
            }
        }
        walk(root, 0)
        found.map { extractTrackMetadata(context, it.uri, it.name) }
    }

    suspend fun extractTrackMetadata(
        context: Context,
        uri: Uri,
        knownDisplayName: String? = null
    ): TrackEntity = withContext(Dispatchers.IO) {
        val fileName = knownDisplayName ?: resolveDisplayName(context, uri) ?: "Audio Track"
        val cleanBaseTitle = fileName.substringBeforeLast('.').ifBlank { fileName }

        var title = cleanBaseTitle
        var artist = "Local Audio"
        var album = "Device Storage"
        var durationMs = 0L
        var bitrateKbps = 0
        var mimeType = context.contentResolver.getType(uri) ?: "audio/*"
        var sampleRateHz = 48_000
        var channelCount = 2
        var bitDepth = 16

        // 1. Standard metadata via MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.let { bitrateKbps = it / 1000 }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() }?.let { mimeType = it }
        } catch (_: Exception) {
            // Keep defaults when container metadata is sparse (e.g. raw .eac3/.ac4)
        } finally {
            runCatching { retriever.release() }
        }

        // 2. Audio track format inspection via MediaExtractor for exact codec/sampleRate/channels
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val trackMime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (trackMime.startsWith("audio/")) {
                    mimeType = trackMime
                    if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        val enc = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        bitDepth = when (enc) {
                            android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
                            android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                            android.media.AudioFormat.ENCODING_PCM_32BIT -> 32
                            else -> 16
                        }
                    } else if (trackMime.contains("flac")) {
                        bitDepth = 24
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Fallback to inferred format
        } finally {
            runCatching { extractor.release() }
        }

        val codecLabel = inferCodecLabel(fileName, mimeType)
        TrackEntity(
            uri = uri.toString(),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs.coerceAtLeast(1000L),
            mimeType = mimeType,
            codecLabel = codecLabel,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            channelCount = channelCount,
            bitrateKbps = bitrateKbps,
            artworkResName = null,
            isBuiltIn = false
        )
    }

    fun resolveDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    suspend fun readTextFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Cannot open file stream.")
        }
    }

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(content)
                } ?: throw IllegalStateException("Cannot open output stream.")
            }
        }
}
