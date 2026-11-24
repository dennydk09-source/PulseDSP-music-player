package com.example.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

enum class LyricsTextAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

data class LyricsAppearanceConfig(
    val fontSizeSp: Float = 15f,
    val alignment: LyricsTextAlignment = LyricsTextAlignment.LEFT
) {
    fun clamped(): LyricsAppearanceConfig = copy(
        fontSizeSp = fontSizeSp.coerceIn(12f, 26f)
    )
}

data class LrcLine(
    val timeMs: Long,
    val text: String
)

data class LrcDocument(
    val lines: List<LrcLine>,
    val sourceLabel: String,
    val isSynced: Boolean,
    val rawLrc: String
)

data class LrcSearchResult(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSec: Int,
    val syncedLyrics: String?,
    val plainLyrics: String?
) {
    val hasSynced: Boolean get() = !syncedLyrics.isNullOrBlank()
    val bestRawText: String get() = syncedLyrics?.takeIf { it.isNotBlank() } ?: plainLyrics.orEmpty()
}

/**
 * Parses standard and multi-timestamp .LRC lyric files (e.g. [00:12.45], [01:03.120], [00:45],
 * [offset:+/-ms]) and provides fast binary-search lookup for Media3 timestamp synchronization.
 */
object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET_TAG_REGEX = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val META_TAG_REGEX = Regex("""^\[[a-zA-Z]+:.*]$""")

    fun parse(rawText: String, sourceLabel: String): LrcDocument {
        val normalized = rawText.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isBlank()) {
            return LrcDocument(emptyList(), sourceLabel, false, "")
        }

        val offsetMs = OFFSET_TAG_REGEX.find(normalized)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val syncedLines = mutableListOf<LrcLine>()
        val plainLines = mutableListOf<LrcLine>()

        normalized.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val matches = TIME_TAG_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lyricText = TIME_TAG_REGEX.replace(trimmed, "").trim()
                if (lyricText.isNotEmpty()) {
                    for (m in matches) {
                        val mins = m.groupValues[1].toLongOrNull() ?: 0L
                        val secs = m.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = m.groupValues.getOrNull(3).orEmpty()
                        val millis = when (fracStr.length) {
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            3 -> fracStr.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val timeMs = (mins * 60_000L + secs * 1_000L + millis + offsetMs).coerceAtLeast(0L)
                        syncedLines.add(LrcLine(timeMs = timeMs, text = lyricText))
                    }
                }
            } else if (!META_TAG_REGEX.matches(trimmed)) {
                plainLines.add(LrcLine(timeMs = -1L, text = trimmed))
            }
        }

        return if (syncedLines.isNotEmpty()) {
            LrcDocument(
                lines = syncedLines.sortedBy { it.timeMs },
                sourceLabel = sourceLabel,
                isSynced = true,
                rawLrc = normalized
            )
        } else {
            LrcDocument(
                lines = plainLines,
                sourceLabel = sourceLabel,
                isSynced = false,
                rawLrc = normalized
            )
        }
    }

    /**
     * Returns the index of the currently active synchronized lyric line for [positionMs],
     * or -1 if playback is before the first timestamp or lyrics are unsynced.
     */
    fun findActiveLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty() || lines.first().timeMs < 0L) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs + 120L) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun formatTimestamp(timeMs: Long): String {
        if (timeMs < 0L) return "--:--"
        val totalSec = timeMs / 1000L
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format("%02d:%02d", mins, secs)
    }
}

/**
 * Resolves, caches, imports, and searches synchronized .LRC lyrics:
 * 1. Offline cached/imported LRC saved per track URI
 * 2. Local sidecar .lrc next to file:// tracks or inside mounted SAF folders
 * 3. Automatic online lookup & interactive search via LRCLIB (https://lrclib.net)
 */
object LyricsRepository {

    private const val LRCLIB_BASE = "https://lrclib.net/api"
    private const val USER_AGENT = "PulseDSP-Android/1.0 (Audiophile Music Player)"

    private fun cacheFileFor(context: Context, trackUri: String): File {
        val dir = File(context.filesDir, "lyrics_cache").apply { mkdirs() }
        val hash = sha256(trackUri)
        return File(dir, "$hash.lrc")
    }

    private fun metaFileFor(context: Context, trackUri: String): File {
        val dir = File(context.filesDir, "lyrics_cache").apply { mkdirs() }
        val hash = sha256(trackUri)
        return File(dir, "$hash.src")
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }

    suspend fun saveCustomLrcForTrack(
        context: Context,
        trackUri: String,
        rawLrc: String,
        sourceLabel: String
    ): LrcDocument = withContext(Dispatchers.IO) {
        val parsed = LrcParser.parse(rawLrc, sourceLabel)
        if (parsed.lines.isNotEmpty()) {
            runCatching {
                cacheFileFor(context, trackUri).writeText(rawLrc)
                metaFileFor(context, trackUri).writeText(sourceLabel)
            }
        }
        parsed
    }

    suspend fun clearCachedLrcForTrack(context: Context, trackUri: String) = withContext(Dispatchers.IO) {
        runCatching { cacheFileFor(context, trackUri).delete() }
        runCatching { metaFileFor(context, trackUri).delete() }
    }

