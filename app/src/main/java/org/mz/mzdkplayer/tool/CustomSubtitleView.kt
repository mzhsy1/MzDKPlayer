


package org.mz.mzdkplayer.tool

import android.util.DisplayMetrics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
@UnstableApi
fun SubtitleView(
    cueGroup: CueGroup?,
    modifier: Modifier = Modifier,
    videoSizeDp: IntSize, // 视频容器尺寸（dp）
    subtitleStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 22.sp
    ),
    backgroundColor: Color = Color.Black.copy(alpha = 0.0f),
    exoPlayer: ExoPlayer,
    forcePGSCenter: Boolean = false
) {
    if (cueGroup == null || cueGroup.cues.isEmpty()) {
        return
    }

    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    // 获取视频源的实际像素尺寸
    val videoSourceWidth = exoPlayer.videoSize.width
    val videoSourceHeight = exoPlayer.videoSize.height

    // 计算视频在容器中的实际显示尺寸和位置
    val displayedVideoRect = if (videoSourceWidth > 0 && videoSourceHeight > 0) {
        calculateDisplayedVideoRect(
            containerWidthDp = videoSizeDp.width,
            containerHeightDp = videoSizeDp.height,
            sourceWidthPx = videoSourceWidth,
            sourceHeightPx = videoSourceHeight,
            density = density
        )
    } else {
        // 无法计算，假设视频填满容器
        VideoDisplayRect(videoSizeDp.width.toFloat(), videoSizeDp.height.toFloat(), 0f, 0f)
    }

    Log.i("SubtitleView", "Video container: ${videoSizeDp.width}x${videoSizeDp.height}")
    Log.i("SubtitleView", "Video source: ${videoSourceWidth}x${videoSourceHeight}")
    Log.i("SubtitleView", "Displayed video: ${displayedVideoRect.widthDp}x${displayedVideoRect.heightDp} at (${displayedVideoRect.offsetXDp}, ${displayedVideoRect.offsetYDp})")

    Box(
        modifier = Modifier.fillMaxSize() // 主容器填满，内部元素使用传入的modifier定位
    ) {
        // 文本字幕 - 使用传入的modifier进行定位，如果未传入则默认底部居中
        Box(
            modifier = if (modifier == Modifier) {
                // 如果传入的是默认的空modifier，则使用底部居中
                Modifier.align(Alignment.BottomCenter)
            } else {
                // 否则使用传入的modifier
                modifier
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                cueGroup.cues.forEach { cue ->
                    // Render text cue
                    cue.text?.toString()?.takeIf { it.isNotEmpty() && it != "null" }?.let { text ->
                        // 创建带居中对齐的文本样式
                        val centeredTextStyle = subtitleStyle.copy(textAlign = TextAlign.Center)

                        // 测量文本尺寸
                        val textLayoutResult = textMeasurer.measure(
                            text = text,
                            style = centeredTextStyle
                        )

                        val textWidthDp = textLayoutResult.size.width / density
                        val textHeightDp = textLayoutResult.size.height / density

                        // 使用Canvas绘制文本
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(textWidthDp.dp)
                                    .height(textHeightDp.dp)
                            ) {
                                // 绘制背景
                                if (backgroundColor.alpha > 0) {
                                    drawRect(
                                        color = backgroundColor,
                                        size = size
                                    )
                                }

                                // 绘制文本 - 使用居中对齐的样式
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = text,
                                    style = centeredTextStyle,
                                    topLeft = androidx.compose.ui.geometry.Offset.Zero
                                )
                            }
                        }
                    }
                }
            }
        }
