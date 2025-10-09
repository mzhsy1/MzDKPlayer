

package org.mz.mzdkplayer.tool
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex

import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text


/**
 * 自定义 SubtitleView 组件，用于在 Jetpack Compose 中显示 Media3 ExoPlayer 的字幕。
 *
 * @param cues 当前需要显示的字幕 Cues 列表。
 * @param modifier Compose Modifier。
 * @param subtitleStyle 字幕的文本样式，例如字体大小、颜色。
 * @param backgroundColor 字幕区域的背景色，通常设置为半透明以增强可读性。
 */
@Composable
@OptIn(UnstableApi::class)
fun CustomSubtitleView(
    cueGroup: CueGroup?,
    modifier: Modifier = Modifier,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = Color.White,
        fontSize = 18.sp // 默认字体大小

    ),
    backgroundColor: Color = Color.Black.copy(alpha = 1.0f) // 默认半透明黑色背景
) {
    // 使用 Box 作为容器，方便定位和添加背景
    val (screenWidthDp, screenHeightDp) = getScreenDimensions()
    Box(
        modifier = modifier
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter // 将字幕放置在底部
    ) {
        // 检查 cueGroup 是否不为 null 且包含 Cue
        if (cueGroup != null && cueGroup.cues.isNotEmpty()) {
            // 使用 Column 垂直排列所有的字幕 Cue
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp), // 字幕行之间的间距
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 遍历并显示 cueGroup 中的每一个 Cue
                cueGroup.cues.forEach { cue ->
                    // Text 组件用于显示单个字幕文本
                    // 注意：cue.text 是 CharSequence，可能需要根据具体需求处理样式

                    // 处理文本内容
                    val textContent = cue.text?.toString()
                    if (!textContent.isNullOrEmpty() && textContent != "null") {
                        Text(
                            text = textContent,
                            style = subtitleStyle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                    // 处理位图内容
                    val bitmap: Bitmap? = cue.bitmap
                    Log.d("SubTitle","cue.position:${cue.position.toString()}")
                    Log.d("SubTitle","cue.size:${cue.size.toString()}")
                    Log.d("SubTitle","cue.bitmapHeight:${cue.bitmapHeight}")
                    Log.d("SubTitle","cue.line:${cue.line}")
                    Log.d("SubTitle","cue.A:${cue.lineAnchor}")
                    // 计算位图的实际尺寸
                    val bitmapWidth = if (cue.size != Cue.DIMEN_UNSET) {
                        (screenWidthDp * cue.size)
                    } else {
                        bitmap?.width?.toFloat()?:50f
                    }

                    val bitmapHeight = if (cue.bitmapHeight != Cue.DIMEN_UNSET) {
                        (screenHeightDp * cue.bitmapHeight)
                    } else {
                        bitmap?.height?.toFloat()?:50f
                    }
                    // 根据锚点类型调整位置
                    val offsetX = when (cue.positionAnchor) {
                        Cue.ANCHOR_TYPE_START -> screenWidthDp * cue.position
                        Cue.ANCHOR_TYPE_MIDDLE -> (screenWidthDp * cue.position) - (bitmapWidth / 2)
                        Cue.ANCHOR_TYPE_END -> (screenWidthDp * cue.position) - bitmapWidth
                        else -> (screenWidthDp * cue.position)
                    }

                    val offsetY = when (cue.lineAnchor) {
                        Cue.ANCHOR_TYPE_START -> (screenHeightDp * cue.line)
                        Cue.ANCHOR_TYPE_MIDDLE -> (screenHeightDp * cue.line) - (bitmapHeight / 2)
                        Cue.ANCHOR_TYPE_END -> (screenHeightDp * cue.line) - bitmapHeight
                        else -> (screenHeightDp * cue.line)
                    }
//                    val offsetX = screenWidthDp * cue.position
//                    val offsetY = screenHeightDp * cue.line
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = offsetX.dp -5.dp,
                                    y = offsetY.dp+5.dp
                                ).fillMaxSize()
                                .zIndex(cue.zIndex.toFloat())
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Subtitle image",
                                modifier = Modifier.width(bitmapWidth.dp).height(bitmapHeight.dp),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center
                            )
                        }
                    }

                }
            }
        }
        // 如果 cueGroup 为 null 或 cues 为空，则不显示任何内容

    }
}
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun getScreenDimensions(): Pair<Int, Int> {
    val configuration = LocalConfiguration.current
    return Pair(configuration.screenWidthDp, configuration.screenHeightDp)
}