    suspend fun resolveLyricsForTrack(
        context: Context,
        track: TrackEntity,
        mountedFolders: List<Uri>
    ): LrcDocument? = withContext(Dispatchers.IO) {
        // 1. Check persisted user-imported or cached LRC for this track
        val cached = cacheFileFor(context, track.uri)
        if (cached.exists() && cached.length() > 0L) {
            val source = runCatching { metaFileFor(context, track.uri).readText().trim() }
                .getOrNull()?.ifBlank { "Saved .LRC" } ?: "Saved .LRC"
            val doc = LrcParser.parse(cached.readText(), source)
            if (doc.lines.isNotEmpty()) return@withContext doc
        }

        // 2. Check sidecar .lrc file next to file:// track (covers built-in reference tracks & direct files)
        val parsedUri = runCatching { Uri.parse(track.uri) }.getOrNull()
        if (parsedUri?.scheme == "file" && !parsedUri.path.isNullOrBlank()) {
            val audioFile = File(parsedUri.path!!)
            val sidecar = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (sidecar.exists() && sidecar.length() > 0L) {
                val doc = LrcParser.parse(sidecar.readText(), "Local Sidecar .LRC")
                if (doc.lines.isNotEmpty()) return@withContext doc
            }
        }

        // 3. Check mounted SAF folders for a matching <baseName>.lrc sidecar
        val baseTitle = track.title.trim()
        val displayBaseName = parsedUri?.let {
            LocalStorageScanner.resolveDisplayName(context, it)?.substringBeforeLast('.')
        }
        val candidateNames = listOfNotNull(displayBaseName, baseTitle)
            .map { "${it.lowercase()}.lrc" }
            .distinct()

        for (folderUri in mountedFolders) {
            val foundDoc = findMatchingLrcInFolder(context, folderUri, candidateNames)
            if (foundDoc != null) {
                val text = LocalStorageScanner.readTextFromUri(context, foundDoc.uri).getOrNull()
                if (!text.isNullOrBlank()) {
                    val doc = LrcParser.parse(text, "Folder Sidecar .LRC")
                    if (doc.lines.isNotEmpty()) return@withContext doc
                }
            }
        }

        // 4. Automatic online fetch via LRCLIB for non-built-in tracks
        if (!track.isBuiltIn && track.title.isNotBlank()) {
            val online = fetchFromLrclib(
                title = track.title,
                artist = track.artist.takeUnless { it.equals("Local Audio", ignoreCase = true) }.orEmpty(),
                durationSec = (track.durationMs / 1000L).toInt()
            )
            if (online != null && online.lines.isNotEmpty()) {
                runCatching {
                    cacheFileFor(context, track.uri).writeText(online.rawLrc)
                    metaFileFor(context, track.uri).writeText(online.sourceLabel)
                }
                return@withContext online
            }
        }

        null
    }

    private fun findMatchingLrcInFolder(
        context: Context,
        folderUri: Uri,
        candidateLowerNames: List<String>
    ): DocumentFile? {
        val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        return runCatching {
            dir.listFiles().firstOrNull { file ->
                !file.isDirectory && file.name?.lowercase() in candidateLowerNames
            }
        }.getOrNull()
    }

    private fun fetchFromLrclib(title: String, artist: String, durationSec: Int): LrcDocument? {
        return runCatching {
            val encodedTrack = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val endpoint = if (artist.isNotBlank() && durationSec > 0) {
                "$LRCLIB_BASE/get?track_name=$encodedTrack&artist_name=$encodedArtist&duration=$durationSec"
            } else {
                val q = URLEncoder.encode("$title $artist".trim(), "UTF-8")
                "$LRCLIB_BASE/search?q=$q"
            }

            val body = httpGet(endpoint) ?: return null
            if (body.trimStart().startsWith("[")) {
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val synced = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                    if (synced != null) {
                        return LrcParser.parse(synced, "LRCLIB Synced")
                    }
                }
                val firstPlain = arr.optJSONObject(0)?.optString("plainLyrics", "")
                    ?.takeIf { it.isNotBlank() && it != "null" }
                if (firstPlain != null) return LrcParser.parse(firstPlain, "LRCLIB Plain")
                null
            } else {
                val obj = JSONObject(body)
                val synced = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                val plain = obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                when {
                    synced != null -> LrcParser.parse(synced, "LRCLIB Synced")
                    plain != null -> LrcParser.parse(plain, "LRCLIB Plain")
                    else -> null
                }
            }
        }.getOrNull()
    }

    suspend fun searchOnline(query: String): Result<List<LrcSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleaned = query.trim()
            if (cleaned.isEmpty()) return@runCatching emptyList()
            val encoded = URLEncoder.encode(cleaned, "UTF-8")
            val body = httpGet("$LRCLIB_BASE/search?q=$encoded")
                ?: throw IllegalStateException("No response from LRCLIB lyrics service. Check internet connection.")
            val arr = JSONArray(body)
            val list = mutableListOf<LrcSearchResult>()
            for (i in 0 until minOf(arr.length(), 15)) {
                val obj = arr.optJSONObject(i) ?: continue
                val synced = obj.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                val plain = obj.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                if (synced == null && plain == null) continue
                list.add(
                    LrcSearchResult(
                        id = obj.optLong("id", i.toLong()),
                        trackName = obj.optString("trackName", obj.optString("name", "Unknown Track")),
                        artistName = obj.optString("artistName", "Unknown Artist"),
                        albumName = obj.optString("albumName", ""),
                        durationSec = obj.optDouble("duration", 0.0).toInt(),
                        syncedLyrics = synced,
                        plainLyrics = plain
                    )
                )
            }
            list.sortedByDescending { it.hasSynced }
        }
    }

    private fun httpGet(urlString: String): String? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}
