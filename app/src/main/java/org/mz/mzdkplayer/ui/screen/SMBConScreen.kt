package org.mz.mzdkplayer.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.magnifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.logic.model.SMBConnection
import org.mz.mzdkplayer.ui.theme.TvTextField

import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel
import java.util.UUID

/**
 * SMB连接界面
 */
@Composable
@Preview
fun SMBConScreen() {
    val viewModel: SMBConViewModel = viewModel()
    val smbListViewModel: SMBListViewModel = viewModel()
    var ip by remember { mutableStateOf("192.168.1.3") }
    var username by remember { mutableStateOf("wang") }
    var password by remember { mutableStateOf("Wa541888") }
    var shareName by remember { mutableStateOf("movies") }
    var aliasName by remember { mutableStateOf("my nas") }
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    Row (modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SMB 状态: $connectionStatus",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            TvTextField(
                value = ip,
                onValueChange = { ip = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "Ip address",
                textStyle = TextStyle(color = Color.White)
            )

            TvTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "Username",
                textStyle = TextStyle(color = Color.White)
            )

            TvTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "password",
                textStyle = TextStyle(color = Color.White)
            )

            TvTextField(
                value = aliasName,
                onValueChange = { aliasName = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "aliasName",
                textStyle = TextStyle(color = Color.White)
            )

            TvTextField(
                value = shareName,
                onValueChange = { shareName = it },
                modifier = Modifier.fillMaxWidth(0.5f),
                placeholder = "shareName",
                textStyle = TextStyle(color = Color.White)
            )

            Button(
                onClick = {
                    viewModel.connectToSMB(ip, username, password, shareName);Log.d(
                    "as",
                    "kslianjie"
                )
                },
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("测试连接", color = Color.White)
            }

            Button(
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
                    };Log.d("as", "kslianjie")
                },
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("save", color = Color.White)
            }

            Button(
                onClick = { viewModel.disconnectSMB() },
                modifier = Modifier.fillMaxWidth(0.5f),
                colors = ButtonDefaults.colors(containerColor = Color.Red)
            ) {
                Text("断开连接")
            }

            //Spacer(modifier = Modifier.height(16.dp))

            //Text("文件列表", style = MaterialTheme.typography.titleMedium, color = Color.White)

        }

        LazyColumn (modifier = Modifier
            .fillMaxHeight()){
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