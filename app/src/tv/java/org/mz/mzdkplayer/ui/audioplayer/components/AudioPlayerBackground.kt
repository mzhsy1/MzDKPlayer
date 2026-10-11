package org.mz.mzdkplayer.ui.audioplayer.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.mz.mzdkplayer.data.model.AudioInfo

@Composable
fun AudioPlayerBackground(
    audioInfo: AudioInfo?,
    coverBitmap: Bitmap?,
    isPlaying: Boolean,
    currentAudioSessionId: Int,
    hasAudioPermission: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // --- 背景层：使用 Coil 3 + AnimatedContent 替换原有的 Blur ---
        AnimatedContent(
            targetState = audioInfo, // 当音频信息变化时触发背景切换
            transitionSpec = {
                fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
            },
            label = "BackgroundAnimation",
            modifier = Modifier.fillMaxSize()
        ) { info ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 优先使用本地路径，其次使用字节数组
                val model = info?.localCoverPath?.ifEmpty { null } ?: info?.artworkData

                if (model != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(model)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.6f // 稍微降低原图亮度
                    )
                } else {
                    // 兜底深色背景
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1C)))
                }

                // === 渐变遮罩层 ===
                // 原来是纯黑 alpha 0.5 的遮罩，现在换成按 3x3 区域从封面采样的网格渐变
                // （Compose 1.12 的 MeshGradientPainter）：同样是压暗封面、保证文字对比度，
                // 但整屏会被封面的配色铺满，播放时颜色带还会沿画面边缘缓慢流动。
                // 取不到封面颜色 / 系统低于 Android 10 时，AmbientMeshGradient 内部会退回纯黑遮罩。
                AmbientMeshGradient(
                    coverBitmap = coverBitmap,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.95f)
                                ),
                                startY = 300f
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                                startX = 400f
                            )
                        )
                )
            }
        }

        // 氛围装饰
        if (isPlaying && currentAudioSessionId > 0 && hasAudioPermission) {
            AudioVisualizer(
                audioSessionId = currentAudioSessionId,
                isPlaying = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .alpha(0.15f),
                barCount = 100,
            )
        }
    }
}
