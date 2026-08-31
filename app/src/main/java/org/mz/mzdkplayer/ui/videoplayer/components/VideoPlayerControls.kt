package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.DanmakuSettingsManager
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalLocale

import androidx.compose.ui.res.stringResource
/**
 * 视频播放器控制按钮区域
 *
 * @param isPlaying 当前播放状态
 * @param contentCurrentPosition 当前播放位置
 * @param state 视频播放器状态
 * @param focusRequester 焦点请求器
 * @param title 标题
 * @param secondaryText 副标题
 * @param tertiaryText 第三行文本
 * @param videoPlayerViewModel ViewModel
 * @param danmakuPlayer 弹幕播放器
 * @param settingsManager 弹幕设置管理器
 * @param getDanmakuConfig 获取弹幕配置的方法
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerControls(
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    player: IMzPlayer,
    state: VideoPlayerState,
    title: String, 
    secondaryText: String, 
    tertiaryText: String,
    videoPlayerViewModel: VideoPlayerViewModel,
    danmakuPlayer: DanmakuPlayer,
    settingsManager: DanmakuSettingsManager, // 添加设置管理器参数
    getDanmakuConfig: () -> DanmakuConfig, // 添加获取配置的方法参数
    focusRequester: FocusRequester = remember { FocusRequester() },
    ffDuration: Int = 15,
    rwDuration: Int = 15,
    onTogglePlayPause: () -> Unit = {}
)
{
    // 构建主框架
    VideoPlayerMainFrame(
        mediaTitle = {
            // 媒体标题区域
            VideoPlayerMediaTitle(
                title = title,
                secondaryText = secondaryText,
                tertiaryText = tertiaryText,
                type = VideoPlayerMediaTitleType.DEFAULT
            )
        },
        mediaActions = {
            // 媒体操作按钮已移至统一设置面板
        },
        seeker = {
            // 进度条区域
            VideoPlayerSeeker(
                state,
                isPlaying,
                onSeek = { player.seekTo(player.duration.times(it).toLong()) }, // Seek 回调
                contentProgress = currentPositionProvider().milliseconds, // 当前进度
                contentDuration = player.duration.milliseconds, // 总时长
                focusRequester = focusRequester,
                ffDuration = ffDuration,
                rwDuration = rwDuration,
                onTogglePlayPause = onTogglePlayPause
            )
        },
        more = null // 更多按钮 (未实现)
    )
}
