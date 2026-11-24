package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.AudioEffectsConfig
import com.example.data.ListeningMode
import com.example.data.SurroundMode

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["uri"], unique = true)]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val codecLabel: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val channelCount: Int,
    val bitrateKbps: Int,
    val artworkResName: String? = null,
    val isBuiltIn: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int = 0
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
)

@Entity(tableName = "audio_effect_presets")
data class AudioEffectPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val modeName: String,
    val bandGainsCsv: String,
    val preampDb: Float,
    val bassBoost: Float,
    val trebleBoost: Float,
    val virtualizerLevel: Float,
    val surroundModeName: String,
    val stereoWidth: Float,
    val compressorEnabled: Boolean,
    val compressorThresholdDb: Float,
    val compressorRatio: Float,
    val compressorMakeupDb: Float,
    val digitalPassthroughEnabled: Boolean = false,
    val aiRationale: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toConfig(): AudioEffectsConfig {
        val gains = bandGainsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        val mode = ListeningMode.entries.firstOrNull { it.name == modeName } ?: ListeningMode.CUSTOM
        val surround = SurroundMode.entries.firstOrNull { it.name == surroundModeName } ?: SurroundMode.OFF
        return AudioEffectsConfig(
            enabled = true,
            activeMode = mode,
            activePresetName = name,
            bandGainsDb = (0 until 10).map { idx -> gains.getOrElse(idx) { 0f } },
            preampDb = preampDb,
            bassBoost = bassBoost,
            trebleBoost = trebleBoost,
            virtualizerLevel = virtualizerLevel,
            surroundMode = surround,
            stereoWidth = stereoWidth,
            compressorEnabled = compressorEnabled,
            compressorThresholdDb = compressorThresholdDb,
            compressorRatio = compressorRatio,
            compressorMakeupDb = compressorMakeupDb,
            digitalPassthroughEnabled = digitalPassthroughEnabled,
            aiRationale = aiRationale
        ).clamped()
    }

    companion object {
        fun fromConfig(name: String, config: AudioEffectsConfig): AudioEffectPresetEntity {
            val safe = config.clamped()
            return AudioEffectPresetEntity(
                name = name.trim().ifBlank { "Custom Preset" },
                modeName = safe.activeMode.name,
                bandGainsCsv = safe.bandGainsDb.joinToString(",") { String.format("%.1f", it) },
                preampDb = safe.preampDb,
                bassBoost = safe.bassBoost,
                trebleBoost = safe.trebleBoost,
                virtualizerLevel = safe.virtualizerLevel,
                surroundModeName = safe.surroundMode.name,
                stereoWidth = safe.stereoWidth,
                compressorEnabled = safe.compressorEnabled,
                compressorThresholdDb = safe.compressorThresholdDb,
                compressorRatio = safe.compressorRatio,
                compressorMakeupDb = safe.compressorMakeupDb,
                digitalPassthroughEnabled = safe.digitalPassthroughEnabled,
                aiRationale = safe.aiRationale
            )
        }
    }
}
