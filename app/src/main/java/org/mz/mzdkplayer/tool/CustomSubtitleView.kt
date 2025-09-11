import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * 自定义 SubtitleView 组件，用于在 Jetpack Compose 中显示 Media3 ExoPlayer 的字幕。
 *
 * @param cues 当前需要显示的字幕 Cues 列表。
 * @param modifier Compose Modifier。
 * @param subtitleStyle 字幕的文本样式，例如字体大小、颜色。
 * @param backgroundColor 字幕区域的背景色，通常设置为半透明以增强可读性。
 */
@Composable
fun CustomSubtitleView(
    cueGroup: CueGroup?,
    modifier: Modifier = Modifier,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = Color.White,
        fontSize = 18.sp // 默认字体大小
    ),
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f) // 默认半透明黑色背景
) {
    // 使用 Box 作为容器，方便定位和添加背景
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter // 将字幕放置在底部
    ) {
        // 检查 cueGroup 是否不为 null 且包含 Cue
        if (cueGroup != null && cueGroup.cues.isNotEmpty()) {
            // 使用 Column 垂直排列所有的字幕 Cue
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp), // 字幕行之间的间距
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 遍历并显示 cueGroup 中的每一个 Cue
                cueGroup.cues.forEach { cue ->
                    // Text 组件用于显示单个字幕文本
                    // 注意：cue.text 是 CharSequence，可能需要根据具体需求处理样式
                    Text(
                        text = cue.text.toString(), // 转换为 String 以便显示
                        style = subtitleStyle,
                        textAlign = TextAlign.Center, // 文本居中对齐
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        // 如果 cueGroup 为 null 或 cues 为空，则不显示任何内容

    }
}


