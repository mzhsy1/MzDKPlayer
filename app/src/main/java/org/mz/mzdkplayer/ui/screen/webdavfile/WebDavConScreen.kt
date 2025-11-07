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
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import org.mz.mzdkplayer.logic.model.WebDavConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.vm.WebDavConViewModel
import org.mz.mzdkplayer.ui.screen.vm.WebDavListViewModel
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyIconButton
import org.mz.mzdkplayer.ui.theme.TvTextField
import java.util.UUID

/**
 * WebDAV 连接界面
 */
@Composable
fun WebDavConScreen() {
    val webDavConViewModel: WebDavConViewModel = viewModel()
    val webDavListViewModel: WebDavListViewModel = viewModel()

    // UI 状态由 ViewModel 管理
    val connectionStatus by webDavConViewModel.connectionStatus.collectAsState()
    val fileList by webDavConViewModel.fileList.collectAsState()
    var currentPath by remember { mutableStateOf("") }

    // 用户输入状态 - baseUrl 现在表示完整的路径
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
                .fillMaxHeight() .fillMaxWidth(0.5f), // 占据左半边,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 连接状态显示
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WebDAV 状态: $connectionStatus",
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
                        is FileConnectionStatus.Connected -> Color.Green
                        is FileConnectionStatus.Connecting -> Color.Yellow
                        is FileConnectionStatus.Error -> Color.Red
                        is FileConnectionStatus.LoadingFile -> Color.Yellow
                        is FileConnectionStatus.FilesLoaded -> Color.Cyan
                        else -> Color.Gray
                    }
                )
            }

            // 输入字段 - baseUrl 现在表示完整路径
            TvTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "完整 WebDAV 路径 (e.g., https://192.168.1.4:5006)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

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
            )

            TvTextField(
                value = aliasName,
                onValueChange = { aliasName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Connection Name (Alias)",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 操作按钮
            MyIconButton(
                text = "测试连接",
                imageVector = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    currentPath = "" // 使用完整的 baseUrl 作为当前路径
                    if (!Tools.validateWebConnectionParams(context, serverAddress = baseUrl)) {
                        return@MyIconButton
                    }
                    webDavConViewModel.connectToWebDav(baseUrl, username, password)
                },
            )

            MyIconButton(
                text = "保存连接",
                imageVector = Icons.Outlined.Star,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    currentPath = baseUrl
                    if (!Tools.validateWebConnectionParams(context, serverAddress = baseUrl)) {
                        return@MyIconButton
                    }

                    if (connectionStatus !is FileConnectionStatus.Connected) {
                        Toast.makeText(context, "请先连接成功后再保存", Toast.LENGTH_SHORT).show()
                        return@MyIconButton
                    }

                    val newConnection = WebDavConnection(
                        id = UUID.randomUUID().toString(),
                        name = aliasName.ifBlank { "未命名WebDav连接" },
                        baseUrl = baseUrl, // 保存完整路径
                        username = username,
                        password = password
                    )
                    if (webDavListViewModel.addConnection(newConnection)) {
                        Toast.makeText(context, "连接已保存", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "保存失败，连接可能已存在", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("WebDavConScreen", "保存连接: $aliasName, 路径: $baseUrl")
                },
            )

            MyIconButton(
                text = "断开连接",
                imageVector = Icons.Outlined.Delete,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    currentPath = ""
                    webDavConViewModel.disconnectWebDav()
                },
            )

            // 显示当前路径
            Text(
                text = "当前路径: $currentPath",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 右侧：文件列表
        when (connectionStatus) {
            is FileConnectionStatus.FilesLoaded if fileList.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    itemsIndexed(fileList) { index, resource ->
                        val resourceName = resource.name
                        val isDirectory = resource.isDirectory
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = webDavConViewModel.isConnected()) {
                                    if (isDirectory) {
                                        // 点击文件夹：进入子目录，使用文件的完整路径
                                        currentPath = resource.path
                                        Log.d("WebDavConScreen", "进入目录: $currentPath")
                                        webDavConViewModel.listFiles(
                                            "${baseUrl.trimEnd('/')}/${currentPath.trimEnd('/').trimStart('/')}",
                                            username,
                                            password
                                        )
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "点击了文件: $resourceName",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isDirectory) R.drawable.localfile else R.drawable.baseline_insert_drive_file_24
                                ),
                                contentDescription = if (isDirectory) "Folder" else "File",
                                tint = if (isDirectory) Color.White else Color.White
                            )
                            Text(
                                text = resourceName,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${resource.size / 1024} KB",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            is FileConnectionStatus.Connecting -> {
                Text(
                    text = "正在连接...",
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(16.dp),
                    color = Color.Gray
                )
            }

            is FileConnectionStatus.Error -> {
                Text(
                    text = (connectionStatus as FileConnectionStatus.Error).message,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(16.dp),
                    color = Color.Red
                )
            }

            else -> {
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
}