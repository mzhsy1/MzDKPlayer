package org.mz.mzdkplayer.ui.phone.component

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView
import org.mz.mzdkplayer.common.SubtitleView
import org.mz.mzdkplayer.player.core.IMzPlayer

/**
 * 手机端播放页的**渲染层**：视频画面 → 自定义字幕 → 弹幕，三层叠在一起。
 *
 * 与电视端 `VideoPlayerViewLayer` 的分工一致，差别只在手机端不做「取视频容器尺寸兜底 PGS 定位」
 * 之外的事（电视端那套 `IntSize` 计算两边共用同一份 `SubtitleView`）。
 *
 * 自定义字幕只对 Exo 生效：VLC 自己有字幕渲染，再叠一层会出现两份字幕。
 */
@OptIn(UnstableApi::class)
@Composable
internal fun PhonePlayerSurface(
    player: IMzPlayer,
    useVlc: Boolean,
    cueGroup: CueGroup?,
    subtitleStyle: TextStyle,
    subBottomPadding: Float,
    subBgColor: Long,
    forcePgsCenter: Boolean,
    showCustomSubtitle: Boolean,
    danmakuPlayer: DanmakuPlayer,
    modifier: Modifier = Modifier,
) {
    // PGS / 位图字幕要靠「容器尺寸 + 视频源尺寸」算实际画面矩形，尺寸变化时重新量一次
    var surfaceSizePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    val surfaceSizeDp = with(density) {
        IntSize(
            width = (surfaceSizePx.width / density).toInt(),
            height = (surfaceSizePx.height / density).toInt(),
        )
    }

    Box(modifier = modifier) {
        player.PlayerView(
            Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .onSizeChanged { surfaceSizePx = it }
        )

        if (showCustomSubtitle && !useVlc) {
            SubtitleView(
                cueGroup = cueGroup,
                subtitleStyle = subtitleStyle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(
                        if (subBottomPadding >= 0f) {
                            Modifier.padding(bottom = subBottomPadding.dp)
                        } else {
                            Modifier.offset(y = (-subBottomPadding).dp)
                        }
                    ),
                videoSizeDp = surfaceSizeDp,
                backgroundColor = Color(subBgColor),
                sourceVideoHeight = player.videoHeight,
                sourceVideoWidth = player.videoWidth,
                forcePGSCenter = forcePgsCenter,
            )
        }

        PhoneDanmakuView(
            danmakuPlayer = danmakuPlayer,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
        )
    }
}

/**
 * 弹幕层：快手 `akdanmaku` 的 `DanmakuView` 包成 Compose。
 *
 * 与电视端 `AkDanmakuPlayer` 同一套做法（视图在 `factory` 里建、之后再把 `DanmakuPlayer`
 * 绑上去；随组件一起释放），只是这里不引电视端的任何 `androidx.tv.material3`。
 */
@Composable
private fun PhoneDanmakuView(
    danmakuPlayer: DanmakuPlayer?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var danmakuView: DanmakuView? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose { danmakuPlayer?.release() }
    }

    LaunchedEffect(danmakuPlayer) {
        danmakuView?.let { danmakuPlayer?.bindView(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                DanmakuView(context).also { danmakuView = it }
            },
        )
    }
}
