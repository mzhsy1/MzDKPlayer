// File: NFSConListScreen.kt

package org.mz.mzdkplayer.ui.screen.nfs // 请根据你的实际包名修改

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Card
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
// --- 导入 NFS 相关的模型和 ViewModel ---
import org.mz.mzdkplayer.logic.model.NFSConnection // 使用 NFS 数据模型
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.ui.screen.vm.NFSListViewModel // 使用 NFS ViewModel
// --- ---
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myCardScaleStyle
import org.mz.mzdkplayer.ui.style.myListItemCoverColor
import java.net.URLEncoder

/**
 * NFS连接列表屏幕
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun NFSConListScreen(mainNavController: NavHostController) {
    // 使用 NFSListViewModel
    val nfsListViewModel: NFSListViewModel = viewModel()
    val connections by nfsListViewModel.connections.collectAsState()
    val isOPanelShow by nfsListViewModel.isOPanelShow.collectAsState()
    // isLongPressInProgress 可能未在此处直接使用，但保留以匹配原始逻辑结构
    val isLongPressInProgress by nfsListViewModel.isLongPressInProgress.collectAsState()

    LaunchedEffect(isOPanelShow) {
        Log.d("NFSList", "isOPanelShow changed: $isOPanelShow")
    }

    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val selectedIndex by nfsListViewModel.selectedIndex.collectAsState()
    val selectedId by nfsListViewModel.selectedId.collectAsState()
    val listState = rememberLazyListState()

    // 焦点管理：面板显示/隐藏时切换焦点
    LaunchedEffect(isOPanelShow) {
        if (isOPanelShow) {
            panelFocusRequester.requestFocus()
        } else {
            listFocusRequester.requestFocus()
        }
    }

    // 当操作面板显示时，按下返回键隐藏面板
    BackHandler(enabled = isOPanelShow) {
        nfsListViewModel.closeOPanel()
    }

    // 面板关闭时，如果之前有选中项，则滚动到该项并请求焦点
    LaunchedEffect(isOPanelShow) {
        if (!isOPanelShow && selectedIndex != -1) {
            listState.animateScrollToItem(selectedIndex)
            // ConnectionCard 内部的 LaunchedEffect 会处理焦点请求
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding()
        ) {
            // 标题
            FCLMainTitle(mainNavController = mainNavController, "NFS文件共享", "NFSConScreen")

            if (connections.isEmpty()) {
                Text(
                    "没有NFS连接",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(10.dp),
                )
            } else {
                // 连接卡片列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.focusRequester(listFocusRequester)
                ) {
                    itemsIndexed(connections) { index, conn ->
                        NFSConnectionCard(
                            index = index,
                            connection = conn,
                            onClick = {
                                // 构建用于导航到 NFS 文件列表的参数
                                // 注意：在实际应用中，直接传递密码/选项可能不安全。
                                // 更好的做法是在 ViewModel 或 Repository 中处理认证，
                                // 或者使用更安全的令牌机制。
                                // 这里为了简化导航示例，暂时采用此方式。
                                try {
                                    // 对参数进行 URL 编码以处理特殊字符
                                    val encodedIp = URLEncoder.encode(conn.serverAddress, "UTF-8")
                                    // NFS 通常不需要用户名，但可能需要其他选项，如 mount options
                                    // val encodedUsername = URLEncoder.encode(conn.username, "UTF-8") // 移除用户名
                                    val encodedSharePath = URLEncoder.encode(conn.shareName, "UTF-8") // 使用 sharePath 而非 shareName

                                    Log.d(
                                        "NFSList", "Navigating to NFSFileListScreen with " +
                                                "IP: $encodedIp, SharePath: $encodedSharePath"
                                    )
                                    // 导航到 NFS 文件列表屏幕，传递编码后的参数
                                    // 注意：URL 路径和参数需要根据你的 NFSFileListScreen 实现调整
                                    mainNavController.navigate(
                                        "NFSFileListScreen/$encodedIp/$encodedSharePath/${URLEncoder.encode("/","UTF-8")}"
                                        // 如果需要传递端口或其他选项，可以在这里添加
                                        // "NFSFileListScreen/$encodedIp/${conn.port}/$encodedSharePath?options=${conn.options}"
                                    )
                                } catch (e: Exception) {
                                    Log.e("NFSList", "Error encoding navigation parameters: ${e.message}")
                                    // 可以添加错误提示 UI
                                }
                            },
                            onLogClick = {
                                // 触发长按逻辑（尽管这里用的是 onClick，但可能是模拟长按或不同交互）
                                nfsListViewModel.setIsLongPressInProgress(true)
                                nfsListViewModel.openOPlane()
                                nfsListViewModel.setSelectedIndex(index)
                                nfsListViewModel.setSelectedId(conn.id)
                                Log.d("NFSList", "Operation panel opened for index: $index, id: ${conn.id}")
                                nfsListViewModel.setIsLongPressInProgress(false)
                            },
                            onDelete = { /* 删除逻辑通常在 ViewModel 或操作面板中处理 */ },
                            viewModel = nfsListViewModel,
                            isOPanelShow = isOPanelShow
                        )
                    }
                }
            }
        }

        // 半透明背景遮罩层，当操作面板显示时出现
        if (isOPanelShow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black.copy(alpha = 0.35f))
                    .clickable(enabled = false) {} // 拦截背景点击
            )
        }

        // 操作面板 (侧滑菜单)
        AnimatedVisibility(
            visible = isOPanelShow,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 30.dp)
                .focusRequester(panelFocusRequester) // 管理面板焦点

        ) {

            LazyColumn(
                modifier = Modifier
                    .background(
                        Color(40, 37, 37, 255), // 深灰色背景
                        shape = RoundedCornerShape(5.dp)
                    )
                    .size(width = 260.dp, height = 180.dp) // 设置固定大小
                    .focusRequester(panelFocusRequester)
                    .focusable(), // 允许面板获取焦点
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically), // 子项间距
            ) {

                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    ListItem(
                        modifier = Modifier
                            .width(230.dp),
                        selected = false,
                        onClick = {
                            Log.d("NFSList", "Delete button pressed: isPressed=$isPressed")
                            if (isPressed) {
                                nfsListViewModel.closeOPanel()
                                nfsListViewModel.deleteConnection(selectedId)
                                Log.d("NFSList", "Deleting connection with id: $selectedId")
                            }
                        },
                        interactionSource = interactionSource,
                        colors = myListItemCoverColor(),
                        headlineContent = {
                            Text(
                                "删除",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                }

                item {
                    ListItem(
                        modifier = Modifier
                            .width(230.dp),
                        selected = false,
                        onClick = {

                            // 例如: mainNavController.navigate("EditNFSScreen/$selectedId")
                            Log.d("NFSList", "Edit button clicked for id: $selectedId")
                            nfsListViewModel.closeOPanel() // 操作后关闭面板
                        },
                        colors = myListItemCoverColor(),
                        headlineContent = {
                            Text(
                                "编辑",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier
                            .width(230.dp),
                        selected = false,
                        onClick = {
                            Log.d("NFSList", "Return button clicked")
                            nfsListViewModel.closeOPanel() // 关闭面板
                        },
                        colors = myListItemCoverColor(),
                        headlineContent = {
                            Text(
                                "返回",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * 单个 NFS 连接卡片
 */
@Composable
fun NFSConnectionCard(
    index: Int,
    connection: NFSConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit, // 这个回调现在用于触发操作面板
    viewModel: NFSListViewModel,
    isOPanelShow: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    // 当操作面板显示/隐藏状态改变时，如果当前卡片是选中的，则请求焦点
    LaunchedEffect(isOPanelShow) {
        if (viewModel.selectedIndex.value == index && !isOPanelShow) {
            Log.d("NFSCard", "Requesting focus for selected card at index: $index")
            focusRequester.requestFocus()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .focusRequester(focusRequester),
        onClick = onClick, // 点击进入文件列表
        colors = myCardColor(),
        border = myCardBorderStyle(),
        scale = myCardScaleStyle(),
        onLongClick = onLogClick // 长按（或点击）打开操作面板
    ) {

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                connection.name?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            }
            // 根据 NFSConnection 数据模型显示信息
            // NFS 通常不需要用户名
            Text("IP: ${connection.serverAddress}")
            // Text("用户: ${connection.username}") // 移除用户名显示
            Text("共享路径: ${connection.shareName}") // 显示共享路径而非共享名
            // 密码/选项通常不显示
        }
    }
}



