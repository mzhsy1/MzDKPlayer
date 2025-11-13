package org.mz.mzdkplayer.ui.screen.ftp

import NoSearchResult
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
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
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.AudioItem
import org.mz.mzdkplayer.logic.model.FTPConnection
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.vm.FTPConViewModel


import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

import org.mz.mzdkplayer.ui.screen.common.TvTextField
import java.net.URLEncoder
import kotlin.text.ifEmpty

@OptIn(UnstableApi::class)
@Composable
fun FTPFileListScreen(
    // path 现在是相对于 FTP 共享根目录的路径
    path: String?, // e.g., "folder1/subfolder"
    navController: NavHostController,
    ftpConnection: FTPConnection
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 使用 Hilt 注入 ViewModel
    val viewModel: FTPConViewModel = viewModel()

    // 收集 ViewModel 中的状态
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }

    var seaText by remember { mutableStateOf("") }
    //  新增：过滤后的文件列表
    val filteredFiles by remember(fileList, seaText) {
        derivedStateOf {
            if (seaText.isBlank()) {
                fileList
            } else {
                fileList.filter { file ->
                    file.name.contains(seaText, ignoreCase = true)
                }
            }
        }
    }
    // 当传入的 path 参数变化时，或者首次进入时，尝试加载文件列表
    LaunchedEffect(path,connectionStatus) { // 依赖 path
        Log.d(
            "FTPFileListScreen",
            "LaunchedEffect triggered with path: $path, status: $connectionStatus"
        )

        when (connectionStatus) {
            is FileConnectionStatus.Connected -> {

                // 已连接，可以安全地列出文件
                Log.d("FTPFileListScreen", "Already connected, listing files for path: $path")
                viewModel.listFiles(path ?: "")
            }

            is FileConnectionStatus.Disconnected -> {
                delay(300)
                // 未连接，尝试连接
                Log.d("FTPFileListScreen", "Disconnected. Attempting to connect.")
                viewModel.connectToFTP(
                    ftpConnection.ip,
                    ftpConnection.port,
                    ftpConnection.username,
                    ftpConnection.password,
                    ftpConnection.shareName // 传递共享名称
                )
            }

            is FileConnectionStatus.Connecting -> {
                // 正在连接，等待...
                Log.d("FTPFileListScreen", "Connecting...")
            }

            is FileConnectionStatus.Error -> {
                // 连接或列表错误
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                Log.e("FTPFileListScreen", "Error state: $errorMessage")
                Toast.makeText(context, "FTP 错误: $errorMessage", Toast.LENGTH_LONG).show()
            }

            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 可选：在离开屏幕时断开连接
            viewModel.disconnectFTP()
            Log.d("FTPFileListScreen", "销毁")
        }
    }

    // 根据连接状态渲染 UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (connectionStatus) {

            is FileConnectionStatus.Error -> {
                // 显示错误信息
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                VAErrorScreen(
                    "加载失败: $errorMessage",
                )
            }
            is FileConnectionStatus.FilesLoaded -> {
                if (fileList.isEmpty()) {
                    FileEmptyScreen("此目录为空")

                } else {
                    // 已连接，显示文件列表
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(all = 10.dp)
                                .fillMaxHeight()
                                .weight(0.7f)
                        ) {

                            //Log.d("fileList", fileList.toString())
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
                                        // FTPFile 使用 isDirectory 方法
                                        val isDirectory = file.isDirectory
                                        val fileName = file.name ?: "Unknown"

                                        ListItem(
                                            selected = false,
                                            onClick = {
                                                coroutineScope.launch {
                                                    if (isDirectory) {
                                                        // 构建子目录路径
                                                        val newPath = if (path.isNullOrEmpty()) {
                                                            fileName
                                                        } else {
                                                            "${path.trimEnd('/')}/$fileName"
                                                        }
                                                        // 对路径进行编码，空路径特殊处理
                                                        val encodedNewPath = URLEncoder.encode(
                                                            newPath.ifEmpty { " " },
                                                            "UTF-8"
                                                        )
                                                        Log.d(
                                                            "FTPFileListScreen",
                                                            "Navigating to subdirectory: $newPath (encoded: $encodedNewPath)"
                                                        )
                                                        // 导航到子目录，传递连接信息
                                                        navController.navigate("FTPFileListScreen/${ftpConnection.ip}/${ftpConnection.username}/${ftpConnection.password}/${ftpConnection.port}/$encodedNewPath")
                                                    } else {
                                                        // 处理文件点击 - 导航到 VideoPlayer

                                                        val fullFileUrl =
                                                            viewModel.getResourceFullUrl(fileName)
                                                        Log.d(
                                                            "FTPFileListScreen",
                                                            "Full file URL: $fullFileUrl"
                                                        )
// // 处理文件点击
                                                        val fileExtension =
                                                            Tools.extractFileExtension(file.name)
                                                        val encodedFileUrl = URLEncoder.encode(
                                                            fullFileUrl,
                                                            "UTF-8"
                                                        )
                                                        //Log.d("FTPFileListScreen", "Navigating to video player: $fullFileUrl (encoded: $encodedFileUrl)")
//
                                                        when {
                                                            Tools.containsVideoFormat(fileExtension) -> {
                                                                // 导航到视频播放器
                                                                navController.navigate(
                                                                    "VideoPlayer/$encodedFileUrl/FTP/${
                                                                        URLEncoder.encode(
                                                                            fileName,
                                                                            "UTF-8"
                                                                        )
                                                                    }/${ URLEncoder.encode(
                                                                        ftpConnection.name,
                                                                        "UTF-8"
                                                                    )}"
                                                                )
                                                            }

                                                            Tools.containsAudioFormat(fileExtension) -> {
                                                                // 构建音频文件列表
                                                                val audioFiles =
                                                                    fileList.filter { ftpFile ->
                                                                        Tools.containsAudioFormat(
                                                                            Tools.extractFileExtension(
                                                                                ftpFile.name
                                                                            )
                                                                        )
                                                                    }

                                                                // 构建文件名到索引的映射（O(N) 一次构建）
                                                                val nameToIndexMap =
                                                                    audioFiles.withIndex()
                                                                        .associateBy(
                                                                            { it.value.name },
                                                                            { it.index })

                                                                // 快速查找索引（O(1)）
                                                                val currentAudioIndex =
                                                                    nameToIndexMap[file.name] ?: -1
                                                                if (currentAudioIndex == -1) {
                                                                    Log.e(
                                                                        "FTPFileListScreen",
                                                                        "未找到文件在音频列表中: ${file.name}"
                                                                    )
                                                                    return@launch

                                                                }

                                                                // 构建播放列表
                                                                val audioItems =
                                                                    audioFiles.map { ftpFile ->
                                                                        AudioItem(
                                                                            uri = viewModel.getResourceFullUrl(
                                                                                ftpFile.name
                                                                            ),
                                                                            fileName = ftpFile.name,
                                                                            dataSourceType = "FTP"
                                                                        )
                                                                    }

                                                                // 设置数据
                                                                MzDkPlayerApplication.clearStringList(
                                                                    "audio_playlist"
                                                                )
                                                                MzDkPlayerApplication.setStringList(
                                                                    "audio_playlist",
                                                                    audioItems
                                                                )
                                                                navController.navigate(
                                                                    "AudioPlayer/$encodedFileUrl/FTP/${
                                                                        URLEncoder.encode(
                                                                            fileName,
                                                                            "UTF-8"
                                                                        )
                                                                    }/${ URLEncoder.encode(
                                                                        ftpConnection.name,
                                                                        "UTF-8"
                                                                    )}/$currentAudioIndex"
                                                                )
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
                                                }
                                            },
                                            colors = MyFileListItemColor(),
                                            modifier = Modifier
                                                .padding(end = 10.dp)
                                                .height(40.dp)
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        focusedFileName = file.name;
                                                        focusedIsDir = file.isDirectory
                                                        focusedMediaUri =
                                                            viewModel.getResourceFullUrl(fileName)
                                                    }
                                                },
                                            scale = ListItemDefaults.scale(
                                                scale = 1.0f,
                                                focusedScale = 1.01f
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
                                                    } else if (Tools.containsAudioFormat(
                                                            Tools.extractFileExtension(file.name)
                                                        )
                                                    ) {

                                                        painterResource(R.drawable.baseline_music_note_24)
                                                    } else {
                                                        painterResource(R.drawable.baseline_insert_drive_file_24)
                                                    },
                                                    contentDescription = null,

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

            else -> { // 显示加载指示器
                LoadingScreen(
                    "正在连接FTP服务器", Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )}
        }
    }
}



