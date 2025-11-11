package org.mz.mzdkplayer.ui.screen.webdavfile

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
import org.mz.mzdkplayer.ui.screen.common.ConnectionCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionCardInfo
import org.mz.mzdkplayer.ui.screen.common.ConnectionListEmpty
import org.mz.mzdkplayer.ui.screen.common.ConnectionListTitle
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.ui.screen.vm.WebDavListViewModel // 使用新的 ViewModel
import java.net.URLEncoder

/**
 * WebDav连接列表屏幕
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun WebDavConListScreen(mainNavController: NavHostController) {
    // 使用 WebDavListViewModel
    val webDavListViewModel: WebDavListViewModel = viewModel()
    val connections by webDavListViewModel.connections.collectAsState()
    val isOPanelShow by webDavListViewModel.isOPanelShow.collectAsState()
    LaunchedEffect(isOPanelShow) {
        Log.d("WebDavList", "isOPanelShow changed: $isOPanelShow")
    }
    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val selectedIndex by webDavListViewModel.selectedIndex.collectAsState()
    val selectedId by webDavListViewModel.selectedId.collectAsState()
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
        webDavListViewModel.closeOPanel()
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
            FCLMainTitle(mainNavController = mainNavController, "WebDav文件共享", "WebDavConScreen")
            // ====== 内容区域m ======
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (connections.isEmpty()) {
                    // 空状态
                    ConnectionListEmpty("WebDav")
                } else {
                    // 连接列表标题
                    ConnectionListTitle(connections.size)
                    // 连接卡片列表
                    LazyColumn(
                        state = listState,
                        modifier  = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)
                            .focusRequester(listFocusRequester),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(connections) { index, conn ->
                            ConnectionCard(
                                modifier = Modifier.onKeyEvent { keyEvent ->
                                    // 检查是否是菜单键 (Key.Menu)
                                    if (keyEvent.key == Key.Menu) {
                                        if (!isOPanelShow) {
                                            webDavListViewModel.openOPlane()
                                            webDavListViewModel.setSelectedIndex(index)
                                            webDavListViewModel.setSelectedId(conn.id)
                                        }
                                        true // 表示已处理
                                    } else {
                                        // 检查原生键码
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_MENU -> {
                                                if (!isOPanelShow) {
                                                    webDavListViewModel.openOPlane()
                                                    webDavListViewModel.setSelectedIndex(index)
                                                    webDavListViewModel.setSelectedId(conn.id)
                                                }
                                                true // 消费事件
                                            }

                                            else -> false
                                        }
                                    }
                                },
                                index = index,
                                connectionCardInfo = ConnectionCardInfo(
                                    name = conn.name ?: "未知",
                                    address = conn.baseUrl ?: "未知",
                                    shareName = "无",
                                    username = conn.username ?: "未知",
                                ),
                                onClick = {
                                    // 构建带认证信息的 URL 用于导航
                                    // 注意：在实际应用中，直接在 URL 中暴露密码可能不安全。
                                    val authPart =
                                        if (conn.username?.isNotBlank() == true && conn.password?.isNotBlank() == true) {
                                            "${conn.username}:${conn.password}@"
                                        } else {
                                            ""
                                        }
                                    // 假设 baseUrl 已经是完整的路径 (e.g., https://example.com/webdav/)
                                    // 如果 baseUrl 不包含协议，需要预先添加
                                    val fullUrl = if (conn.baseUrl?.startsWith("http") ?: true) {
                                        conn.baseUrl
                                    } else {
                                        "http://$authPart${conn.baseUrl}" // 或 https，根据实际情况
                                    }

                                    try {
                                        val encodedUrl = URLEncoder.encode(fullUrl, "UTF-8")
                                        Log.d(
                                            "WebDavList",
                                            "Navigating to WebDavFileListScreen with URL: $encodedUrl"
                                        )
                                        mainNavController.navigate("WebDavFileListScreen/$encodedUrl/${conn.username}/${conn.password}")
                                    } catch (e: Exception) {
                                        Log.e("WebDavList", "Error encoding URL: ${e.message}")

                                    }
                                },
                                onDelete = { /* 删除逻辑通常在 ViewModel 或操作面板中处理 */ },
                                onLogClick = {
                                    webDavListViewModel.openOPlane()
                                    webDavListViewModel.setSelectedIndex(index)
                                    webDavListViewModel.setSelectedId(conn.id)
                                    Log.d(
                                        "WebDavList",
                                        "Operation panel opened for index: $index, id: ${conn.id}"
                                    )
                                },
                                isSelected = webDavListViewModel.selectedIndex.value == index && !isOPanelShow,
                                isOPanelShow = isOPanelShow,
                                selectedIndex = webDavListViewModel.selectedIndex.value,
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
                webDavListViewModel.deleteConnection(selectedId)
                webDavListViewModel.closeOPanel()
            },
            onClickForCancel = {
                webDavListViewModel.closeOPanel()
            })
    }
}
