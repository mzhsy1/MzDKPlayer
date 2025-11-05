// 文件路径: package org.mz.mzdkplayer.ui.screen.nfs (请根据你的实际包名修改)

package org.mz.mzdkplayer.ui.screen.nfs

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
import org.mz.mzdkplayer.logic.model.NFSConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.vm.NFSConViewModel
import org.mz.mzdkplayer.ui.screen.vm.NFSConnectionStatus // 假设存在对应的状态类
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLEncoder

/**
 * NFS 文件列表屏幕
 *
 * @param sharePath NFS 共享的根路径 (e.g., "/shared/videos")
 * @param subPath 当前浏览的子路径，相对于共享根路径 (e.g., "movies/action")
 * @param navController 导航控制器
 * @param nfsConnection NFS 连接信息数据类
 */
@OptIn(UnstableApi::class)
@Composable
fun NFSFileListScreen(
    sharePath: String, // NFS 共享的根路径/movies

    navController: NavHostController,
    nfsConnection: NFSConnection
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 使用 Hilt 注入 ViewModel
    val viewModel: NFSConViewModel = viewModel()

    // 收集 ViewModel 中的状态
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
    // ViewModel 内部可能需要维护当前完整路径或子路径，这里假设它维护的是相对于共享根的子路径
    //val currentSubPath by viewModel.currentSubPath.collectAsState()

    // 当传入的 subPath 参数变化时，或者首次进入时，尝试加载文件列表
    LaunchedEffect(sharePath, connectionStatus) { // 依赖 subPath
        Log.d(
            "NFSFileListScreen",
            "LaunchedEffect triggered with subPath: $sharePath, status: $connectionStatus"
        )

        when (connectionStatus) {
            is NFSConnectionStatus.Connected -> {
                delay(300)
                // 已连接，可以安全地列出文件
                //Log.d("NFSFileListScreen", "Already connected, listing files for subPath: $subPath")
                Log.d("sharePath", sharePath)
                viewModel.listFiles(sharePath)
            }

            is NFSConnectionStatus.Disconnected -> {
                // 未连接，尝试连接
                Log.d("NFSFileListScreen", "Disconnected. Attempting to connect.")
                Log.d("sharePath", sharePath)
                viewModel.connectToNFS(
                    nfsConnection
                )
            }

            is NFSConnectionStatus.Connecting -> {
                // 正在连接，等待...
                Log.d("NFSFileListScreen", "Connecting...")
            }

            is NFSConnectionStatus.Error -> {
                // 连接或列表错误
                val errorMessage = (connectionStatus as NFSConnectionStatus.Error).message
                Log.e("NFSFileListScreen", "Error state: $errorMessage")
                Toast.makeText(context, "NFS 错误: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 可选：在离开屏幕时断开连接或清理资源
            // viewModel.disconnectNFS()
            Log.d("NFSFileListScreen", "销毁")
        }
    }

    // 根据连接状态渲染 UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (connectionStatus) {
            is NFSConnectionStatus.Connecting -> {
                LoadingScreen(
                    "正在连接NFS服务器", Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            is NFSConnectionStatus.Error -> {
                // 显示错误信息
                val errorMessage = (connectionStatus as NFSConnectionStatus.Error).message
                Text(
                    "加载失败: $errorMessage",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                // 可以添加一个重试按钮
            }

            is NFSConnectionStatus.Connected -> {
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

                            Log.d("NFSFileListScreen", "Displaying fileList: $fileList")

                            items(fileList) { file ->
                                // 假设有一个类似 FTPFile 的 NFSFile 类，或使用通用文件信息类
                                // 这里假设 file 有 isDirectory: Boolean 和 name: String? 属性
                                val isDirectory = file.isDirectory
                                val fileName = file.name ?: "Unknown"

                                ListItem(
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch {
                                            val newSubPath = file.path

                                            // 对新路径进行编码
                                            val encodedNewSubPath =
                                                URLEncoder.encode(
                                                    newSubPath.ifEmpty { " " },
                                                    "UTF-8"
                                                )
                                            if (isDirectory) {
                                                // 构建新的子路径

//                                                Log.d(
//                                                    "NFSFileListScreen",
//                                                    "Navigating to subdirectory: ${file.path} (encoded: $fileName$fileName)"
//                                                )
                                                // 导航到子目录，传递连接信息和新的子路径
                                                // 注意 URL 路径结构可能需要根据你的导航图调整
                                                navController.navigate(
                                                    "NFSFileListScreen/${nfsConnection.serverAddress}/${
                                                        URLEncoder.encode(
                                                            nfsConnection.shareName,
                                                            "UTF-8"
                                                        )
                                                    }/$encodedNewSubPath"
                                                )
                                            } else {
                                                // 处理文件点击 - 导航到 VideoPlayer
                                                // 构造完整的 NFS URL 或文件系统路径
                                                // 例如: nfs://<ip>/<sharePath>/<subPath>/<fileName>
                                                // 或者如果已挂载:nfs://192.168.1.4:/fs/1000/nfs/:/moves/as.mkv
                                                Log.d(
                                                    "NFSFileListScreen",
                                                    "Navigating to subdirectory: ${file.path} (encoded: $fileName$fileName)"
                                                )
                                                val fullFileUrl =
                                                    "nfs://${nfsConnection.serverAddress}:${
                                                        URLEncoder.encode(
                                                            nfsConnection.shareName,
                                                            "UTF-8"
                                                        )
                                                    }:${
                                                        URLEncoder.encode(
                                                            newSubPath.ifEmpty { " " },
                                                            "UTF-8"
                                                        )
                                                    }"
                                                Log.d(
                                                    "NFSFileListScreen",
                                                    "Full file URL: $fullFileUrl"
                                                )

                                                val encodedFileUrl =
                                                    URLEncoder.encode(fullFileUrl, "UTF-8")
                                                if (Tools.containsVideoFormat(
                                                        Tools.extractFileExtension(
                                                            file.name
                                                        )
                                                    )
                                                ) {
                                                    // 导航到视频播放器
                                                    navController.navigate(
                                                        "VideoPlayer/$encodedFileUrl/NFS/${
                                                            URLEncoder.encode(
                                                                file.name,
                                                                "UTF-8"
                                                            )
                                                        }"
                                                    )
                                                } else if (Tools.containsAudioFormat(Tools.extractFileExtension(file.name))) {
                                                    // ✅ 构建音频文件列表
                                                    val audioFiles = fileList.filter { nfsFile ->
                                                        Tools.containsAudioFormat(Tools.extractFileExtension(nfsFile.name))
                                                    }

                                                    // ✅ 构建文件名到索引的映射（O(N) 一次构建）
                                                    val nameToIndexMap = audioFiles.withIndex().associateBy({ it.value.name }, { it.index })

                                                    // ✅ 快速查找索引（O(1)）
                                                    val currentAudioIndex = nameToIndexMap[file.name] ?: -1
                                                    if (currentAudioIndex == -1) {
                                                        Log.e("FTPFileListScreen", "未找到文件在音频列表中: ${file.name}")
                                                        return@launch

                                                    }

                                                    // ✅ 构建播放列表
                                                    val audioItems = audioFiles.map { nfsFile ->
                                                        AudioItem(
                                                            uri = "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${nfsFile.path}" ,
                                                            fileName = nfsFile.name,
                                                            dataSourceType = "HTTP"
                                                        )
                                                    }
                                                    // 设置数据
                                                    MzDkPlayerApplication.clearStringList("audio_playlist")
                                                    MzDkPlayerApplication.setStringList("audio_playlist", audioItems)
                                                    navController.navigate(
                                                        "AudioPlayer/$encodedFileUrl/NFS/${
                                                            URLEncoder.encode(
                                                                file.name,
                                                                "UTF-8"
                                                            )
                                                        }/$currentAudioIndex"
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "不支持的格式",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
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
                                                focusedFileName = file.name;
                                                focusedIsDir = file.isDirectory
                                                focusedMediaUri =
                                                    "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${file.path.ifEmpty { " " }}"
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
                                            file.name, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, fontSize = 10.sp
                                        )
                                    }
                                    // supportingContent = { Text(file.rawListing ?: "") } // 可以显示原始信息
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

            is NFSConnectionStatus.Disconnected -> {
                // 显示未连接提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("未连接到 NFS 服务器")
                    // 可以添加一个按钮来触发连接
                }
            }
        }
    }
}



