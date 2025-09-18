package org.mz.mzdkplayer.ui.screen.smbfile

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

import org.mz.mzdkplayer.logic.model.SMBConnection
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
    var ip by remember { mutableStateOf("192.168.1.") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("movies") }
    var aliasName by remember { mutableStateOf("my nas") }

    // 全局跟踪当前活跃的输入框ID（初始为null）
    val activeFieldId = remember { mutableStateOf<String?>(null) }
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
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
                    maxLines = 2
                )
                Icon(painter = painterResource( R.drawable.baseline_circle_24),contentDescription = null,tint=if (connectionStatus=="已连接") Color.Green else Color.Red)
            }


            TvTextField(
                value = ip,
                onValueChange = { ip = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "Ip address",
                colors = myTTFColor(),
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
                placeholder = "password",

                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = aliasName,
                onValueChange = { aliasName = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "aliasName",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            TvTextField(
                value = shareName,
                onValueChange = { shareName = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "shareName",
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )

            MyIconButton(
                text = "测试连接",
                imageVector = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(0.5f),
                enabled = true,
                onClick = {
                    viewModel.connectToSMB(ip, username, password, shareName);viewModel.changeSMBTest()
                },
            )

            MyIconButton(
                text = "保存连接",
                imageVector = Icons.Outlined.Star,
                modifier = Modifier.fillMaxWidth(0.5f),
                onClick = {
                    if (smbListViewModel.addConnection(
                            SMBConnection(
                                UUID.randomUUID().toString(),
                                aliasName,
                                ip,
                                username,
                                password,
                                shareName
                            )
                        )
                    ) {
                        Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
                    };Log.d("as", "K")
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
                            // 点击下载文件（需指定本地路径）
                            //viewModel.downloadFile(fileName, "/sdcard/Download/$fileName")
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = fileName,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White
                    )

                }
            }
        }
    }
}