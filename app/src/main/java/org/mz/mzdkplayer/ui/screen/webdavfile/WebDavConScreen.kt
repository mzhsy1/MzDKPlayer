// 文件路径: org/mz/mzdkplayer/ui/screen/webdavfile/WebDavConScreen.kt
package org.mz.mzdkplayer.ui.screen.webdavfile

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
import org.mz.mzdkplayer.logic.model.WebDavConnection
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkConnectionStatus
import org.mz.mzdkplayer.ui.screen.vm.WebDavConViewModel
import org.mz.mzdkplayer.ui.screen.vm.WebDavConnectionStatus
import org.mz.mzdkplayer.ui.screen.vm.WebDavListViewModel
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyIconButton

import org.mz.mzdkplayer.ui.theme.TvTextField

import java.util.UUID

/**
 * WebDAV 连接界面
 */
@Composable

fun WebDavConScreen(
    // 允许外部传入 ViewModel，便于测试和依赖注入

) {
    val webDavConViewModel: WebDavConViewModel = viewModel()
    val webDavListViewModel: WebDavListViewModel = viewModel()
    // UI 状态由 ViewModel 管理
    val connectionStatus by webDavConViewModel.connectionStatus.collectAsState()
    val fileList by webDavConViewModel.fileList.collectAsState()
    val currentPath by webDavConViewModel.currentPath.collectAsState()

    // 用户输入状态
    var baseUrl by remember { mutableStateOf("https://192.168.1.4:5006") }
    var username by remember { mutableStateOf("wang") }
    var password by remember { mutableStateOf("Wa541888") }
    var aliasName by remember { mutableStateOf("My WebDAV Server") }

    // 用于控制键盘
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：连接配置和控制面板
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 连接状态显示 - 修复：使用 connectionStatus.toString() 或自定义逻辑
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 修改这里，调用 toString() 或直接使用 when 判断
                Text(
                    text = "WebDAV 状态: ${connectionStatus.toString()}", // 使用重写的 toString()
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(100.dp, 400.dp),
                    maxLines = 1
                )
                // 状态指示灯
                Icon(
                    painter = painterResource(R.drawable.baseline_circle_24),
                    contentDescription = null,
                    tint = when (connectionStatus) {
                        is WebDavConnectionStatus.Connected -> Color.Green
                        is WebDavConnectionStatus.Connecting -> Color.Yellow
                        is WebDavConnectionStatus.Error -> Color.Red
                        else -> Color.Gray // Disconnected
                    }
                )
            }

            // 输入字段
            TvTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "Base URL (e.g., https://192.168.1.4:5006)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "Username",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                colors = myTTFColor(),
                placeholder = "Password",
                textStyle = TextStyle(color = Color.White),
                // 可以考虑设置为密码输入类型
                // visualTransformation = PasswordVisualTransformation()
            )

            TvTextField(
                value = aliasName,
                onValueChange = { aliasName = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "Connection Name (Alias)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 操作按钮
            MyIconButton(
                text = "测试连接",
                imageVector = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(0.5f),
                
                onClick = {
                    keyboardController?.hide() // 隐藏键盘
                    webDavConViewModel.connectToWebDav(baseUrl, username, password)
                },
            )

            MyIconButton(
                text = "保存连接",
                imageVector = Icons.Outlined.Star,
                modifier = Modifier.fillMaxWidth(0.5f),
                // 只有在已连接时才允许保存
                onClick = {
                    keyboardController?.hide()
                    if (baseUrl.isBlank()) {
                        Toast.makeText(context, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                        return@MyIconButton
                    }

                    if (connectionStatus !is WebDavConnectionStatus.Connected){
                        Toast.makeText(context, "请先连接成功后再保存", Toast.LENGTH_SHORT).show()
                        return@MyIconButton
                    }
                    val newConnection = WebDavConnection( // 使用您定义的数据类
                        id = UUID.randomUUID().toString(),
                        name = aliasName.ifBlank { "未命名WebDav连接" },
                        baseUrl = baseUrl,
                        username = username,
                        password = password // 再次提醒：明文存储不安全
                    )
                    if (webDavListViewModel.addConnection(newConnection)) {
                        Toast.makeText(context, "连接已保存", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "保存失败，连接可能已存在", Toast.LENGTH_SHORT)
                            .show()
                    }
                    Log.d("WebDavConScreen", "保存连接: $aliasName")
                },
            )

            MyIconButton(
                text = "断开连接",
                imageVector = Icons.Outlined.Delete,
                modifier = Modifier.fillMaxWidth(0.5f),
                // 只有在已连接或连接出错时才允许断开
//                enabled = connectionStatus is WebDavConnectionStatus.Connected ||
//                        connectionStatus is WebDavConnectionStatus.Error ||
//                        connectionStatus is WebDavConnectionStatus.Connecting,
                onClick = {
                    keyboardController?.hide()
                    webDavConViewModel.disconnectWebDav()
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
        if (connectionStatus is WebDavConnectionStatus.Connected && fileList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f) // 占据剩余空间
            ) {
                // 文件/文件夹列表项
                itemsIndexed(fileList) { index, resource ->
                    val resourceName = resource.name ?: "Unknown"
                    val isDirectory = resource.isDirectory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = webDavConViewModel.isConnected()) { // 只有连接时才能点击
                                if (isDirectory) {
                                    // 点击文件夹：进入子目录
                                    val newPath = if (currentPath.isEmpty()) {
                                        resourceName
                                    } else {
                                        "${currentPath}/$resourceName"
                                    }
                                    Log.d("newPath",newPath)
                                    webDavConViewModel.listFiles(newPath)
                                } else {
                                    // 点击文件：可以触发下载或其他操作
                                    Toast.makeText(context, "点击了文件: $resourceName", Toast.LENGTH_SHORT).show()
                                    // TODO: 实现文件下载逻辑
                                    // val fullUrl = webDavConViewModel.getResourceFullUrl(resourceName)
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
                        // 大小 (可选)
                        resource.contentLength?.let {
                            Text(
                                text = "${it / 1024} KB", // 简单转换为 KB
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else if (connectionStatus is WebDavConnectionStatus.Connecting) {
            // 可选：显示连接中提示
            Text(
                text = "正在连接...",
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(16.dp),
                color = Color.Gray
            )
        } else if (connectionStatus is WebDavConnectionStatus.Error) {
            // 可选：显示错误信息
            Text(
                text = (connectionStatus as WebDavConnectionStatus.Error).message,
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



