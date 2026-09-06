// 文件路径: package org.mz.mzdkplayer.ui.screen.nfs (请根据你的实际包名修改)

package org.mz.mzdkplayer.ui.screen.nfs

import NoSearchResult
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.mz.mzdkplayer.tool.Tools.toBase64
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.MediaInfoExtractorFormFileName
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.CirCleIconButton
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.FileIcon
import org.mz.mzdkplayer.ui.screen.common.FileName
import org.mz.mzdkplayer.ui.screen.common.FileSize
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.MediaFocusedFileName
import org.mz.mzdkplayer.ui.screen.common.MediaInfoLoading
import org.mz.mzdkplayer.ui.screen.common.MediaPreviewSection
import org.mz.mzdkplayer.ui.screen.common.MediaReleaseDate
import org.mz.mzdkplayer.ui.screen.common.MediaTitle
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.common.showToast
import org.mz.mzdkplayer.ui.screen.vm.AudioViewModel
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.screen.vm.NFSConViewModel
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor
import org.mz.mzdkplayer.ui.theme.myTTFColor
import java.net.URLDecoder

import java.net.URLEncoder
import kotlin.time.Duration.Companion.milliseconds

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
    nfsConnection: NFSConnection,
    settingsViewModel: SettingsViewModel = viewModel()
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
    var seaText by remember { mutableStateOf("") }
    val movieViewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }// 新增：获取MovieViewModel
    val settingsState by settingsViewModel.uiState.collectAsState()
    // 新增：电影信息状态
    val focusedMovie by movieViewModel.focusedMovie.collectAsState()
    var mediaId by remember { mutableIntStateOf(-1) }
    var focusedIsVideo by remember { mutableStateOf(false) }
    // 获取 AudioViewModel
    val audioViewModel: AudioViewModel = viewModelWithFactory {
        RepositoryProvider.createAudioViewModel() // 不需要 context 了
    }
    val isAudioScanning by audioViewModel.isScanning.collectAsState()
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
    val isScanning by movieViewModel.isScanning.collectAsState()
    val currentScanIndex by movieViewModel.currentScanIndex.collectAsState() // 新增：引入当前进度
    val totalScanCount by movieViewModel.totalScanCount.collectAsState() // 新增：引入总数
