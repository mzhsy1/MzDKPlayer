package org.mz.mzdkplayer.ui.screen.history

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.magnifier
import androidx.paging.LoadState
import androidx.paging.compose.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.*
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.DashboardTopBarItemIndicator
import org.mz.mzdkplayer.ui.screen.common.EmptyStateView
import org.mz.mzdkplayer.ui.screen.common.TwoArcLoading
import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.net.URLEncoder

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaHistoryScreen(
    navController: NavHostController,
    viewModel: MediaHistoryViewModel
) {
    // Paging 3 数据源
    val videoHistoryItems = viewModel.videoHistory.collectAsLazyPagingItems()
    val audioHistoryItems = viewModel.audioHistory.collectAsLazyPagingItems()

    // 简单的 Tab 切换状态
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Video, 1: Audio

    // 假设 isTabRowFocused 状态逻辑
    val isTabRowFocused by remember { mutableStateOf(false) }
    // 👇 添加一个 FocusRequester
    val buttonFocusRequester = remember { FocusRequester() }

    // 👇 关键：页面加载后，主动把焦点丢给按钮
    LaunchedEffect(Unit) {
        buttonFocusRequester.requestFocus()
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

        Text(text = stringResource(R.string.ui_label_playback_history), style = MaterialTheme.typography.headlineMedium, color = Color.White)

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(selectedTabIndex = selectedTab,
            modifier = Modifier.focusRequester(buttonFocusRequester),
            indicator  = { tabPositions, _ ->
            if (selectedTab >= 0) {
                DashboardTopBarItemIndicator(
                    currentTabPosition = tabPositions[selectedTab],
                    anyTabFocused = isTabRowFocused,
                    shape = ShapeDefaults.ExtraSmall,
                    activeColor = Color(0xFF000000),
                    inactiveColor =Color(0xFFFFFFFF),
                )
            }
        }) {
            Tab(
                selected = selectedTab == 0,
                onFocus = { selectedTab = 0 },
                onClick = { selectedTab = 0 }
            ) {
                val textColor by animateColorAsState(
                    targetValue = if (selectedTab == 0) Color.Black else Color.White,
                    animationSpec = tween(durationMillis = 100)
                )
                // 修正 Tab Count
                Text(stringResource(R.string.ui_label_video_history_count,videoHistoryItems.itemCount), modifier = Modifier.padding(12.dp), color = textColor)
            }
            Tab(
                selected = selectedTab == 1,
                onFocus = { selectedTab = 1 },
                onClick = { selectedTab = 1 }
            ) {
                val textColor by animateColorAsState(
                    targetValue = if (selectedTab == 1) Color.Black else Color.White,
                    animationSpec = tween(durationMillis = 100)
                )
                // 修正 Tab Count
                Text(stringResource(R.string.ui_label_audio_history_count,audioHistoryItems.itemCount), modifier = Modifier.padding(12.dp), color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            when {
                // 首次加载：列表区先给转圈，避免空白页误以为没有记录
                videoHistoryItems.itemCount == 0 && videoHistoryItems.loadState.refresh is LoadState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TwoArcLoading(Modifier.size(56.dp))
                    }
                }
                // 空态：图标 + 标题 + 说明，与媒体库空态同一套组件
                videoHistoryItems.itemCount == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            iconRes = R.drawable.historyoff24dp,
                            title = stringResource(R.string.ui_label_no_playback_history),
                            description = stringResource(R.string.ui_label_no_playback_history_desc),
                            accent = Color(0xFF64B5F6)
                        )
                    }
                }
                else -> {
                    // === 视频历史 (Grid) ===
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 使用 Paging 3 的 items 扩展函数，传入 videoHistoryItems
                        items(
                            count = videoHistoryItems.itemCount,
                            // 如果需要 key，注意这里要通过 index 获取 item
                            key = { index ->
                                // peek 也就是不触发加载地查看数据，防止 key 生成时触发过多加载
                                val item = videoHistoryItems.peek(index)
                                item?.history?.mediaUri ?: index
                            }
                        ) { index ->
                            val item = videoHistoryItems[index]
                            // item 现在是 MediaHistoryItem 类型 (或 null)
                            if (item != null) {
                                VideoHistoryCard(
                                    historyItem = item,
                                    onClick = {
                                        val record = item.history
                                        val encodedUri = record.mediaUri.toBase64()
                                        val encodedFileName = record.fileName.toBase64()
                                        val encodedConnName = record.connectionName.toBase64()
                                        // 导航逻辑
                                        Log.d("HIS", record.protocolName)
                                        val protocolName = if (record.protocolName == "本地文件") {
                                            "LOCAL"
                                        } else {
                                            record.protocolName
                                        }
                                        navController.navigate("VideoPlayer/$encodedUri/${protocolName}/$encodedFileName/$encodedConnName")
                                    }
                                )
                            } else {
                                // 可以显示一个占位符或加载动画
                                Spacer(Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }
        } else {
            when {
                audioHistoryItems.itemCount == 0 && audioHistoryItems.loadState.refresh is LoadState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TwoArcLoading(Modifier.size(56.dp))
                    }
                }
                audioHistoryItems.itemCount == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateView(
                            iconRes = R.drawable.musichistory24dp,
                            title = stringResource(R.string.ui_label_no_playback_history),
                            description = stringResource(R.string.ui_label_no_playback_history_desc),
                            accent = Color(0xFF81C784)
                        )
                    }
                }
                else -> {
                    // === 音频历史 (List) ===
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 使用 Paging 3 的 items 扩展函数，传入 audioHistoryItems
                        items(
                            count = audioHistoryItems.itemCount,
                            key = { index ->
                                val item = audioHistoryItems.peek(index)
                                item?.mediaUri ?: index
                            }
                        ) { index ->
                            val record = audioHistoryItems[index]
                            // record 现在是 AudioHistoryRecord 类型 (或 null)
                            if (record != null) {
                                ListItem(
                                    selected = false,
                                    onClick = {
                                        // 音频导航逻辑 (待补充)
                                    },
                                    colors = MyFileListItemColor(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    // 和视频库 / 文件列表同一套图标底：绿色小方块 + 音符
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(Color(0xFF81C784).copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(R.drawable.baseline_music_note_24),
                                                contentDescription = null,
                                                tint = Color(0xFF81C784),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    headlineContent = { Text(record.fileName, maxLines = 1) },
                                    supportingContent = {
                                        Text(record.connectionName, maxLines = 1)
                                    },
                                    trailingContent = {
                                        Text(
                                            text = "${record.getPlaybackPercentage()}%",
                                            fontSize = 12.sp,
                                            color = Color(0xFF9A96A6)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}