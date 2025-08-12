package org.mz.mzdkplayer.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
    val focusRequester =remember { FocusRequester() }
    LaunchedEffect(isOPanelShow)
    { Log.d("isOPanelShow",isOPanelShow.toString()) }
    val panelFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }

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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp).focusRequester(listFocusRequester)) {
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
                LazyColumn (){
                    itemsIndexed(connections) { index, conn ->
                        ConnectionCard(
                            connection = conn,
                            onClick = {
                                smbListViewModel.selectConnection(conn)
                                //onNavigateToFiles(conn)
                            },
                            onDelete = { smbListViewModel.deleteConnection(conn.id) },
                            viewModel = smbListViewModel
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = isOPanelShow, modifier = Modifier.align(Alignment.Center).focusRequester(panelFocusRequester)) {
            LazyColumn(
                modifier = Modifier
                    .background(Color.Black, shape = RoundedCornerShape(5))
                    .border(2.dp, shape = RoundedCornerShape(5), color = Color.Gray)
                    .size(200.dp)
            ) {
                item {
                    ListItem(
                        true,
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
    connection: SMBConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: SMBListViewModel
) {

    Box() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            onClick = onClick,
            colors = myCardColor(),
            border = myCardBorderStyle(),
            scale = myCardScaleStyle(),
            onLongClick = {
                viewModel.openOPlane();Log.d(
                "openOPlane",
                viewModel.isOPanelShow.value.toString()
            )
            }
        ) {

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(connection.name, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "删除")
                    }
                }
                Text("IP: ${connection.ip}")
                Text("共享目录: ${connection.shareName}")
            }
        }


    }
}