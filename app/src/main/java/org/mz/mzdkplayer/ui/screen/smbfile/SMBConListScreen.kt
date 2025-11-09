package org.mz.mzdkplayer.ui.screen.smbfile

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Border

import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.SMBConnection
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel
import org.mz.mzdkplayer.ui.style.myListItemBorder
import org.mz.mzdkplayer.ui.style.myListItemCoverColor
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import java.net.URLEncoder

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SMBConListScreen(mainNavController: NavHostController) {
    val smbListViewModel: SMBListViewModel = viewModel()
    val connections by smbListViewModel.connections.collectAsState()
    val isOPanelShow by smbListViewModel.isOPanelShow.collectAsState()
    val selectedIndex by smbListViewModel.selectedIndex.collectAsState()
    val selectedId by smbListViewModel.selectedId.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(isOPanelShow) {
        Log.d("isOPanelShow", isOPanelShow.toString())
    }

    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isOPanelShow) {
        if (isOPanelShow) {
            panelFocusRequester.requestFocus()
        } else {
            listFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isOPanelShow) {
        smbListViewModel.closeOPanel()
    }

    LaunchedEffect(isOPanelShow) {
        if (!isOPanelShow && selectedIndex != -1) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)) // 深黑背景
        ) {
            // ====== 标题栏 ======
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E)) // 深灰标题栏
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.storage24dp),
                            contentDescription = "SMB",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "网络存储",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "SMB 文件共享",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB0B0B0) // 浅灰色
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 添加新连接按钮
                        MyIconButton(
                            modifier = Modifier.padding(end = 12.dp),
                            onClick = { mainNavController.navigate("SMBConScreen") },
                            text = "添加连接",
                            icon = R.drawable.add24dp,
                        )

                        MyIconButton(
                            onClick = { mainNavController.navigate("SMBConScreen") },
                            text = "帮助",
                            icon = R.drawable.help24,
                        )
                    }
                }
            }

            // ====== 内容区域 ======
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (connections.isEmpty()) {
                    // 空状态设计
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.storage24dp),
                                contentDescription = "Empty",
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "暂无 SMB 连接",
                                color = Color(0xFF999999),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "点击右上角按钮添加您的第一个 SMB 连接",
                                color = Color(0xFF777777),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // 连接列表标题
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已保存的连接 (${connections.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)
                            .focusRequester(listFocusRequester),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(connections) { index, conn ->
                            ConnectionCard(
                                index = index,
                                connection = conn,
                                onClick = {
                                    mainNavController.navigate(
                                        "SMBFileListScreen/${
                                            URLEncoder.encode(
                                                "smb://${conn.username}:${conn.password}@${conn.ip}/${conn.shareName}/",
                                                "UTF-8"
                                            )
                                        }"
                                    )
                                },
                                onLogClick = {
                                    smbListViewModel.openOPlane()
                                    smbListViewModel.setSelectedIndex(index)
                                    smbListViewModel.setSelectedId(conn.id)
                                },
                                onDelete = { },
                                viewModel = smbListViewModel,
                                isOPanelShow = isOPanelShow
                            )
                        }
                    }
                }
            }
        }

        // 操作面板遮罩层
        if (isOPanelShow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}
            )
        }

        // 操作面板（右侧弹出）
        AnimatedVisibility(
            visible = isOPanelShow,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 30.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Color(0xFF2D2D2D),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .width(260.dp)
                    .focusRequester(panelFocusRequester)
                    .focusable()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // 面板标题
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF424242))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "连接操作",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 使用 Column 替代 LazyColumn 确保内容居中
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OperationListItem(
                        text = "删除连接",
                        textColor = Color(0xFFF44336),
                        true,
                        onClick = {
                            smbListViewModel.deleteConnection(selectedId)
                            smbListViewModel.closeOPanel()
                        }
                    )

                    OperationListItem(
                        text = "编辑信息",
                        textColor = Color.White,
                        onClick = { /* TODO: 编辑逻辑 */ }
                    )

                    OperationListItem(
                        text = "取消",
                        textColor = Color(0xFFB0B0B0),
                        onClick = { smbListViewModel.closeOPanel() }
                    )
                }
            }
        }

        @Composable
        fun OperationListItem(
            text: String,
            textColor: Color,
            onClick: () -> Unit
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            Box(
                modifier = Modifier
                    .width(220.dp) // 固定宽度确保居中
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (isPressed) {
                            onClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp), // 增加垂直内边距
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun OperationListItem(
    text: String,
    textColor: Color,
    isDel: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    ListItem(
        modifier = Modifier
            .width(220.dp)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp)),
        selected = false,
        onClick = {
            if (isPressed) {
                onClick()
            }
        },
        interactionSource = interactionSource,
        colors = myListItemCoverColor(),
        //border = myListItemBorder(),
        headlineContent = {
            if (isDel) {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,

                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Medium
                )
            }
        }

    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectionCard(
    index: Int,
    connection: SMBConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit,
    viewModel: SMBListViewModel,
    isOPanelShow: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val isSelected = viewModel.selectedIndex.collectAsState().value == index && !isOPanelShow

    LaunchedEffect(isOPanelShow) {
        if (isSelected) {
            focusRequester.requestFocus()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .focusRequester(focusRequester),
        onClick = onClick,
        onLongClick = onLogClick,
        colors = CardDefaults.colors(
            containerColor = Color(0xFF2D2D2D), // 卡片背景色
            contentColor = Color.White, // 内容文字颜色
            focusedContainerColor = Color.White, // 聚焦时背景色
            focusedContentColor = Color.Black,
            pressedContainerColor = Color(0xFF37474F), // 按下时背景色
            pressedContentColor = Color.White
        ),
        scale = CardDefaults.scale(
            scale = 1f,
            focusedScale = 1.03f, // 聚焦时轻微放大
            pressedScale = 0.98f // 按下时轻微缩小
        ),
        border = myCardBorderStyle(),
        glow = CardDefaults.glow(
            glow = androidx.tv.material3.Glow.None,
//            focusedGlow = androidx.tv.material3.Glow(
//                color = Color.White.copy(alpha = 0.2f), // 聚焦时光晕效果
//                radius = 12.dp
//            ),
            pressedGlow = androidx.tv.material3.Glow.None
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标区域
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = if (isSelected) Color(0xFF424242) else Color(0xFF37474F),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.storage24dp),
                    contentDescription = "SMB Connection",
                    tint = if (isSelected) Color.White else Color(0xFFB0B0B0),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 内容区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 连接名称
                connection.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,

                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 连接详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ConnectionInfoItem(
                        label = "服务器",
                        value = connection.ip
                    )

                    ConnectionInfoItem(
                        label = "共享目录",
                        value = connection.shareName
                    )

                    if (connection.username?.isNotEmpty() ?: true) {
                        ConnectionInfoItem(
                            label = "用户名",
                            value = connection.username
                        )
                    }
                }
            }

            // 状态指示器
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = Color(0xFF4CAF50), // 绿色在线状态
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
fun ConnectionInfoItem(
    label: String,
    value: String?
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0B0B0), // 浅灰色标签
            fontSize = 12.sp
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}