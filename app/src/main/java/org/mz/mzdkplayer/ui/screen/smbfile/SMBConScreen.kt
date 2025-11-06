package org.mz.mzdkplayer.ui.screen.smbfile


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

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.FileConnectionStatus

import org.mz.mzdkplayer.logic.model.SMBConnection
import org.mz.mzdkplayer.tool.Tools

import org.mz.mzdkplayer.ui.theme.TvTextField

import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel

import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyIconButton
import java.util.UUID

/**
 * SMB连接界面
 */
@Composable
@Preview
fun SMBConScreen() {
    val viewModel: SMBConViewModel = viewModel()
    val smbListViewModel: SMBListViewModel = viewModel()
    var ip by remember { mutableStateOf("192.168.1.4") }
    var username by remember { mutableStateOf("wang") }
    var password by remember { mutableStateOf("Wa541888") }
    var shareName by remember { mutableStateOf("movies") }
    var aliasName by remember { mutableStateOf("my nas") }

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
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(
                    text = "SMB 状态: $connectionStatus",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(100.dp,400.dp),
                    maxLines = 1
                )
                // 状态指示灯
                Icon(
                    painter = painterResource(R.drawable.baseline_circle_24), // 确保有此图标资源
                    contentDescription = null,
                    tint = when (connectionStatus) {
                        is FileConnectionStatus.Connected -> Color.Green
                        is FileConnectionStatus.Connecting -> Color.Yellow
                        is FileConnectionStatus.Error -> Color.Red
                        is FileConnectionStatus.LoadingFile -> Color.Yellow
                        is FileConnectionStatus.FilesLoaded -> Color.Cyan
                        else -> Color.Gray // Disconnected
                    }
                )
            }


            TvTextField(
                value = ip,
                onValueChange = { ip = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "服务器地址",
                colors = myTTFColor(),
            )

            TvTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "用户名",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                colors = myTTFColor(),
                placeholder = "密码",

                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = aliasName,
                onValueChange = { aliasName = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "别名",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            // 分享名称输入
            TvTextField(
                value = shareName,
                onValueChange = {
                    if (!it.startsWith("/")) {
                        shareName = it
                    }
                },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "分享名称（不能以'/'开头）",
                colors = myTTFColor(),
                textStyle = TextStyle(color = if (isShareNameValid) Color.White else Color.Red),
            )

            MyIconButton(
                text = "测试连接",
                imageVector = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(0.5f),
                enabled = true,
                onClick = {
                    if (!Tools.validateSMBConnectionParams(context,ip,shareName)) {
                        return@MyIconButton
                    }
                    viewModel.testConnectSMB(ip, username, password, shareName)
                    //viewModel.listSMBFiles(config = SMBConfig(ip,shareName,"/",username,password))
                },
            )

            MyIconButton(
                text = "保存连接",
                imageVector = Icons.Outlined.Star,

                modifier = Modifier.fillMaxWidth(0.5f),
                onClick = {
                    if (!Tools.validateSMBConnectionParams(context,ip,shareName)) {
                        return@MyIconButton
                    }

                    if  (isConnected) {
                        if (smbListViewModel.addConnection(
                                SMBConnection(
                                    UUID.randomUUID().toString(),
                                    aliasName.ifBlank { "未命名SMB连接" },
                                    ip,
                                    username,
                                    password,
                                    shareName
                                )
                            )
                        ) {
                            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "添加失败,已经有相同的连接存在", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "请先连接成功后再保存", Toast.LENGTH_SHORT).show()
                    }
                },

            )

            MyIconButton(
                text = "断开连接",
                imageVector = Icons.Outlined.Delete,

                modifier = Modifier.fillMaxWidth(0.5f),
                onClick = { viewModel.disconnectSMB() },
            )


        }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
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
    }
}
