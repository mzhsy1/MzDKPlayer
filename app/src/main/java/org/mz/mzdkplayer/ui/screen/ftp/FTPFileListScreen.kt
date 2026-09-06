package org.mz.mzdkplayer.ui.screen.ftp

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
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import org.mz.mzdkplayer.tool.Tools.toBase64
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
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.FileConnectionStatus
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
import org.mz.mzdkplayer.ui.screen.common.MzToast
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.common.rememberMzToastState
import org.mz.mzdkplayer.ui.screen.vm.FTPConViewModel


import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.vm.AudioViewModel
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import java.net.URLEncoder
import kotlin.text.ifEmpty
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun FTPFileListScreen(
    // path 现在是相对于 FTP 共享根目录的路径
    path: String?, // e.g., "folder1/subfolder"
    navController: NavHostController,
    ftpConnection: FTPConnection,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 使用 Hilt 注入 ViewModel
    val viewModel: FTPConViewModel = viewModel()

    // 收集 ViewModel 中的状态
    val toastState = rememberMzToastState()
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
    val movieViewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }// 新增：获取MovieViewModel
    // 新增：电影信息状态
    val focusedMovie by movieViewModel.focusedMovie.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val isScanning by movieViewModel.isScanning.collectAsState()
    val currentScanIndex by movieViewModel.currentScanIndex.collectAsState() // 新增：引入当前进度
    val totalScanCount by movieViewModel.totalScanCount.collectAsState() // 新增：引入总数
    var mediaId by remember { mutableIntStateOf(-1) }
    var seaText by remember { mutableStateOf("") }
    var focusedIsVideo by remember { mutableStateOf(false) }
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
    val audioViewModel: AudioViewModel = viewModelWithFactory {
        RepositoryProvider.createAudioViewModel() // 不需要 context 了
    }
    val isAudioScanning by audioViewModel.isScanning.collectAsState()
    // 当传入的 path 参数变化时，或者首次进入时，尝试加载文件列表
    LaunchedEffect(path, connectionStatus) { // 依赖 path
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
                delay(300.milliseconds)
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
                toastState.show("${context.getString(R.string.ui_label_ftp_error)} $errorMessage", coroutineScope)
            }

            else -> {}
        }
    }
