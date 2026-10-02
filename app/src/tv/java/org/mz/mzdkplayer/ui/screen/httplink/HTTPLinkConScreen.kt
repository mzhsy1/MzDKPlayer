package org.mz.mzdkplayer.ui.screen.httplink

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
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.HTTPLinkConnection // 使用提供的数据模型
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.viewmodel.HTTPLinkConViewModel

import org.mz.mzdkplayer.viewmodel.HTTPLinkListViewModel // 假设你也有一个管理 HTTPLink 连接列表的 ViewModel
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.MzToast
import org.mz.mzdkplayer.ui.screen.common.RemoteInputQRPanel
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.common.ConnectionFormCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionStatusPill
import org.mz.mzdkplayer.ui.screen.common.connectionStatusColor
import org.mz.mzdkplayer.ui.screen.common.rememberMzToastState
import java.util.UUID

/**
 * HTTP Link 连接与文件浏览界面
 */
@Composable
fun HTTPLinkConScreen(
    mainNavController: NavHostController,
    connectionId: String? = null,
    httpLinkListViewModel: HTTPLinkListViewModel
) {
    // 使用 HTTPLink 的 ViewModel
    val httpLinkConViewModel: HTTPLinkConViewModel = viewModel()
    //val httpLinkListViewModel: HTTPLinkListViewModel = viewModel() // 如果不需要保存功能，可以移除

    // UI 状态由 ViewModel 管理
    val connectionStatus by httpLinkConViewModel.connectionStatus.collectAsState()
    val fileList by httpLinkConViewModel.fileList.collectAsState()

    val toastState = rememberMzToastState()
    val coroutineScope = rememberCoroutineScope()

    // 编辑模式：连接列表带 connId 进入时，用已有连接回填表单
    val editingConnection = remember(connectionId) {
        connectionId?.let { httpLinkListViewModel.getConnectionById(it) }
    }

    // 用户输入状态 - HTTPLink 需要服务器地址和共享名称
    var serverAddress by remember { mutableStateOf(editingConnection?.serverAddress ?: "") } // HTTP 服务器地址 (例如 http://192.168.1.4:81)
    var shareName by remember { mutableStateOf(editingConnection?.shareName ?: "") } // HTTPLink 共享路径 (例如 /movies)
    var aliasName by remember { mutableStateOf(editingConnection?.name ?: "") } // 连接别名

    // 用于控制键盘
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf("") }
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
                text = stringResource(R.string.ui_label_http_link_status, connectionStatus.toString()),
                color = connectionStatusColor(connectionStatus),
            )

            ConnectionFormCard(title = stringResource(R.string.ui_label_nginx_file_sharing)) {
                TvTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_server_address),
                    placeholder = "http://192.168.1.4:81",
                )

                TvTextField(
                    value = shareName,
                    onValueChange = { shareName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_shared_directory),
                    placeholder = stringResource(R.string.ui_label_http_link_shared_path),
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
                // 操作按钮 - 连接
                MyIconButton(
                    text = stringResource(R.string.ui_label_test_connection),
                    icon = R.drawable.check24dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp), // 平分宽度并加右边距
                    onClick = {
                        keyboardController?.hide() // 隐藏键盘
                        currentPath =""
                        if (!Tools.validateConnectionParams(serverAddress, shareName = shareName,aliasName=aliasName)){
                            return@MyIconButton
                        }
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
                        httpLinkConViewModel.connectToHTTPLink(normalizedUrl) // 传递确保以 / 结尾的完整 URL
                    },
                )

                // 操作按钮 - 保存连接 (假设你有 HTTPLinkListViewModel)
                MyIconButton(
                    text = stringResource(R.string.ui_label_save_connection),
                    icon = R.drawable.save24dp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp), // 平分宽度并加左边距
                    // 只有在已连接时才允许保存
                    //enabled = connectionStatus is FileConnectionStatus.Connected,
                    onClick = {
                        keyboardController?.hide()
                        currentPath =""
                        if (!Tools.validateConnectionParams(serverAddress, shareName = shareName,aliasName=aliasName)) {
                            return@MyIconButton
                        }
                        // 创建 HTTPLinkConnection 数据对象
                        val newConnection = HTTPLinkConnection(
                            id = editingConnection?.id ?: UUID.randomUUID().toString(),
                            name = aliasName.ifBlank { context.getString(R.string.ui_label_unnamed_http_connection) },
                            serverAddress = serverAddress.trimEnd('/'),
                            shareName = if (!shareName.endsWith("/")) shareName.plus("/")  else shareName
                        )
                        val editing = editingConnection
                        if (editing != null) {
                            // 编辑模式：只改了别名这类非连接信息时，不必重新测试连接
                            val networkChanged = serverAddress.trimEnd('/') != editing.serverAddress ||
                                    shareName != editing.shareName
                            if (networkChanged && !httpLinkConViewModel.isConnected()) {
                                toastState.show(context.getString(R.string.ui_label_save_after_successful_connection), coroutineScope)
                                return@MyIconButton
                            }
                            httpLinkListViewModel.updateConnection(newConnection)
                            toastState.show(context.getString(R.string.ui_label_http_link_connection_saved), coroutineScope)
                            mainNavController.popBackStack()
                        } else if (httpLinkConViewModel.isConnected()) {
                            // 假设 HTTPLinkListViewModel 有 addConnection 方法
                            if (httpLinkListViewModel.addConnection(newConnection)) {
                                toastState.show(context.getString(R.string.ui_label_http_link_connection_saved), coroutineScope)
                            } else {
                                toastState.show(
                                    context.getString(R.string.ui_label_save_failed_connection_exists),
                                    coroutineScope
                                )
                            }
                        } else {
                            toastState.show(context.getString(R.string.ui_label_save_after_successful_connection), coroutineScope)
                        }
                        Log.d("HTTPLinkConScreen", "保存连接: $aliasName")
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
                    currentPath =""
                    httpLinkConViewModel.disconnectHTTPLink()
                },
            )

            // 显示当前路径 (可选)
            Text(
                text = "${stringResource(R.string.ui_label_http_current_path)} ${serverAddress.trimEnd('/')}/${shareName.trimEnd('/').trimStart('/')}/${currentPath}",
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
            if (connectionStatus is FileConnectionStatus.FilesLoaded) {
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
                                                currentPath += resource.path
                                                httpLinkConViewModel.listFiles("${serverAddress.trimEnd('/')}/${shareName.trimEnd('/').trimStart('/')}/${currentPath}") // 传递相对路径
                                                Log.d(
                                                    "HTTPLinkConScreen",
                                                    "进入目录: ${serverAddress.trimEnd('/')}/${shareName.trimEnd('/').trimStart('/')}/${currentPath}/, path: "
                                                )
                                            } else {
                                                // 点击文件：可以触发播放或其他操作
                                                val fileUrl =
                                                    httpLinkConViewModel.getResourceFullUrl(
                                                        resourceName
                                                    )
                                                toastState.show(
                                                    context.getString(R.string.ui_label_http_file_clicked,resourceName),
                                                    coroutineScope
                                                )

                                                // 可以使用 `fileUrl` 或 `resource` 的其他信息
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
                        text = stringResource(R.string.ui_label_directory_empty),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else if (connectionStatus is FileConnectionStatus.Connecting) {
                // 显示连接中提示
                Text(
                    text = stringResource(R.string.ui_label_connecting),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Gray
                )
            } else if (connectionStatus is FileConnectionStatus.Error) {
                // 显示错误信息
                Text(
                    text = (connectionStatus as FileConnectionStatus.Error).message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Red
                )}
                else if (connectionStatus is FileConnectionStatus.Disconnected) {
                    // 未连接时显示
                    RemoteInputQRPanel { config ->
                        // WebDav 可能字段含义不同，这里灵活映射
                        // 比如 config.ip 映射给 baseUrl
                        config.ip?.let { if(it.isNotBlank())  serverAddress= it} // 甚至可以拼接
                        config.aliasName?.let { if(it.isNotBlank()) aliasName = it }
                        config.shareName?.let { if(it.isNotBlank()) shareName = it }

                    }
                }
             else {
                // Disconnected 状态
                Text(
                    text = stringResource(R.string.ui_label_connect_http_link_server_first),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.Gray
                )
            }
        }
    }
    MzToast(state = toastState)
}






