package org.mz.mzdkplayer.ui.screen.smbfile

import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent


import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController


import kotlinx.coroutines.delay
import org.mz.mzdkplayer.ui.screen.common.ConOpPanel

import org.mz.mzdkplayer.ui.screen.common.ConnectionCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionCardInfo
import org.mz.mzdkplayer.ui.screen.common.ConnectionListEmpty
import org.mz.mzdkplayer.ui.screen.common.ConnectionListTitle
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel

import java.net.URLEncoder

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SMBConListScreen(mainNavController: NavHostController) {
    val smbListViewModel: SMBListViewModel = viewModel()
    val connections by smbListViewModel.connections.collectAsState()
    val isOPanelShow by smbListViewModel.isOPanelShow.collectAsState()
    val selectedIndex by smbListViewModel.selectedIndex.collectAsState()
    val selectedId by smbListViewModel.selectedId.collectAsState()
    val listState = rememberLazyListState()
    //val context = LocalContext.current

    LaunchedEffect(isOPanelShow) {
        Log.d("isOPanelShow", isOPanelShow.toString())
    }

    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isOPanelShow) {
        if (isOPanelShow) {
            delay(350) // 200ms动画 + 100ms保险
            panelFocusRequester.requestFocus()
        } else {
            if (selectedIndex != -1) {
                listState.animateScrollToItem(selectedIndex)
            }
            // 精确等待滚动完成
            while (listState.isScrollInProgress) {
                delay(200)
            }
            panelFocusRequester.freeFocus()
            listFocusRequester.requestFocus()

        }
    }


    BackHandler(enabled = isOPanelShow) {
        smbListViewModel.closeOPanel()
        panelFocusRequester.freeFocus()
    }

//    LaunchedEffect(isOPanelShow) {
//        if (!isOPanelShow && selectedIndex != -1) {
//            listState.animateScrollToItem(selectedIndex)
//        }
//    }
    Box(modifier = Modifier
        .fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)) // 深黑背景
        )
        {
            // 标题
            FCLMainTitle(mainNavController = mainNavController, "SMB文件共享", "SMBConScreen")
            // ====== 内容区域m ======
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (connections.isEmpty()) {
                    // 空状态设计
                    ConnectionListEmpty("SMB")
                } else {
                    // 连接列表标题
                    ConnectionListTitle(connections.size)
                    // 卡片列表
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
                                            smbListViewModel.openOPlane()
                                            smbListViewModel.setSelectedIndex(index)
                                            smbListViewModel.setSelectedId(conn.id)
                                        }
                                        true // 表示已处理
                                    } else {
                                        // 检查原生键码
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_MENU -> {
                                                if (!isOPanelShow) {
                                                    smbListViewModel.openOPlane()
                                                    smbListViewModel.setSelectedIndex(index)
                                                    smbListViewModel.setSelectedId(conn.id)
                                                }
                                                true // 消费事件
                                            }
                                            else -> false
                                        }
                                    }
                                },
                                connectionCardInfo = ConnectionCardInfo(
                                    name = conn.name ?: "未知",
                                    address = conn.ip ?: "未知",
                                    shareName = conn.shareName ?: "未知",
                                    username = conn.username ?: "无",
                                ),
                                onClick = {
                                    Log.d("SMBClick", "SMBClick")
                                    mainNavController.navigate(
                                        "SMBFileListScreen/${
                                            URLEncoder.encode(
                                                "smb://${conn.username}:${conn.password}@${conn.ip}/${conn.shareName}/",
                                                "UTF-8"
                                            )
                                        }"
                                    )
                                    smbListViewModel.setSelectedIndex(index)
                                    smbListViewModel.setSelectedId(conn.id)
                                },
                                onLogClick = {
                                    Log.d("SMBClick", "SMBClickL")
                                    smbListViewModel.openOPlane()
                                    smbListViewModel.setSelectedIndex(index)
                                    smbListViewModel.setSelectedId(conn.id)
                                },
                                onDelete = { },
                                isSelected = smbListViewModel.selectedIndex.collectAsState().value == index && !isOPanelShow,
                                isOPanelShow = isOPanelShow,
                                selectedIndex = smbListViewModel.selectedIndex.value
                            )
                        }
                    }
                }
            }
        }

        // 操作面板遮罩层
        if (isOPanelShow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .focusable(enabled = false)
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
                smbListViewModel.deleteConnection(selectedId)
                smbListViewModel.closeOPanel()
            },
            onClickForCancel = {
                smbListViewModel.closeOPanel()
            })




    }
}


