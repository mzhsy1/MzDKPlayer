package org.mz.mzdkplayer.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_history")
data class MediaHistoryEntity(
    @PrimaryKey
    val mediaUri: String, // 作为主键，与 MediaCacheEntity 的 videoUri 对应
    val fileName: String,
    val playbackPosition: Long,
    val mediaDuration: Long,
    val protocolName: String,
    val connectionName: String,
    val serverAddress: String,
    val timestamp: Long,
    val mediaType: String // "VIDEO" or "AUDIO"
) {
    fun getPlaybackPercentage(): Int {
        if (mediaDuration <= 0) return 0
        return ((playbackPosition.toDouble() / mediaDuration) * 100).toInt().coerceIn(0, 100)
    }
}