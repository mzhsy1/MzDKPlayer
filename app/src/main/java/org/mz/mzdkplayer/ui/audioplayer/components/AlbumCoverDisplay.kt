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
import androidx.compose.runtime.remember
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

@Composable
fun AlbumCoverDisplay(
    coverArt: Bitmap?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotationAngle = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotationAngle.animateTo(
                targetValue = rotationAngle.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 30000, easing = LinearEasing)
                )
            )
        } else {
            rotationAngle.stop()
        }
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
                // CD背景 - 青春渐变色（粉蓝渐变）
//                drawCircle(
//                    brush = Brush.radialGradient(
//                        colors = listOf(
//                            Color(0xFF74B9FF), // 亮蓝色
//                            Color(0xFFA29BFE), // 紫色
//                            Color(0xFFFD79A8)  // 粉色
//                        ),
//                        center = center,
//                        radius = cdRadius
//                    ),
//                    radius = cdRadius
//                )
                // CD背景 - 马卡龙渐变
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF81ECEC), // 薄荷蓝
                            Color(0xFF74B9FF), // 天空蓝
                            Color(0xFFA29BFE)  // 淡紫色
                        ),
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

                // CD中心孔 - 青春橙色
//                val centerHoleRadius = cdRadius * 0.12f
//                drawCircle(
//                    color = Color(0xFFE17055),
//                    radius = centerHoleRadius,
//                    center = center
//                )
                // 中心孔 - 柠檬黄
//                val centerHoleRadius = cdRadius * 0.12f
//                drawCircle(
//                    color = Color(0xFFFECA57),
//                    radius = centerHoleRadius,
//                    center = center
//                )

//                // 中心孔边框 - 亮黄色
//                drawCircle(
//                    color = Color(0xFFFDCB6E),
//                    radius = centerHoleRadius * 1.1f,
//                    center = center,
//                    style = Stroke(width = 1.5.dp.toPx())
//                )

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

//                // 添加一些青春感的装饰圆点
//                drawCircle(
//                    color = Color(0xFFFDCB6E).copy(alpha = 0.6f),
//                    radius = cdRadius * 0.05f,
//                    center = Offset(cdRadius * 1.3f, cdRadius * 0.4f)
//                )
//                drawCircle(
//                    color = Color(0xFF00B894).copy(alpha = 0.6f),
//                    radius = cdRadius * 0.04f,
//                    center = Offset(cdRadius * 1.4f, cdRadius * 0.7f)
//                )
//                drawCircle(
//                    color = Color(0xFFFD79A8).copy(alpha = 0.6f),
//                    radius = cdRadius * 0.03f,
//                    center = Offset(cdRadius * 1.2f, cdRadius * 0.9f)
//                )
            }
        }
    }
}