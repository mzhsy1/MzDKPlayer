package org.mz.mzdkplayer.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 媒体播放历史记录数据类
 * @param mediaUri 媒体播放的完整URI
 * @param fileName 文件名
 * @param playbackPosition 退出播放器时的播放时间（毫秒）
 * @param mediaDuration 媒体总时长（毫秒）
 * @param protocolName 协议名称（如SMB、NFS、FILE、HTTP等）
 * @param connectionName 当前连接的名称（对于网络协议）或"本地文件"等
 * @param serverAddress 当前连接的服务器地址（对于网络协议）或本地路径等
 * @param timestamp 记录创建时间
 * @param mediaType 媒体类型（VIDEO、AUDIO等）
 */
data class MediaHistoryRecord(
    val mediaUri: String = "",
    val fileName: String = "",
    val playbackPosition: Long = 0, // 默认为0，表示从头开始播放
    val mediaDuration: Long = 0, // 媒体总时长，默认为0
    val protocolName: String = "",
    val connectionName: String = "",
    val serverAddress: String = "",
    val timestamp: Long = System.currentTimeMillis(), // 默认为当前时间
    val mediaType: String = "VIDEO" // 媒体类型，默认为视频
) {
    /**
     * 将播放位置转换为可读的时间格式 (MM:SS)
     */
    fun getFormattedPosition(): String {
        val totalSeconds = playbackPosition / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.Companion.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /**
     * 获取格式化的日期时间
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 检查媒体是否已播放完毕（播放位置大于媒体时长的95%可认为已播放完毕）
     */
    fun isPlaybackCompleted(): Boolean {
        return if (mediaDuration > 0) {
            playbackPosition >= mediaDuration * 0.95
        } else {
            false // 如果没有媒体时长信息，无法判断是否播放完毕
        }
    }

    /**
     * 获取播放进度百分比
     */
    fun getPlaybackPercentage(): Int {
        return if (mediaDuration > 0) {
            ((playbackPosition.toDouble() / mediaDuration) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    /**
     * 判断是否为视频类型
     */
    fun isVideo(): Boolean {
        return mediaType.equals("VIDEO", ignoreCase = true)
    }

    /**
     * 判断是否为音频类型
     */
    fun isAudio(): Boolean {
        return mediaType.equals("AUDIO", ignoreCase = true)
    }
}