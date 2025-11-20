package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import java.io.File

@Composable

fun LoadingScreen(
    text: String = "正在加载",
    modifier: Modifier,
    fontSize: Int = 26,
    arcSize: Int = 80,
    subtitle: String = "",
    subtitleFontSize: Int = 20
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TwoArcLoading(modifier = Modifier.size(arcSize.dp))
            Spacer(modifier = Modifier.height(16.dp)) // 用 Spacer 控制间距，自适应
            Text(
                text = text,
                fontSize = fontSize.sp,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = subtitle,
                fontSize = subtitleFontSize.sp,
                color = Color.Gray
            )

        }
    }
}

@Composable
fun TwoArcLoading(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 8f, // 可配置
    arcSweepAngle: Float = 120f, // 更饱满的弧
    rotationDurationMillis: Int = 1200 // 稍慢一点更优雅
) {
    val transition = rememberInfiniteTransition(label = "loading_rotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = InfiniteRepeatableSpec(
            tween(
                durationMillis = rotationDurationMillis,
                easing = FastOutSlowInEasing
            )
        ),
        label = "arc_rotation"
    )

    Canvas(modifier = modifier) {
        val canvasSize = size
        val centerX = canvasSize.width / 2
        val centerY = canvasSize.height / 2
        val radius = (minOf(canvasSize.width, canvasSize.height) * 0.4f) // 自适应半径，留边距

        // 第一个弧：从 angle 开始
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = arcSweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // 第二个弧：对面 180° 位置
        drawArc(
            color = color,
            startAngle = angle + 180f,
            sweepAngle = arcSweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun FileEmptyScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_folder_off_24),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}