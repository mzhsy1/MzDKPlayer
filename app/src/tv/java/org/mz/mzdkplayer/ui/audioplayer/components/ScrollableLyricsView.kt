package org.mz.mzdkplayer.ui.audioplayer.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.LyricEntry
import kotlin.math.roundToInt
import kotlin.text.ifEmpty
import kotlin.time.Duration

// --- 歌词组件 ---

@SuppressLint("UnrememberedMutableState")
@Composable
fun ScrollableLyricsView(
    currentPositionProvider: () -> Duration,
    parsedLyrics: List<LyricEntry>,
    topMaskColor: Color,
    bottomMaskColor: Color
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var lastHighlightedIndex by remember { mutableIntStateOf(-1) }

    // 查找当前应高亮的歌词索引
    val highlightedIndex by derivedStateOf {
        if (parsedLyrics.isEmpty()) return@derivedStateOf -1
        val currentPosition = currentPositionProvider()
        var index = parsedLyrics.indexOfLast { it.time <= currentPosition }
        index = index.coerceAtLeast(0)
        index
    }

    // 自动滚动逻辑
    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex >= 0 && highlightedIndex != lastHighlightedIndex && parsedLyrics.isNotEmpty()) {
            lastHighlightedIndex = highlightedIndex
            coroutineScope.launch {
                // 核心修改：由于我们加了巨大的 ContentPadding，
                // scrollToItem(index) 默认会将该 Item 滚动到 Padding 的边缘（也就是屏幕中间）。
                // 为了视觉完美，我们微调一点偏移量（减去字体高度的一半，比如 20px），让文字中心对齐中线。
                lazyListState.animateScrollToItem(highlightedIndex, scrollOffset = -30)
            }
        }
    }

    if (parsedLyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.ui_label_no_lyrics_yet),
                fontSize = 20.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    } else {
        // 使用 BoxWithConstraints 获取当前容器的高度
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            // 计算高度的一半
            val halfHeight = maxHeight / 2
            // 转换为 PaddingValues
            // 减去一点点高度(比如 30.dp)，是为了让第一句和最后一句视觉上更靠近真正的物理中心
            val listPadding = PaddingValues(
                top = halfHeight - 30.dp,
                bottom = halfHeight - 30.dp
            )

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    // 核心修改：使用 calculated padding 替代 verticalArrangement 的居中
                    contentPadding = listPadding,
                    // 这里只需要普通的间距即可，居中完全由 padding 控制
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start,
                    userScrollEnabled = true // 允许用户手动滑动查看
                ) {
                    itemsIndexed(parsedLyrics) { index, entry ->
                        val isHighlighted = index == highlightedIndex

                        // 字体大小动画
                        val fontSize by animateFloatAsState(
                            targetValue = if (isHighlighted) 24f else 18f, // 稍微加大了一点字号，TV端更清晰
                            animationSpec = tween(durationMillis = 300),
                            label = "fontSize"
                        )

                        // 透明度动画
                        val alpha by animateFloatAsState(
                            targetValue = if (isHighlighted) 1f else 0.4f,
                            animationSpec = tween(durationMillis = 300),
                            label = "alpha"
                        )

                        Text(
                            text = entry.text.ifEmpty { "..." },
                            fontSize = fontSize.sp,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(alpha)
                                // 增加点击/选中反馈区域（可选）
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // 如果需要遮罩，可以在这里添加 Box 覆盖层
                // ...
            }
        }
    }
}