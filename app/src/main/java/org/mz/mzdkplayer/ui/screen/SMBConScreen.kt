package org.mz.mzdkplayer.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel

/**
 * SMB连接界面
 */
@Composable
fun SMBConScreen() {
    val viewModel: SMBConViewModel = viewModel()
    var ip by remember { mutableStateOf("192.168.1.3") }
    var username by remember { mutableStateOf("wang") }
    var password by remember { mutableStateOf("Wa541888") }
    var shareName by remember { mutableStateOf("movies") }
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val fileList by viewModel.fileList.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SMB 状态: $connectionStatus",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("服务器 IP", color = Color.White,) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名", color = Color.White,) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码", color = Color.White,) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shareName,
            onValueChange = { shareName = it },

            label = { Text("共享目录", color = Color.White,) },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.connectToSMB(ip, username, password, shareName);Log.d("as","kslianjie") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("连接", color = Color.White,)
        }

        Button(
            onClick = { viewModel.disconnectSMB() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.colors(containerColor = Color.Red)
        ) {
            Text("断开连接")
        }

        //Spacer(modifier = Modifier.height(16.dp))

        //Text("文件列表", style = MaterialTheme.typography.titleMedium, color = Color.White)

        LazyColumn {
            itemsIndexed(fileList) { index,fileName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 点击下载文件（需指定本地路径）
                            //viewModel.downloadFile(fileName, "/sdcard/Download/$fileName")
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = fileName, modifier = Modifier.fillMaxWidth(), color = Color.White)
                    //Icon(Icons.Default, contentDescription = "下载")
                }
            }
        }
    }
}