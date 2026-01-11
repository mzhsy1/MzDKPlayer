package org.mz.mzdkplayer.data.model

data class MzTrack(
    val id: String,          // 轨道ID (VLC是Int转String, ExoPlayer用索引或ID)
    val name: String,        // 轨道名称 (如 "English", "Stereo")
    val language: String? = null,
    val isSelected: Boolean = false,
    val type: TrackType,     // 轨道类型
    val formatInfo: String? = null // 额外的格式信息 (如 "AC3 5.1", "1080p")
)

enum class TrackType {
    AUDIO, VIDEO, SUBTITLE
}