package org.mz.mzdkplayer.ui.screen.nfs

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.NFSConnection // 引入 NFS 数据模型
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.viewmodel.NFSConViewModel


import org.mz.mzdkplayer.viewmodel.NFSListViewModel // 假设你也有一个管理 NFS 连接列表的 ViewModel
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.RemoteInputQRPanel
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.common.ConnectionFormCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionStatusPill
import org.mz.mzdkplayer.ui.screen.common.connectionStatusColor
import org.mz.mzdkplayer.ui.screen.common.showToast
import java.util.Locale
import java.util.UUID

/**
 * NFS 连接与文件浏览界面
 */
@Composable
fun NFSConScreen(
    mainNavController: NavHostController,
    connectionId: String? = null,
    nfsListViewModel: NFSListViewModel
) {
    // 使用 NFS 的 ViewModel
    val nfsConViewModel: NFSConViewModel = viewModel()
    // 假设你也有一个管理连接列表的 ViewModel
    //val nfsListViewModel: NFSListViewModel = viewModel() // 如果不需要保存功能，可以移除

    // UI 状态由 ViewModel 管理
    val connectionStatus by nfsConViewModel.connectionStatus.collectAsState()
    val fileList by nfsConViewModel.fileList.collectAsState()
    val currentPath by nfsConViewModel.currentPath.collectAsState()

    // 编辑模式：连接列表带 connId 进入时，用已有连接回填表单
    val editingConnection = remember(connectionId) {
        connectionId?.let { nfsListViewModel.getConnectionById(it) }
    }

    // 用户输入状态 - NFS 需要服务器地址和导出路径
    var serverAddress by remember { mutableStateOf(editingConnection?.serverAddress ?: "") } // NFS 服务器地址
    var shareName by remember { mutableStateOf(editingConnection?.shareName ?: "") } // NFS 导出路径
    var aliasName by remember { mutableStateOf(editingConnection?.name ?: "") } // 连接别名

    // 用于控制键盘
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：连接配置和控制面板
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
                .fillMaxWidth(0.5f) // 占据左半边
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionStatusPill(
                text = stringResource(R.string.ui_label_nfs_connection_status, connectionStatus.toString()),
                color = connectionStatusColor(connectionStatus),
            )

            ConnectionFormCard(title = stringResource(R.string.ui_label_nfs_file_sharing)) {
                TvTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_server_address),
                    placeholder = stringResource(R.string.ui_label_nfs_server_address),
                )

                TvTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_shared_directory),
                    placeholder = stringResource(R.string.ui_label_nfs_export_path),
                )

                TvTextField(
                    value = aliasName,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { aliasName = it },
                    label = stringResource(R.string.ui_label_connection_alias),
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween, // 让两个按钮之间有间距
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 操作按钮 - 测试连接
                MyIconButton(
                    text = stringResource(R.string.ui_label_test_connection),
                    icon = R.drawable.check24dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp), // 平分宽度并加右边距
                    enabled = connectionStatus != FileConnectionStatus.Connecting, // 连接中时禁用
                    onClick = {
                        keyboardController?.hide() // 隐藏键盘
                        if (!Tools.validateConnectionParams(serverAddress, shareName, aliasName = aliasName)) {
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
                    text = stringResource(R.string.ui_label_save_connection),
                    icon = R.drawable.save24dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp), // 平分宽度并加左边距
                    onClick = {
                        keyboardController?.hide()
                        if (!Tools.validateConnectionParams(serverAddress, shareName,aliasName=aliasName)) {
                            return@MyIconButton
                        }
                        // 创建 NfsConnection 数据对象
                        val newConnection = NFSConnection(
                            id = editingConnection?.id ?: UUID.randomUUID().toString(),
                            name = aliasName.ifBlank { context.getString(R.string.ui_label_unnamed_nfs_connection)  },
                            serverAddress = serverAddress,
                            shareName
                        )
                        val editing = editingConnection
                        if (editing != null) {
                            // 编辑模式：只改了别名这类非连接信息时，不必重新测试连接
                            val networkChanged = serverAddress != editing.serverAddress ||
                                    shareName != editing.shareName
                            if (networkChanged && !nfsConViewModel.isConnected()) {
                                showToast(context, context.getString(R.string.ui_label_save_after_successful_connection))
                                return@MyIconButton
                            }
                            nfsListViewModel.updateConnection(newConnection)
                            showToast(context, context.getString(R.string.ui_label_nfs_connection_saved))
                            mainNavController.popBackStack()
                        } else if (nfsConViewModel.isConnected()) {
                            // 假设 NfsListViewModel 有 addConnection 方法
                            if (nfsListViewModel.addConnection(newConnection)) {
                                showToast(context, context.getString(R.string.ui_label_nfs_connection_saved))
                            } else {
                                showToast(
                                    context,
                                    context.getString(R.string.ui_label_save_failed_connection_exists)
                                )
                            }
                        } else {
                            showToast(context, context.getString(R.string.ui_label_save_after_successful_connection))
                        }
                        Log.d("NfsConScreen", "保存连接: $aliasName")
                    },
                )
            }

            // 断开连接按钮
            MyIconButton(
                text = stringResource(R.string.ui_label_disconnect),
                icon = R.drawable.linkoff24dp,
                modifier = Modifier.fillMaxWidth(),
                // 只有在已连接或连接出错时才允许断开
                onClick = {
                    keyboardController?.hide()
                    nfsConViewModel.disconnectNfs()
                },
            )

            // 显示当前路径 (可选)
            Text(
                text = stringResource(R.string.ui_label_current_path,currentPath),
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
                                                    showToast(
                                                        context,
                                                        context.getString(R.string.ui_label_http_file_clicked,resourceName)
                                                    )

                                                    // 可以使用 nfsFile 的路径等信息
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
                            text = stringResource(R.string.ui_label_directory_empty),
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
                is FileConnectionStatus.Disconnected -> {
                    // 未连接时显示
                    RemoteInputQRPanel { config ->
                        // WebDav 可能字段含义不同，这里灵活映射
                        // 比如 config.ip 映射给 baseUrl
                        config.ip?.let { if(it.isNotBlank())  serverAddress= it} // 甚至可以拼接
                        config.aliasName?.let { if(it.isNotBlank()) aliasName = it }
                        config.shareName?.let { if(it.isNotBlank()) shareName = it }

                    }
                }
                else -> {
                    // 显示连接中提示
                    Text(
                        text = stringResource(R.string.ui_label_preparing_to_retrieve_files),
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



