package org.mz.mzdkplayer.ui.screen.smbfile

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
import org.mz.mzdkplayer.logic.model.SMBConnection
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myCardScaleStyle
import org.mz.mzdkplayer.ui.style.myListItemCoverColor
import java.net.URLEncoder

/**
 * SMB列表
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable

fun SMBListScreen(mainNavController: NavHostController) {
    val smbListViewModel: SMBListViewModel = viewModel()
    val connections by smbListViewModel.connections.collectAsState()
    val isOPanelShow by smbListViewModel.isOPanelShow.collectAsState()
    val isLongPressInProgress by smbListViewModel.isLongPressInProgress.collectAsState()
    LaunchedEffect(isOPanelShow)
    { Log.d("isOPanelShow", isOPanelShow.toString()) }
    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val selectedIndex by smbListViewModel.selectedIndex.collectAsState()
    val selectedId by smbListViewModel.selectedId.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(isOPanelShow) {
        if (isOPanelShow) {
            panelFocusRequester.requestFocus()
        } else {
            listFocusRequester.requestFocus()
        }
    }

    // 当操作面板显示时，按下返回键隐藏面板
    BackHandler(enabled = isOPanelShow) {
        smbListViewModel.closeOPanel()  // 调用 ViewModel 中的方法隐藏面板
    }
    // 面板关闭时恢复焦点
    LaunchedEffect(isOPanelShow) {
        if (!isOPanelShow && selectedIndex != -1) {
            // 确保选中项在视图中
            listState.animateScrollToItem(selectedIndex)
            // 请求焦点
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(20.dp)

        ) {
            // 添加新连接按钮
            MyIconButton(
                "添加新SMB链接",
                Icons.Outlined.Add,
                Modifier.padding(10.dp),
                onClick = { mainNavController.navigate("SMBConScreen") })
            if (connections.isEmpty()) {
                Text("没有SMB连接", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(10.dp),)
            } else {
                // 连接卡片列表
                LazyColumn(state = listState, modifier = Modifier.focusRequester(listFocusRequester)) {
                    itemsIndexed(connections) { index, conn ->
                        ConnectionCard(
                            index = index,
                            connection = conn,
                            onClick = {

                                mainNavController.navigate(
                                    "SMBFileListScreen/${URLEncoder.encode("smb://${conn.username}:${conn.password}@${conn.ip}/${conn.shareName}/","UTF-8")}")
                            },
                            onLogClick = {
                                smbListViewModel.setIsLongPressInProgress(true)  // 标记长按开始
                                smbListViewModel.openOPlane();smbListViewModel.setSelectedIndex(
                                index
                            );smbListViewModel.setSelectedId(conn.id);Log.d(
                                "openOPlane",
                                smbListViewModel.isOPanelShow.value.toString()
                            );Log.d(
                                "setSelectedIndex",
                                smbListViewModel.selectedIndex.value.toString()
                            )
                                smbListViewModel.setIsLongPressInProgress(false)
                            },
                            onDelete = { },
                            viewModel = smbListViewModel,
                            isOPanelShow = isOPanelShow
                        )
                    }
                }
            }
        }
// 在 Box 中添加一个拦截层
        if (isOPanelShow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black.copy(alpha = 0.35f))
                    .clickable(enabled = false) {} // 完全拦截所有交互
            )
        }
        AnimatedVisibility(
            visible = isOPanelShow,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 30.dp)
                .focusRequester(panelFocusRequester)// 添加焦点捕获，防止焦点流失

        ) {

            LazyColumn(
                modifier = Modifier
                    .background(
                        Color(40, 37, 37, 255),
                        shape = RoundedCornerShape(5)
                    )
                    .size(260.dp)
                    .focusRequester(panelFocusRequester)
                    .focusable(),  // 关键：允许获取焦点
                horizontalAlignment = Alignment.CenterHorizontally, // 水平居中对齐所有子项
                verticalArrangement = Arrangement.Center, // 水平居中对齐所有子项
            ) {


                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    ListItem(
                        modifier = Modifier
                            .width(230.dp)
                            .padding(top = 10.dp),  // 添加水平内边距,
                        selected = false,
                        onClick = {
                            Log.d("isPressed", isPressed.toString())
                            if (isPressed) {
                                smbListViewModel.closeOPanel()
                                smbListViewModel.deleteConnection(selectedId);Log.d(
                                    "selectedId",
                                    selectedId
                                )// 处理确认键
                            }
                        },
                        interactionSource = interactionSource, // 绑定 InteractionSource,
                        colors = myListItemCoverColor(),
                        headlineContent = {
                            Text(
                                "删除", modifier = Modifier.fillMaxWidth(),  // 文字容器充满宽度
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                }

                item {
                    ListItem(
                        modifier = Modifier
                            .width(230.dp)
                            .padding(top = 10.dp),  // 添加水平内边距,,
                        selected = false,
                        onClick = {},
                        colors = myListItemCoverColor(),

                        headlineContent = {
                            Text(
                                "编辑", modifier = Modifier.fillMaxWidth(),  // 文字容器充满宽度
                                textAlign = TextAlign.Center
                            )
                        },
                    )
                }
                item {
                    ListItem(
                        modifier = Modifier
                            .width(230.dp)
                            .padding(top = 10.dp),  // 添加水平内边距,,
                        selected = false,
                        onClick = { smbListViewModel.closeOPanel() },
                        colors = myListItemCoverColor(),
                        headlineContent = {
                            Text(
                                "返回", modifier = Modifier.fillMaxWidth(),  // 文字容器充满宽度
                                textAlign = TextAlign.Center
                            )
                        },
                    )
                }
            }
        }
    }
}


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

    // 当isOPanelShow发生改变时请求焦点
    LaunchedEffect(isOPanelShow) {
        /** 当此项被选中且OPanel关闭的请况下!isOPanelShow是当
         * isOPanelShow为true时!isOPanelShow为false, focusRequester.requestFocus()
         * 不会执行，不会与正在显示的OPanel抢焦点
         */
        if (viewModel.selectedIndex.value == index && !isOPanelShow) {
            Log.d("当此项被选中时请求焦点", viewModel.selectedIndex.value.toString())
            focusRequester.requestFocus()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .focusRequester(focusRequester),
        onClick = onClick ,
        colors = myCardColor(),
        border = myCardBorderStyle(),
        scale = myCardScaleStyle(),
        onLongClick = onLogClick
    ) {

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(connection.name, style = MaterialTheme.typography.titleMedium)
            }
            Text("IP: ${connection.ip}")
            Text("共享目录: ${connection.shareName}")
        }
    }


}