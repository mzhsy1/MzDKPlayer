package org.mz.mzdkplayer.ui.audioplayer.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Compose 1.12 的网格渐变（`androidx.compose.ui.graphics.MeshGradientPainter`，1.12.0 起在
 * `androidx.compose.ui:ui` 制品里）落地到电视端音乐播放器背景。
 *
 * 和线性 / 径向渐变的区别：那两种渐变只有一条（或一个中心）渐变轴，颜色只能沿轴插值；
 * 网格渐变用 (rows+1)x(columns+1) 个顶点描述一张双三次贝塞尔曲面，每个顶点自带颜色，
 * 顶点位置和颜色一起参与插值，所以能得到「多色连续流动」的效果。
 *
 * 这里的用法：一个 3x3 顶点（rows = 2, columns = 2）的网格，顶点颜色从当前封面图按 3x3 区域
 * 采样得到，播放中让顶点沿画面边缘缓慢滑动（颜色带跟着扫过），暂停时定格。
 */

/**
 * 网格渐变的不透明度。
 *
 * 别再往低调：封面图是垫在网格**下面**的（alpha 0.6），网格只有 0.5 时封面还剩 30% 可见度，
 * 整屏看起来仍然是「一张放大的封面照」，网格只是在后面当了一层雾。
 * 0.82 之后背景的主色由网格说了算，封面退成一点点纹理。
 */
private const val MESH_ALPHA = 0.82f

/** 取不到顶点色 / 系统低于 Android 10 时用的纯黑遮罩，沿用改动前的 0.5，不要跟 [MESH_ALPHA] 混用 */
private const val FALLBACK_SCRIM_ALPHA = 0.5f

/** 网格行 / 列数：2x2 个面片 = 3x3 个顶点 */
private const val MESH_ROWS = 2
private const val MESH_COLUMNS = 2

/** 采样缩略图的边长。要能被 (MESH_COLUMNS + 1) 整除，每格均色对应一个顶点 */
private const val SAMPLE_GRID = 48

/**
 * 颜色亮度上限。取色来自封面，白底 / 亮底封面能抽出接近纯白的主色，
 * 直接铺上去会把整个背景抬成灰蒙蒙的一片（黑位被拉起来，歌词也跟着失去对比度）。
 * 压到 0.34 以内，算是「暗色环境光」而不是一块彩色贴纸；再往上就要撞上歌词可读性了。
 */
private const val MAX_LIGHTNESS = 0.34f

/** 颜色亮度下限：深色封面别一路掉到纯黑，留一点亮度才能看出色相 */
private const val MIN_LIGHTNESS = 0.06f

/** 顶点漂移一个来回的时长 */
private const val DRIFT_DURATION_MS = 20_000

/**
 * 「环境网格渐变」背景层：从 [coverBitmap] 取色，铺一张会缓慢流动的网格渐变。
 *
 * 取不到颜色时（没有封面 / 位图已回收）退化成改动前的纯黑遮罩，保证画面口径不变。
 *
 * @param coverBitmap 当前曲目的封面位图，用来采样顶点色
 * @param isPlaying 播放中才让网格流动；暂停时定格，避免白跑动画
 * @param animateDrift 关掉后就是一张静态网格渐变（只画一次，不产生逐帧开销）
 */
@Composable
internal fun AmbientMeshGradient(
    coverBitmap: Bitmap?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    animateDrift: Boolean = true,
) {
    // Compose 1.12 的实现走 `Canvas.drawVertices`，官方注释写明「API 29 起才硬件加速」。
    // 电视端 minSdk 是 23，低版本上这条路径只能软件三角化、每帧都要重算，得不偿失，
    // 所以低于 Q 直接退化成纯黑遮罩（和改动前完全一致）。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Box(modifier.background(Color.Black.copy(alpha = FALLBACK_SCRIM_ALPHA)))
        return
    }

    val meshColors = rememberMeshColors(coverBitmap)
    if (meshColors == null) {
        Box(modifier.background(Color.Black.copy(alpha = FALLBACK_SCRIM_ALPHA)))
        return
    }

    // 缓慢往复漂移。key 换成 isPlaying = false 时 LaunchedEffect 被取消，
    // 动画值停在原地 -> 画面定格；重新播放再从当前值接着走。
    val drift = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, animateDrift) {
        if (!isPlaying || !animateDrift) return@LaunchedEffect
        drift.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = DRIFT_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
    }

    val painter = remember(meshColors) {
        MeshGradientPainter(
            rows = MESH_ROWS,
            columns = MESH_COLUMNS,
            // 双三次（Catmull-Rom）颜色插值：2x2 面片之间不会出现双线性插值那种明显的色带接缝
            hasBicubicColor = true,
        ) {
            // 只在绘制期读 drift.value：这次读取会被登记在 draw 阶段，
            // 值变化只触发重绘、不触发重组，省掉逐帧的组合开销。
            val angle = drift.value * 2f * PI.toFloat()
            val swayX = sin(angle)
            val swayY = cos(angle)

            // 四角和中心钉死 / 微动，边中点只沿自己那条边滑动 ——
            // 这样顶点永远不会缩到画面内侧，网格始终铺满整屏，不会露出下面的封面。
            setVertex(0, 0, Offset(0f, 0f), meshColors[0])
            setVertex(0, 1, Offset(0.5f + 0.14f * swayX, 0f), meshColors[1])
            setVertex(0, 2, Offset(1f, 0f), meshColors[2])
            setVertex(1, 0, Offset(0f, 0.5f - 0.14f * sin(angle * 0.9f)), meshColors[3])
            setVertex(
                1,
                1,
                Offset(0.5f + 0.06f * sin(angle * 1.3f), 0.5f + 0.06f * cos(angle * 1.1f)),
                meshColors[4],
            )
            setVertex(1, 2, Offset(1f, 0.5f + 0.14f * cos(angle * 0.9f)), meshColors[5])
            setVertex(2, 0, Offset(0f, 1f), meshColors[6])
            setVertex(2, 1, Offset(0.5f - 0.14f * swayY, 1f), meshColors[7])
            setVertex(2, 2, Offset(1f, 1f), meshColors[8])
        }
    }

    Box(modifier.paint(painter, alpha = MESH_ALPHA))
}

