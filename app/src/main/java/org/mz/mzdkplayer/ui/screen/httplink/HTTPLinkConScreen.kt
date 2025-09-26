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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.HTTPLinkConnection // 使用提供的数据模型
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkConViewModel
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkConnectionStatus
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkListViewModel // 假设你也有一个管理 HTTPLink 连接列表的 ViewModel
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.TvTextField
import java.util.Locale
import java.util.UUID

/**
 * HTTP Link 连接与文件浏览界面
 */
@Composable
fun HTTPLinkConScreen(
    // 可以在这里添加导航控制器等参数，如果需要的话
) {
    // 使用 HTTPLink 的 ViewModel
    val httpLinkConViewModel: HTTPLinkConViewModel = viewModel()
    // 假设你也有一个管理连接列表的 ViewModel
    val httpLinkListViewModel: HTTPLinkListViewModel = viewModel() // 如果不需要保存功能，可以移除

    // UI 状态由 ViewModel 管理
    val connectionStatus by httpLinkConViewModel.connectionStatus.collectAsState()
    val fileList by httpLinkConViewModel.fileList.collectAsState()
    val currentPath by httpLinkConViewModel.currentPath.collectAsState()

    // 用户输入状态 - HTTPLink 需要服务器地址和共享名称
    var serverAddress by remember { mutableStateOf("http://192.168.1.4:81") } // HTTP 服务器地址 (例如 http://192.168.1.4:81)
    var shareName by remember { mutableStateOf("/movies") } // HTTPLink 共享路径 (例如 /movies)
    var aliasName by remember { mutableStateOf("My HTTP Link Server") } // 连接别名

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
                    text = "HTTP Link 状态: ${connectionStatus.toString()}",
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
                        is HTTPLinkConnectionStatus.Connected -> Color.Green
                        is HTTPLinkConnectionStatus.Connecting -> Color.Yellow
                        is HTTPLinkConnectionStatus.Error -> Color.Red
                        else -> Color.Gray // Disconnected
                    }
                )
            }

            // 输入字段 - HTTPLink 服务器地址
            TvTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Server Address (e.g., http://192.168.1.4:81)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 输入字段 - HTTPLink 共享路径
            TvTextField(
                value = shareName,
                onValueChange = { shareName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Share Path (e.g., /movies)",
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
                // 操作按钮 - 连接
                MyIconButton(
                    text = "连接",
                    imageVector = Icons.Outlined.Check,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp), // 平分宽度并加右边距
                    enabled = connectionStatus != HTTPLinkConnectionStatus.Connecting, // 连接中时禁用
                    onClick = {
                        keyboardController?.hide() // 隐藏键盘
                        // 构建完整的 URL，确保以 / 结尾
                        val fullUrl = if (shareName.startsWith("/")) {
                            "$serverAddress$shareName"
                        } else {
                            "$serverAddress/$shareName"
                        }
                        // 确保最终 URL 以 / 结尾，以便访问目录
                        val normalizedUrl = if (!fullUrl.endsWith("/")) {
                            "$fullUrl/"
                        } else {
                            fullUrl
                        }
                        Log.d("HTTPLinkConScreen", "构建的完整 URL: $normalizedUrl")
                        // 创建临时连接对象用于连接
                        val tempConnection = HTTPLinkConnection(
                            id = UUID.randomUUID().toString(), // 临时 ID
                            name = aliasName,
                            serverAddress = serverAddress,
                            shareName = shareName
                        )
                        httpLinkConViewModel.connectToHTTPLink(normalizedUrl) // 传递确保以 / 结尾的完整 URL
                    },
                )

                // 操作按钮 - 保存连接 (假设你有 HTTPLinkListViewModel)
                MyIconButton(
                    text = "保存连接",
                    imageVector = Icons.Outlined.Star,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp), // 平分宽度并加左边距
                    // 只有在已连接时才允许保存
                    enabled = connectionStatus is HTTPLinkConnectionStatus.Connected,
                    onClick = {
                        keyboardController?.hide()
                        // 创建 HTTPLinkConnection 数据对象
                        val newConnection = HTTPLinkConnection(
                            id = UUID.randomUUID().toString(),
                            name = aliasName,
                            serverAddress = serverAddress,
                            shareName = shareName
                        )
                        // 假设 HTTPLinkListViewModel 有 addConnection 方法
                        if (httpLinkListViewModel.addConnection(newConnection)) {
                            Toast.makeText(context, "HTTP Link 连接已保存", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "保存失败，连接可能已存在",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.d("HTTPLinkConScreen", "保存连接: $aliasName")
                    },
                )
            }

            // 断开连接按钮
            MyIconButton(
                text = "断开连接",
                imageVector = Icons.Outlined.Delete,
                modifier = Modifier.fillMaxWidth(),
                // 只有在已连接或连接出错时才允许断开
                enabled = connectionStatus is HTTPLinkConnectionStatus.Connected ||
                        connectionStatus is HTTPLinkConnectionStatus.Error ||
                        connectionStatus is HTTPLinkConnectionStatus.Connecting,
                onClick = {
                    keyboardController?.hide()
                    httpLinkConViewModel.disconnectHTTPLink()
                },
            )

            // 显示当前路径 (可选)
            Text(
                text = "当前路径: ${httpLinkConViewModel.getCurrentFullUrl()}",
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
            if (connectionStatus is HTTPLinkConnectionStatus.Connected) {
                if (fileList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 文件/文件夹列表项
                        itemsIndexed(fileList) { index, resource ->
                            val resourceName = resource.name
                            val isDirectory = resource.isDirectory

                            // 过滤掉 "." 和 ".." 目录项 (如果服务器返回了它们)
                            if (resourceName != "." && resourceName != "..") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = httpLinkConViewModel.isConnected()) { // 只有连接时才能点击
                                            if (isDirectory) {
                                                // 点击文件夹：进入子目录
                                                val newSubPath = if (currentPath.isEmpty() || currentPath == "/") {
                                                    resource.path // 如果在根目录，直接使用 resource.path
                                                } else {
                                                    "$currentPath/${resource.path}" // 否则拼接当前路径
                                                }
                                                httpLinkConViewModel.listFiles(newSubPath) // 传递相对路径
                                                Log.d("HTTPLinkConScreen", "进入目录: $resourceName, path: $newSubPath")
                                            } else {
                                                // 点击文件：可以触发播放或其他操作
                                                val fileUrl = httpLinkConViewModel.getResourceFullUrl(resourceName)
                                                Toast.makeText(
                                                    context,
                                                    "点击了文件: $resourceName\nURL: $fileUrl",
                                                    Toast.LENGTH_LONG // 长一些以便显示 URL
                                                ).show()
                                                // TODO: 实现文件播放逻辑
                                                // 可以使用 `fileUrl` 或 `resource` 的其他信息
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
                                    // 大小 (可选) - HTTPLinkResource 当前不包含大小信息
                                    // 如果需要大小，需要修改 HTTPLinkResource 和解析逻辑
                                    // 例如，从 PRE 标签中的文件列表解析大小信息
                                    // 此处暂时不显示，因为解析逻辑未包含大小
                                    // Text(
                                    //     text = sizeText,
                                    //     color = Color.Gray,
                                    //     style = MaterialTheme.typography.bodySmall
                                    // )
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
            } else if (connectionStatus is HTTPLinkConnectionStatus.Connecting) {
                // 显示连接中提示
                Text(
                    text = "正在连接...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Gray
                )
            } else if (connectionStatus is HTTPLinkConnectionStatus.Error) {
                // 显示错误信息
                Text(
                    text = (connectionStatus as HTTPLinkConnectionStatus.Error).message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Red
                )
            } else {
                // Disconnected 状态
                Text(
                    text = "请先连接 HTTP Link 服务器",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Gray
                )
            }
        }
    }
}

// --- 预览 (如果需要) ---
@Preview(showBackground = true)
@Composable
fun HTTPLinkConScreenPreview() {
    // 注意：预览时 ViewModel 需要特殊处理或使用 Hilt 注入
    // 这里只是一个简单的结构预览
    HTTPLinkConScreen()
}




