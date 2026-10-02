// 文件路径: org/mz/mzdkplayer/ui/screen/ftpfile/FTPConScreen.kt
// (请根据你的实际项目结构调整包名和文件路径)
package org.mz.mzdkplayer.ui.screen.ftp

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection // 引入 FTP 数据模型
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.viewmodel.FTPConViewModel // 引入 FTP ViewModel

import org.mz.mzdkplayer.viewmodel.FTPListViewModel // 引入 FTP List ViewModel
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.MzToast
import org.mz.mzdkplayer.ui.screen.common.RemoteInputQRPanel
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.common.ConnectionFormCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionStatusPill
import org.mz.mzdkplayer.ui.screen.common.connectionStatusColor
import org.mz.mzdkplayer.ui.screen.common.rememberMzToastState
import java.util.Locale

import java.util.UUID

/**
 * FTP 连接界面
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun FTPConScreen(
    mainNavController: NavHostController,
    connectionId: String? = null,
    ftpListViewModel: FTPListViewModel
) {
    // 使用 FTP 的 ViewModel
    val ftpConViewModel: FTPConViewModel = viewModel()
    //val ftpListViewModel: FTPListViewModel = viewModel()

    // UI 状态由 ViewModel 管理
    val connectionStatus by ftpConViewModel.connectionStatus.collectAsState()
    val fileList by ftpConViewModel.fileList.collectAsState()
    val currentPath by ftpConViewModel.currentPath.collectAsState()

    val toastState = rememberMzToastState()
    val coroutineScope = rememberCoroutineScope()

    // 编辑模式：连接列表带 connId 进入时，用已有连接回填表单
    val editingConnection = remember(connectionId) {
        connectionId?.let { ftpListViewModel.getConnectionById(it) }
    }

    // 用户输入状态 - 注意 FTP 需要服务器地址和端口
    var server by remember { mutableStateOf(editingConnection?.ip ?: "") } // 服务器地址
    var port by remember { mutableStateOf((editingConnection?.port ?: 21).toString()) } // FTP 端口，默认 21
    var username by remember { mutableStateOf(editingConnection?.username ?: "") }
    var password by remember { mutableStateOf(editingConnection?.password ?: "") }
    var aliasName by remember { mutableStateOf(editingConnection?.name ?: "") } // 连接别名
    var shareName by remember { mutableStateOf(editingConnection?.shareName ?: "") } // FTP 共享文件夹名称

    // 用于控制键盘
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：连接配置和控制面板
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight().fillMaxWidth(0.5f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionStatusPill(
                text = stringResource(R.string.ui_label_ftp_status),
                color = connectionStatusColor(connectionStatus),
            )

            ConnectionFormCard(title = stringResource(R.string.ui_label_ftp_file_sharing)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TvTextField(
                        value = server,
                        onValueChange = { server = it },
                        modifier = Modifier.weight(0.65f),
                        label = stringResource(R.string.ui_label_server_address),
                        placeholder = stringResource(R.string.ui_label_ip_address_example),
                    )

                    TvTextField(
                        value = port,
                        onValueChange = { newValue ->
                            // 简单校验端口号为数字，允许空值
                            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                port = newValue
                            }
                        },
                        modifier = Modifier.weight(0.35f),
                        label = stringResource(R.string.ui_label_port_example),
                    )
                }

                TvTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_username),
                )

                TvTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_password),
                    isPassword = true,
                )

                TvTextField(
                    value = aliasName,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { aliasName = it },
                    label = stringResource(R.string.ui_label_connection_alias),
                )

                TvTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_initial_shared_folder_name),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, // 可选：让两个按钮之间有间距
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 操作按钮
                MyIconButton(
                    text = stringResource(R.string.ui_label_test_connection),
                    icon = R.drawable.check24dp,
                    modifier = Modifier.weight(1f) .padding(end = 8.dp), // 可选：加点右边距，避免贴太紧,//  平分宽度,
                    enabled = connectionStatus != FileConnectionStatus.Connecting, // 连接中时禁用
                    onClick = {
                        keyboardController?.hide() // 隐藏键盘
                        if (!Tools.validateConnectionParams(server, shareName = shareName, aliasName = aliasName)) {
                            return@MyIconButton
                        }
                        val portInt = port.toIntOrNull() ?: 21 // 转换端口，失败则默认 21
                        ftpConViewModel.connectToFTP(server, portInt, username, password, shareName)
                    },
                )

                MyIconButton(
                    text = stringResource(R.string.ui_label_save_connection),
                    icon = R.drawable.save24dp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp), // 可选：加点右边距，避免贴太紧 ,// ⬅️ 平分宽度,
                    // 只有在已连接时才允许保存
                    // enabled = connectionStatus is FileConnectionStatus.Connected,
                    onClick = {

                        keyboardController?.hide()
                        // 验证必填项
                        if (!Tools.validateConnectionParams(server, shareName = shareName, aliasName = aliasName)) {
                            return@MyIconButton
                        }
                        val portInt = port.toIntOrNull() ?: 21 // 保存时也转换端口，空值则默认 21
                        // 创建 FTPConnection 数据对象
                        val newConnection = FTPConnection(
                            id = editingConnection?.id ?: UUID.randomUUID().toString(),
                            name = aliasName.ifBlank { context.getString(R.string.ui_label_unnamed_ftp_connection)},
                            ip = server, // 使用 ip 字段存储服务器地址
                            username = username,
                            password = password, // 再次提醒：明文存储不安全
                            shareName = shareName ,// 存储共享文件夹名称
                            port = portInt
                        )
                        val editing = editingConnection
                        if (editing != null) {
                            // 编辑模式：只改了别名这类非连接信息时，不必重新测试连接
                            val networkChanged = server != editing.ip ||
                                    portInt != editing.port ||
                                    username != editing.username ||
                                    password != editing.password ||
                                    shareName != editing.shareName
                            if (networkChanged && !ftpConViewModel.isConnected()) {
                                toastState.show(context.getString(R.string.ui_label_save_after_successful_connection), coroutineScope)
                                return@MyIconButton
                            }
                            ftpListViewModel.updateConnection(newConnection)
                            toastState.show(context.getString(R.string.ui_label_ftp_connection_saved), coroutineScope)
                            mainNavController.popBackStack()
                        } else if (ftpConViewModel.isConnected()) {
                            if (ftpListViewModel.addConnection(newConnection)) {
                                toastState.show(context.getString(R.string.ui_label_ftp_connection_saved), coroutineScope)
                            } else {
                                toastState.show(context.getString(R.string.ui_label_save_failed_connection_exists), coroutineScope)
                            }
                        } else {
                            toastState.show(context.getString(R.string.ui_label_save_after_successful_connection), coroutineScope)
                        }
                        Log.d("FtpConScreen", "保存连接: $aliasName")
                    },
                )
            }
            MyIconButton(
                text = stringResource(R.string.ui_label_disconnect),
                icon = R.drawable.linkoff24dp,
                modifier = Modifier.fillMaxWidth(),
                // 只有在已连接或连接出错时才允许断开
//                enabled = connectionStatus is FileConnectionStatus.Connected ||
//                        connectionStatus is FileConnectionStatus.Error ||
//                        connectionStatus is FileConnectionStatus.Connecting,
                onClick = {
                    keyboardController?.hide()
                    ftpConViewModel.disconnectFTP()
                },
            )

            // 显示当前路径 (可选)
            Text(
                "${stringResource(R.string.ui_label_current_path_truncated)} ${if (currentPath.startsWith("/")) currentPath else "/$currentPath"}",
                color = Color.LightGray,

                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 右侧：文件列表
        // 只有在已连接且有文件时才显示列表
        when (connectionStatus) {
            is FileConnectionStatus.FilesLoaded if fileList.isNotEmpty() -> {
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
                                            toastState.show(
                                                context.getString(R.string.ui_label_current_path_truncated,resourceName),
                                                coroutineScope
                                            )

                                        }
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 图标 (简单区分文件夹和文件)
                                Icon(
                                    modifier = Modifier.size(30.dp),
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
                                            String.format(Locale.US, "%.1f KB", size.toDouble() / 1024)
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
            }

            is FileConnectionStatus.Connecting -> {
                // 显示连接中提示
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
                // 显示错误信息
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
                    config.ip?.let { if(it.isNotBlank()) server =it } // 甚至可以拼接
                    config.username?.let { if(it.isNotBlank()) username = it }
                    config.password?.let { if(it.isNotBlank()) password = it }
                    config.aliasName?.let { if(it.isNotBlank()) aliasName = it }
                    config.shareName?.let { if(it.isNotBlank()) shareName = it }
                }
            }
            else -> {
                // Disconnected 或 Connected 但列表为空
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
    MzToast(state = toastState)
}
}



