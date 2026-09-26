package org.mz.mzdkplayer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "audio_cache",indices = [
    Index(value = ["title"]),
    Index(value = ["album"]),
    Index(value = ["artist"]),
    Index(value = ["dateAdded"]) // 新增：加速按添加时间排序
])
data class AudioCacheEntity(
    @PrimaryKey
    val audioUri: String,
    val dataSourceType: String,
    val fileName: String,
    val connectionName: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long = 0,
    val localCoverPath: String? = null, // 播放后回写的封面路径
    val lyrics: String? = null,         // 播放后回写的内嵌歌词，
    // 详细信息
    val isDetailsLoaded: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val bit: Long = 0,
    val sampleRate: String = "",
    val bitsPerSample: Int = 16,
)