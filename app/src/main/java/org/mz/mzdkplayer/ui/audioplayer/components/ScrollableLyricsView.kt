package org.mz.mzdkplayer.ui.audioplayer.components

import LyricEntry
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import kotlin.text.ifEmpty
import kotlin.time.Duration

// --- 歌词组件 ---

/**
 * 可滚动的歌词视图 (使用已解析的歌词列表)，并尝试将高亮行保持在中间。
 * 如果歌词较少，则居中显示；如果没有歌词，则显示“暂无歌词”。
 *
 * @param currentPosition 当前播放位置 (Duration)
 * @param parsedLyrics 已解析的歌词列表 (包含时间戳)
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun ScrollableLyricsView(currentPosition: Duration, parsedLyrics: List<LyricEntry>) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
                // 尝试计算使目标项大致居中的偏移量。
                // 假设每行文本大约占用 30dp + padding (约为 44px on mdpi, ~66px on xhdpi etc.)
                // 我们希望它出现在列表可视区域的大致中间。
                // 一种常见的近似做法是滚动到该项，然后向上偏移半个列表高度。
                // lazyListState.layoutInfo.viewportEndOffset - viewportStartOffset 是可视区总高度(px)
                // 我们假定单个item平均高度约60px用于计算 offset.
                val avgItemHeightPx = 60 // 启发式估计，可根据实际UI调整
                val viewportHeightPx = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val estimatedCenterOffset = (viewportHeightPx / 2) - (avgItemHeightPx / 2)

                // animateScrollToItem 第二个参数是相对于该项顶部的偏移量，
                // 负数表示向上滚动更多，从而让该项处于更中心的位置。
                lazyListState.animateScrollToItem(highlightedIndex, -estimatedCenterOffset)
            }
        }
    }


    if (parsedLyrics.isEmpty()) {
        // 如果没有歌词，显示居中的提示信息
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center, // 内容垂直居中
            horizontalAlignment = Alignment.CenterHorizontally // 内容水平居中
        ) {
            item {
                Text(
                    text = "暂无歌词",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center, // 文本本身也居中对齐（虽然在Column居中时效果不明显）
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    } else {
        // 如果有歌词，正常显示歌词列表
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp), // 行间距
            horizontalAlignment = Alignment.Start // 文本左对齐
            // 注意：默认情况下，LazyColumn 内容会从顶部开始布局。
            // 如果内容不足以填满屏幕，它不会自动居中。
            // 要实现内容不足时居中，需要更复杂的布局或自定义测量，这在 LazyColumn 中比较困难。
            // 或者可以考虑使用普通的 Column 并包裹在可垂直滚动的 Box/Column 中，
            // 并根据内容高度动态调整 Column 的 verticalArrangement。
            // 但对于大多数播放器场景，歌词从顶部开始滚动是标准行为。
            // 如果确实需要“内容不足时居中”，请告知，我可以提供替代方案。
        ) {
            itemsIndexed(parsedLyrics) { index, entry ->
                Text(
                    text = entry.text.ifEmpty { "..." },
                    fontSize = if (index == highlightedIndex) 18.sp else 16.sp,
                    fontWeight = if (index == highlightedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == highlightedIndex) Color.White else Color.Gray,
                    textAlign = TextAlign.Start, // 确保文本左对齐
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp) // 增加左右 padding
                        .alpha(if (index == highlightedIndex) 1f else 0.7f)
                )
            }
        }
    }
}



