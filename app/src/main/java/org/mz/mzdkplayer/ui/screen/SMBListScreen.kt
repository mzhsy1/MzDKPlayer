package org.mz.mzdkplayer.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape

import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.logic.model.SMBConnection
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myCardScaleStyle
import org.mz.mzdkplayer.ui.theme.myListItemBorder
import org.mz.mzdkplayer.ui.theme.myListItemColor

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
                .focusRequester(listFocusRequester)
        ) {
            // 添加新连接按钮
            MyIconButton(
                "添加新SMB链接",
                Icons.Outlined.Add,
                Modifier.padding(10.dp),
                onClick = { mainNavController.navigate("SMBConScreen") })
            if (connections.isEmpty()) {
                Text("no links", color = Color.White, fontSize = 20.sp)
            } else {
                // 连接卡片列表
                LazyColumn(state = listState) {
                    itemsIndexed(connections) { index, conn ->
                        ConnectionCard(
                            index = index,
                            connection = conn,
                            onClick = {
                                smbListViewModel.selectConnection(conn)

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

        AnimatedVisibility(
            visible = isOPanelShow,
            modifier = Modifier
                .align(Alignment.Center)

        ) {

            LazyColumn(
                modifier = Modifier
                    .background(Color.Black, shape = RoundedCornerShape(5))
                    .border(2.dp, shape = RoundedCornerShape(5), color = Color.Gray)
                    .size(200.dp) .focusRequester(panelFocusRequester)
                    .focusable()  // 关键：允许获取焦点
                ,
            ) {


                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                        ListItem(
                            modifier = Modifier,
                            selected = true,
                            onClick = {
                                Log.d("isPressed",isPressed.toString())
                                if (isPressed){
                                smbListViewModel.closeOPanel()
                                smbListViewModel.deleteConnection(selectedId);Log.d(
                                "selectedId",
                                selectedId
                            )// 处理确认键
                            }},
                            interactionSource = interactionSource, // 绑定 InteractionSource,
                            colors = myListItemColor(),
                            border = myListItemBorder(),
                            headlineContent = { Text("删除", color = Color.White) }
                        )
                    }

                item {
                    ListItem(
                        false,
                        onClick = {},
                        colors = myListItemColor(),
                        border = myListItemBorder(),
                        headlineContent = { Text("删除", color = Color.Black) },
                    )
                }
                item {
                    ListItem(
                        false,
                        onClick = {},
                        colors = myListItemColor(),
                        border = myListItemBorder(),
                        headlineContent = { Text("删除", color = Color.Black) },
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
        onClick = { Unit },
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