// 处理焦点变化和媒体播放
    LaunchedEffect(focusedFileName, focusedIsDir, focusedIsVideo,settingsState.ftp) {
        // 1. 基础校验：如果是目录、无文件名、或者不是视频，直接清空信息
        if (focusedFileName == null || focusedIsDir || !focusedIsVideo) {
            movieViewModel.clearFocusedMovie()
            return@LaunchedEffect
        }
        // settingsState.ftp 为 false 代表 "禁止自动刮削/仅本地数据" (防止重复入库)
        if (settingsState.ftp) {
            // === 模式 A：自动刮削 (主数据源) ===
            // 场景：这是用户的主要观看路径，允许自动联网获取信息。
            Log.d("FTPFileListScreen", "自动模式: 触发搜索/刮削: $focusedFileName")
            movieViewModel.searchFocusedMovie(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "FTP",
                connectionName = ftpConnection.name?:context.getString(R.string.ui_label_unknown_connection)
            )

        } else {
            // === 模式 B：仅查询数据库 (防重复) ===
            // 场景：用户不希望此协议自动产生新数据，但如果之前"手动批量扫描"过，这里应该显示出来。
            Log.d("FTPFileListScreen", "禁止自动刮削模式: 仅查询数据库: $focusedFileName")
            movieViewModel.getFocusedInfo(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "FTP",
                connectionName = ftpConnection.name?:context.getString(R.string.ui_label_unknown_connection)
            )
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
                    "${stringResource(R.string.ui_label_loading_failed)} $errorMessage",
                )
            }

            is FileConnectionStatus.FilesLoaded -> {
                if (fileList.isEmpty()) {
                    FileEmptyScreen(stringResource(R.string.ui_label_directory_empty))

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
                                        NoSearchResult(text = "${stringResource(R.string.ui_label_no_match_truncated)} \"$seaText\" ${
                                            stringResource(
                                                R.string.ui_label_files_suffix
                                            )
                                        }")
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
                                                    // --- 目录处理 ---
                                                    if (isDirectory) {
                                                        // 构建子目录路径
                                                        val newPath = if (path.isNullOrEmpty() || path == "/") {
                                                            // 根目录或空路径直接使用文件名
                                                            fileName
                                                        } else {
                                                            // 当前路径 + 文件名，确保路径分隔符正确
                                                            "${path.trimEnd('/')}/$fileName"
                                                        }

                                                        val encodedNewPath = newPath.ifEmpty { " " }.toBase64()
                                                        val encodedIp = (ftpConnection.ip ?: "").toBase64()
                                                        val encodedUsername = (ftpConnection.username ?: "").toBase64()
                                                        val encodedPassword = (ftpConnection.password ?: "").toBase64()
                                                        val encodedConnName = (ftpConnection.name ?: "").toBase64()

                                                        Log.d(
                                                            "FTPFileListScreen",
                                                            "Navigating to subdirectory: $newPath (encoded: $encodedNewPath)"
                                                        )
                                                        // 导航到子目录，传递连接信息
                                                        navController.navigate("FTPFileListScreen/$encodedIp/$encodedUsername/$encodedPassword/${ftpConnection.port}/$encodedNewPath/$encodedConnName")

                                                    } else {
                                                        // --- 文件点击处理：提取公共编码变量 ---

                                                        val fullFileUrl = viewModel.getResourceFullUrl(fileName)
                                                        val fileExtension = Tools.extractFileExtension(fileName)

                                                        // 统一处理 URL、文件名、连接名的编码和错误
                                                        val encodedFileUrl = fullFileUrl.toBase64()
                                                        val encodedFileName = fileName.toBase64()
                                                        val encodedConnectionName = (ftpConnection.name ?: "").toBase64()

                                                        when {

                                                            Tools.containsVideoFormat(fileExtension) -> {
                                                                // 构建视频播放列表
                                                                val videoFiles = filteredFiles.filter { ftpFile ->
                                                                    !ftpFile.isDirectory && Tools.containsVideoFormat(Tools.extractFileExtension(ftpFile.name))
                                                                }
                                                                val videoItems = videoFiles.map { ftpFile ->
                                                                    VideoItem(
                                                                        uri = viewModel.getResourceFullUrl(ftpFile.name),
                                                                        fileName = ftpFile.name,
                                                                        dataSourceType = "FTP",
                                                                        connectionName = ftpConnection.name ?: ""
                                                                    )
                                                                }
                                                                VideoPlaylistRepository.setPlaylist(videoItems)

                                                                Log.d(
                                                                    "FTPFileListScreen",
                                                                    "movieId:$mediaId"
                                                                ) // 假设 mediaId 在外部作用域

                                                                // 检查是否有媒体信息（mediaId > 0, 且是焦点文件, 且未隐藏详情）
                                                                if (mediaId > 0 && focusedFileName == file.name && !settingsState.hideDetails) {
                                                                    val mediaInfoFN =
                                                                        MediaInfoExtractorFormFileName.extract(
                                                                            file.name
                                                                        )
                                                                    val route =
                                                                        if (mediaInfoFN.mediaType == "movie") {
                                                                            "MovieDetails/$encodedFileUrl/FTP/$encodedFileName/${encodedConnectionName}/$mediaId"
                                                                        } else {
                                                                            // 注意：这里假设 season/episode 可以安全地转换为 Int
                                                                            "TVSeriesDetails/$encodedFileUrl/FTP/$encodedFileName/${encodedConnectionName}/$mediaId/${mediaInfoFN.season.toInt()}/${mediaInfoFN.episode.toInt()}"
                                                                        }
                                                                    navController.navigate(route)
                                                                } else {
                                                                    // 没有电影信息，直接播放
                                                                    navController.navigate(
                                                                        "VideoPlayer/$encodedFileUrl/FTP/$encodedFileName/$encodedConnectionName"
                                                                    )
                                                                }
                                                                // 导航到视频播放器

                                                            }

                                                            Tools.containsAudioFormat(fileExtension) -> {
                                                                // 构建音频文件列表（只包含音频文件）
                                                                val audioFiles =
                                                                    fileList.filter { ftpFile ->
                                                                        !ftpFile.isDirectory && Tools.containsAudioFormat(
                                                                            Tools.extractFileExtension(ftpFile.name)
                                                                        )
                                                                    }

                                                                // 快速查找索引
                                                                val currentAudioIndex =
                                                                    audioFiles.withIndex()
                                                                        .firstOrNull { it.value.name == fileName }
                                                                        ?.index ?: -1

                                                                if (currentAudioIndex == -1) {
                                                                    Log.e("FTPFileListScreen", "未找到文件在音频列表中: $fileName")
                                                                    return@launch
                                                                }

                                                                // 构建播放列表
                                                                val audioItems =
                                                                    audioFiles.map { ftpFile ->
                                                                        AudioItem(
                                                                            uri = viewModel.getResourceFullUrl(ftpFile.name),
                                                                            fileName = ftpFile.name,
                                                                            dataSourceType = "FTP"
                                                                        )
                                                                    }

                                                                // 设置数据到 Repository
                                                                AudioPlaylistRepository.setPlaylist(audioItems)

                                                                // 导航到音频播放器，带上播放列表索引
                                                                navController.navigate(
                                                                    "AudioPlayer/$encodedFileUrl/FTP/$encodedFileName/$encodedConnectionName/$currentAudioIndex"
                                                                )
                                                            }

                                                            Tools.containsImageFileExtension(fileExtension) -> {
                                                                navController.navigate(
                                                                    "PicViewer/$encodedFileUrl/FTP/$encodedFileName/$encodedConnectionName"
                                                                )
                                                            }

                                                            else -> {
                                                                toastState.show(
                                                                    context.getString(R.string.ui_label_unsupported_file_format,fileExtension),
                                                                    coroutineScope
                                                                )
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
                                                        mediaId = -1
                                                        focusedMediaUri = viewModel.getResourceFullUrl(fileName)
                                                        focusedIsVideo =
                                                            Tools.containsVideoFormat(
                                                                Tools.extractFileExtension(
                                                                    file.name
                                                                ))

                                                    }
                                                },
                                            scale = ListItemDefaults.scale(
                                                scale = 1.0f,
                                                focusedScale = 1.01f
                                            ),
                                            leadingContent = {
                                                val fileExtension = Tools.extractFileExtension(file.name)
                                                FileIcon(isDirectory,fileExtension)
                                            },
                                            headlineContent = {
                                                FileName(fileName)
                                            },
                                            trailingContent = {
                                                // 只有文件才显示大小，目录可以留空或显示项数
                                                FileSize(file.isDirectory,file.size)
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
                            verticalArrangement = Arrangement.Top
                        ) {
                            TvTextField(
                                value = seaText,
                                onValueChange = { seaText = it },
                                modifier = Modifier.fillMaxWidth(),
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
                                ) {
                                    // 按钮行
                                    // --- 视频扫描按钮 ---
                                    CirCleIconButton(
                                        icon = painterResource(R.drawable.videoadd24dp),
                                        // 动态显示 tooltip 内容
                                        tooltip = if (isScanning && totalScanCount > 0)
                                            "${stringResource(R.string.ui_label_getting_info)} $currentScanIndex/$totalScanCount"
                                        else stringResource(R.string.ui_label_bulk_add_to_video_library),
                                        onClick = {
                                            if (!settingsState.ftp) {
                                                toastState.show(
                                                    context.getString(R.string.ui_label_scraping_not_enabled),
                                                    coroutineScope
                                                )
                                            } else {
                                                // 1. 获取要扫描的视频文件列表
                                                coroutineScope.launch {
                                                    val scanList = if (settingsState.recursiveScanLevel > 0) {
                                                        viewModel.scanVideosRecursive(
                                                            path = path ?: "",
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
                                                            file.name to viewModel.getResourceFullUrl(file.name)
                                                        }
                                                    }

                                                    if (scanList.isEmpty()) {
                                                        toastState.show(
                                                            context.getString(R.string.ui_label_no_video_files_in_directory),
                                                            coroutineScope
                                                        )
                                                    } else {
                                                        // 3. 调用 ViewModel 开始后台任务
                                                        toastState.show(
                                                            context.getString(R.string.ui_label_start_background_info_retrieval),
                                                            coroutineScope
                                                        )
                                                        movieViewModel.batchScrapeVideoInfo(
                                                            videoList = scanList,
                                                            dataSourceType = "FTP",
                                                            connectionName = ftpConnection.name
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
                                            if (!settingsState.ftp) {
                                                toastState.show(
                                                    context.getString(R.string.ui_label_scraping_not_enabled),
                                                    coroutineScope
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
                                                    toastState.show(
                                                        context.getString(R.string.ui_label_no_audio_files_found),
                                                        coroutineScope
                                                    )
                                                    return@CirCleIconButton
                                                }

                                                // 2. 只有文件名和URI是必须的
                                                val list = audioFiles.map {
                                                    it.name to viewModel.getResourceFullUrl(it.name)
                                                }

                                                // 3. 直接调用，瞬间完成
                                                audioViewModel.batchScrapeAudioInfo(
                                                    audioList = list,
                                                    dataSourceType = "FTP",
                                                    connectionName = ftpConnection.name
                                                        ?: "未知连接"
                                                )
                                                toastState.show(
                                                    context.getString(R.string.ui_label_added_music_in_background,list.size),
                                                    coroutineScope
                                                )
                                            }
                                        }
                                    )
                                }

                            }
                        }
                    }
                }

            }

            else -> { // 显示加载指示器
                LoadingScreen(
                    stringResource(R.string.ui_label_connecting_to_ftp_server), Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
        MzToast(state = toastState)
    }
}



