// File: HTTPLinkFileListScreen.kt

package org.mz.mzdkplayer.ui.screen.httplink

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
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
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkConViewModel
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLEncoder


/**
 * HTTP 链接文件列表屏幕
 *
 * @param path HTTP 服务器地址和共享路径完整路径 w(e.g., "http://192.168.1.100:8080/nas/movies/")
 * @param navController 导航控制器
 */
@Composable
fun HTTPLinkFileListScreen(
    path: String?,
    navController: NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 使用 ViewModel
    val viewModel: HTTPLinkConViewModel = viewModel()

    // 收集 ViewModel 中的状态
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()


    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
    // 当传入的 serverAddressAndShare, effectiveSubPath 参数变化时，或者首次进入时，尝试加载文件列表
// 标准化 path：确保非空时以 "/" 结尾
    val normalizedPath = path?.let { p ->
        if (p.endsWith("/")) p else "$p/"
    }

    // 如果 normalizedPath 为 null，可以提前返回或显示错误
    if (normalizedPath == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("无效的路径", color = Color.Red)
        }
        return
    }

    // 专门监听连接状态变化，连接成功后检查是否需要导航到初始子路径
    LaunchedEffect(path, connectionStatus) {
        when (connectionStatus) {
            is FileConnectionStatus.Connected -> {
                // 连接成功后，检查当前路径是否与目标路径一致

                viewModel.listFiles(normalizedPath)

            }

            is FileConnectionStatus.Disconnected -> {
                delay(300)
                viewModel.connectToHTTPLink(normalizedPath)

            }

            is FileConnectionStatus.Error -> {
                // 如果连接或加载出错，不再自动重试，等待用户操作或导航离开
                Log.e(
                    "HTTPLinkFileListScreen",
                    "Connection or listing failed: ${(connectionStatus as FileConnectionStatus.Error).message}"
                )
            }

            else -> {
                // 其他状态，如 Connecting 或 Disconnected，不做特殊处理
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 可选：在离开屏幕时断开连接或清理资源
            // ViewModel 的 onCleared 会处理清理，通常不需要在此处手动断开
            Log.d("HTTPLinkFileListScreen", "Screen disposed")
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_error_24),
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "加载失败",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
            }

            is FileConnectionStatus.FilesLoaded -> {
                if (fileList.isEmpty()) {
                    FileEmptyScreen("此目录为空")
                } else {
                    // 已连接，显示文件列表
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxHeight()
                                .weight(0.7f)
                        ) {

                            // Log.d("HTTPLinkFileListScreen", "Displaying fileList: $fileList")

                            items(fileList) { resource ->
                                // 这里假设 resource 有 isDirectory: Boolean 和 name: String, path: String 属性
                                val isDirectory = resource.isDirectory
                                val resourceName = resource.name // 这里应该已经是完整的文件/目录名
                                val resourcePath = resource.path // 相对于 baseUrl 的路径

                                ListItem(
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch {
                                            if (isDirectory) {


                                                // normalizedPath 已带 /，所以直接拼接 resourceName 即可
                                                val newFullPath = "${normalizedPath}${resourceName}"
                                                val encodedNewSubPath =
                                                    URLEncoder.encode(newFullPath, "UTF-8")
                                                navController.navigate("HTTPLinkFileListScreen/$encodedNewSubPath")
                                            } else {
                                                // 处理文件点击 - 导航到 VideoPlayer
                                                // 构造完整的 HTTP URL
                                                val fullFileUrl =
                                                    viewModel.getResourceFullUrl(resourceName)

                                                Log.d(
                                                    "HTTPLinkFileListScreen",
                                                    "Full file URL before encoding: $fullFileUrl"
                                                )

                                                val encodedFileUrl =
                                                    URLEncoder.encode(fullFileUrl, "UTF-8")
                                                Log.d(
                                                    "HTTPLinkFileListScreen",
                                                    "Encoded file URL: $encodedFileUrl"
                                                )

                                                val fileExtension =
                                                    Tools.extractFileExtension(resource.name)
                                                when {
                                                    Tools.containsVideoFormat(fileExtension) -> {
                                                        // 导航到视频播放器
                                                        navController.navigate(
                                                            "VideoPlayer/$encodedFileUrl/HTTP/${
                                                                URLEncoder.encode(
                                                                    resource.name,
                                                                    "UTF-8"
                                                                )
                                                            }"
                                                        )
                                                    }

                                                    Tools.containsAudioFormat(fileExtension) -> {
                                                        // ✅ 构建音频文件列表
                                                        val audioFiles =
                                                            fileList.filter { httpFile ->
                                                                Tools.containsAudioFormat(
                                                                    Tools.extractFileExtension(
                                                                        httpFile.name
                                                                    )
                                                                )
                                                            }

                                                        // ✅ 构建文件名到索引的映射（O(N) 一次构建）
                                                        val nameToIndexMap = audioFiles.withIndex()
                                                            .associateBy(
                                                                { it.value.name },
                                                                { it.index })

                                                        // ✅ 快速查找索引（O(1)）
                                                        val currentAudioIndex =
                                                            nameToIndexMap[resource.name] ?: -1
                                                        if (currentAudioIndex == -1) {
                                                            Log.e(
                                                                "HTTPFileListScreen",
                                                                "未找到文件在音频列表中: ${resource.name}"
                                                            )
                                                            return@launch

                                                        }

                                                        // ✅ 构建播放列表
                                                        val audioItems =
                                                            audioFiles.map { httpFile ->
                                                                AudioItem(
                                                                    uri = viewModel.getResourceFullUrl(
                                                                        httpFile.name
                                                                    ),
                                                                    fileName = httpFile.name,
                                                                    dataSourceType = "HTTP"
                                                                )
                                                            }
                                                        // 设置数据
                                                        MzDkPlayerApplication.clearStringList("audio_playlist")
                                                        MzDkPlayerApplication.setStringList(
                                                            "audio_playlist",
                                                            audioItems
                                                        )
                                                        navController.navigate(
                                                            "AudioPlayer/$encodedFileUrl/HTTP/${
                                                                URLEncoder.encode(
                                                                    resource.name,
                                                                    "UTF-8"
                                                                )
                                                            }/$currentAudioIndex"
                                                        )
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
                                        .padding(end = 10.dp)
                                        .height(40.dp)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                focusedFileName = resource.name;
                                                focusedIsDir = isDirectory
                                                focusedMediaUri =
                                                    viewModel.getResourceFullUrl(resourceName)
                                            }
                                        },
                                    scale = ListItemDefaults.scale(
                                        scale = 1.0f,
                                        focusedScale = 1.01f
                                    ),
                                    leadingContent = {
                                        Icon(
                                            painter = if (resource.isDirectory) {
                                                painterResource(R.drawable.baseline_folder_24)
                                            } else if (Tools.containsVideoFormat(
                                                    Tools.extractFileExtension(resource.name)
                                                )
                                            ) {

                                                painterResource(R.drawable.moviefileicon)
                                            } else if (Tools.containsAudioFormat(
                                                    Tools.extractFileExtension(resource.name)
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
                                        // 显示完整的文件名
                                        Text(
                                            resourceName, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, fontSize = 10.sp
                                        )
                                    }
                                    // supportingContent = { Text(resource.rawListing ?: "") } // 可以显示原始信息
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

            else -> {
                LoadingScreen(
                    "正在加载HTTP文件", Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

        }
    }
}



