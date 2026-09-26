package org.mz.mzdkplayer.player.core


// 统一的轨道类型
enum class MzTrackType { AUDIO, VIDEO, SUBTITLE }

// 统一的视频轨道模型
data class MzVideoTrack(
    val id: String,              // 唯一标识
    val index: Int,              // 在底层列表中的索引
    val height: Int,
    val bitrate: Int,
    val codecs: String,
    val isSelected: Boolean,
    val isDolbyVision: Boolean = false,
    val isHdr10: Boolean = false,
    val rawData: Any? = null     // 保留底层原始对象引用以备后用
)

// 统一的音频/字幕轨道模型
data class MzBasicTrack(
    val id: String,
    val index: Int,
    val name: String,              // 用于显示 label，比如 "[外部加载] zh.srt"
    val isSelected: Boolean,
    val language: String = "",
    val mimeType: String = "",     // 用于 codecs，比如 "application/x-subrip" 或 "audio/ac3"
    val channelCount: Int = 2,     // 音频声道数
    val sampleRate: Int = 0,       // 音频采样率
    val bitrate: Int = 0,          // 音频比特率
    val rawData: Any? = null       // 保留底层原始对象引用
)