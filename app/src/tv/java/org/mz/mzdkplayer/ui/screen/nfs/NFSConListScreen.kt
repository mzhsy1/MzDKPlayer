// File: NFSConListScreen.kt

package org.mz.mzdkplayer.ui.screen.nfs // 请根据你的实际包名修改

import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import org.mz.mzdkplayer.tool.Tools.toBase64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.ConOpPanel
// --- 导入 NFS 相关的模型和 ViewModel ---
import org.mz.mzdkplayer.ui.screen.common.ConnectionCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionCardInfo
import org.mz.mzdkplayer.ui.screen.common.ConnectionListEmpty
import org.mz.mzdkplayer.ui.screen.common.ConnectionListTitle
import org.mz.mzdkplayer.ui.screen.common.DeleteConfirmDialog
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.viewmodel.NFSListViewModel // 使用 NFS ViewModel
import java.net.URLEncoder

/**
 * NFS连接列表屏幕
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun NFSConListScreen(mainNavController: NavHostController, nfsListViewModel: NFSListViewModel) {

    val connections by nfsListViewModel.connections.collectAsState()
    val isOPanelShow by nfsListViewModel.isOPanelShow.collectAsState()

    LaunchedEffect(isOPanelShow) {
        Log.d("NFSList", "isOPanelShow changed: $isOPanelShow")
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
            FCLMainTitle(mainNavController = mainNavController, stringResource(R.string.ui_label_nfs_file_sharing), "NFSConScreen")
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
                                    name = conn.name ?: "--",
                                    address = conn.serverAddress ?: "--",
                                    shareName = conn.shareName ?: "--",
                                    username = "--",
                                ),
                                onClick = {
                                    // 构建用于导航到 NFS 文件列表的参数
                                    // 注意：在实际应用中，直接传递密码/选项可能不安全。
                                    try {
                                        val encodedIp = (conn.serverAddress ?: "").toBase64()
                                        val encodedSharePath = (conn.shareName ?: "").toBase64()
                                        val encodedSubPath = "/".toBase64()
                                        val encodedName = (conn.name ?: "").toBase64()

                                        Log.d(
                                            "NFSList", "Navigating to NFSFileListScreen with " +
                                                    "IP: $encodedIp, SharePath: $encodedSharePath"
                                        )
                                        // 导航到 NFS 文件列表屏幕，传递编码后的参数
                                        mainNavController.navigate(
                                            "NFSFileListScreen/$encodedIp/$encodedSharePath/$encodedSubPath/$encodedName"
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
                showDeleteDialog = true
                nfsListViewModel.closeOPanel()
            },
            onClickForCancel = {
                nfsListViewModel.closeOPanel()
            })
        // 把弹窗挂载在最外层，保证它不会随着面板的消失而消失
        if (showDeleteDialog) {
            DeleteConfirmDialog(
                title = "删除连接",
                message = "确定要删除这个 NFS 连接吗？",
                onConfirm = {
                    // 点击确认后，才真正执行删除操作
                    // 这里的 selectedId 需要根据你父组件的逻辑传过来
                    Log.d("selectedId",selectedId)
                    nfsListViewModel.deleteConnection(selectedId)


                },
                onDismiss = {
                    // 关闭弹窗
                    showDeleteDialog = false
                }
            )
        }
    }
}





