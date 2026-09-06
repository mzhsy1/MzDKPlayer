package org.mz.mzdkplayer.player.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import org.mz.mzdkplayer.tool.SubtitleScanner
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
    // 播放完成回调
    var onPlaybackEnded: (() -> Unit)?
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

/**
 * 协程版自动加载同名字幕：
 * 1. 在 IO 线程扫描视频所在目录（本地 + SMB/NFS/FTP/WebDAV/HTTP），找出与视频同名的字幕文件；
 * 2. 找到后切回主线程交给播放器加载；
 * 3. 返回实际找到的字幕条数（未找到返回 0，调用方据此决定是否提示）。
 */
suspend fun autoLoadSameNameSubtitles(videoUri: String, dataSourceType: String, player: IMzPlayer): Int {
    val found = SubtitleScanner.scan(videoUri, dataSourceType)

    if (found.isNotEmpty()) {
        player.addExternalSubtitles(found)
    }
    return found.size
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
