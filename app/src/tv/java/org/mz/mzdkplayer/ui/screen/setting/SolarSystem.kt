package org.mz.mzdkplayer.ui.screen.setting
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import java.util.Locale
import kotlin.collections.List
import kotlin.math.cos
import kotlin.math.sin
import org.mz.mzdkplayer.R
/**
 * 太阳系行星数据结构
 * @property name 行星名称
 * @property auRadius 轨道半径 (天文单位 AU)
 * @property periodDays 轨道周期 (地球日)
 * @property planetSizeDp 行星在屏幕上的显示尺寸 (dp)
 * @property color 行星颜色
 */
data class Planet(
    val name: String,
    val auRadius: Double,
    val periodDays: Double,
    val planetSizeDp: Dp,
    val color: Color
)

// 轨道周期数据 (近似值)
// 兼顾视觉与相对趋势的比例方案
private val SOLAR_SYSTEM_BODIES = listOf(
    // 内行星：保持基本可见度，微小差距体现物理趋势
    Planet("水星 (Mercury)", 0.39, 88.0, 3.5.dp, Color(0xFF808080)),
    Planet("金星 (Venus)", 0.72, 224.7, 4.8.dp, Color(0xFFFF8C00)),
    Planet("地球 (Earth)", 1.00, 365.25, 5.0.dp, Color(0xFF0077BE)),
    Planet("火星 (Mars)", 1.52, 687.0, 4.0.dp, Color(0xFFC1440E)),

    // 外行星：显著放大，体现“巨行星”特征
    Planet("木星 (Jupiter)", 5.20, 4331.0, 14.0.dp, Color(0xFFB8A279)),
    Planet("土星 (Saturn)", 9.58, 10747.0, 12.0.dp, Color(0xFFE5D592))
)

// 模拟时间加速倍数 (50倍真实时间)
private const val TIME_SCALE_FACTOR = 500000.0

// 缩放比例: 1 天文单位 (AU) 对应多少 DP
// 变更为 20 dp/AU，以适应 960x540 屏幕
private const val AU_TO_DP_SCALE = 20f

// 轨道基础偏移量 (DP)
// 保证最小轨道在放大后的太阳 (半径 70.dp) 之外
private val BASE_ORBIT_OFFSET_DP = 80.dp

/**
 * 太阳系模型 Composable 函数
 * @param modifier Composable 的修饰符
 */
@Composable
fun SolarSystem(modifier: Modifier = Modifier) {
    val stars = remember {
    List(150) {
        val x = (0..960).random().toFloat()
        val y = (0..540).random().toFloat()
        val alpha = (30..100).random() / 100f // 随机透明度 0.3f 到 1.0f
        Offset(x, y) to alpha
    }
}
    // 跟踪自应用启动以来的总纳秒数
    var totalTimeNanos by remember { mutableLongStateOf(0L) }

    // 使用 LaunchedEffect 启动一个持续运行的动画循环
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                // 计算帧间隔 (纳秒)
                val deltaNanos = frameTimeNanos - lastFrameNanos
                if (lastFrameNanos > 0L) {
                    // 累加总时间 (纳秒)
                    totalTimeNanos += deltaNanos
                }
                lastFrameNanos = frameTimeNanos
            }
        }
    }

    val density = LocalDensity.current

    // 模拟的时间，单位为天
    // 纳秒 -> 秒: / 1_000_000_000.0
    // 秒 -> 天: / (24 * 60 * 60)
    // 模拟天数: * TIME_SCALE_FACTOR
    val simulatedDays = remember(totalTimeNanos) {
        (totalTimeNanos / 1_000_000_000.0) / 86400.0 * TIME_SCALE_FACTOR
    }

    Box(
        // 目标尺寸 960x540 dp，适用于 TV 屏幕
        modifier = modifier.fillMaxSize()
            .background(Color.Black), // 黑色背景模拟宇宙
        contentAlignment = Alignment.Center
    ) {
        // 主画布，用于绘制太阳、轨道和行星
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            // 0. 绘制背景繁星
//            stars.forEach { (pos, alpha) ->
//                // 使用取模 (%) 确保星星坐标均匀分布在当前 Canvas 实际的宽高内
//                val x = pos.x % size.width
//                val y = pos.y % size.height
//
//                drawCircle(
//                    color = Color.White.copy(alpha = alpha),
//                    radius = 0.8.dp.toPx(),
//                    center = Offset(x, y)
//                )
//            }
            // 1. 绘制太阳 (Sun) - 位于中心
            // 半径调整为 70.dp
            val sunRadius = with(density) { 70.dp.toPx() }
            drawCircle(
                color = Color.Yellow,
                center = Offset(centerX, centerY),
                radius = sunRadius
            )

            // 2. 绘制行星及其轨道
            SOLAR_SYSTEM_BODIES.forEach { planet ->
                // 计算轨道半径 (以像素为单位)
                // 轨道半径 = 基础偏移 + (AU距离 * AU缩放比例)
                val orbitalDistanceDp = BASE_ORBIT_OFFSET_DP + (planet.auRadius * AU_TO_DP_SCALE).dp
                val orbitRadiusPx = with(density) { orbitalDistanceDp.toPx() }

                // 绘制轨道 (中心点在画布中心)
                drawCircle(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    center = Offset(centerX, centerY),
                    radius = orbitRadiusPx,
                    style = Stroke(width = 1.dp.toPx()) // 轨道线细一点
                )

                // 计算行星在轨道上的当前角度 (弧度)
                // 角度 = (2 * PI / 周期) * 模拟天数
                val angularVelocity = (2.0 * Math.PI) / planet.periodDays
                val angle = (angularVelocity * simulatedDays)

                // 计算行星的位置 (x, y)
                // 使用简单的圆形轨道方程: x = r * cos(theta), y = r * sin(theta)
                val planetX = centerX + (orbitRadiusPx * cos(angle)).toFloat()
                val planetY = centerY + (orbitRadiusPx * sin(angle)).toFloat()

                // 绘制行星
                val planetRadiusPx = with(density) { planet.planetSizeDp.toPx() }
                drawCircle(
                    color = planet.color,
                    center = Offset(planetX, planetY),
                    radius = planetRadiusPx
                )
                if (planet.name.contains("土星")) {
                    val ringWidth = planetRadiusPx * 2.2f  // 环的横向半径
                    val ringHeight = planetRadiusPx * 0.8f // 环的纵向高度，扁平化处理

                    drawOval(
                        color = planet.color.copy(alpha = 0.6f), // 环稍微透明一点
                        topLeft = Offset(planetX - ringWidth, planetY - ringHeight),
                        size = androidx.compose.ui.geometry.Size(ringWidth * 2, ringHeight * 2),
                        style = Stroke(width = 2.dp.toPx()) // 环的粗细
                    )
                }
            }
        }

        // TV 界面信息：时间流速和当前模拟天数
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(R.string.ui_label_time_flow_rate, TIME_SCALE_FACTOR),
                color = Color.White,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                // 直接传入字符串资源 ID 和对应的变量
                text = stringResource(R.string.ui_label_simulated_days, simulatedDays),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 太阳标签
        Text(
            text = "Sun",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 80.dp) // 标签向下偏移，避开扩大的太阳
        )
    }
}

// 预览 Composable
//@Preview(showBackground = true, device = "spec:width=960dp,height=540dp,dpi=240")
//@Composable
//fun SolarSystemPreview() {
//
//    MaterialTheme {
//        SolarSystem(
//            modifier = Modifier
//                .fillMaxSize()
//        )
//    }
//}