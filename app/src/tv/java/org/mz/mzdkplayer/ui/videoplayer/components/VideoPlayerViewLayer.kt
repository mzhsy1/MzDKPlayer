


package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.tool.SubtitleView

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.VideoPlayerViewLayer(
    player: IMzPlayer,
    isCusSubtitleViewVis: Boolean,
    useVlc: Boolean,
    currentCueGroup: CueGroup?,
    customSubtitleStyle: TextStyle,
    subBottomPadding: Float,
    subBgColor: Long,
    videoSizeDp: IntSize,
    forcePgsCenter: Boolean,
    mDanmakuPlayer: DanmakuPlayer,
    onVideoSizeChanged: (IntSize) -> Unit
) {
    // 播放器底层渲染
    player.PlayerView(modifier = Modifier
        .align(Alignment.Center)
        .onSizeChanged { size ->
            onVideoSizeChanged(size)
        })

    // 自定义字幕层
    if (isCusSubtitleViewVis && !useVlc) {
        SubtitleView(
            cueGroup = currentCueGroup,
            subtitleStyle = customSubtitleStyle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (subBottomPadding >= 0f) Modifier.padding(bottom = subBottomPadding.dp)
                    else Modifier.offset(y = (-subBottomPadding).dp)
                ),
            videoSizeDp = videoSizeDp,
            backgroundColor = Color(subBgColor),
            sourceVideoHeight = player.videoHeight,
            sourceVideoWidth = player.videoWidth,
            forcePGSCenter = forcePgsCenter
        )
    }

    // 弹幕层
    AkDanmakuPlayer(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.TopCenter),
        danmakuPlayer = mDanmakuPlayer
    )
}
