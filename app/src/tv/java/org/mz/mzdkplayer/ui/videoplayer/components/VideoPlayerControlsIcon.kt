package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun VideoPlayerControlsIcon(
    modifier: Modifier = Modifier,
    state: VideoPlayerState,
    isPlaying: Boolean,
    icon: Painter,
    contentDescription: String? = null,
    tooltipText: String? = null,
    focusable: Boolean = true,
    /** 圆形按钮直径。控制栏里的播放/暂停比换集按钮大一档，图标内边距按比例缩放 */
    buttonSize: Dp = 40.dp,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val iconPadding = buttonSize * 0.2f
    val focusManager = LocalFocusManager.current

    // 外层 Box 仅作为定位锚点
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 原有的按钮 Surface
        if (focusable) {
            Surface(
                modifier = Modifier
                    .size(buttonSize) // 固定按钮尺寸
                    .moveFocusOnHorizontalKeys(focusManager),
                onClick = onClick,
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Black.copy(0.9f), contentColor = Color.White,
                    focusedContainerColor = Color.White, focusedContentColor = Color.Black
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                interactionSource = interactionSource
            ) {
                Icon(
                    icon,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(iconPadding),
                    contentDescription = contentDescription,
                    tint = LocalContentColor.current
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Black.copy(0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(iconPadding),
                    contentDescription = contentDescription,
                    tint = Color.White
                )
            }
        }

        // 提示框容器
        if (tooltipText != null) {
            // 【关键修改】：用一个宽高强行设为 0 的 Box 做包裹
            // 这样它就不会撑开父布局，也就不会挤压旁边的按钮了
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .requiredSize(0.dp)
            ) {
                AnimatedVisibility(
                    visible = isFocused,
                    modifier = Modifier
                        // 相对于这个 0dp 的 Box 的底部居中对齐
                        .align(Alignment.BottomCenter)
                        // 往上提一点点，给按钮留出间距
                        .offset(y = (-20).dp)
                        // 允许提示框真实大小溢出
                        .wrapContentSize(unbounded = true),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tooltipText,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 左右键在按钮之间切换焦点。
 *
 * 播放页根节点的 `onKeyEvent` 统一处理方向键做快进/快退，而它在按键冒泡路径上比 Compose 的
 * 「焦点移动」更早生效 —— 不在这里消费的话，左右键一律被当成 seek，焦点永远困在同一枚按钮上。
 *
 * 按下（KeyDown）也必须一起吞掉：播放页把「左右按下」当成长按 seek 的起点开始按节拍步进，
 * 只消费松手的话，那个协程就拿不到 UP、永远停不下来（实测表现是一路步进到片尾）。
 * 到边的按钮（最左按左 / 最右按右）没有可去的地方，这次按键就当没发生 —— 想 seek 先按「上」
 * 把焦点交回进度条，那边才是播放页处理左右键的地盘。
 */
private fun Modifier.moveFocusOnHorizontalKeys(focusManager: FocusManager): Modifier =
    this.onKeyEvent { event ->
        val direction = when (event.key) {
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            else -> return@onKeyEvent false
        }
        if (event.type == KeyEventType.KeyDown) return@onKeyEvent true
        if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
        focusManager.moveFocus(direction)
        true
    }