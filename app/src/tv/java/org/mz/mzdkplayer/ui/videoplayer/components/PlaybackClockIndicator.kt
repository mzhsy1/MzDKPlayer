package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 暂停时右上角的当前时间。
 *
 * 只在暂停时出现：播放中那个位置是实时网速（两者互斥，不会叠在一起）。
 * 长时间暂停的人（去泡茶、接电话、哄孩子）回来能立刻知道几点了，不用摸手机。
 *
 * 样式与 `NetworkSpeedIndicator` 保持一致（半透明黑底 + 4dp 圆角），只是字号大一点。
 */
@Composable
fun PlaybackClockIndicator(modifier: Modifier = Modifier) {
    // 只在主线程协程里用，不需要为线程安全额外加锁；remember 避免每秒重建 formatter
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var timeText by remember { mutableStateOf(formatter.format(Date())) }

    LaunchedEffect(formatter) {
        while (true) {
            timeText = formatter.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
