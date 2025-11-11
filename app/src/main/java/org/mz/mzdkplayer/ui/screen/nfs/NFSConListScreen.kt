// File: NFSConListScreen.kt

package org.mz.mzdkplayer.ui.screen.nfs // 请根据你的实际包名修改

import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import org.mz.mzdkplayer.ui.screen.common.ConOpPanel
// --- 导入 NFS 相关的模型和 ViewModel ---
import org.mz.mzdkplayer.ui.screen.common.ConnectionCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionCardInfo
import org.mz.mzdkplayer.ui.screen.common.ConnectionListEmpty
import org.mz.mzdkplayer.ui.screen.common.ConnectionListTitle
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.ui.screen.vm.NFSListViewModel // 使用 NFS ViewModel
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (connections.isEmpty()) {
                    // 空状态
                    ConnectionListEmpty("NFS")
                } else {
                    // 连接列表标题
                    ConnectionListTitle(connections.size)
                    // 连接卡片列表
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
                                modifier = Modifier.onKeyEvent { keyEvent ->
                                    // 检查是否是菜单键 (Key.Menu)
                                    if (keyEvent.key == Key.Menu) {
                                        if (!isOPanelShow) {
                                            nfsListViewModel.openOPlane()
                                            nfsListViewModel.setSelectedIndex(index)
                                            nfsListViewModel.setSelectedId(conn.id)
                                        }
                                        true // 表示已处理
                                    } else {
                                        // 检查原生键码
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_MENU -> {
                                                if (!isOPanelShow) {
                                                    nfsListViewModel.openOPlane()
                                                    nfsListViewModel.setSelectedIndex(index)
                                                    nfsListViewModel.setSelectedId(conn.id)
                                                }
                                                true // 消费事件
                                            }

                                            else -> false
                                        }
                                    }
                                },
                                connectionCardInfo = ConnectionCardInfo(
                                    name = conn.name ?: "未知",
                                    address = conn.serverAddress ?: "未知",
                                    shareName = conn.shareName ?: "未知",
                                    username = "无",
                                ),
                                onClick = {
                                    // 构建用于导航到 NFS 文件列表的参数
                                    // 注意：在实际应用中，直接传递密码/选项可能不安全。
                                    try {
                                        // 对参数进行 URL 编码以处理特殊字符
                                        val encodedIp =
                                            URLEncoder.encode(conn.serverAddress, "UTF-8")
                                        // NFS 通常不需要用户名，但可能需要其他选项，如 mount options
                                        // val encodedUsername = URLEncoder.encode(conn.username, "UTF-8") // 移除用户名
                                        val encodedSharePath = URLEncoder.encode(
                                            conn.shareName,
                                            "UTF-8"
                                        ) // 使用 sharePath 而非 shareName

                                        Log.d(
                                            "NFSList", "Navigating to NFSFileListScreen with " +
                                                    "IP: $encodedIp, SharePath: $encodedSharePath"
                                        )
                                        // 导航到 NFS 文件列表屏幕，传递编码后的参数
                                        // 注意：URL 路径和参数需要根据你的 NFSFileListScreen 实现调整
                                        mainNavController.navigate(
                                            "NFSFileListScreen/$encodedIp/$encodedSharePath/${
                                                URLEncoder.encode(
                                                    "/",
                                                    "UTF-8"
                                                )
                                            }"
                                            // 如果需要传递端口或其他选项，可以在这里添加
                                            // "NFSFileListScreen/$encodedIp/${conn.port}/$encodedSharePath?options=${conn.options}"
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                            "NFSList",
                                            "Error encoding navigation parameters: ${e.message}"
                                        )
                                        // 可以添加错误提示 UI
                                    }
                                },
                                onDelete = { /* 删除逻辑通常在 ViewModel 或操作面板中处理 */ },
                                onLogClick = {

                                    nfsListViewModel.openOPlane()
                                    nfsListViewModel.setSelectedIndex(index)
                                    nfsListViewModel.setSelectedId(conn.id)
                                    Log.d(
                                        "NFSList",
                                        "Operation panel opened for index: $index, id: ${conn.id}"
                                    )
                                },
                                isSelected = nfsListViewModel.selectedIndex.value == index && !isOPanelShow,
                                isOPanelShow = isOPanelShow,
                                selectedIndex = nfsListViewModel.selectedIndex.value
                            )
                        }
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

        // 操作面板（右侧弹出）
        ConOpPanel(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 30.dp),
            isOPanelShow,
            panelFocusRequester,
            onClickForDel = {
                Log.d("selectedId",selectedId)
                nfsListViewModel.deleteConnection(selectedId)
                nfsListViewModel.closeOPanel()
            },
            onClickForCancel = {
                nfsListViewModel.closeOPanel()
            })
    }
}





