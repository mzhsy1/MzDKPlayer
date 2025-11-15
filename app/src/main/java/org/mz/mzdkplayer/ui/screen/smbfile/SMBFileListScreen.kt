package org.mz.mzdkplayer.ui.screen.smbfile

import NoSearchResult
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.tool.builderPlayer
import org.mz.mzdkplayer.tool.setupPlayer

import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen

import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel

import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(UnstableApi::class)
@Composable
fun SMBFileListScreen(path: String?, navController: NavHostController,connectionName: String="") {
    val context = LocalContext.current
    val viewModel: SMBConViewModel = viewModel()
    val files by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(true) }
    var focusedMediaUri by remember { mutableStateOf("") }
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    var seaText by remember { mutableStateOf("") }
    //  新增：过滤后的文件列表
    val filteredFiles by remember(files, seaText) {
        derivedStateOf {
            if (seaText.isBlank()) {
                files
            } else {
                files.filter { file ->
                    file.name.contains(seaText, ignoreCase = true)
                }
            }
        }
    }
    LaunchedEffect(connectionName) {
        Log.d("SMBFileListScreenF","connectionNameF:$connectionName")
    }
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
            is FileConnectionStatus.Disconnected -> {
                Log.d("SMBFileListScreen", "未连接，开始连接: ${smbConfig.server}")
                delay(300)
                viewModel.connectToSMB(
                    smbConfig.server,
                    smbConfig.username,
                    smbConfig.password,
                    smbConfig.share
                )
            }

            is FileConnectionStatus.Connected -> {
                Log.d("SMBFileListScreen", "已连接，列出文件: ${smbConfig.path}")
                viewModel.listSMBFiles(smbConfig)
            }

            is FileConnectionStatus.Error -> {
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                Log.e("SMBFileListScreen", "连接错误: $errorMessage")
                Toast.makeText(context, "SMB错误: $errorMessage", Toast.LENGTH_LONG).show()
            }

            is FileConnectionStatus.LoadingFile -> {
                Log.d("SMBFileListScreen", "正在加载文件...")
            }

            is FileConnectionStatus.FilesLoaded -> {
                Log.d("SMBFileListScreen", "文件加载完成")
            }

            is FileConnectionStatus.Connecting -> {
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

            is FileConnectionStatus.FilesLoaded -> {
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
                            when {
                                // 搜索无结果
                                filteredFiles.isEmpty() && seaText.isNotBlank() -> {
                                    item {
                                        NoSearchResult(text = "没有匹配 \"$seaText\" 的文件")
                                    }
                                }

                                // 目录本身为空（未搜索时）

                                else -> {
                                    items(filteredFiles) { file ->
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
                                                        Log.e(
                                                            "SMBFileListScreen",
                                                            "路径编码失败: $e"
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "路径编码失败",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    }
                                                    navController.navigate("SMBFileListScreen/$encodedPath/$connectionName")
                                                } else {
                                                    // 处理文件点击
                                                    val fileExtension =
                                                        Tools.extractFileExtension(file.name)

                                                    when {
                                                        Tools.containsVideoFormat(fileExtension) -> {
                                                            val encodedUri = try {
                                                                URLEncoder.encode(
                                                                    "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}",
                                                                    "UTF-8"
                                                                )
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "SMBFileListScreen",
                                                                    "视频URI编码失败: $e"
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "视频路径编码失败",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@ListItem

                                                            }
                                                            val encodedFileName = try {
                                                                URLEncoder.encode(
                                                                    file.name,
                                                                    "UTF-8"
                                                                )
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "SMBFileListScreen",
                                                                    "文件名编码失败: $e"
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "文件名编码失败",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@ListItem

                                                            }
                                                            Log.d("SMBFileListScreen","connectionName:$connectionName")
                                                            navController.navigate("VideoPlayer/$encodedUri/SMB/$encodedFileName/${connectionName}")
                                                        }

                                                        Tools.containsAudioFormat(fileExtension) -> {
                                                            //  构建音频文件列表
                                                            val audioFiles =
                                                                files.filter { smbFile ->
                                                                    Tools.containsAudioFormat(
                                                                        Tools.extractFileExtension(
                                                                            smbFile.name
                                                                        )
                                                                    )
                                                                }

                                                            //  构建文件名到索引的映射（O(N) 一次构建）
                                                            val nameToIndexMap =
                                                                audioFiles.withIndex()
                                                                    .associateBy(
                                                                        { it.value.name },
                                                                        { it.index })

                                                            //  快速查找索引（O(1)）
                                                            val currentAudioIndex =
                                                                nameToIndexMap[file.name] ?: -1
                                                            if (currentAudioIndex == -1) {
                                                                Log.e(
                                                                    "SMBFileListScreen",
                                                                    "未找到文件在音频列表中: ${file.name}"
                                                                )
                                                                return@ListItem

                                                            }

                                                            //  构建播放列表
                                                            val audioItems =
                                                                audioFiles.map { smbFile ->
                                                                    AudioItem(
                                                                        uri = "smb://${smbFile.username}:${smbFile.password}@${smbFile.server}/${smbFile.share}${smbFile.fullPath}",
                                                                        fileName = smbFile.name,
                                                                        dataSourceType = "SMB"
                                                                    )
                                                                }

                                                            // 设置数据
                                                            MzDkPlayerApplication.clearStringList("audio_playlist")
                                                            MzDkPlayerApplication.setStringList(
                                                                "audio_playlist",
                                                                audioItems
                                                            )

                                                            val encodedUri = try {
                                                                URLEncoder.encode(
                                                                    "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}",
                                                                    "UTF-8"
                                                                )
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "SMBFileListScreen",
                                                                    "音频URI编码失败: $e"
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "音频路径编码失败",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@ListItem
                                                            }

                                                            val encodedFileName = try {
                                                                URLEncoder.encode(
                                                                    file.name,
                                                                    "UTF-8"
                                                                )
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "SMBFileListScreen",
                                                                    "文件名编码失败: $e"
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    "文件名编码失败",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@ListItem
                                                            }

                                                            //  传递当前音频项在播放列表中的索引
                                                            navController.navigate("AudioPlayer/$encodedUri/SMB/$encodedFileName/${connectionName}/$currentAudioIndex")
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
                                            colors = MyFileListItemColor(),
                                            modifier = Modifier
                                                .padding(end = 10.dp)
                                                .height(40.dp)
                                                .onFocusChanged { focusState ->
                                                    if (focusState.isFocused) {
                                                        focusedFileName = file.name
                                                        focusedIsDir = file.isDirectory
                                                        focusedMediaUri =
                                                            "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}"
                                                        Log.d(
                                                            "SMBFileListScreen",
                                                            "焦点变化: ${file.name}, 是目录: $focusedIsDir"
                                                        )
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
                                                        Tools.containsVideoFormat(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        ) ->
                                                            painterResource(R.drawable.moviefileicon)

                                                        Tools.containsAudioFormat(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        ) ->
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
                            }

                        }

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.3f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TvTextField(
                                value = seaText,
                                onValueChange = { seaText = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = myTTFColor(),
                                placeholder = "请输入文件名",
                                textStyle = TextStyle(color = Color.White),
                            )
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

            is FileConnectionStatus.Error -> {
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                VAErrorScreen(
                    "加载失败: $errorMessage",
                )
            }

            else -> {
                LoadingScreen(
                    "正在连接SMB服务器",
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}


