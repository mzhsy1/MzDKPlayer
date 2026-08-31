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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // 外层 Box 仅作为定位锚点
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 原有的按钮 Surface
        if (focusable) {
            Surface(
                modifier = Modifier.size(40.dp), // 固定按钮尺寸
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
                        .padding(8.dp),
                    contentDescription = contentDescription,
                    tint = LocalContentColor.current
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
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