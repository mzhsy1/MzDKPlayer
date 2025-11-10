package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.SMBConnection

import org.mz.mzdkplayer.ui.screen.smbfile.ConnectionInfoItem
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import java.net.URLEncoder

/**
 * ====== 标题栏 ======
 */
@Composable
fun FCLMainTitle(mainNavController: NavHostController, titleText: String,addTargetRouter: String) {

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
                           text = titleText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "网络存储",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0B0B0) // 浅灰色
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 添加新连接按钮
                    MyIconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = { mainNavController.navigate(addTargetRouter) },
                        text = "添加连接",
                        icon = R.drawable.add24dp,
                    )

                    MyIconButton(
                        onClick = { /**TODO 转到设置帮助页面**/ },
                        text = "帮助",
                        icon = R.drawable.help24,
                    )
                }
            }
        }
    }
// 文件卡片
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectionCard(
    index: Int,
    connectionCardInfo: ConnectionCardInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit,
    isSelected: Boolean,
    isOPanelShow: Boolean
)
{
    val focusRequester = remember { FocusRequester() }

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
data class ConnectionCardInfo(
    val name:String,
    val address: String,
    val shareName:String,
    val username:String = "无"
)


