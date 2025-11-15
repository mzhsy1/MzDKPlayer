// 文件路径: package org.mz.mzdkplayer.ui.screen.webdavfile.WebDavFileListScreen.kt

package org.mz.mzdkplayer.ui.screen.webdavfile

import NoSearchResult
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
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
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.vm.WebDavConViewModel
import java.net.URLEncoder
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen

import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

import org.mz.mzdkplayer.ui.screen.common.TvTextField

@OptIn(UnstableApi::class)
@Composable
fun WebDavFileListScreen(
    // path 现在是完整的 WebDAV URL 路径
    path: String?, // e.g., "https://192.168.1.4:5006/folder1/subfolder"
    navController: NavHostController,
    webDavConnection: WebDavConnection
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: WebDavConViewModel = viewModel()

    // 收集 ViewModel 中的状态
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
// 过滤后的文件列表
    var seaText by remember { mutableStateOf("") }
    // ✅ 新增：过滤后的文件列表
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
    LaunchedEffect(path, connectionStatus) {
        Log.d(
            "WebDavFileListScreen",
            "LaunchedEffect triggered with path: $path, status: $connectionStatus"
        )

        when (connectionStatus) {
            is FileConnectionStatus.Connected -> {
                delay(300)
                // 已连接，可以安全地列出文件
                //Log.d("WebDavFileListScreen", "Already connected, listing files for path: $path")
                viewModel.listFiles(
                    path ?.trimEnd('/').plus('/'),
                    webDavConnection.username,
                    webDavConnection.password
                )
            }

            is FileConnectionStatus.Disconnected -> {
                // 未连接，尝试连接
                viewModel.connectToWebDav(
                    webDavConnection.baseUrl, // 使用连接的基础URL进行认证
                    webDavConnection.username,
                    webDavConnection.password
                )
              //  Log.d("WebDavFileListScreen", "Disconnected. Waiting for connection trigger.")
            }

            is FileConnectionStatus.Connecting -> {
                // 正在连接，等待...
               // Log.d("WebDavFileListScreen", "Connecting...")
            }

            is FileConnectionStatus.Error -> {
                // 连接或列表错误
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                Log.e("WebDavFileListScreen", "Error state: $errorMessage")
                Toast.makeText(context, "WebDAV 错误: $errorMessage", Toast.LENGTH_LONG).show()
            }

            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectWebDav()
            Log.d("WebDavFileListScreen", "销毁")
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
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                VAErrorScreen("加载失败: $errorMessage")
            }

            is FileConnectionStatus.FilesLoaded -> {
                if (fileList.isEmpty()) {
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

                                else -> {
                                    items(filteredFiles) { file ->
                                        val isDirectory = file.isDirectory
                                        val fileName = file.name
                                        // 处理文件点击
                                        val fileExtension = Tools.extractFileExtension(file.name)
                                        val fullFileUrl = path ?: "" // 直接使用文件的完整路径
                                        val authenticatedUrl =viewModel.buildAuthenticatedUrl(fullFileUrl,
                                            username = webDavConnection.username?:""
                                            , password = webDavConnection.password?:"").trimEnd('/')
                                        val encodedFileUrl = URLEncoder.encode(
                                            "${authenticatedUrl}/${
                                                fileName.trimEnd('/').trimStart('/')
                                            }",
                                            "UTF-8"
                                        )
//                                        Log.d(
//                                            "WebDavFileListScreen",
//                                            "Navigating to media player: $fullFileUrl (encoded: $encodedFileUrl)"
//                                        )
                                        ListItem(
                                            selected = false,
                                            onClick = {
                                                coroutineScope.launch {
                                                    if (isDirectory) {
                                                        // 构建子目录的完整 URL 路径
                                                        val encodedNewPath = URLEncoder.encode("${path?.trimEnd('/') ?:""}/${fileName.trimEnd('/').trimStart('/')}/", "UTF-8")
                                                        Log.d(
                                                            "WebDavFileListScreen",
                                                            "Navigating to subdirectory: $path to ${path?.trimEnd('/')}/${fileName.trimEnd('/').trimStart('/')}/"
                                                        )
                                                        Log.d(
                                                            "WebDavFileListScreen",
                                                            "$encodedNewPath"
                                                        )
                                                        navController.navigate("WebDavFileListScreen/$encodedNewPath/${webDavConnection.username}/${webDavConnection.password}/${ URLEncoder.encode(
                                                            webDavConnection.name,
                                                            "UTF-8"
                                                        )}")
                                                    } else {
                                                        when {
                                                            Tools.containsVideoFormat(fileExtension) -> {
                                                                Log.d(
                                                                    "encodedFileUrl",
                                                                    encodedFileUrl
                                                                )
                                                                navController.navigate(
                                                                    "VideoPlayer/$encodedFileUrl/WEBDAV/${
                                                                        URLEncoder.encode(
                                                                            fileName,
                                                                            "UTF-8"
                                                                        )
                                                                    }"
                                                                )
                                                            }

                                                            Tools.containsAudioFormat(fileExtension) -> {
                                                                val audioFiles =
                                                                    fileList.filter { webdavFile ->
                                                                        Tools.containsAudioFormat(
                                                                            Tools.extractFileExtension(
                                                                                webdavFile.name
                                                                            )
                                                                        )
                                                                    }

                                                                val nameToIndexMap =
                                                                    audioFiles.withIndex()
                                                                        .associateBy(
                                                                            { it.value.name },
                                                                            { it.index })

                                                                val currentAudioIndex =
                                                                    nameToIndexMap[file.name] ?: -1
                                                                if (currentAudioIndex == -1) {
                                                                    Log.e(
                                                                        "WebDavFileListScreen",
                                                                        "未找到文件在音频列表中: ${file.name}"
                                                                    )
                                                                    return@launch
                                                                }
                                                                val audioItems =
                                                                    audioFiles.map { webdavFile ->
                                                                        AudioItem(
                                                                            uri = "${authenticatedUrl}/${
                                                                                webdavFile.name.trimEnd('/').trimStart('/')
                                                                            }",
                                                                            fileName = webdavFile.name,
                                                                            dataSourceType = "WEBDAV"
                                                                        )
                                                                    }

                                                                MzDkPlayerApplication.clearStringList(
                                                                    "audio_playlist"
                                                                )
                                                                MzDkPlayerApplication.setStringList(
                                                                    "audio_playlist",
                                                                    audioItems
                                                                )

                                                                navController.navigate(
                                                                    "AudioPlayer/$encodedFileUrl/WEBDAV/${
                                                                        URLEncoder.encode(
                                                                            fileName,
                                                                            "UTF-8"
                                                                        )
                                                                    }/$currentAudioIndex/${ URLEncoder.encode(
                                                                        webDavConnection.name,
                                                                        "UTF-8"
                                                                    )}"
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
                                                        focusedFileName = file.name
                                                        focusedIsDir = file.isDirectory
                                                        focusedMediaUri = file.path
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

            else -> {
                LoadingScreen(
                    "正在加载WebDav文件",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}