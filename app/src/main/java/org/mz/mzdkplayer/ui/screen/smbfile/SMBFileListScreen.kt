package org.mz.mzdkplayer.ui.screen.smbfile

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.SmbMediaInfoExtractor
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.containsVideoFormat
import org.mz.mzdkplayer.ui.screen.vm.FTPConViewModel
import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel
import org.mz.mzdkplayer.ui.screen.vm.SMBConnectionStatus
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun SMBFileListScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current

    val viewModel: SMBConViewModel = viewModel()
    val files by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    LaunchedEffect(path,connectionStatus) {

        val decodedPath = URLDecoder.decode(path ?: "", "UTF-8")
        if (decodedPath.isEmpty()) return@LaunchedEffect

        // 解析SMB路径格式: smb://username:password@server/share/path
        val smbConfig = viewModel.parseSMBPath(decodedPath)
//        if (smbConfig == null) {
//            Log.e("FileScreen", "Invalid SMB path format")
//            return@LaunchedEffect
//        }
//
//        try {
//            // 关键修复：使用withContext(Dispatchers.IO)在后台线程执行网络操作
//            val smbFiles = withContext(Dispatchers.IO) {
//                listSMBFiles(smbConfig)
//            }
//            files.addAll(smbFiles)
//        } catch (e: Exception) {
//            Log.e("FileScreen", "Error listing SMB files", e)
//        }
        when (connectionStatus) {
            is SMBConnectionStatus.Connected -> {
                // 已连接，可以安全地列出文件
                Log.d("SMBFileListScreen", "Already connected, listing files for path: $path")
                viewModel.listSMBFiles(smbConfig)
            }

            is SMBConnectionStatus.Disconnected -> {
                // 未连接，尝试连接
                Log.d("SMBFileListScreen", "Disconnected. Attempting to connect.")
                viewModel.connectToSMB(
                    smbConfig.server,
                    smbConfig.username,
                    smbConfig.password,
                    smbConfig.share // 传递共享名称
                )
            }

            is SMBConnectionStatus.Connecting -> {
                // 正在连接，等待...
                Log.d("SMBFileListScreen", "Connecting...")
            }

            is SMBConnectionStatus.Error -> {
                // 连接或列表错误
                val errorMessage = (connectionStatus as SMBConnectionStatus.Error).message
                Log.e("SMBFileListScreen", "Error state: $errorMessage")
                Toast.makeText(context, "SMB 错误: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        items(files) { file ->
            ListItem(
                selected = false,

                onClick = {

                    if (file.isDirectory) {
                        val newPath = viewModel.buildSMBPath(
                            file.server,
                            file.share,
                            file.fullPath,
                            file.username,
                            file.password
                        )
                        val encoded = URLEncoder.encode(newPath, "UTF-8")
                        navController.navigate("SMBFileListScreen/$encoded")
                    } else {
                        // 处理文件点击，比如播放视频
                        Log.d("file.fullPath",file.fullPath)
                        val  smbMediaInfoExtractor = SmbMediaInfoExtractor(context)
                       // smbMediaInfoExtractor.extractMetadata(mediaUri = "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}".toUri())
                         navController.navigate("VideoPlayer/${URLEncoder.encode("smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}", "UTF-8")}/SMB")
                    }
                },
                colors = myListItemColor(),
                modifier = Modifier.padding(10.dp),
                scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.02f),
                leadingContent = {
                    Log.d("SMBSc",Tools.extractFileExtension(file.name))
                    Icon(
                        painter = if (file.isDirectory) {
                            painterResource(R.drawable.baseline_folder_24)
                        }  else if (Tools.extractFileExtension(file.name)=="mkv") {
                            Log.d("SMBSc","R.drawable.mkv")
                            painterResource(R.drawable.mkv)
                        }
//                        else if (containsVideoFormat(Tools.extractFileExtension(file.name))) {
//                            painterResource(R.drawable.baseline_video_file_24)
else {
                            painterResource(R.drawable.baseline_insert_drive_file_24)
                        },
                        contentDescription = null,

                    )
                },
                headlineContent = { Text(file.name) }
            )
        }
    }
}








