package org.mz.mzdkplayer.ui.screen.common

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R


import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor


/**
 * ====== 标题栏 ======
 */
@Composable
fun FCLMainTitle(
    mainNavController: NavHostController,
    titleText: String,
    addTargetRouter: String = "",
    isLocalFile: Boolean = false
) {

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
                    painter = if (!isLocalFile) painterResource(id = R.drawable.storage24dp) else painterResource(
                        id = R.drawable.localfile
                    ),
                    contentDescription = "SMB",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (!isLocalFile) "网络存储" else "本地存储",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0B0B0) // 浅灰色
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 添加新连接按钮
                if (!isLocalFile) {
                    MyIconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = { mainNavController.navigate(addTargetRouter) },
                        text = "添加连接",
                        icon = R.drawable.add24dp,
                    )
                }

                MyIconButton(
                    onClick = { /**TODO 转到设置帮助页面**/ },
                    text = "帮助",
                    icon = R.drawable.help24,
                )
            }
        }
    }
}

/**
 * 连接列表标题
 */

@Composable
fun ConnectionListTitle(conSize: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已保存的连接 ($conSize)",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 空列表状态
 */

@Composable
fun ConnectionListEmpty(poolText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    )
    {
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
                text = "暂无 $poolText 连接",
                color = Color(0xFF999999),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "点击右上角按钮添加您的第一个 $poolText 连接",
                color = Color(0xFF777777),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 文件列表通用卡片
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectionCard(
    modifier: Modifier,
    index: Int,
    connectionCardInfo: ConnectionCardInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit,
    isSelected: Boolean,
    isOPanelShow: Boolean,
    selectedIndex: Int,
) {
    val focusRequester = remember { FocusRequester() }
    // 当操作面板显示/隐藏状态改变时，如果当前卡片是选中的，则请求焦点
    LaunchedEffect(!isOPanelShow) {
        if (selectedIndex == index && !isOPanelShow) {
            Log.d("Card", "Requesting focus for selected card at index: $index")
            focusRequester.freeFocus()
            focusRequester.requestFocus()
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .focusRequester(focusRequester),
        onClick = onClick,
        onLongClick = onLogClick,
        interactionSource = interactionSource,
        colors = myCardColor(),
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
                Text(
                    text = connectionCardInfo.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,

                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 连接详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ConnectionInfoItem(
                        label = "服务器",
                        value = connectionCardInfo.address
                    )

                    ConnectionInfoItem(
                        label = "共享目录",
                        value = connectionCardInfo.shareName
                    )

                    if (connectionCardInfo.username.isNotEmpty()) {
                        ConnectionInfoItem(
                            label = "用户名",
                            value = connectionCardInfo.username
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

/**
 * // 操作面板（右侧弹出）
 */
@Composable
fun ConOpPanel(
    modifier: Modifier,
    isOPanelShow: Boolean = false,
    panelFocusRequester: FocusRequester,
    onClickForDel: () -> Unit,
    onClickForCancel: () -> Unit
) {
    AnimatedVisibility(
        visible = isOPanelShow,
        modifier = modifier,

        ) {

        Column(
            modifier = Modifier
                .background(
                    Color(0xFF2D2D2D),
                    shape = RoundedCornerShape(12.dp)
                )
                .width(260.dp)
                .focusRequester(panelFocusRequester)
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
                    onClick = onClickForDel
//                        smbListViewModel.deleteConnection(selectedId)
//                        smbListViewModel.closeOPanel()

                )

                OperationListItem(
                    text = "编辑信息",
                    textColor = Color.White,
                    onClick = { /* TODO: 编辑逻辑 */ }
                )

                OperationListItem(
                    text = "取消",
                    textColor = Color(0xFFB0B0B0),
                    onClick = onClickForCancel
                )
            }
        }
    }
}


data class ConnectionCardInfo(
    val name: String,
    val address: String,
    val shareName: String,
    val username: String = "无"
)


