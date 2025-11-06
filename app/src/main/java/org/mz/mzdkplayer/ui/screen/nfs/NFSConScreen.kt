package org.mz.mzdkplayer.ui.screen.nfs

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import org.mz.mzdkplayer.logic.model.NFSConnection // 引入 NFS 数据模型
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.vm.NFSConViewModel


import org.mz.mzdkplayer.ui.screen.vm.NFSListViewModel // 假设你也有一个管理 NFS 连接列表的 ViewModel
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.TvTextField
import java.util.Locale
import java.util.UUID

/**
 * NFS 连接与文件浏览界面
 */
@Composable
fun NFSConScreen(
    // 可以在这里添加导航控制器等参数，如果需要的话
) {
    // 使用 NFS 的 ViewModel
    val nfsConViewModel: NFSConViewModel = viewModel()
    // 假设你也有一个管理连接列表的 ViewModel
    val nfsListViewModel: NFSListViewModel = viewModel() // 如果不需要保存功能，可以移除

    // UI 状态由 ViewModel 管理
    val connectionStatus by nfsConViewModel.connectionStatus.collectAsState()
    val fileList by nfsConViewModel.fileList.collectAsState()
    val currentPath by nfsConViewModel.currentPath.collectAsState()

    // 用户输入状态 - NFS 需要服务器地址和导出路径
    var serverAddress by remember { mutableStateOf("192.168.1.4") } // NFS 服务器地址
    var shareName by remember { mutableStateOf("/fs/1000/nfs") } // NFS 导出路径
    var aliasName by remember { mutableStateOf("My NFS Server") } // 连接别名

    // 用于控制键盘
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：连接配置和控制面板
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
                .fillMaxWidth(0.5f), // 占据左半边
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 连接状态显示
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NFS 状态: $connectionStatus",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(100.dp, 400.dp),
                    maxLines = 1
                )
                // 状态指示灯
                Icon(
                    painter = painterResource(R.drawable.baseline_circle_24), // 确保有此图标资源
                    contentDescription = null,
                    tint = when (connectionStatus) {
                        is FileConnectionStatus.Connected -> Color.Green
                        is FileConnectionStatus.Connecting -> Color.Yellow
                        is FileConnectionStatus.Error -> Color.Red
                        is FileConnectionStatus.LoadingFile -> Color.Yellow
                        is FileConnectionStatus.FilesLoaded -> Color.Cyan
                        else -> Color.Gray // Disconnected
                    }
                )
            }

            // 输入字段 - NFS 服务器地址
            TvTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "NFS Server Address (e.g., 192.168.1.4)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 输入字段 - NFS 导出路径
            TvTextField(
                value = shareName,
                onValueChange = { shareName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Export Path (e.g., /fs/1000/nfs)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 输入字段 - 连接别名
            TvTextField(
                value = aliasName,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { aliasName = it },
                placeholder = "Connection Name (Alias)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween, // 让两个按钮之间有间距
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 操作按钮 - 测试连接
                MyIconButton(
                    text = "测试连接",
                    imageVector = Icons.Outlined.Check,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp), // 平分宽度并加右边距
                    enabled = connectionStatus != FileConnectionStatus.Connecting, // 连接中时禁用
                    onClick = {
                        keyboardController?.hide() // 隐藏键盘
                        if (!Tools.validateConnectionParams(context, serverAddress, shareName)) {
                            return@MyIconButton
                        }
                        // 创建临时连接对象用于测试
                        val tempConnection = NFSConnection(
                            id = UUID.randomUUID().toString(), // 临时 ID
                            name = aliasName,
                            serverAddress = serverAddress,
                            shareName

                        )
                        nfsConViewModel.connectToNFS(tempConnection, true)
                        // nfsConViewModel.listFiles("/")
                    },
                )

                // 操作按钮 - 保存连接 (假设你有 NfsListViewModel)
                MyIconButton(
                    text = "保存连接",
                    imageVector = Icons.Outlined.Star,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp), // 平分宽度并加左边距
                    onClick = {
                        keyboardController?.hide()
                        if (!Tools.validateConnectionParams(context, serverAddress, shareName)) {
                            return@MyIconButton
                        }
                        if (!nfsConViewModel.isConnected()) {
                            Toast.makeText(context, "请先连接成功后再保存", Toast.LENGTH_SHORT)
                                .show()
                            return@MyIconButton
                        }
                        // 创建 NfsConnection 数据对象
                        val newConnection = NFSConnection(
                            id = UUID.randomUUID().toString(),
                            name = aliasName.ifBlank { "未命名NFS连接" },
                            serverAddress = serverAddress,
                            shareName
                        )
                        // 假设 NfsListViewModel 有 addConnection 方法
                        if (nfsListViewModel.addConnection(newConnection)) {
                            Toast.makeText(context, "NFS 连接已保存", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "保存失败，连接可能已存在",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.d("NfsConScreen", "保存连接: $aliasName")
                    },
                )
            }

            // 断开连接按钮
            MyIconButton(
                text = "断开连接",
                imageVector = Icons.Outlined.Delete,
                modifier = Modifier.fillMaxWidth(),
                // 只有在已连接或连接出错时才允许断开
                onClick = {
                    keyboardController?.hide()
                    nfsConViewModel.disconnectNfs()
                },
            )

            // 显示当前路径 (可选)
            Text(
                text = "当前路径: $currentPath",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 右侧：文件列表
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth() // 剩余的右半边
                .weight(1f) // 占据剩余空间
        ) {
            when (connectionStatus) {
                is FileConnectionStatus.FilesLoaded -> {
                    if (fileList.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 文件/文件夹列表项
                            itemsIndexed(fileList) { index, nfsFile ->
                                val resourceName = nfsFile.name ?: "Unknown"
                                // NfsFile 使用 isDirectory 属性判断
                                val isDirectory = nfsFile.isDirectory

                                // 过滤掉 "." 和 ".." 目录项 (如果 NFS 服务器返回了它们)
                                if (resourceName != "." && resourceName != "..") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = nfsConViewModel.isConnected()) { // 只有连接时才能点击
                                                if (isDirectory) {
                                                    // 点击文件夹：进入子目录
                                                    nfsConViewModel.navigateToSubdirectory(
                                                        resourceName
                                                    )
                                                    Log.d("NfsConScreen", "进入目录: $resourceName")
                                                } else {
                                                    // 点击文件：可以触发播放或其他操作
                                                    Toast.makeText(
                                                        context,
                                                        "点击了文件: $resourceName",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    // 可以使用 nfsFile 的路径等信息
                                                }
                                            }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 图标 (简单区分文件夹和文件)
                                        Icon(
                                            painter = painterResource(
                                                if (isDirectory) R.drawable.localfile else R.drawable.baseline_insert_drive_file_24 // 替换为您的图标资源
                                            ),
                                            contentDescription = if (isDirectory) "Folder" else "File",
                                            tint = if (isDirectory) Color.White else Color.White
                                        )
                                        // 名称
                                        Text(
                                            text = resourceName,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        // 大小 (可选) - NfsFile 使用 getSize()
                                        val size = nfsFile.lengthEx()

                                        if (size >= 0) { // getSize() 返回 -1 表示大小未知
                                            val sizeText = when {
                                                size >= 1024 * 1024 * 1024 -> {
                                                    // GB
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f GB",
                                                        size.toDouble() / (1024 * 1024 * 1024)
                                                    )
                                                }

                                                size >= 1024 * 1024 -> {
                                                    // MB
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f MB",
                                                        size.toDouble() / (1024 * 1024)
                                                    )
                                                }

                                                size >= 1024 -> {
                                                    // KB
                                                    String.format(
                                                        Locale.US,
                                                        "%.1f KB",
                                                        size.toDouble() / 1024
                                                    )
                                                }

                                                else -> {
                                                    // Bytes
                                                    "$size B"
                                                }
                                            }
                                            Text(
                                                text = sizeText,
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Connected 但列表为空
                        Text(
                            text = "目录为空",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            color = Color.Gray
                        )
                    }
                }

                is FileConnectionStatus.Error -> {
                    // 显示错误信息
                    Text(
                        text = (connectionStatus as FileConnectionStatus.Error).message,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        color = Color.Red
                    )
                }

                else -> {
                    // 显示连接中提示
                    Text(
                        text = "正在准备获取文件...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        color = Color.Gray
                    )
                }

            }
        }
    }
}

// --- 预览 (如果需要) ---



