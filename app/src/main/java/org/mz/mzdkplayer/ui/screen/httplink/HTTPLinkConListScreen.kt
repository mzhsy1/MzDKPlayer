// File: HTTPLinkConListScreen.kt

package org.mz.mzdkplayer.ui.screen.httplink // 请根据你的实际包名修改

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
// --- 导入 HTTP 相关的模型和 ViewModel ---
import org.mz.mzdkplayer.logic.model.HTTPLinkConnection // 使用 HTTP 数据模型
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkListViewModel // 使用 HTTP ViewModel
// --- ---
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myCardScaleStyle
import org.mz.mzdkplayer.ui.style.myListItemCoverColor
import java.net.URLEncoder

/**
 * HTTP链接连接列表屏幕
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun HTTPLinkConListScreen(mainNavController: NavHostController) {
    // 使用 HTTPLinkListViewModel
    val httpLinkListViewModel: HTTPLinkListViewModel = viewModel()
    val connections by httpLinkListViewModel.connections.collectAsState()
    val isOPanelShow by httpLinkListViewModel.isOPanelShow.collectAsState()
    // isLongPressInProgress 可能未在此处直接使用，但保留以匹配原始逻辑结构
    val isLongPressInProgress by httpLinkListViewModel.isLongPressInProgress.collectAsState()

    LaunchedEffect(isOPanelShow) {
        Log.d("HTTPLinkList", "isOPanelShow changed: $isOPanelShow")
    }

    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val selectedIndex by httpLinkListViewModel.selectedIndex.collectAsState()
    val selectedId by httpLinkListViewModel.selectedId.collectAsState()
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
        httpLinkListViewModel.closeOPanel()
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
            FCLMainTitle(mainNavController = mainNavController, "NGINX文件共享", "HTTPConScreen")



            if (connections.isEmpty()) {
                Text(
                    "没有HTTP链接",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(10.dp),
                )
            } else {
                // 链接卡片列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.focusRequester(listFocusRequester)
                ) {
                    itemsIndexed(connections) { index, conn ->
                        HTTPLinkConnectionCard(
                            index = index,
                            connection = conn,
                            onClick = {
                                // 构建用于导航到 HTTP 文件列表的参数
                                try {
                                    // 对参数进行 URL 编码以处理特殊字符
                                    val encodedServerAddress = URLEncoder.encode(conn.serverAddress, "UTF-8")
                                    val encodedShareName = URLEncoder.encode(conn.shareName, "UTF-8")

                                    Log.d(
                                        "HTTPLinkList", "Navigating to HTTPLinkFileListScreen with " +
                                                "ServerAddress: $encodedServerAddress, ShareName: $encodedShareName"
                                    )
                                    // 导航到 HTTP 文件列表屏幕，传递编码后的参数
                                    mainNavController.navigate(
                                        "HTTPLinkFileListScreen/$encodedServerAddress$encodedShareName"
                                    )
                                } catch (e: Exception) {
                                    Log.e("HTTPLinkList", "Error encoding navigation parameters: ${e.message}")
                                    // 可以添加错误提示 UI
                                }
                            },
                            onLogClick = {
                                // 触发长按逻辑（尽管这里用的是 onClick，但可能是模拟长按或不同交互）
                                httpLinkListViewModel.setIsLongPressInProgress(true)
                                httpLinkListViewModel.openOPlane()
                                httpLinkListViewModel.setSelectedIndex(index)
                                httpLinkListViewModel.setSelectedId(conn.id)
                                Log.d("HTTPLinkList", "Operation panel opened for index: $index, id: ${conn.id}")
                                httpLinkListViewModel.setIsLongPressInProgress(false)
                            },
                            onDelete = { /* 删除逻辑通常在 ViewModel 或操作面板中处理 */ },
                            viewModel = httpLinkListViewModel,
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
                            Log.d("HTTPLinkList", "Delete button pressed: isPressed=$isPressed")
                            if (isPressed) {
                                httpLinkListViewModel.closeOPanel()
                                httpLinkListViewModel.deleteConnection(selectedId)
                                Log.d("HTTPLinkList", "Deleting connection with id: $selectedId")
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

                            // 例如: mainNavController.navigate("EditHTTPLinkScreen/$selectedId")
                            Log.d("HTTPLinkList", "Edit button clicked for id: $selectedId")
                            httpLinkListViewModel.closeOPanel() // 操作后关闭面板
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
                            Log.d("HTTPLinkList", "Return button clicked")
                            httpLinkListViewModel.closeOPanel() // 关闭面板
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
 * 单个 HTTP 链接连接卡片
 */
@Composable
fun HTTPLinkConnectionCard(
    index: Int,
    connection: HTTPLinkConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit, // 这个回调现在用于触发操作面板
    viewModel: HTTPLinkListViewModel,
    isOPanelShow: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    // 当操作面板显示/隐藏状态改变时，如果当前卡片是选中的，则请求焦点
    LaunchedEffect(isOPanelShow) {
        if (viewModel.selectedIndex.value == index && !isOPanelShow) {
            Log.d("HTTPLinkCard", "Requesting focus for selected card at index: $index")
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
            // 根据 HTTPLinkConnection 数据模型显示信息
            Text("服务器地址: ${connection.serverAddress}")
            Text("挂载目录: ${connection.shareName}")
        }
    }
}