// 是否正在加载
    var isLoading by remember { mutableStateOf(true) }
    // 当传入的 subPath 参数变化时，或者首次进入时，尝试加载文件列表
    LaunchedEffect(sharePath, connectionStatus) { // 依赖 subPath
        Log.d(
            "NFSFileListScreen",
            "LaunchedEffect triggered with subPath: $sharePath, status: $connectionStatus"
        )

        when (connectionStatus) {
            is FileConnectionStatus.Connected -> {

                // 已连接，可以安全地列出文件
                Log.d("NFSFileListScreen", "已连接")
                Log.d("sharePath", sharePath)
                viewModel.listFiles(sharePath)
            }

            is FileConnectionStatus.Disconnected -> {
                delay(300.milliseconds)
                // 未连接，尝试连接
                Log.d("NFSFileListScreen", "未连接")
                Log.d("sharePath", sharePath)
                viewModel.connectToNFS(
                    nfsConnection
                )
            }

//            is FileConnectionStatus.Connecting -> {
//                // 正在连接，等待...
//                Log.d("NFSFileListScreen", "正在连接...")
//                isLoading = true
//            }

            is FileConnectionStatus.Error -> {
                // 连接或列表错误
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                Log.e("NFSFileListScreen", "Error state: $errorMessage")
                showToast(context, context.getString(R.string.ui_label_nfs_error,errorMessage))
            }
            else -> {

            }


        }
    }
    // 处理焦点变化和媒体播放
    LaunchedEffect(focusedFileName, focusedIsDir, focusedIsVideo,settingsState.nfs) {
        // 1. 基础校验：如果是目录、无文件名、或者不是视频，直接清空信息
        if (focusedFileName == null || focusedIsDir || !focusedIsVideo) {
            movieViewModel.clearFocusedMovie()
            return@LaunchedEffect
        }
        // 2. 根据设置决定策略
        // settingsState.nfs 为 false 代表 "禁止自动刮削/仅本地数据" (防止重复入库)
        if (settingsState.nfs) {
            // === 模式 A：自动刮削 (主数据源) ===
            // 场景：这是用户的主要观看路径，允许自动联网获取信息。
            Log.d("NFSFileListScreen", "自动模式: 触发搜索/刮削: $focusedFileName")
            movieViewModel.searchFocusedMovie(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "NFS",
                connectionName = nfsConnection.name?:"未知连接"
            )

        } else {
            // === 模式 B：仅查询数据库 (防重复) ===
            // 场景：用户不希望此协议自动产生新数据，但如果之前"手动批量扫描"过，这里应该显示出来。
            Log.d("NFSFileListScreen", "禁止自动刮削模式: 仅查询数据库: $focusedFileName")
            movieViewModel.getFocusedInfo(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "NFS",
                connectionName = nfsConnection.name?:"未知连接"
            )
        }

    }
    DisposableEffect(Unit) {
        onDispose {
            // 可选：在离开屏幕时断开连接或清理资源
            viewModel.disconnectNfs()
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

            is FileConnectionStatus.Error -> {
                // 显示错误信息
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                VAErrorScreen(
                    "${stringResource(R.string.ui_label_loading_failed,errorMessage)}",
                )
                // 可以添加一个重试按钮
            }

            is FileConnectionStatus.FilesLoaded-> {
                if (fileList.isEmpty()) {
                    FileEmptyScreen(stringResource(R.string.ui_label_directory_empty))

                }else{
                    // 已连接，显示文件列表
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxHeight()
                                .weight(0.7f)
                        ) {

                            when {
                                filteredFiles.isEmpty() && seaText.isNotBlank() -> {
                                    item {
                                        NoSearchResult(text = "${stringResource(R.string.ui_label_no_match_truncated)} \"$seaText\" ${
                                            stringResource(
                                                R.string.ui_label_files_suffix
                                            )
                                        }")
                                    }
                                }
                                else -> items(filteredFiles)
                                { file ->
                                    // 假设有一个类似 FTPFile 的 NFSFile 类，或使用通用文件信息类
                                    // 这里假设 file 有 isDirectory: Boolean 和 name: String? 属性
                                    val isDirectory = file.isDirectory
                                    val fileName = file.name ?: "Unknown"
                                    ListItem(
                                        selected = false,
                                        onClick = {
                                            coroutineScope.launch {
                                                val newSubPath = file.path

                                                val encodedNewSubPath = newSubPath.ifEmpty { " " }.toBase64()
                                                if (isDirectory) {
                                                    // 构建新的子路径

//                                                Log.d(
//                                                    "NFSFileListScreen",
//                                                    "Navigating to subdirectory: ${file.path} (encoded: $fileName$fileName)"
//                                                )
                                                    val encodedIp = (nfsConnection.serverAddress ?: "").toBase64()
                                                    val encodedShareName = (nfsConnection.shareName ?: "").toBase64()
                                                    val encodedName = (nfsConnection.name ?: "").toBase64()

                                                    // 导航到子目录，传递连接信息和新的子路径
                                                    navController.navigate(
                                                        "NFSFileListScreen/$encodedIp/$encodedShareName/$encodedNewSubPath/$encodedName"
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
                                                    val fullFileUrl = "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${newSubPath.ifEmpty { " " }}"
                                                    val encodedFileUrl = fullFileUrl.toBase64()
                                                    val encodedFileName = file.name.toBase64()
                                                    val encodedConnectionName = (nfsConnection.name ?: "").toBase64()

                                                    if (Tools.containsVideoFormat(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        )
                                                    ) {
                                                        // 构建视频播放列表
                                                        val videoFiles = filteredFiles.filter { nfsFile ->
                                                            !nfsFile.isDirectory && Tools.containsVideoFormat(Tools.extractFileExtension(nfsFile.name))
                                                        }
                                                        val videoItems = videoFiles.map { nfsFile ->
                                                            VideoItem(
                                                                uri = "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${nfsFile.path}",
                                                                fileName = nfsFile.name,
                                                                dataSourceType = "NFS",
                                                                connectionName = nfsConnection.name ?: ""
                                                            )
                                                        }
                                                        VideoPlaylistRepository.setPlaylist(videoItems)

                                                        // 检查是否有媒体信息（mediaId > 0, 且是焦点文件, 且未隐藏详情）
                                                        if (mediaId > 0 && focusedFileName == file.name && !settingsState.hideDetails) {
                                                            // 导航到视频播放器
                                                            val mediaInfoFN =
                                                                MediaInfoExtractorFormFileName.extract(
                                                                    file.name
                                                                )
                                                            val route =
                                                                if (mediaInfoFN.mediaType == "movie") {
                                                                    "MovieDetails/$encodedFileUrl/NFS/$encodedFileName/$encodedConnectionName/$mediaId"
                                                                } else {
                                                                    // 注意：这里假设 season/episode 可以安全地转换为 Int
                                                                    "TVSeriesDetails/$encodedFileUrl/NFS/$encodedFileName/$encodedConnectionName/$mediaId/${mediaInfoFN.season.toInt()}/${mediaInfoFN.episode.toInt()}"
                                                                }
                                                            navController.navigate(route)
                                                        } else {
                                                        // 没有电影信息，直接播放
                                                        navController.navigate("VideoPlayer/$encodedFileUrl/NFS/$encodedFileName/$encodedConnectionName")
                                                    }
                                                    } else if (Tools.containsAudioFormat(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        )
                                                    ) {
                                                        //  构建音频文件列表
                                                        val audioFiles =
                                                            fileList.filter { nfsFile ->
                                                                Tools.containsAudioFormat(
                                                                    Tools.extractFileExtension(
                                                                        nfsFile.name
                                                                    )
                                                                )
                                                            }

                                                        //  构建文件名到索引的映射（O(N) 一次构建）
                                                        val nameToIndexMap = audioFiles.withIndex()
                                                            .associateBy(
                                                                { it.value.name },
                                                                { it.index })

                                                        //  快速查找索引（O(1)）
                                                        val currentAudioIndex =
                                                            nameToIndexMap[file.name] ?: -1
                                                        if (currentAudioIndex == -1) {
                                                            Log.e(
                                                                "FTPFileListScreen",
                                                                "未找到文件在音频列表中: ${file.name}"
                                                            )
                                                            return@launch

                                                        }


                                                        val audioItems = audioFiles.map { nfsFile ->
                                                            AudioItem(
                                                                uri = "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${nfsFile.path}",
                                                                fileName = nfsFile.name,
                                                                dataSourceType = "HTTP"
                                                            )
                                                        }
                                                        // 设置数据
                                                        AudioPlaylistRepository.setPlaylist(
                                                            audioItems
                                                        )
                                                        navController.navigate(
                                                            "AudioPlayer/$encodedFileUrl/NFS/$encodedFileName/$encodedConnectionName/$currentAudioIndex"
                                                        )
                                                    } else if (Tools.containsImageFileExtension(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        )
                                                    ) {
                                                        navController.navigate(
                                                            "PicViewer/$encodedFileUrl/NFS/$encodedFileName/$encodedConnectionName"
                                                        )
                                                    } else {
                                                        showToast(
                                                            context,
                                                            context.getString(R.string.ui_label_unsupported_file_format,"test")
                                                        )
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
                                                    mediaId = -1
                                                    focusedIsVideo =
                                                        Tools.containsVideoFormat(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        )
                                                    focusedMediaUri = "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${file.path.ifEmpty { " " }}"
                                                }

                                            },
                                        scale = ListItemDefaults.scale(
                                            scale = 1.0f,
                                            focusedScale = 1.01f
                                        ),
                                        leadingContent = {
                                            val fileExtension =
                                                Tools.extractFileExtension(file.name)
                                            FileIcon(isDirectory, fileExtension)
                                        },
                                        headlineContent = {
                                            FileName(fileName)
                                        },
                                        trailingContent = {
                                            // 只有文件才显示大小，目录可以留空或显示项数
                                            FileSize(file.isDirectory, file.length())
                                        }
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.3f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        )
                        {
                            // 搜索框放在最上面
                            TvTextField(
                                value = seaText,
                                onValueChange = { seaText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                colors = myTTFColor(),
                                placeholder =stringResource(R.string.ui_label_please_enter_filename),
                                textStyle = TextStyle(color = Color.White),
                            )
                            // 2. 中间的海报和文字区域（包裹在一个 Column 里）
                            MediaPreviewSection(
                                focusedMovie = focusedMovie,
                                focusedFileName = focusedFileName,
                                focusedIsDir = focusedIsDir,
                                modifier = Modifier.weight(1f),
                                onMediaIdResolved = { id ->
                                    mediaId = id // 更新父组件持有的状态，供 ListItem 点击逻辑使用
                                }
                            )
                            // 3. 底部的进度和按钮区域
                            // 不再嵌套在上面的 Column 里，而是直接放在最外层 Column 的底部
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp), // 距离底部边缘一点间距
                                horizontalAlignment = Alignment.CenterHorizontally
                            )
                            {
                                // 进度显示区：固定高度 30.dp 左右，避免布局跳动

                                // 进度显示区：固定高度 30.dp 左右，避免布局跳动
                                Box(
                                    modifier = Modifier.height(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val progressText = when {
                                        isScanning -> if (totalScanCount > 0) "${stringResource(R.string.ui_label_getting_video_info)} $currentScanIndex/$totalScanCount" else stringResource(R.string.ui_label_preparing_video_scan)
                                        isAudioScanning -> stringResource(R.string.ui_label_parsing_music_filename)
                                        else -> null // 返回 null 不显示
                                    }
                                    progressText?.let {
                                        Text(text = it, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
// 按钮行
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                )
                                {
                                    // 按钮行
                                    // --- 视频扫描按钮 ---
                                    CirCleIconButton(
                                        icon = painterResource(R.drawable.videoadd24dp),
                                        // 动态显示 tooltip 内容
                                        tooltip = if (isScanning && totalScanCount > 0)
                                            "${stringResource(R.string.ui_label_getting_info)} $currentScanIndex/$totalScanCount"
                                        else stringResource(R.string.ui_label_bulk_add_to_video_library),
                                        onClick = {
                                            if (!settingsState.nfs) {
                                                showToast(
                                                    context,
                                                    context.getString(R.string.ui_label_scraping_not_enabled)
                                                )
                                            } else {
                                                // 1. 获取要扫描的视频文件列表
                                                coroutineScope.launch {
                                                    val scanList = if (settingsState.recursiveScanLevel > 0) {
                                                        viewModel.scanVideosRecursive(
                                                            path = sharePath,
                                                            maxDepth = settingsState.recursiveScanLevel
                                                        )
                                                    } else {
                                                        // 非递归，只取当前层级
                                                        fileList.filter { file ->
                                                            !file.isDirectory &&
                                                                    Tools.containsVideoFormat(
                                                                        Tools.extractFileExtension(
                                                                            file.name
                                                                        )
                                                                    )
                                                        }.map { file ->
                                                            file.name to "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${file.path.ifEmpty { " " }}"
                                                        }
                                                    }

                                                    if (scanList.isEmpty()) {
                                                        showToast(
                                                            context,
                                                            context.getString(R.string.ui_label_no_video_files_in_directory)
                                                        )
                                                    } else {
                                                        // 3. 调用 ViewModel 开始后台任务
                                                        showToast(
                                                            context,
                                                            context.getString(R.string.ui_label_start_background_info_retrieval)
                                                        )
                                                        movieViewModel.batchScrapeVideoInfo(
                                                            videoList = scanList,
                                                            dataSourceType = "NFS",
                                                            connectionName = nfsConnection.name
                                                                ?: "未知连接"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    // --- 音乐扫描按钮 ---
                                    CirCleIconButton(
                                        icon = painterResource(R.drawable.musicnoteadd_24dp),
                                        tooltip = if (isAudioScanning) stringResource(R.string.ui_label_parsing_filename) else  stringResource(R.string.ui_label_bulk_add_to_music_library),
                                        onClick = {
                                            if (!settingsState.nfs) {
                                                showToast(
                                                    context,
                                                    context.getString(R.string.ui_label_scraping_not_enabled)
                                                )
                                            } else {
                                                // 1. 过滤音频文件
                                                val audioFiles = fileList.filter {
                                                    !it.isDirectory && Tools.containsAudioFormat(
                                                        Tools.extractFileExtension(
                                                            it.name
                                                        )
                                                    )
                                                }

                                                if (audioFiles.isEmpty()) {
                                                    showToast(
                                                        context,
                                                        context.getString(R.string.ui_label_no_audio_files_found)
                                                    )
                                                    return@CirCleIconButton
                                                }

                                                // 2. 只有文件名和URI是必须的
                                                val list = audioFiles.map {
                                                    it.name to "nfs://${nfsConnection.serverAddress}:${nfsConnection.shareName}:${it.path.ifEmpty { " " }}"
                                                }

                                                // 3. 直接调用，瞬间完成
                                                audioViewModel.batchScrapeAudioInfo(
                                                    audioList = list,
                                                    dataSourceType = "NFS",
                                                    connectionName = nfsConnection.name
                                                        ?: "未知连接"
                                                )
                                                showToast(
                                                    context,
                                                    context.getString(R.string.ui_label_added_music_in_background,list.size)
                                                )
                                            }
                                        }
                                    )
                                }

                            }
                        }
                    }   }
            }
            else  -> {
                LoadingScreen(
                    stringResource(R.string.ui_label_loading_nfs_files),
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }
}



