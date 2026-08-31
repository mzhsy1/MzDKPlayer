package org.mz.mzdkplayer



import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LaunchScreen(
    onFinish: () -> Unit,
    durationMillis: Long = 100 // 默认显示 0 秒
) {
    // 可选：添加淡入淡出动画
    val alpha by animateFloatAsState(
        targetValue = 1f,
        label = "splash_alpha"
    )
    // 获取 Context
    val context = LocalContext.current

    // 模拟启动过程（如初始化数据库、检查登录状态等）
    LaunchedEffect(Unit) {
        // 👇 关键修改：切到 IO 线程去执行文件读写操作
        withContext(Dispatchers.IO) {
            // 假设你已经把方法放进了 Tools 工具类中，并且字体叫 TvFont.ttf
            //Tools.prepareSubtitleFont(context, "SmileySans-Oblique.ttf")
        }

        delay(durationMillis.milliseconds)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // 背景色，可替换为你的品牌色
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {

        Column {
            Icon(
                painter = painterResource(id = R.drawable.baseline_play_arrow_24), // 替换为你自己的启动图标
                contentDescription = "App Logo",
                tint = Color.White,
                modifier = Modifier.size(200.dp)
            )
            Text("MzDKPlayer", fontSize = 40.sp, color = Color.White)
        }


    }
}