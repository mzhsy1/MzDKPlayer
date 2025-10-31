package org.mz.mzdkplayer.ui.screen.smbfile

import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.AudioItem
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.tool.builderPlayer
import org.mz.mzdkplayer.tool.setupPlayer
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel
import org.mz.mzdkplayer.ui.screen.vm.SMBConnectionStatus
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(UnstableApi::class)
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

    // 处理路径变化和连接状态
    LaunchedEffect(path, connectionStatus) {
        val decodedPath = try {
            URLDecoder.decode(path ?: "", "UTF-8")
        } catch (e: Exception) {
            Log.e("SMBFileListScreen", "路径解码失败: $e")
            Toast.makeText(context, "路径格式错误", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        if (decodedPath.isEmpty()) {
            Log.w("SMBFileListScreen", "路径为空")
            return@LaunchedEffect
        }

        // 解析SMB路径
        val smbConfig = viewModel.parseSMBPath(decodedPath)
        if (smbConfig.server.isEmpty()) {
            Log.e("SMBFileListScreen", "无效的SMB路径: $decodedPath")
            Toast.makeText(context, "无效的SMB路径", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        when (connectionStatus) {
            is SMBConnectionStatus.Disconnected -> {
                Log.d("SMBFileListScreen", "未连接，开始连接: ${smbConfig.server}")
                viewModel.connectToSMB(
                    smbConfig.server,
                    smbConfig.username,
                    smbConfig.password,
                    smbConfig.share
                )
            }
            is SMBConnectionStatus.Connected -> {
                Log.d("SMBFileListScreen", "已连接，列出文件: ${smbConfig.path}")
                viewModel.listSMBFiles(smbConfig)
            }
            is SMBConnectionStatus.Error -> {
                val errorMessage = (connectionStatus as SMBConnectionStatus.Error).message
                Log.e("SMBFileListScreen", "连接错误: $errorMessage")
                Toast.makeText(context, "SMB错误: $errorMessage", Toast.LENGTH_LONG).show()
            }
            is SMBConnectionStatus.LoadingFile -> {
                Log.d("SMBFileListScreen", "正在加载文件...")
            }
            is SMBConnectionStatus.LoadingFiled -> {
                Log.d("SMBFileListScreen", "文件加载完成")
            }
            is SMBConnectionStatus.Connecting -> {
                Log.d("SMBFileListScreen", "正在连接...")
            }
        }
    }

    // 处理焦点变化和媒体播放
    LaunchedEffect(focusedFileName, focusedIsDir) {
        // 释放之前的播放器
        exoPlayer?.release()

        if (!focusedIsDir && focusedFileName != null) {
            val extension = Tools.extractFileExtension(focusedFileName)
            if (Tools.containsVideoFormat(extension)) {
                Log.d("SMBFileListScreen", "准备播放视频: $focusedFileName")

                try {
                    exoPlayer = withContext(Dispatchers.Main) {
                        builderPlayer(mediaUri = focusedMediaUri, context, dataSourceType = "SMB")
                    }

                    withContext(Dispatchers.Main) {
                        setupPlayer(
                            exoPlayer!!,
                            focusedMediaUri,
                            "SMB",
                            context,
                            { mediaInfoMap ->
                                Log.d("SMBFileListScreen", "媒体信息: $mediaInfoMap")
                            },
                            onError = { errorMessage ->
                                Log.e("SMBFileListScreen", "播放错误: $errorMessage")
                                //Toast.makeText(context, "播放错误: $errorMessage", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SMBFileListScreen", "播放器初始化失败: ${e.message}", e)
                   // Toast.makeText(context, "播放器初始化失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            Log.d("SMBFileListScreen", "界面销毁，释放资源")
            exoPlayer?.release()
            viewModel.disconnectSMB()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (connectionStatus) {
            is SMBConnectionStatus.Connecting -> {
                LoadingScreen(
                    "正在连接SMB服务器",
                    Modifier.fillMaxSize().background(Color.Black)
                )
            }

            is SMBConnectionStatus.Connected, is SMBConnectionStatus.LoadingFiled -> {
                if (files.isEmpty()) {
                    FileEmptyScreen("此目录为空")
                } else {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxHeight()
                                .weight(0.7f)
                        ) {
                            items(files) { file ->
                                ListItem(
                                    selected = false,

                                    onClick = {
                                        if (file.isDirectory) {
                                            // 导航到子目录
                                            val newPath = viewModel.buildSMBPath(
                                                file.server,
                                                file.share,
                                                file.fullPath,
                                                file.username,
                                                file.password
                                            )
                                            val encodedPath = try {
                                                URLEncoder.encode(newPath, "UTF-8")
                                            } catch (e: Exception) {
                                                Log.e("SMBFileListScreen", "路径编码失败: $e")
                                                Toast.makeText(context, "路径编码失败", Toast.LENGTH_SHORT).show()

                                            }
                                            navController.navigate("SMBFileListScreen/$encodedPath")
                                        } else {
                                            // 处理文件点击
                                            val fileExtension = Tools.extractFileExtension(file.name)

                                            when {
                                                Tools.containsVideoFormat(fileExtension) -> {
                                                    val encodedUri = try {
                                                        URLEncoder.encode(
                                                            "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}",
                                                            "UTF-8"
                                                        )
                                                    } catch (e: Exception) {
                                                        Log.e("SMBFileListScreen", "视频URI编码失败: $e")
                                                        Toast.makeText(context, "视频路径编码失败", Toast.LENGTH_SHORT).show()

                                                    }
                                                    val encodedFileName = try {
                                                        URLEncoder.encode(file.name, "UTF-8")
                                                    } catch (e: Exception) {
                                                        Log.e("SMBFileListScreen", "文件名编码失败: $e")
                                                        Toast.makeText(context, "文件名编码失败", Toast.LENGTH_SHORT).show()

                                                    }
                                                    navController.navigate("VideoPlayer/$encodedUri/SMB/$encodedFileName")
                                                }
                                                Tools.containsAudioFormat(fileExtension) -> {
                                                    val audioFiles = files.filter {
                                                        Tools.containsAudioFormat(Tools.extractFileExtension(it.name))
                                                    }
                                                    val audioItems = audioFiles.map { audioFile ->
                                                        AudioItem(
                                                            uri = "smb://${audioFile.username}:${audioFile.password}@${audioFile.server}/${audioFile.share}${audioFile.fullPath}",
                                                            fileName = audioFile.name,
                                                            dataSourceType = "SMB"
                                                        )
                                                    }
                                                    // 设置数据
                                                    MzDkPlayerApplication.clearStringList("audio_playlist")
                                                    MzDkPlayerApplication.setStringList("audio_playlist", audioItems)
                                                    val encodedUri = try {
                                                        URLEncoder.encode(
                                                            "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}",
                                                            "UTF-8"
                                                        )
                                                    } catch (e: Exception) {
                                                        Log.e("SMBFileListScreen", "音频URI编码失败: $e")
                                                        Toast.makeText(context, "音频路径编码失败", Toast.LENGTH_SHORT).show()

                                                    }
                                                    val encodedFileName = try {
                                                        URLEncoder.encode(file.name, "UTF-8")
                                                    } catch (e: Exception) {
                                                        Log.e("SMBFileListScreen", "文件名编码失败: $e")
                                                        Toast.makeText(context, "文件名编码失败", Toast.LENGTH_SHORT).show()

                                                    }
                                                    navController.navigate("AudioPlayer/$encodedUri/SMB/$encodedFileName")
                                                }
                                                else -> {
                                                    Toast.makeText(
                                                        context,
                                                        "不支持的文件格式: $fileExtension",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = myListItemColor(),
                                    modifier = Modifier
                                        .padding(end = 10.dp).height(40.dp)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedFileName = file.name
                                                focusedIsDir = file.isDirectory
                                                focusedMediaUri = "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}"
                                                Log.d("SMBFileListScreen", "焦点变化: ${file.name}, 是目录: $focusedIsDir")
                                            }
                                        },
                                    scale = ListItemDefaults.scale(
                                        scale = 1.0f,
                                        focusedScale = 1.01f
                                    ),
                                    leadingContent = {
                                        Icon(
                                            painter = when {
                                                file.isDirectory -> painterResource(R.drawable.baseline_folder_24)
                                                Tools.containsVideoFormat(Tools.extractFileExtension(file.name)) ->
                                                    painterResource(R.drawable.moviefileicon)
                                                Tools.containsAudioFormat(Tools.extractFileExtension(file.name)) ->
                                                    painterResource(R.drawable.baseline_music_note_24)
                                                else -> painterResource(R.drawable.baseline_insert_drive_file_24)
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            file.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 10.sp
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
                            focusedFileName?.let { fileName ->
                                Text(
                                    fileName,
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "未连接到 SMB 服务器",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    // 可以添加连接按钮
                }
            }

            is SMBConnectionStatus.Error -> {
                val errorMessage = (connectionStatus as SMBConnectionStatus.Error).message
                Text(
                    "加载失败: $errorMessage",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            SMBConnectionStatus.LoadingFile -> {
                LoadingScreen(
                    "正在加载SMB文件",
                    Modifier.fillMaxSize().background(Color.Black)
                )
            }
        }
    }
}



