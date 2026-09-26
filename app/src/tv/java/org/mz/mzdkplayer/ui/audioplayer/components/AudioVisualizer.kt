package org.mz.mzdkplayer.ui.audioplayer.components


import android.media.audiofx.Visualizer
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow


@Composable
fun AudioVisualizer(
    audioSessionId: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 50,
    color: Color = Color.Cyan
) {
    var tick by remember { mutableLongStateOf(0L) }
    val fftData = remember { mutableStateOf(ByteArray(0)) }
    val lastHeights = remember { FloatArray(barCount) }

    DisposableEffect(audioSessionId) {
        if (audioSessionId <= 0) return@DisposableEffect onDispose {}
        val visualizer = Visualizer(audioSessionId).apply {
            // 减小 captureSize 有助于提高刷新灵敏度，尝试用 512 或 1024
            captureSize = 512
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, s: Int) {}
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, s: Int) {
                    if (fft != null && isPlaying) {
                        fftData.value = fft
                        tick = System.nanoTime()
                    }
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            enabled = true
        }
        onDispose {
            visualizer.enabled = false
            visualizer.release()
        }
    }

    Canvas(modifier = modifier) {
        val refresh = tick
        val data = fftData.value
        if (data.isEmpty()) return@Canvas

        val gap = 2.dp.toPx()
        val barWidth = (size.width - (barCount - 1) * gap) / barCount
        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

        // FFT 数据长度通常为 captureSize
        val n = data.size / 2

        for (i in 0 until barCount) {
            // 1. 改进采样索引：Android FFT 前几位是极低频，跳过第0位(直流分量)
            // 使用幂函数曲线采样，让低频占比例缩小，高频占比例增大
            val fraction = i.toFloat() / barCount
            val index = (fraction.toDouble().pow(1.5) * (n - 2)).toInt().coerceIn(1, n - 1)

            val r = data[index * 2].toFloat()
            val img = data[index * 2 + 1].toFloat()
            val magnitude = hypot(r, img)

            // 2. 核心改进：指数级增益补偿 (Boost)
            // i 越往右（高频），补偿系数越大。Math.pow 让右侧补偿呈指数上升
            val multiplier = 1.0f + fraction.toDouble().pow(2.0).toFloat() * 15f

            // 3. 计算目标高度，并加入一个微小的随机基础高度，防止死掉
            var targetHeight = (magnitude * multiplier * (size.height / 100f))

            // 如果是在播放中，给个灵动的最小高度
            val minHeight = if (isPlaying) 8f else 0f
            targetHeight = targetHeight.coerceIn(minHeight, size.height)

            // 4. 平滑算法：上升快，下降慢
            val currentHeight = if (targetHeight >= lastHeights[i]) {
                // 上升：取目标值和旧值的中值，减少跳变感
                (targetHeight + lastHeights[i]) / 2f
            } else {
                // 下降：模拟重力下落
                lastHeights[i] * 0.82f
            }
            lastHeights[i] = currentHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - currentHeight),
                size = Size(barWidth, currentHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}