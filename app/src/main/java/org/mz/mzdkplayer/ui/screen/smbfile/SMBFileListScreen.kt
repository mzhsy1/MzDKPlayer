package org.mz.mzdkplayer.ui.screen.smbfile

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text

import org.mz.mzdkplayer.R

import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.tool.builderPlayer
import org.mz.mzdkplayer.tool.setupPlayer
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen

import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel
import org.mz.mzdkplayer.ui.screen.vm.SMBConnectionStatus
import org.mz.mzdkplayer.ui.style.myListItemColor
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun SMBFileListScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current

    val viewModel: SMBConViewModel = viewModel()
    val files by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(true) }
    var focusedMediaUri by remember { mutableStateOf("") }
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    LaunchedEffect(path, connectionStatus) {

        val decodedPath = URLDecoder.decode(path ?: "", "UTF-8")
        if (decodedPath.isEmpty()) return@LaunchedEffect

        // 解析SMB路径格式: smb://username:password@server/share/path
        val smbConfig = viewModel.parseSMBPath(decodedPath)

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
            SMBConnectionStatus.LoadingFile -> {
            }
            SMBConnectionStatus.LoadingFiled -> {
            }
        }
    }
    LaunchedEffect(focusedFileName, focusedIsDir) {
        exoPlayer?.release()


        if (!focusedIsDir&&Tools.containsVideoFormat(
                Tools.extractFileExtension(focusedFileName)
            )) {
            Log.d("focusedIsDir", false.toString())
            Log.d("focusedIsDir","获取媒体信息")
            exoPlayer = builderPlayer(mediaUri = focusedMediaUri,context, dataSourceType = "SMB")
            setupPlayer(exoPlayer!!, focusedMediaUri, "SMB", context, { mediaInfoMap ->
                // 在这里处理获取到的媒体信息
               Log.d("focusedIsDir", mediaInfoMap.toString())

                // 可以在这里使用媒体信息进行后续操作
                // 例如更新UI、保存到数据库等
            },
            onError = { errorMessage ->
                // 在发生错误时处理
                Log.e("focusedIsDir", "Error occurred: $errorMessage")
            })
            //Log.d("focusedIsDir",setupPlayer(exoPlayer!!,focusedMediaUri,"SMB",context).toString())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            // 可选：在离开屏幕时断开连接
            exoPlayer?.release()
            viewModel.disconnectSMB()
            Log.d("SMBFileListScreen", "销毁")
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (connectionStatus) {
            is SMBConnectionStatus.Connecting -> {
                LoadingScreen("正在连接SMB服务器")
            }
            is SMBConnectionStatus.Connected, is SMBConnectionStatus.LoadingFiled -> {
                if (files.isEmpty()) {
                    FileEmptyScreen("此目录为空")

                } else {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxHeight()
                                .weight(0.7f)
                        ) {
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
                                            Log.d("file.fullPath", file.fullPath)
                                            //val  smbMediaInfoExtractor = SmbMediaInfoExtractor(context)
                                            // smbMediaInfoExtractor.extractMetadata(mediaUri = "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}".toUri())
                                            navController.navigate(
                                                "VideoPlayer/${
                                                    URLEncoder.encode(
                                                        "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}",
                                                        "UTF-8"
                                                    )
                                                }/SMB"
                                            )
                                        }
                                    },
                                    colors = myListItemColor(),
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                focusedFileName = file.name;
                                                focusedIsDir = file.isDirectory
                                                focusedMediaUri = "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}"
                                            }

                                        },
                                    scale = ListItemDefaults.scale(
                                        scale = 1.0f,
                                        focusedScale = 1.02f
                                    ),
                                    leadingContent = {
                                        Icon(
                                            painter = if (file.isDirectory) {
                                                painterResource(R.drawable.baseline_folder_24)
                                            } else if (Tools.containsVideoFormat(
                                                    Tools.extractFileExtension(file.name)
                                                )
                                            ) {

                                                painterResource(R.drawable.moviefileicon)
                                            } else {
                                                painterResource(R.drawable.baseline_insert_drive_file_24)
                                            },
                                            contentDescription = null,

                                            )
                                    },
                                    headlineContent = {
                                        Text(
                                            file.name, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, fontSize = 12.sp
                                        )
                                    }
                                )
                            }

                        }

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.3f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            VideoBigIcon(
                                focusedIsDir,
                                focusedFileName,
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth()
                            )
                            focusedFileName?.let {
                                Text(
                                    it,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                }
            }

            SMBConnectionStatus.Disconnected -> {
                // 显示未连接提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("未连接到 SMB 服务器")
                    // 可以添加一个按钮来触发连接
                }
            }

            is SMBConnectionStatus.Error -> {
                // 显示错误信息
                val errorMessage = (connectionStatus as SMBConnectionStatus.Error).message
                Text(
                    "加载失败: $errorMessage",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            SMBConnectionStatus.LoadingFile -> {
                LoadingScreen("正在加载文件")
            }

        }
    }

}