/**
 * 从封面图里取一组网格顶点色。
 *
 * 采样要读整张位图，放到 Default 线程做，避免卡住合成帧。
 * 返回 null 表示取不到颜色（没有封面 / 位图已回收），调用方自行降级。
 */
@Composable
private fun rememberMeshColors(coverBitmap: Bitmap?): List<Color>? {
    val colors by produceState<List<Color>?>(initialValue = null, coverBitmap) {
        value = if (coverBitmap == null || coverBitmap.isRecycled) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching { coverBitmap.sampleMeshColors() }.getOrNull()
            }
        }
    }
    return colors
}

/**
 * 把封面缩成 3x3 个区域、每个区域取平均色，正好一个顶点一个颜色。
 *
 * 这里**不用** Palette：实测 Palette 只会给出「主色 / 鲜艳 / 柔和 / 明暗」几个候选，
 * 一张四色渐变封面也会被它归并成同一个洋红，铺出来只是一个单色径向渐变，
 * 完全看不出网格的多色流动。直接按位置采样，背景就是封面配色本身的平滑投影。
 *
 * 缩略分两步：直接 `createScaledBitmap(3, 3)` 是双线性插值、等于只挑了几个像素点，
 * 遇到高光会整块偏色；先缩到 48x48 再对每 16x16 小块求平均，才是真正的区域均色。
 */
private fun Bitmap.sampleMeshColors(): List<Color>? {
    if (width <= 0 || height <= 0) return null

    val columns = MESH_COLUMNS + 1
    val rows = MESH_ROWS + 1
    val cell = SAMPLE_GRID / columns

    val scaled = Bitmap.createScaledBitmap(this, SAMPLE_GRID, SAMPLE_GRID, true)
    try {
        val pixels = IntArray(SAMPLE_GRID * SAMPLE_GRID)
        scaled.getPixels(pixels, 0, SAMPLE_GRID, 0, 0, SAMPLE_GRID, SAMPLE_GRID)

        // 顺序按顶点数组铺：index = row * columns + column，和 setVertex(row, column, ...) 对齐
        return List(columns * rows) { index ->
            val cellRow = index / columns
            val cellColumn = index % columns

            var red = 0
            var green = 0
            var blue = 0
            for (y in cellRow * cell until (cellRow + 1) * cell) {
                for (x in cellColumn * cell until (cellColumn + 1) * cell) {
                    val pixel = pixels[y * SAMPLE_GRID + x]
                    red += (pixel shr 16) and 0xFF
                    green += (pixel shr 8) and 0xFF
                    blue += pixel and 0xFF
                }
            }

            val count = cell * cell
            toAmbientColor(AndroidColor.rgb(red / count, green / count, blue / count))
        }
    } finally {
        // createScaledBitmap 在尺寸相同时会原样返回 this，那种情况不能回收
        if (scaled !== this) scaled.recycle()
    }
}

/** 区域均色 -> 顶点色：压饱和度 + 把亮度夹进 [MIN_LIGHTNESS, MAX_LIGHTNESS]，得到一个「暗、带色」的环境光 */
private fun toAmbientColor(argb: Int): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = (hsl[1] * 0.85f).coerceIn(0f, 1f)
    hsl[2] = hsl[2].coerceIn(MIN_LIGHTNESS, MAX_LIGHTNESS)
    return Color(ColorUtils.HSLToColor(hsl))
}
