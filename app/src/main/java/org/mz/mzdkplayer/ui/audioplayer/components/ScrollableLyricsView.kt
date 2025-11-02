package org.mz.mzdkplayer.ui.audioplayer.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import kotlin.text.ifEmpty
import kotlin.time.Duration

// --- 歌词组件 ---

/**
 * 可滚动的歌词视图 (使用已解析的歌词列表)，并尝试将高亮行保持在中间。
 * 如果歌词较少，则居中显示；如果没有歌词，则显示"暂无歌词"。
 * 添加了上下边界的自然过渡效果，适配黑色背景，以及字体大小动画
 *
 * @param currentPosition 当前播放位置 (Duration)
 * @param parsedLyrics 已解析的歌词列表 (包含时间戳)
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun ScrollableLyricsView(currentPosition: Duration, parsedLyrics: List<LyricEntry>) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var lastHighlightedIndex by remember { mutableIntStateOf(-1) }

    // 查找当前应高亮的歌词索引
    val highlightedIndex by derivedStateOf {
        if (parsedLyrics.isEmpty()) return@derivedStateOf -1
        var index = parsedLyrics.indexOfLast { it.time <= currentPosition }
        index = index.coerceAtLeast(0)
        index
    }

    // 自动滚动到高亮行，并尽量使其居中
    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex >= 0 && highlightedIndex != lastHighlightedIndex && parsedLyrics.isNotEmpty()) {
            lastHighlightedIndex = highlightedIndex
            coroutineScope.launch {
                // 尝试计算使目标项大致居中的偏移量
                val avgItemHeightPx = 60 // 启发式估计，可根据实际UI调整
                val viewportHeightPx = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val estimatedCenterOffset = (viewportHeightPx / 2) - (avgItemHeightPx / 2)

                // animateScrollToItem 第二个参数是相对于该项顶部的偏移量
                lazyListState.animateScrollToItem(highlightedIndex, -estimatedCenterOffset)
            }
        }
    }

    // 定义自然过渡效果的渐变色 - 适配黑色背景
    val blurHeight = 25.dp
    val topGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.8f),           // 深色开始
            Color.Black.copy(alpha = 0.4f),           // 中间透明度
            Color.Transparent                         // 结束透明
        ),
        startY = 0f,
        endY = with(density) { blurHeight.toPx() }
    )

    val bottomGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,                       // 开始透明
            Color.Black.copy(alpha = 0.4f),           // 中间透明度
            Color.Black.copy(alpha = 0.8f)            // 深色结束
        ),
        startY = 0f,
        endY = with(density) { blurHeight.toPx() }
    )

    if (parsedLyrics.isEmpty()) {
        // 如果没有歌词，显示居中的提示信息
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无歌词",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    } else {
        // 如果有歌词，正常显示歌词列表，并添加自然过渡效果
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 主要的歌词列表
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize(), // 移除了 padding，让内容填满整个区域
                verticalArrangement = Arrangement.spacedBy(8.dp), // 行间距
                horizontalAlignment = Alignment.Start
            ) {
                itemsIndexed(parsedLyrics) { index, entry ->
                    // 为字体大小添加动画
                    val fontSize by animateFloatAsState(
                        targetValue = if (index == highlightedIndex) 20f else 16f,
                        animationSpec = tween(durationMillis = 300),
                        label = "fontSize"
                    )

                    Text(
                        text = entry.text.ifEmpty { "..." },
                        fontSize = fontSize.sp,
                        fontWeight = if (index == highlightedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == highlightedIndex) Color.White else Color.Gray,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 4.dp)
                            .alpha(if (index == highlightedIndex) 1f else 0.7f)
                    )
                }
            }

            // 顶部过渡效果 - 覆盖在内容上方，但不增加额外高度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blurHeight)
                    .background(topGradient)
            )

            // 底部过渡效果 - 覆盖在内容下方，但不增加额外高度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blurHeight)
                    .align(Alignment.BottomCenter)
                    .background(bottomGradient)
            )
        }
    }
}



