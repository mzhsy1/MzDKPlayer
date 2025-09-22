// 文件路径: org/mz/mzdkplayer/ui/screen/ftpfile/FTPConScreen.kt
// (请根据你的实际项目结构调整包名和文件路径)
package org.mz.mzdkplayer.ui.screen.ftp

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.FTPConnection // 引入 FTP 数据模型
import org.mz.mzdkplayer.ui.screen.vm.FTPConViewModel // 引入 FTP ViewModel
import org.mz.mzdkplayer.ui.screen.vm.FTPConnectionStatus // 引入 FTP 状态枚举
import org.mz.mzdkplayer.ui.screen.vm.FTPListViewModel // 引入 FTP List ViewModel
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.TvTextField
import java.util.Locale

import java.util.UUID

/**
 * FTP 连接界面
 */
@Composable
@Preview(showBackground = true)
fun FTPConScreen(
    // 允许外部传入 ViewModel，便于测试和依赖注入
) {
    // 使用 FTP 的 ViewModel
    val ftpConViewModel: FTPConViewModel = viewModel()
    val ftpListViewModel: FTPListViewModel = viewModel()

    // UI 状态由 ViewModel 管理
    val connectionStatus by ftpConViewModel.connectionStatus.collectAsState()
    val fileList by ftpConViewModel.fileList.collectAsState()
    val currentPath by ftpConViewModel.currentPath.collectAsState()

    // 用户输入状态 - 注意 FTP 需要服务器地址和端口
    var server by remember { mutableStateOf("192.168.1.4") } // 服务器地址
    var port by remember { mutableStateOf("21") } // FTP 端口，默认 21
    var username by remember { mutableStateOf("wang") }
    var password by remember { mutableStateOf("Wa541888") }
    var aliasName by remember { mutableStateOf("My FTP Server") } // 连接别名
    var shareName by remember { mutableStateOf("movies") } // FTP 共享文件夹名称

    // 用于控制键盘
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：连接配置和控制面板
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight().fillMaxWidth(0.5f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 连接状态显示
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FTP 状态: ${connectionStatus.toString()}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(100.dp, 400.dp),
                    maxLines = 2
                )
                // 状态指示灯
                Icon(
                    painter = painterResource(R.drawable.baseline_circle_24), // 确保有此图标资源
                    contentDescription = null,
                    tint = when (connectionStatus) {
                        is FTPConnectionStatus.Connected -> Color.Green
                        is FTPConnectionStatus.Connecting -> Color.Yellow
                        is FTPConnectionStatus.Error -> Color.Red
                        else -> Color.Gray // Disconnected
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(),) {
                // 输入字段 - FTP 服务器地址
                TvTextField(
                    value = server,
                    onValueChange = { server = it },
                    modifier = Modifier.weight(0.6f),
                    placeholder = "FTP Server (e.g., 192.168.1.4)",
                    colors = myTTFColor(),
                    textStyle = TextStyle(color = Color.White),
                )

                // 输入字段 - FTP 端口
                TvTextField(
                    value = port,
                    modifier = Modifier.weight(0.4f).padding(start = 8.dp),
                    onValueChange = { newValue ->
                        // 简单校验端口号为数字
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            port = newValue
                        }
                    },
                    placeholder = "Port (e.g., 21)",
                    colors = myTTFColor(),
                    textStyle = TextStyle(color = Color.White),
                )
            }

            TvTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Username",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                colors = myTTFColor(),
                placeholder = "Password",
                textStyle = TextStyle(color = Color.White),
                // 可以考虑设置为密码输入类型 (如果 TvTextField 支持)
                // visualTransformation = PasswordVisualTransformation()
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

            // 输入字段 - 初始共享文件夹名称 (可选)
            TvTextField(
                value = shareName,
                onValueChange = { shareName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Initial Share Folder (Optional)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, // 可选：让两个按钮之间有间距
                        modifier = Modifier.fillMaxWidth(),
            ) {
                // 操作按钮
                MyIconButton(
                    text = "测试连接",
                    imageVector = Icons.Outlined.Check,
                    modifier = Modifier.weight(1f) .padding(end = 8.dp), // 可选：加点右边距，避免贴太紧,// ⬅️ 平分宽度,
                    enabled = connectionStatus != FTPConnectionStatus.Connecting, // 连接中时禁用
                    onClick = {
                        keyboardController?.hide() // 隐藏键盘
                        val portInt = port.toIntOrNull() ?: 21 // 转换端口，失败则默认 21
                        ftpConViewModel.connectToFTP(server, portInt, username, password, shareName)
                    },
                )

                MyIconButton(
                    text = "保存连接",
                    imageVector = Icons.Outlined.Star,
                    modifier = Modifier.weight(1f).padding(start = 8.dp), // 可选：加点右边距，避免贴太紧 ,// ⬅️ 平分宽度,
                    // 只有在已连接时才允许保存
                   // enabled = connectionStatus is FTPConnectionStatus.Connected,
                    onClick = {
                        keyboardController?.hide()
                        val portInt = port.toIntOrNull() ?: 21 // 保存时也转换端口
                        // 创建 FTPConnection 数据对象
                        val newConnection = FTPConnection(
                            id = UUID.randomUUID().toString(),
                            name = aliasName,
                            ip = server, // 使用 ip 字段存储服务器地址
                            username = username,
                            password = password, // 再次提醒：明文存储不安全
                            shareName = shareName ,// 存储共享文件夹名称
                            port = port.toInt()
                        )
                        if (ftpListViewModel.addConnection(newConnection)) {
                            Toast.makeText(context, "FTP 连接已保存", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "保存失败，连接可能已存在", Toast.LENGTH_SHORT)
                                .show()
                        }
                        Log.d("FtpConScreen", "保存连接: $aliasName")
                    },
                )
            }
            MyIconButton(
                text = "断开连接",
                imageVector = Icons.Outlined.Delete,
                modifier = Modifier.fillMaxWidth(),
                // 只有在已连接或连接出错时才允许断开
                enabled = connectionStatus is FTPConnectionStatus.Connected ||
                        connectionStatus is FTPConnectionStatus.Error ||
                        connectionStatus is FTPConnectionStatus.Connecting,
                onClick = {
                    keyboardController?.hide()
                    ftpConViewModel.disconnectFTP()
                },
            )

            // 显示当前路径 (可选)
            Text(
                text = "当前路径: /$currentPath",
                color = Color.LightGray,

                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 右侧：文件列表
        // 只有在已连接且有文件时才显示列表
        if (connectionStatus is FTPConnectionStatus.Connected && fileList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight().fillMaxWidth(0.5f)
                    .weight(1f) // 占据剩余空间
            ) {
                // 文件/文件夹列表项
                itemsIndexed(fileList) { index, ftpFile ->
                    val resourceName = ftpFile.name ?: "Unknown"
                    // FTPFile 使用 isDirectory 方法判断
                    val isDirectory = ftpFile.isDirectory

                    // 过滤掉 "." 和 ".." 目录项 (如果 FTP 服务器返回了它们)
                    if (resourceName != "." && resourceName != "..") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = ftpConViewModel.isConnected()) { // 只有连接时才能点击
                                    if (isDirectory) {
                                        // 点击文件夹：进入子目录
                                        // FTPConViewModel 的 listFiles 方法期望相对路径 (不带开头的 /)
                                        val newPath = if (currentPath.isEmpty()) {
                                            resourceName
                                        } else {
                                            "${currentPath}/$resourceName"
                                        }
                                        Log.d("FtpConScreen", "进入目录: $newPath")
                                        ftpConViewModel.listFiles(newPath)
                                    } else {
                                        // 点击文件：可以触发下载或其他操作
                                        Toast.makeText(context, "点击了文件: $resourceName", Toast.LENGTH_SHORT).show()
                                        // TODO: 实现文件下载逻辑
                                        // 可以使用 ftpConViewModel.getResourceFullUrl(resourceName) 获取 FTP URL
                                        // val fullUrl = ftpConViewModel.getResourceFullUrl(resourceName)
                                        // viewModel.downloadFile(fullUrl, localPath)
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
                            // 大小 (可选) - FTPFile 使用 getSize()
                            val size = ftpFile.size

                            if (size >= 0) { // getSize() 返回 -1 表示大小未知
                                val sizeText = when {
                                    size >= 1024 * 1024 * 1024 -> {
                                        // GB
                                        String.format(Locale.US,"%.1f GB", size.toDouble() / (1024 * 1024 * 1024))
                                    }
                                    size >= 1024 * 1024 -> {
                                        // MB
                                        String.format(Locale.US,"%.1f MB", size.toDouble() / (1024 * 1024))
                                    }
                                    size >= 1024 -> {
                                        // KB
                                        String.format(Locale.US,"%.1f KB", size.toDouble() / 1024)
                                    }
                                    else -> {
                                        // Bytes
                                        "$size B"
                                    }
                                }
                                Text(
                                    text = sizeText, // 简单转换为 KB
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        } else if (connectionStatus is FTPConnectionStatus.Connecting) {
            // 显示连接中提示
            Text(
                text = "正在连接...",
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(16.dp),
                color = Color.Gray
            )
        } else if (connectionStatus is FTPConnectionStatus.Error) {
            // 显示错误信息
            Text(
                text = (connectionStatus as FTPConnectionStatus.Error).message,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(16.dp),
                color = Color.Red
            )
        } else {
            // Disconnected 或 Connected 但列表为空
            Text(
                text = "无文件",
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(16.dp),
                color = Color.Gray
            )
        }
    }
}



