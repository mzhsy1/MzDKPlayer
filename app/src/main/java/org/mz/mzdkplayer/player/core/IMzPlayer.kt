package org.mz.mzdkplayer.player.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerStatus

interface IMzPlayer {
    val isPlaying: Boolean
    val isPlayingFlow: StateFlow<Boolean>
    val currentPosition: Long
    val duration: Long
    val playerStatus: StateFlow<VideoPlayerStatus>
    // 轨道状态流，UI直接监听这些流来刷新面板
    val videoTracks: StateFlow<List<MzVideoTrack>>
    val audioTracks: StateFlow<List<MzBasicTrack>>
    val subtitleTracks: StateFlow<List<MzBasicTrack>>
    /**
     * 视频源的原始宽度（像素），用于 PGS/位图字幕正确定位
     */
    val videoWidth: Int

    /**
     * 视频源的原始高度（像素）
     */
    val videoHeight: Int
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekForward(ms: Long = 30000)
    fun seekBack(ms: Long = 30000)

    // 轨道切换接口
    fun selectVideoTrack(track: MzVideoTrack)
    fun selectAudioTrack(track: MzBasicTrack)
    fun selectSubtitleTrack(track: MzBasicTrack)
    // 统一的错误回调
    var onError: ((String) -> Unit)?
    // 统一的字幕/排版信息回调 (对应原 onCues)
    var onCuesChanged: ((Any) -> Unit)?
    fun release()

    // 核心：把渲染视图交给实现类去做，Compose里直接调用
    @Composable
    fun PlayerView(modifier: Modifier)

    // 批量添加外部字幕，避免播放器频繁重启
    fun addExternalSubtitles(subtitles: List<Pair<String, String>>)

    // ISO 标题流 (用于蓝光ISO文件)
    val isoTitles: StateFlow<List<MzIsoTitle>>
    // 切换 ISO 标题
    fun selectIsoTitle(index: Int)

    /**
     * 设置播放倍速
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * 当前播放倍速
     */
    val playbackSpeed: StateFlow<Float>

    /**
     * 设置视频比例
     */
    fun setAspectRatio(ratio: MzAspectRatio)

    /**
     * 当前视频比例
     */
    val aspectRatio: StateFlow<MzAspectRatio>
}

fun autoLoadSameNameSubtitles(videoUri: String, player: IMzPlayer) {
    val lastDotIndex = videoUri.lastIndexOf('.')
    if (lastDotIndex <= 0) return

    val basePath = videoUri.substring(0, lastDotIndex)
    val exts = listOf("ass", "srt", "ssa", "vtt")

    val validSubs = mutableListOf<Pair<String, String>>()

    exts.forEach { ext ->
        val subUri = "$basePath.$ext"

        if (videoUri.startsWith("file:///")) {
            // 本地文件：顺手查一下文件存不存在，这步不涉及扫目录，基本不耗时
            val path = subUri.removePrefix("file:///")
            if (java.io.File(path).exists()) {
                validSubs.add(subUri to "[外部加载] $ext")
            }
        } else {
            // 网络协议 (smb/http等)：不扫描，直接莽，交给播放器底层去碰壁
            validSubs.add(subUri to "[外部加载] $ext")
        }
    }

    // 塞给播放器
    if (validSubs.isNotEmpty()) {
        player.addExternalSubtitles(validSubs)
    }
}

// 1. 在 IMzPlayer.kt 文件中增加一个新的数据类
data class MzIsoTitle(
    val index: Int,
    val name: String,
    val durationText: String, // 🌟 新增：格式化后的时长，如 "02:15:30"
    val isSelected: Boolean
)

enum class MzAspectRatio(val description: String) {
    FIT("自动适应"),
    STRETCH("拉伸铺满"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    ZOOM("裁剪填充")
}