// === 位图字幕 ===
        Box(modifier = Modifier.fillMaxSize()) {
            cueGroup.cues.forEach { cue ->
                cue.bitmap?.let { bitmap ->
                    // >>> 强制居中逻辑：覆盖 position/line/anchor
                    // 替换从这里开始
                    val anchorInfo = if (forcePGSCenter) {
                        SubtitleAnchorInfo(
                            x = 0.5f,
                            y = 0.90f,
                            positionAnchor = Cue.ANCHOR_TYPE_MIDDLE,
                            lineAnchor = Cue.ANCHOR_TYPE_START
                        )
                    } else {
                        val resolvedX = if (cue.position != Cue.DIMEN_UNSET) cue.position.coerceIn(0f, 1f) else 0.5f
                        val resolvedY = if (cue.line != Cue.DIMEN_UNSET) cue.line.coerceIn(0f, 1f) else 0.5f
                        SubtitleAnchorInfo(
                            x = resolvedX,
                            y = resolvedY,
                            positionAnchor = cue.positionAnchor,
                            lineAnchor = cue.lineAnchor
                        )
                    }

                    val (x, y, positionAnchor, lineAnchor) = anchorInfo

                    val originalBitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

                    val targetWidthDp = if (!forcePGSCenter && cue.size != Cue.DIMEN_UNSET) {
                        displayedVideoRect.widthDp * cue.size
                    } else {
                        bitmap.width / density
                    }

                    val targetHeightDp = if (!forcePGSCenter && cue.bitmapHeight != Cue.DIMEN_UNSET) {
                        displayedVideoRect.heightDp * cue.bitmapHeight
                    } else {
                        bitmap.height / density
                    }

                    // 尺寸计算逻辑保持不变
                    val (bitmapWidthDp, bitmapHeightDp) = when {
                        !forcePGSCenter && cue.size != Cue.DIMEN_UNSET && cue.bitmapHeight != Cue.DIMEN_UNSET -> {
                            val widthBasedHeight = targetWidthDp / originalBitmapAspectRatio
                            val heightBasedWidth = targetHeightDp * originalBitmapAspectRatio
                            if (widthBasedHeight <= targetHeightDp) {
                                targetWidthDp to widthBasedHeight
                            } else {
                                heightBasedWidth to targetHeightDp
                            }
                        }

                        !forcePGSCenter && cue.size != Cue.DIMEN_UNSET -> {
                            targetWidthDp to (targetWidthDp / originalBitmapAspectRatio)
                        }

                        !forcePGSCenter && cue.bitmapHeight != Cue.DIMEN_UNSET -> {
                            (targetHeightDp * originalBitmapAspectRatio) to targetHeightDp
                        }

                        else -> {
                            (bitmap.width / density) to (bitmap.height / density)
                        }
                    }

                    // 使用覆盖后的 anchor 计算偏移
                    val contentOffsetX = when (positionAnchor) {
                        Cue.ANCHOR_TYPE_START -> displayedVideoRect.widthDp * x
                        Cue.ANCHOR_TYPE_MIDDLE -> displayedVideoRect.widthDp * x - bitmapWidthDp / 2
                        Cue.ANCHOR_TYPE_END -> displayedVideoRect.widthDp * x - bitmapWidthDp
                        else -> displayedVideoRect.widthDp * x
                    }

                    val contentOffsetY = when (lineAnchor) {
                        Cue.ANCHOR_TYPE_START -> displayedVideoRect.heightDp * y
                        Cue.ANCHOR_TYPE_MIDDLE -> displayedVideoRect.heightDp * y - bitmapHeightDp / 2
                        Cue.ANCHOR_TYPE_END -> displayedVideoRect.heightDp * y - bitmapHeightDp
                        else -> displayedVideoRect.heightDp * y
                    }

                    val finalOffsetX = displayedVideoRect.offsetXDp + contentOffsetX
                    val finalOffsetY = displayedVideoRect.offsetYDp + contentOffsetY

                    Box(
                        modifier = Modifier
                            .offset(x = finalOffsetX.dp, y = finalOffsetY.dp)
                            .width(bitmapWidthDp.dp)
                            .height(bitmapHeightDp.dp)
                            .zIndex(cue.zIndex.toFloat())
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dstWidthPx = (bitmapWidthDp * density).toInt()
                            val dstHeightPx = (bitmapHeightDp * density).toInt()
                            drawImage(
                                image = bitmap.asImageBitmap(),
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(bitmap.width, bitmap.height),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(dstWidthPx, dstHeightPx),
                                alpha = 1.0f,
                                blendMode = DefaultBlendMode
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 数据类表示视频显示区域
 */
data class VideoDisplayRect(
    val widthDp: Float,
    val heightDp: Float,
    val offsetXDp: Float,
    val offsetYDp: Float
)

/**
 * 计算视频在容器中按 ContentScale.Fit（保持宽高比，居中）显示时的实际尺寸和偏移。
 *
 * @return VideoDisplayRect(显示宽度 dp, 显示高度 dp, X偏移 dp, Y偏移 dp)
 */
private fun calculateDisplayedVideoRect(
    containerWidthDp: Int,
    containerHeightDp: Int,
    sourceWidthPx: Int,
    sourceHeightPx: Int,
    density: Float
): VideoDisplayRect {
    val containerWidthPx = containerWidthDp * density
    val containerHeightPx = containerHeightDp * density

    val sourceAspectRatio = sourceWidthPx.toFloat() / sourceHeightPx.toFloat()
    val containerAspectRatio = containerWidthPx / containerHeightPx

    val (displayedWidthPx, displayedHeightPx) = if (sourceAspectRatio > containerAspectRatio) {
        // 视频更宽，限制宽度，高度按比例缩放
        val w = containerWidthPx
        val h = w / sourceAspectRatio
        w to h
    } else {
        // 视频更高，限制高度，宽度按比例缩放
        val h = containerHeightPx
        val w = h * sourceAspectRatio
        w to h
    }

    // 居中对齐的偏移
    val offsetX = (containerWidthPx - displayedWidthPx) / 2
    val offsetY = (containerHeightPx - displayedHeightPx) / 2

    return VideoDisplayRect(
        widthDp = displayedWidthPx / density,
        heightDp = displayedHeightPx / density,
        offsetXDp = offsetX / density,
        offsetYDp = offsetY / density
    )
}

/**
 * Returns the current screen dimensions in dp.
 *
 * @return A [Pair] of [Int] values representing the screen width and height in dp.
 */
@Composable
private fun getScreenDimensions(): Pair<Int, Int> {
    val context = LocalContext.current
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val widthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
    val heightDp = (displayMetrics.heightPixels / displayMetrics.density).toInt()
    return Pair(widthDp, heightDp)
}



private data class SubtitleAnchorInfo(
    val x: Float,
    val y: Float,
    val positionAnchor: Int,
    val lineAnchor: Int
)