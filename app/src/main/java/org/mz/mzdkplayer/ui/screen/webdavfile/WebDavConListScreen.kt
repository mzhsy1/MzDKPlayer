package org.mz.mzdkplayer.ui.screen.webdavfile

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
import org.mz.mzdkplayer.logic.model.WebDavConnection // 使用新的数据模型
import org.mz.mzdkplayer.ui.screen.vm.WebDavListViewModel // 使用新的 ViewModel
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myCardScaleStyle
import org.mz.mzdkplayer.ui.style.myListItemCoverColor
import java.net.URLEncoder

/**
 * WebDav连接列表屏幕
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun WebDavListScreen(mainNavController: NavHostController) {
    // 使用 WebDavListViewModel
    val webDavListViewModel: WebDavListViewModel = viewModel()
    val connections by webDavListViewModel.connections.collectAsState()
    val isOPanelShow by webDavListViewModel.isOPanelShow.collectAsState()
    // isLongPressInProgress 可能未在此处直接使用，但保留以匹配原始逻辑结构
    val isLongPressInProgress by webDavListViewModel.isLongPressInProgress.collectAsState()

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
                .padding(20.dp)
        ) {
            // 添加新连接按钮
            MyIconButton(
                "添加新WebDav链接",
                Icons.Outlined.Add,
                Modifier.padding(10.dp),
                onClick = { mainNavController.navigate("WebDavConScreen") } // 导航到添加连接屏幕
            )

            if (connections.isEmpty()) {
                Text(
                    "没有WebDav连接",
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
                        WebDavConnectionCard(
                            index = index,
                            connection = conn,
                            onClick = {
                                // 构建带认证信息的 URL 用于导航
                                // 注意：在实际应用中，直接在 URL 中暴露密码可能不安全。
                                // 更好的做法是在 ViewModel 或 Repository 中处理认证。
                                // 这里为了简化导航示例，暂时采用此方式。
                                val authPart = if (conn.username.isNotBlank() && conn.password.isNotBlank()) {
                                    "${conn.username}:${conn.password}@"
                                } else {
                                    ""
                                }
                                // 假设 baseUrl 已经是完整的路径 (e.g., https://example.com/webdav/)
                                // 如果 baseUrl 不包含协议，需要预先添加
                                val fullUrl = if (conn.baseUrl.startsWith("http")) {
                                    conn.baseUrl
                                } else {
                                    "http://$authPart${conn.baseUrl}" // 或 https，根据实际情况
                                }

                                try {
                                    val encodedUrl = URLEncoder.encode(fullUrl, "UTF-8")
                                    Log.d("WebDavList", "Navigating to WebDavFileListScreen with URL: $encodedUrl")
                                    mainNavController.navigate("WebDavFileListScreen/$encodedUrl/${conn.username}/${conn.password}")
                                } catch (e: Exception) {
                                    Log.e("WebDavList", "Error encoding URL: ${e.message}")
                                    // 可以添加错误提示 UI
                                }
                            },
                            onLogClick = {
                                // 触发长按逻辑（尽管这里用的是 onClick，但可能是模拟长按或不同交互）
                                webDavListViewModel.setIsLongPressInProgress(true)
                                webDavListViewModel.openOPlane()
                                webDavListViewModel.setSelectedIndex(index)
                                webDavListViewModel.setSelectedId(conn.id)
                                Log.d("WebDavList", "Operation panel opened for index: $index, id: ${conn.id}")
                                webDavListViewModel.setIsLongPressInProgress(false)
                            },
                            onDelete = { /* 删除逻辑通常在 ViewModel 或操作面板中处理 */ },
                            viewModel = webDavListViewModel,
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
                            Log.d("WebDavList", "Delete button pressed: isPressed=$isPressed")
                            if (isPressed) {
                                webDavListViewModel.closeOPanel()
                                webDavListViewModel.deleteConnection(selectedId)
                                Log.d("WebDavList", "Deleting connection with id: $selectedId")
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
                            // TODO: 实现编辑功能或导航到编辑屏幕
                            // 例如: mainNavController.navigate("EditWebDavScreen/$selectedId")
                            Log.d("WebDavList", "Edit button clicked for id: $selectedId")
                            webDavListViewModel.closeOPanel() // 操作后关闭面板
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
                            Log.d("WebDavList", "Return button clicked")
                            webDavListViewModel.closeOPanel() // 关闭面板
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
 * 单个 WebDav 连接卡片
 */
@Composable
fun WebDavConnectionCard(
    index: Int,
    connection: WebDavConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit, // 这个回调现在用于触发操作面板
    viewModel: WebDavListViewModel,
    isOPanelShow: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    // 当操作面板显示/隐藏状态改变时，如果当前卡片是选中的，则请求焦点
    LaunchedEffect(isOPanelShow) {
        if (viewModel.selectedIndex.value == index && !isOPanelShow) {
            Log.d("WebDavCard", "Requesting focus for selected card at index: $index")
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
                Text(connection.name, style = MaterialTheme.typography.titleMedium)
            }
            // 根据 WebDavConnection 数据模型显示信息
            Text("URL: ${connection.baseUrl}")
            Text("用户: ${connection.username}")
            // 密码通常不显示
        }
    }
}