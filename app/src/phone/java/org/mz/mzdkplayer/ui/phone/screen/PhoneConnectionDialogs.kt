package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.HTTPLinkConnection
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.data.model.SMBConnection
import org.mz.mzdkplayer.data.model.WebDavConnection
import java.util.UUID

/**
 * 各协议「新增连接 / 删除连接」对话框（第三阶段）。
 *
 * 五种协议的字段大同小异，共用 [ConnectionDialogFrame] + [FormTextField] 一套骨架，
 * 每个协议只声明自己的字段与落库口径（与电视端 `*ConScreen` 的字段一致，别只留一半）。
 */

/** FTP 默认端口，与电视端一致 */
private const val FTP_DEFAULT_PORT = 21

@Composable
private fun ConnectionDialogFrame(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = onSave) {
                Text(stringResource(R.string.ui_label_save_connection))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_label_cancel)) }
        },
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun AddSmbConnectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (SMBConnection) -> Unit,
) {
    var alias by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ConnectionDialogFrame(
        title = stringResource(R.string.phone_files_add_connection_title, "SMB"),
        canSave = address.isNotBlank() && shareName.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                SMBConnection(
                    id = UUID.randomUUID().toString(),
                    name = alias.trim().ifBlank { address.trim() },
                    ip = address.trim(),
                    username = username.trim(),
                    password = password,
                    shareName = shareName.trim().trimStart('/'),
                )
            )
        },
    ) {
        FormTextField(alias, { alias = it }, stringResource(R.string.ui_label_connection_alias))
        FormTextField(address, { address = it }, stringResource(R.string.ui_label_server_address))
        FormTextField(
            shareName,
            { shareName = it },
            stringResource(R.string.ui_label_share_name_no_leading_slash),
        )
        FormTextField(username, { username = it }, stringResource(R.string.ui_label_username))
        FormTextField(
            password,
            { password = it },
            stringResource(R.string.ui_label_password),
            isPassword = true,
        )
    }
}

@Composable
internal fun AddFtpConnectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (FTPConnection) -> Unit,
) {
    var alias by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(FTP_DEFAULT_PORT.toString()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var startDirectory by remember { mutableStateOf("") }

    ConnectionDialogFrame(
        title = stringResource(R.string.phone_files_add_connection_title, "FTP"),
        canSave = address.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                FTPConnection(
                    id = UUID.randomUUID().toString(),
                    name = alias.trim().ifBlank { address.trim() },
                    ip = address.trim(),
                    port = port.trim().toIntOrNull() ?: FTP_DEFAULT_PORT,
                    username = username.trim(),
                    password = password,
                    shareName = startDirectory.trim(),
                )
            )
        },
    ) {
        FormTextField(alias, { alias = it }, stringResource(R.string.ui_label_connection_alias))
        FormTextField(address, { address = it }, stringResource(R.string.ui_label_server_address))
        FormTextField(
            port,
            { port = it },
            stringResource(R.string.phone_field_port),
            keyboardType = KeyboardType.Number,
        )
        FormTextField(username, { username = it }, stringResource(R.string.ui_label_username))
        FormTextField(
            password,
            { password = it },
            stringResource(R.string.ui_label_password),
            isPassword = true,
        )
        FormTextField(
            startDirectory,
            { startDirectory = it },
            stringResource(R.string.phone_field_start_directory),
        )
    }
}

@Composable
internal fun AddNfsConnectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (NFSConnection) -> Unit,
) {
    var alias by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var exportPath by remember { mutableStateOf("") }

    ConnectionDialogFrame(
        title = stringResource(R.string.phone_files_add_connection_title, "NFS"),
        canSave = address.isNotBlank() && exportPath.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                NFSConnection(
                    id = UUID.randomUUID().toString(),
                    name = alias.trim().ifBlank { address.trim() },
                    serverAddress = address.trim(),
                    // 导出路径必须带前导 /，否则播放地址会拼出 nfs://host:export:... 这种取不到 host 的 URI
                    shareName = exportPath.trim().let { if (it.startsWith("/")) it else "/$it" },
                )
            )
        },
    ) {
        FormTextField(alias, { alias = it }, stringResource(R.string.ui_label_connection_alias))
        FormTextField(address, { address = it }, stringResource(R.string.ui_label_server_address))
        FormTextField(
            exportPath,
            { exportPath = it },
            stringResource(R.string.phone_field_export_path),
        )
    }
}

@Composable
internal fun AddWebDavConnectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (WebDavConnection) -> Unit,
) {
    var alias by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ConnectionDialogFrame(
        title = stringResource(R.string.phone_files_add_connection_title, "WebDAV"),
        canSave = baseUrl.trim().startsWith("http"),
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                WebDavConnection(
                    id = UUID.randomUUID().toString(),
                    name = alias.trim().ifBlank { baseUrl.trim() },
                    baseUrl = baseUrl.trim(),
                    username = username.trim(),
                    password = password,
                )
            )
        },
    ) {
        FormTextField(alias, { alias = it }, stringResource(R.string.ui_label_connection_alias))
        FormTextField(
            baseUrl,
            { baseUrl = it },
            stringResource(R.string.phone_field_webdav_url),
            keyboardType = KeyboardType.Uri,
        )
        FormTextField(username, { username = it }, stringResource(R.string.ui_label_username))
        FormTextField(
            password,
            { password = it },
            stringResource(R.string.ui_label_password),
            isPassword = true,
        )
    }
}

@Composable
internal fun AddHttpConnectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (HTTPLinkConnection) -> Unit,
) {
    var alias by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var directory by remember { mutableStateOf("") }

    ConnectionDialogFrame(
        title = stringResource(R.string.phone_files_add_connection_title, "HTTP"),
        canSave = address.trim().startsWith("http"),
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                HTTPLinkConnection(
                    id = UUID.randomUUID().toString(),
                    name = alias.trim().ifBlank { address.trim() },
                    serverAddress = address.trim().trimEnd('/'),
                    // 目录路径按用户输入原样存（可能与电视端存的不完全一样），拼接口径统一在 httpInitialDirectory 里
                    shareName = directory.trim(),
                )
            )
        },
    ) {
        FormTextField(alias, { alias = it }, stringResource(R.string.ui_label_connection_alias))
        FormTextField(
            address,
            { address = it },
            stringResource(R.string.ui_label_server_address),
            keyboardType = KeyboardType.Uri,
        )
        FormTextField(
            directory,
            { directory = it },
            stringResource(R.string.ui_label_shared_directory),
        )
    }
}

@Composable
internal fun DeleteConnectionDialog(
    connectionName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.phone_files_delete_confirm_title)) },
        text = {
            Text(stringResource(R.string.phone_files_delete_confirm_text, connectionName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ui_label_delete_connection))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_label_cancel)) }
        },
    )
}
