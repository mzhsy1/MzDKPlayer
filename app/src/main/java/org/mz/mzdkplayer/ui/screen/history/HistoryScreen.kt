package org.mz.mzdkplayer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.mz.mzdkplayer.logic.model.MediaHistoryRecord
import org.mz.mzdkplayer.ui.screen.vm.MediaHistoryViewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text


import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.theme.MyListItemCoverColor
import org.mz.mzdkplayer.ui.theme.myTTFColor
import java.util.Locale
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaHistoryScreen(
    navController: NavHostController,
    viewModel: MediaHistoryViewModel = viewModel()
) {
    val historyList by viewModel.history.collectAsState()
    val isHistoryPanelShow by viewModel.isHistoryPanelShow.collectAsState()
    var searchText by remember { mutableStateOf("") }

    // 默认加载最近20条记录
    LaunchedEffect(Unit) {
        viewModel.getRecentHistory(20)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放历史",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // 全部历史按钮
            MyIconButton(
                modifier = Modifier,
                onClick = {
                    viewModel.resetToAllHistory()
                    searchText = ""
                },
                text = "全部历史",
                icon = R.drawable.history24dp // 请替换为实际的历史图标资源
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 搜索栏
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardDefaults.shape(shape = MaterialTheme.shapes.medium),
            onClick = {}
//            colors = CardDefaults.colors(
//                containerColor = Color(0xFF2D2D2D)
//            )
        ) {
            TvTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    if (it.isNotEmpty()) {
                        viewModel.searchHistoryByFileName(it)
                    } else {
                        viewModel.getRecentHistory(20)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = myTTFColor(),
                placeholder =
                    "请输入文件名搜索"
                ,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 历史记录列表
        if (historyList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { record ->
                    HistoryListItem(
                        record = record,
                        onItemClick = {
                            // 处理历史记录项点击
                            viewModel.selectHistoryRecord(record)
                        },
                        onItemLongClick = {
                            // 处理长按删除
                            viewModel.deleteHistory(record)
                        }
                    )
                }
            }
        } else {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.history24dp), // 请替换为实际的空状态图标
                        contentDescription = "空历史",
                        tint = Color(0xFF666666),
                        modifier = Modifier
                            .width(64.dp)
                            .height(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchText.isNotEmpty()) "未找到相关历史记录" else "暂无播放历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        // 统计信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onClick = {}
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "总计: ${viewModel.getHistoryCount()} 条记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCCCCCC)
                )

                if (searchText.isNotEmpty()) {
                    Text(
                        text = "搜索结果: ${historyList.size} 条",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryListItem(
    record: MediaHistoryRecord,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    Card(
        onClick = onItemClick,
        onLongClick = onItemLongClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF2D2D2D),
            focusedContainerColor = Color(0xFF3D3D3D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
    ) {
        ListItem(
            modifier = Modifier.padding(16.dp),
            colors = MyListItemCoverColor(),
            selected = false,
            onClick = {},
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // 媒体类型标签
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (record.isVideo()) Color(0xFF2196F3) else Color(0xFF9C27B0),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (record.isVideo()) "视频" else "音频",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            supportingContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 协议和连接信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${record.protocolName} • ${record.connectionName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCCCCCC)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 播放进度和日期
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 播放进度
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "进度: ${record.getFormattedPosition()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )

                            if (record.mediaDuration > 0) {
                                Text(
                                    text = " / ${record.getFormattedDuration()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )

                                // 进度条
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .width(80.dp)
                                        .height(4.dp)
                                        .background(Color(0xFF444444), MaterialTheme.shapes.small)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(4.dp)
                                            .width((80 * record.getPlaybackPercentage() / 100).dp)
                                            .background(Color(0xFF4CAF50), MaterialTheme.shapes.small)
                                    )
                                }
                            }
                        }

                        // 播放日期
                        Text(
                            text = record.getFormattedDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        )
    }
}



@Composable
fun MyIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    icon: Int
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF2D2D2D),
            focusedContainerColor = Color(0xFF3D3D3D)
        ),
        scale = CardDefaults.scale(focusedScale = 1.05f),
//        border = CardDefaults.border(
//            focusedBorderWidth = 2.dp,
//            focusedBorderColor = Color(0xFF4CAF50)
//        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = Color.White
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

// 扩展函数，用于格式化媒体时长
fun MediaHistoryRecord.getFormattedDuration(): String {
    val totalSeconds = mediaDuration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
}

// 注意：您需要在res/drawable目录中添加以下图标资源：
// - history_icon.xml (历史图标)
// - history_empty_icon.xml (空状态图标)