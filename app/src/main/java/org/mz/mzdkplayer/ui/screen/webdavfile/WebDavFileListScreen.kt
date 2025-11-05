// 文件路径: package org.mz.mzdkplayer.ui.screen.webdavfile.WebDavFileListScreen.kt

package org.mz.mzdkplayer.ui.screen.webdavfile

import android.util.Log
import android.widget.Toast
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
import org.mz.mzdkplayer.logic.model.WebDavConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.vm.WebDavConViewModel
import org.mz.mzdkplayer.ui.screen.vm.WebDavConnectionStatus // 导入状态类
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLEncoder
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.logic.model.AudioItem
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen

@Composable
fun WebDavFileListScreen(
    // path 现在是相对于 WebDAV 根目录的路径
    path: String?, // e.g., "folder1/subfolder"
    navController: NavHostController,
    webDavConnection: WebDavConnection

) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: WebDavConViewModel = viewModel() // 使用 Hilt 注入
    // 收集 ViewModel 中的状态
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
    // val currentPath by viewModel.currentPath.collectAsState() // 当前 Screen 不直接使用

    // 当传入的 path 参数变化时，或者首次进入时，尝试加载文件列表
    // 注意：这里不再直接调用 listFiles，而是在 LaunchedEffect 中根据状态处理
    LaunchedEffect(path) { // 依赖 path

        Log.d(
            "WebDavFileListScreen",
            "LaunchedEffect triggered with path: $path, status: $connectionStatus"
        )

        when (connectionStatus) {
            is WebDavConnectionStatus.Connected -> {
                delay(300)
                // 已连接，可以安全地列出文件
                Log.d("WebDavFileListScreen", "Already connected, listing files for path: $path")
                viewModel.listFiles(path ?: "")
            }

            is WebDavConnectionStatus.Disconnected -> {
                // 未连接，尝试连接（假设 ViewModel 知道如何获取 URL/凭证，或者需要从外部传入）
                // *** 这里是关键：需要触发连接逻辑 ***
                // 方式一：如果 ViewModel 可以从设置获取信息
                viewModel.connectToWebDav(
                    webDavConnection.baseUrl,
                    webDavConnection.username,
                    webDavConnection.password
                ) // 假设无参版本会从设置读取
                // 方式二：如果需要从外部传入（例如，通过一个设置 Screen 或启动参数）
                // 你需要在这里调用 viewModel.connectToWebDav(baseUrl, username, password)
                // 由于我们不知道具体来源，这里暂时不自动调用，让用户或上一个 Screen 触发连接
                Log.d("WebDavFileListScreen", "Disconnected. Waiting for connection trigger.")
                // 可以显示一个提示，或者导航到设置页面
            }

            is WebDavConnectionStatus.Connecting -> {
                // 正在连接，等待...
                Log.d("WebDavFileListScreen", "Connecting...")
            }

            is WebDavConnectionStatus.Error -> {
                // 连接或列表错误
                val errorMessage = (connectionStatus as WebDavConnectionStatus.Error).message
                Log.e("WebDavFileListScreen", "Error state: $errorMessage")
                // 可以显示 Toast 或 Snackbar
                Toast.makeText(context, "WebDAV 错误: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
    DisposableEffect(Unit) {

        onDispose {
            viewModel.disconnectWebDav()
            Log.d("WebDavFileListScreen", "销毁")
        }
    }


    // 根据连接状态渲染 UI
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        when (connectionStatus) {
            is WebDavConnectionStatus.Connecting -> {
                // 显示加载指示器
                Text(
                    "正在连接 WebDAV...",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 60.dp)
                )
            }

            is WebDavConnectionStatus.Error -> {
                // 显示错误信息
                val errorMessage = (connectionStatus as WebDavConnectionStatus.Error).message
                Text("加载失败: $errorMessage", modifier = Modifier.align(Alignment.Center))
                // 可以添加一个重试按钮
                // Button(onClick = { /* 触发重连或重新加载 */ }) {
                //     Text("重试")
                // }
            }

            is WebDavConnectionStatus.Connected -> {
                // 已连接，显示文件列表
                if (fileList.isEmpty()) {
                    FileEmptyScreen("此目录为空")
                } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    LazyColumn(modifier = Modifier
                        .padding(10.dp)
                        .fillMaxHeight()
                        .weight(0.7f)) {

                            items(fileList) { file ->
                                // Sardine 库中的 DavResource 通常提供了 isDirectory 方法
                                val isDirectory = file.isDirectory
                                val fileName = file.name ?: "Unknown"
                                //Log.d("WebDav",file.path)

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
                                                val encodedNewPath =
                                                    URLEncoder.encode(
                                                        newPath.ifEmpty { " " },
                                                        "UTF-8"
                                                    ) // 空路径特殊处理
                                                Log.d(
                                                    "WebDavFileListScreen",
                                                    "Navigating to subdirectory: $newPath (encoded: $encodedNewPath)"
                                                )
                                                navController.navigate("WebDavFileListScreen/$encodedNewPath/${webDavConnection.username}/${webDavConnection.password}")
                                            } else {
                                                // 处理文件点击 - 导航到 VideoPlayer
                                                // 需要获取文件的完整 URL
                                                // 注意：getResourceFullUrl 应该基于当前 path 和 filename 计算
                                                // 确保 ViewModel 的逻辑正确 https://<username>:<password>@192.168.1.4:5006/movies/as.mkv
                                                val fileExtension = Tools.extractFileExtension(file.name)
                                                val fullFileUrl =
                                                    viewModel.getResourceFullUrl(fileName) // 假设内部处理了 path
                                                val userInfo =
                                                    "${webDavConnection.username}:${webDavConnection.password}"
                                                val uri = fullFileUrl.toUri()
                                                val newAuthority =
                                                    "$userInfo@${uri.authority}" // 👈 关键！加上 @ 和原始 host:port

                                                val authenticatedUrl = uri.buildUpon()
                                                    .encodedAuthority(newAuthority) // 👈 设置完整的 authority（含 userinfo@host:port）
                                                    .build()
                                                val encodedFileUrl = URLEncoder.encode(
                                                    authenticatedUrl.toString(),
                                                    "UTF-8"
                                                )
                                                Log.d(
                                                    "WebDavFileListScreen",
                                                    "Navigating to video player: $fullFileUrl (encoded: $encodedFileUrl)"
                                                )
                                                when {
                                                    Tools.containsVideoFormat(fileExtension) -> {

                                                        // 导航到视频播放器
                                                        navController.navigate("VideoPlayer/$encodedFileUrl/WEBDAV/${ URLEncoder.encode(
                                                            fileName,
                                                            "UTF-8"
                                                        )}")
                                                    }
                                                    Tools.containsAudioFormat(fileExtension) -> {
                                                        // ✅ 构建音频文件列表
                                                        val audioFiles = fileList.filter { webdavFile ->
                                                            Tools.containsAudioFormat(Tools.extractFileExtension(webdavFile.name))
                                                        }

                                                        // ✅ 构建文件名到索引的映射（O(N) 一次构建）
                                                        val nameToIndexMap = audioFiles.withIndex().associateBy({ it.value.name }, { it.index })

                                                        // ✅ 快速查找索引（O(1)）
                                                        val currentAudioIndex = nameToIndexMap[file.name] ?: -1
                                                        if (currentAudioIndex == -1) {
                                                            Log.e("SMBFileListScreen", "未找到文件在音频列表中: ${file.name}")
                                                            return@launch

                                                        }

                                                        // ✅ 构建播放列表
                                                        val audioItems = audioFiles.map { webdavFile ->
                                                            AudioItem(
                                                                uri = webdavFile.path.toUri().buildUpon()
                                                                .encodedAuthority(newAuthority).scheme(uri.scheme) // 👈 设置完整的 authority（含 userinfo@host:port）
                                                                .build().toString(),
                                                                fileName = webdavFile.name,
                                                                dataSourceType = "WEBDAV"
                                                            )
                                                        }

                                                        // 设置数据
                                                        MzDkPlayerApplication.clearStringList("audio_playlist")
                                                        MzDkPlayerApplication.setStringList("audio_playlist", audioItems)

                                                        navController.navigate("AudioPlayer/$encodedFileUrl/WEBDAV/${ URLEncoder.encode(
                                                            fileName,
                                                            "UTF-8"
                                                        )}/$currentAudioIndex")
                                                        //navController.navigate("AudioPlayer/$encodedUri/SMB/$encodedFileName")
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
                                    colors = myListItemColor(),
                                    modifier = Modifier
                                        .padding(end = 10.dp).height(40.dp)
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
                                    headlineContent = {     Text(
                                        file.name, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, fontSize = 10.sp
                                    ) }
                                    // supportingContent = { Text(file.modified?.toString() ?: "") }
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

            is WebDavConnectionStatus.Disconnected -> {
                // 显示未连接提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("未连接到 WebDAV 服务器")
                    // 可以添加一个按钮来导航到设置或触发连接
                    // Button(onClick = { /* 导航到设置或调用 connect */ }) {
                    //     Text("连接")
                    // }
                }

            }
        }
    }
}

