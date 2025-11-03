package org.mz.mzdkplayer.ui.audioplayer.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.tool.ColorExtractor

@Composable
fun AlbumCoverDisplay(
    coverArt: Bitmap?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotationAngle = remember { Animatable(0f) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var currentCoverArt by remember { mutableStateOf<Bitmap?>(null) }
    var shouldAnimate by remember { mutableStateOf(false) }

    // 检测专辑封面变化
    LaunchedEffect(coverArt) {
        if (coverArt != currentCoverArt) {
            currentCoverArt = coverArt

            // 如果封面为null，停止动画
            if (coverArt == null) {
                rotationAngle.stop()
                rotationAngle.snapTo(0f)
                shouldAnimate = false
            } else {
                // 停止当前动画并重置角度
                rotationAngle.stop()
                rotationAngle.snapTo(0f)

                // 提取专辑图片的主题色
                gradientColors = withContext(Dispatchers.IO) {
                    ColorExtractor.extractColorsFromBitmap(coverArt)
                }

                // 如果当前正在播放，标记需要启动动画
                shouldAnimate = isPlaying

                // 如果当前正在播放，立即启动动画
                if (isPlaying) {
                    rotationAngle.animateTo(
                        targetValue = rotationAngle.value + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 30000, easing = LinearEasing)
                        )
                    )
                }
            }
        }
    }

    // 播放状态改变时的动画控制
    LaunchedEffect(isPlaying) {
        if (isPlaying && currentCoverArt != null && !rotationAngle.isRunning) {
            // 启动旋转动画（仅当封面不为null时）
            rotationAngle.animateTo(
                targetValue = rotationAngle.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 30000, easing = LinearEasing)
                )
            )
        } else if (!isPlaying || currentCoverArt == null) {
            // 暂停动画（无论是否为null）
            rotationAngle.stop()
        }
    }

    // 如果没有提取到颜色，使用默认颜色
    val finalColors = if (gradientColors.isNotEmpty()) {
        gradientColors
    } else {
        listOf(
            Color(0xFF81ECEC), // 薄荷蓝
            Color(0xFF74B9FF), // 天空蓝
            Color(0xFFA29BFE)  // 淡紫色
        )
    }

    Box(
        modifier = modifier.size(240.dp)
    ) {
        Canvas(
            modifier = Modifier.size(240.dp)
        ) {
            val canvasSize = size.minDimension
            val center = Offset(canvasSize / 2, canvasSize / 2)
            val cdRadius = canvasSize / 2
            val albumRadius = cdRadius * 0.75f

            rotate(rotationAngle.value, pivot = center) {
                // CD背景 - 使用从专辑图片提取的主题色渐变
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = finalColors,
                        center = center,
                        radius = cdRadius
                    ),
                    radius = cdRadius
                )

                // 创建圆形裁剪路径
                val clipPath = Path().apply {
                    addOval(
                        Rect(
                            center.x - albumRadius,
                            center.y - albumRadius,
                            center.x + albumRadius,
                            center.y + albumRadius
                        )
                    )
                }

                // 在圆形区域内绘制专辑封面
                clipPath(clipPath) {
                    if (coverArt != null) {
                        val imageBitmap = coverArt.asImageBitmap()
                        val albumDiameter = albumRadius * 2

                        // 计算缩放比例以填充圆形
                        val scale = maxOf(
                            albumDiameter / imageBitmap.width,
                            albumDiameter / imageBitmap.height
                        )

                        val scaledWidth = imageBitmap.width * scale
                        val scaledHeight = imageBitmap.height * scale

                        val imageLeft = center.x - scaledWidth / 2
                        val imageTop = center.y - scaledHeight / 2

                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(
                                imageBitmap.width,
                                imageBitmap.height
                            ),
                            dstOffset = IntOffset(
                                imageLeft.toInt(),
                                imageTop.toInt()
                            ),
                            dstSize = IntSize(
                                scaledWidth.toInt(),
                                scaledHeight.toInt()
                            )
                        )
                    } else {
                        // 默认专辑封面背景 - 青春绿色
                        drawCircle(
                            color = Color(0xFF00B894),
                            radius = albumRadius,
                            center = center
                        )
                    }
                }

                // CD封面边框 - 亮白色
                drawCircle(
                    color = Color.White,
                    radius = albumRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // CD光泽效果 - 更明亮的反光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(cdRadius * 0.25f, cdRadius * 0.25f),
                        radius = cdRadius * 0.6f
                    ),
                    radius = cdRadius
                )
            }
        }
    }
}



