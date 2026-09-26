// 文件路径: org/mz/mzdkplayer/ui/screen/webdavfile/WebDavConScreen.kt

package org.mz.mzdkplayer.ui.screen.webdavfile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.viewmodel.WebDavConViewModel
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel
import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.tool.MzToastManager
import org.mz.mzdkplayer.ui.screen.common.RemoteInputQRPanel
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import java.util.UUID

/**
 * WebDAV 连接界面
 */
@Composable
fun WebDavConScreen(webDavListViewModel: WebDavListViewModel) {
    val webDavConViewModel: WebDavConViewModel = viewModel()
    //val webDavListViewModel: WebDavListViewModel = viewModel()

    // UI 状态由 ViewModel 管理
    val connectionStatus by webDavConViewModel.connectionStatus.collectAsState()
    val fileList by webDavConViewModel.fileList.collectAsState()
    var currentPath by remember { mutableStateOf("") }

    // 用户输入状态 - baseUrl 现在表示完整的路径
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var aliasName by remember { mutableStateOf("") }

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
                    text = stringResource(R.string.ui_label_webdav_connection_status,connectionStatus.toString()),
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
// 建议先拼接好完整的提示文字
            val placeholderText = "${stringResource(R.string.ui_hint_webdav_path)} ${stringResource(R.string.ui_hint_http_only_lan)}"
            // 输入字段 - baseUrl 现在表示完整路径
            TvTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = placeholderText,
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.ui_label_username),
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                colors = myTTFColor(),
                placeholder = stringResource(R.string.ui_label_password),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = aliasName,
                onValueChange = { aliasName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.ui_label_connection_alias),
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 操作按钮
            MyIconButton(
                text = stringResource(R.string.ui_label_test_connection),
                icon = R.drawable.check24dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    currentPath = "" // 使用完整的 baseUrl 作为当前路径
                    if (!Tools.validateWebConnectionParams(serverAddress = baseUrl)) {
                        return@MyIconButton
                    }
                    webDavConViewModel.connectToWebDav(baseUrl, username, password,true)
                },
            )

            MyIconButton(
                text = stringResource(R.string.ui_label_save_connection),
                icon = R.drawable.save24dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    currentPath = baseUrl
                    if (!Tools.validateWebConnectionParams(serverAddress = baseUrl)) {
                        return@MyIconButton
                    }

                    if (!webDavConViewModel.isConnected()) {
                        MzToastManager.show(context.getString(R.string.ui_label_save_after_successful_connection))
                        return@MyIconButton
                    }

                    val newConnection = WebDavConnection(
                        id = UUID.randomUUID().toString(),
                        name = aliasName.ifBlank { context.getString(R.string.ui_label_unnamed_webdav_connection) },
                        baseUrl = baseUrl, // 保存完整路径
                        username = username,
                        password = password
                    )
                    if (webDavListViewModel.addConnection(newConnection)) {
                        MzToastManager.show(context.getString(R.string.ui_label_connection_saved))
                    } else {
                        MzToastManager.show(
                            context.getString(R.string.ui_label_save_failed_connection_exists)
                        )
                    }
                    Log.d("WebDavConScreen", "保存连接: $aliasName, 路径: $baseUrl")
                },
            )

            MyIconButton(
                text = stringResource(R.string.ui_label_disconnect),
                icon = R.drawable.linkoff24dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    currentPath = ""
                    webDavConViewModel.disconnectWebDav()
                },
            )

            // 显示当前路径
            Text(
                text = "${stringResource(R.string.ui_label_http_current_path)} $currentPath",
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
                                        MzToastManager.show(
                                            context.getString(R.string.ui_label_http_file_clicked,resourceName)
                                        )
                                    }
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(30.dp),
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
                    text = stringResource(R.string.ui_label_connecting),
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
            is FileConnectionStatus.Disconnected -> {
                // 未连接时显示
                RemoteInputQRPanel { config ->
                    // WebDav 可能字段含义不同，这里灵活映射
                    // 比如 config.ip 映射给 baseUrl
                    config.ip?.let { if(it.isNotBlank()) baseUrl = it } // 甚至可以拼接
                    config.username?.let { if(it.isNotBlank()) username = it }
                    config.password?.let { if(it.isNotBlank()) password = it }
                    config.aliasName?.let { if(it.isNotBlank()) aliasName = it }
                }
            }
            else -> {
                Text(
                    text = stringResource(R.string.ui_label_no_files),
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