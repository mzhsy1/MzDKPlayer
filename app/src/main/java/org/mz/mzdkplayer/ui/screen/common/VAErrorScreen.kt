package org.mz.mzdkplayer.ui.screen.common


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

@Composable
fun VAErrorScreen(errorText: String = "未知错误") {
    // 使用 Surface 可以方便地设置背景色和内容颜色

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // 添加整体内边距
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState()) // 允许内容滚动以防文本过长
                    .padding(24.dp), // 给内容区域添加内边距
                horizontalAlignment = Alignment.CenterHorizontally, // 水平居中 Column 内的元素
                verticalArrangement = Arrangement.spacedBy(16.dp) // 设置元素间的垂直间距
            ) {
                // 错误图标
                Image(
                    painter = painterResource(id = R.drawable.error24), // 请确保资源存在
                    contentDescription = "Error Icon",
                    modifier = Modifier.size(100.dp) // 可选：调整图标大小
                )

                // 主错误标题
                Text(
                    text = "出错了",
                    style = MaterialTheme.typography.headlineSmall, // 使用 Material3 标题样式
                    fontWeight = FontWeight.Bold, // 加粗
                    color = Color.White, // 使用主题文字颜色
                    textAlign = TextAlign.Center
                )

                // 错误详细信息
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium, // 使用 Material3 正文样式
                    color = Color.White.copy(alpha = 0.8f), // 稍微降低不透明度
                    textAlign = TextAlign.Center
                )
            }

    }
}


