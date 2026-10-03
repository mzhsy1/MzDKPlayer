package org.mz.mzdkplayer.ui.screen.smbfile


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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus

import org.mz.mzdkplayer.data.model.SMBConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.common.showToast

import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.common.ConnectionFormCard
import org.mz.mzdkplayer.ui.screen.common.ConnectionStatusPill
import org.mz.mzdkplayer.ui.screen.common.connectionStatusColor

import org.mz.mzdkplayer.viewmodel.SMBConViewModel

import org.mz.mzdkplayer.viewmodel.SMBListViewModel
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.RemoteInputQRPanel
import java.util.UUID

/**
 * SMB连接界面
 */
@Composable

fun SMBConScreen(
    mainNavController: NavHostController,
    connectionId: String? = null,
    smbListViewModel: SMBListViewModel = viewModel()
) {
    val viewModel: SMBConViewModel = viewModel()
    // 编辑模式：连接列表带 connId 进入时，用已有连接回填表单
    val editingConnection = remember(connectionId) {
        connectionId?.let { smbListViewModel.getConnectionById(it) }
    }
    val isEditing = editingConnection != null
    var ip by remember { mutableStateOf(editingConnection?.ip ?: "192.168.1.2") }
    var username by remember { mutableStateOf(editingConnection?.username ?: "") }
    var password by remember { mutableStateOf(editingConnection?.password ?: "") }
    var shareName by remember { mutableStateOf(editingConnection?.shareName ?: "") }
    var aliasName by remember { mutableStateOf(editingConnection?.name ?: "电影") }
    val keyboardController = LocalSoftwareKeyboardController.current
    // 全局跟踪当前活跃的输入框ID（初始为null）
    //val activeFieldId = remember { mutableStateOf<String?>(null) }
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    //val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current


    // 验证分享名称是否以/开头
    val isShareNameValid = !shareName.startsWith("/")
    //val shareNameError = if (!isShareNameValid) "分享名称不能以'/'开头" else ""

    // 检查是否已连接
    val isConnected = connectionStatus is FileConnectionStatus.Connected ||
            connectionStatus is FileConnectionStatus.FilesLoaded ||
            connectionStatus is FileConnectionStatus.LoadingFile

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
                .fillMaxWidth(0.5f) // 明确指定占一半宽度
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        )
        {
            ConnectionStatusPill(
                text = stringResource(R.string.ui_label_smb_connection_status, connectionStatus.toString()),
                color = connectionStatusColor(connectionStatus),
            )

            ConnectionFormCard(title = stringResource(R.string.ui_label_smb_file_sharing)) {
                TvTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_server_address),
                    placeholder = stringResource(R.string.ui_label_ip_address_example),
                )

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
                    value = shareName,
                    onValueChange = {
                        if (!it.startsWith("/")) {
                            shareName = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_share_name_no_leading_slash),
                    isError = !isShareNameValid,
                )

                TvTextField(
                    value = aliasName,
                    onValueChange = { aliasName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ui_label_connection_alias),
                    placeholder = stringResource(R.string.ui_label_unnamed_smb_connection),
                )
            }

            // 测试与保存并排一行：矮屏（960×540dp）下少占一行高度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MyIconButton(
                    text = stringResource(R.string.ui_label_test_connection),
                    icon = R.drawable.check24dp,
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    onClick = {
                        if (!Tools.validateSMBConnectionParams(ip, shareName, aliasName)) {
                            return@MyIconButton
                        }
                        viewModel.testConnectSMB(ip, username, password, shareName)
                        //viewModel.listSMBFiles(config = SMBConfig(ip,shareName,"/",username,password))
                    },
                )

                MyIconButton(
                    text = stringResource(R.string.ui_label_save_connection),
                    icon = R.drawable.save24dp,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!Tools.validateSMBConnectionParams(ip, shareName, aliasName)) {
                            return@MyIconButton
                        }

                        val connection = SMBConnection(
                            id = editingConnection?.id ?: UUID.randomUUID().toString(),
                            name = aliasName.ifBlank { context.getString(R.string.ui_label_unnamed_smb_connection) },
                            ip = ip,
                            username = username,
                            password = password,
                            shareName = shareName
                        )

                        val editing = editingConnection
                        if (editing != null) {
                            // 编辑模式：只改了别名这类非连接信息时，不必重新测试连接
                            val networkChanged = ip != editing.ip ||
                                    username != editing.username ||
                                    password != editing.password ||
                                    shareName != editing.shareName
                            if (networkChanged && !isConnected) {
                                showToast(context, context.getString(R.string.ui_label_save_after_successful_connection))
                                return@MyIconButton
                            }
                            smbListViewModel.updateConnection(connection)
                            showToast(context, context.getString(R.string.ui_label_connection_saved))
                            mainNavController.popBackStack()
                        } else if (isConnected) {
                            if (smbListViewModel.addConnection(connection)) {
                                showToast(context, context.getString(R.string.ui_label_added_successfully))
                            } else {
                                showToast(
                                    context,
                                    context.getString(R.string.ui_label_save_failed_connection_exists)
                                )
                            }
                        } else {
                            showToast(context, context.getString(R.string.ui_label_save_after_successful_connection))
                        }
                    },
                )
            }

            MyIconButton(
                text = stringResource(R.string.ui_label_disconnect),
                icon = R.drawable.linkoff24dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    keyboardController?.hide()
                    Log.i("SMBCON", "断开连接")
                    viewModel.disconnectSMB()
                },
            )


        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            if (isConnected && fileList.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    itemsIndexed(fileList) { index, fileName ->
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clickable {

                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = fileName.name,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                fontSize = 20.sp,

                                )

                        }
                    }
                }
            } else {
                // 2. 未连接：显示扫码组件
                // 只需这一行代码！
                RemoteInputQRPanel { config ->
                    // 这里处理回调，自动填充
                    config.ip?.let { if (it.isNotBlank()) ip = it }
                    config.username?.let { if (it.isNotBlank()) username = it }
                    config.password?.let { if (it.isNotBlank()) password = it }
                    config.shareName?.let { if (it.isNotBlank()) shareName = it }
                    config.aliasName?.let { if (it.isNotBlank()) aliasName = it }
                }
            }
        }
    }
}

