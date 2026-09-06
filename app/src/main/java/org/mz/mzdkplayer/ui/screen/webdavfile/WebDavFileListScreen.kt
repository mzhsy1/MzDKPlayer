

package org.mz.mzdkplayer.ui.screen.webdavfile

import NoSearchResult
import android.util.Log
import org.mz.mzdkplayer.ui.screen.common.MzToastManager
import androidx.annotation.OptIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.ListItem
import org.mz.mzdkplayer.tool.Tools.toBase64
import androidx.tv.material3.ListItemDefaults
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.vm.WebDavConViewModel
import java.net.URLEncoder
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.MediaInfoExtractorFormFileName
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
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.vm.AudioViewModel
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun WebDavFileListScreen(
    // path 现在是完整的 WebDAV URL 路径
    path: String?, // e.g., "https://192.168.1.4:5006/folder1/subfolder"
    navController: NavHostController,
    webDavConnection: WebDavConnection,
    settingsViewModel: SettingsViewModel =viewModel()
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
    var focusedIsVideo by remember { mutableStateOf(false) }
    val movieViewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }// 新增：获取MovieViewModel
    // 新增：电影信息状态
    val focusedMovie by movieViewModel.focusedMovie.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    var mediaId by remember { mutableIntStateOf(-1) }
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
    val isScanning by movieViewModel.isScanning.collectAsState()
    val currentScanIndex by movieViewModel.currentScanIndex.collectAsState() // 新增：引入当前进度
    val totalScanCount by movieViewModel.totalScanCount.collectAsState() // 新增：引入总数
    val audioViewModel: AudioViewModel = viewModelWithFactory {
        RepositoryProvider.createAudioViewModel() // 不需要 context 了
    }
    val isAudioScanning by audioViewModel.isScanning.collectAsState()
    // 当传入的 path 参数变化时，或者首次进入时，尝试加载文件列表
    LaunchedEffect(path, connectionStatus) {
        Log.d(
            "WebDavFileListScreen",
            "LaunchedEffect triggered with path: $path, status: $connectionStatus"
        )

        when (connectionStatus) {
            is FileConnectionStatus.Connected -> {
                delay(300.milliseconds)
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
                MzToastManager.show(context.getString(R.string.ui_label_webdav_error,errorMessage))
            }

            else -> {}
        }
    }
    LaunchedEffect(focusedFileName, focusedIsDir, focusedIsVideo,settingsState.webdav) {
        // 1. 基础校验：如果是目录、无文件名、或者不是视频，直接清空信息
        if (focusedFileName == null || focusedIsDir || !focusedIsVideo) {
            movieViewModel.clearFocusedMovie()
            return@LaunchedEffect
        }
        // 2. 根据设置决定策略
        // settingsState.webdav 为 false 代表 "禁止自动刮削/仅本地数据" (防止重复入库)
        if (settingsState.webdav) {
            // === 模式 A：自动刮削 (主数据源) ===
            // 场景：这是用户的主要观看路径，允许自动联网获取信息。
            Log.d("WebDavFileListScreen", "自动模式: 触发搜索/刮削: $focusedFileName")
            movieViewModel.searchFocusedMovie(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "WEBDAV",
                connectionName=webDavConnection.name?:"未知连接"
            )

        } else {
            // === 模式 B：仅查询数据库 (防重复) ===
            // 场景：用户不希望此协议自动产生新数据，但如果之前"手动批量扫描"过，这里应该显示出来。
            Log.d("WebDavFileListScreen", "禁止自动刮削模式: 仅查询数据库: $focusedFileName")
            movieViewModel.getFocusedInfo(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "WEBDAV",
                connectionName=webDavConnection.name?:"未知连接"
            )
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
                VAErrorScreen(
                    "${stringResource(R.string.ui_label_loading_failed,errorMessage)}",
                )
            }

            is FileConnectionStatus.FilesLoaded -> {
                if (fileList.isEmpty()) {
                    FileEmptyScreen(stringResource(R.string.ui_label_directory_empty))
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
                                        NoSearchResult(text = "${stringResource(R.string.ui_label_no_match_truncated)} \"$seaText\" ${
                                            stringResource(
                                                R.string.ui_label_files_suffix
                                            )
                                        }")
                                    }
                                }

                                else -> {
                                    items(filteredFiles) { file ->
                                        val isDirectory = file.isDirectory
                                        val fileName = file.name
                                        // 处理文件点击
                                        val fileExtension = Tools.extractFileExtension(file.name)
                                        val fullFileUrl = path ?: "" // 直接使用文件的完整路径
                                        val authenticatedUrl = viewModel.buildAuthenticatedUrl(fullFileUrl,
                                            username = webDavConnection.username?:""
                                            , password = webDavConnection.password?:"").trimEnd('/')
                                        val encodedFileUrl = "${authenticatedUrl}/${fileName.trimEnd('/').trimStart('/')}".toBase64()
                                        val encodedFileName = fileName.toBase64()
                                        val encodedConnectionName = (webDavConnection.name ?: "").toBase64()
                                        ListItem(
                                            selected = false,
                                            onClick = {
                                                coroutineScope.launch {
                                                    if (isDirectory) {
                                                        val rawNewPath = "${path?.trimEnd('/') ?:""}/${fileName.trimEnd('/').trimStart('/')}/"
                                                        val encodedNewPath = rawNewPath.toBase64()
                                                        val encodedUsername = (webDavConnection.username ?: "").toBase64()
                                                        val encodedPassword = (webDavConnection.password ?: "").toBase64()
                                                        val encodedName = (webDavConnection.name ?: "").toBase64()

                                                        Log.d(
                                                            "WebDavFileListScreen",
                                                            "Navigating to subdirectory: $path to $rawNewPath"
                                                        )
                                                        navController.navigate("WebDavFileListScreen/$encodedNewPath/$encodedUsername/$encodedPassword/$encodedName")
                                                    } else {
                                                        when {
                                                            Tools.containsVideoFormat(fileExtension) -> {
                                                                // 构建视频播放列表
                                                                val videoFiles = filteredFiles.filter { webdavFile ->
                                                                    !webdavFile.isDirectory && Tools.containsVideoFormat(Tools.extractFileExtension(webdavFile.name))
                                                                }
                                                                val videoItems = videoFiles.map { webdavFile ->
                                                                    val videoUrl = "${authenticatedUrl}/${webdavFile.name.trimEnd('/').trimStart('/')}"
                                                                    VideoItem(
                                                                        uri = videoUrl,
                                                                        fileName = webdavFile.name,
                                                                        dataSourceType = "WEBDAV",
                                                                        connectionName = webDavConnection.name ?: ""
                                                                    )
                                                                }
                                                                VideoPlaylistRepository.setPlaylist(videoItems)

                                                                Log.d(
                                                                    "WebDavFileListScreen",
                                                                    file.path
                                                                )
                                                                if (mediaId > 0 && focusedFileName == file.name && !settingsState.hideDetails) {
                                                                    val mediaInfoFN =
                                                                        MediaInfoExtractorFormFileName.extract(
                                                                            file.name
                                                                        )
                                                                    if (mediaInfoFN.mediaType == "movie") {
                                                                        navController.navigate("MovieDetails/$encodedFileUrl/WEBDAV/$encodedFileName/$encodedConnectionName/$mediaId")
                                                                    } else {
                                                                        navController.navigate("TVSeriesDetails/$encodedFileUrl/WEBDAV/$encodedFileName/$encodedConnectionName/$mediaId/${mediaInfoFN.season.toInt()}/${mediaInfoFN.episode.toInt()}")
                                                                    }
                                                                } else {
                                                                    navController.navigate(
                                                                        "VideoPlayer/$encodedFileUrl/WEBDAV/$encodedFileName/$encodedConnectionName"
                                                                    )
                                                                }
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

                                                                AudioPlaylistRepository.setPlaylist(
                                                                    audioItems
                                                                )

                                                                navController.navigate(
                                                                    "AudioPlayer/$encodedFileUrl/WEBDAV/$encodedFileName/$encodedConnectionName/$currentAudioIndex"
                                                                )
                                                            }
                                                            Tools.containsImageFileExtension(fileExtension) -> {
                                                                navController.navigate(
                                                                    "PicViewer/$encodedFileUrl/WEBDAV/$encodedFileName/$encodedConnectionName"
                                                                )
                                                            }

                                                            else -> {
                                                                MzToastManager.show(
                                                                    context.getString(R.string.ui_label_unsupported_file_format,fileExtension)
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
                                                        focusedFileName = file.name
                                                        focusedIsDir = file.isDirectory
                                                        mediaId = -1
                                                        focusedIsVideo = Tools.containsVideoFormat(
                                                            Tools.extractFileExtension(
                                                                file.name
                                                            )
                                                        )
                                                        focusedMediaUri = "${authenticatedUrl}/${
                                                            fileName.trimEnd('/').trimStart('/')
                                                        }"
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
                            verticalArrangement = Arrangement.Center
                        ) {
                            TvTextField(
                                value = seaText,
                                onValueChange = { seaText = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = myTTFColor(),
                                placeholder =stringResource(R.string.ui_label_please_enter_filename),
                                textStyle = TextStyle(color = Color.White),
                            )
                            // 添加弹性空间，让海报区域在垂直方向上居中
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
                                    val fullFileUrl = path ?: "" // 直接使用文件的完整路径
                                    val authenticatedUrl =viewModel.buildAuthenticatedUrl(fullFileUrl,
                                        username = webDavConnection.username?:""
                                        , password = webDavConnection.password?:"").trimEnd('/')
                                    CirCleIconButton(
                                        icon = painterResource(R.drawable.videoadd24dp),
                                        // 动态显示 tooltip 内容
                                        tooltip = if (isScanning && totalScanCount > 0)
                                            "${stringResource(R.string.ui_label_getting_info)} $currentScanIndex/$totalScanCount"
                                        else stringResource(R.string.ui_label_bulk_add_to_video_library),
                                        onClick = {
                                            if (!settingsState.webdav) {
                                                MzToastManager.show(
                                                    context.getString(R.string.ui_label_scraping_not_enabled)
                                                )
                                            } else {
                                                // 1. 获取要扫描的视频文件列表
                                                coroutineScope.launch {
                                                    val scanList = if (settingsState.recursiveScanLevel > 0) {
                                                        viewModel.scanVideosRecursive(
                                                            fullPath = path ?: "",
                                                            maxDepth = settingsState.recursiveScanLevel,
                                                            username = webDavConnection.username,
                                                            password = webDavConnection.password
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
                                                            val uri = "${authenticatedUrl}/${
                                                                file.name.trimEnd('/').trimStart('/')
                                                            }"
                                                            file.name to uri
                                                        }
                                                    }

                                                    if (scanList.isEmpty()) {
                                                        MzToastManager.show(
                                                            context.getString(R.string.ui_label_no_video_files_in_directory)
                                                        )
                                                    } else {
                                                        // 3. 调用 ViewModel 开始后台任务
                                                        MzToastManager.show(
                                                            context.getString(R.string.ui_label_start_background_info_retrieval)
                                                        )
                                                        movieViewModel.batchScrapeVideoInfo(
                                                            videoList = scanList,
                                                            dataSourceType = "WEBDAV",
                                                            connectionName = webDavConnection.name
                                                                ?: "未知连接名"
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
                                            if (!settingsState.webdav) {
                                                MzToastManager.show(
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
                                                    MzToastManager.show(
                                                        context.getString(R.string.ui_label_no_audio_files_found)
                                                    )
                                                    return@CirCleIconButton
                                                }

                                                // 2. 只有文件名和URI是必须的
                                                val list = audioFiles.map {
                                                    it.name to "${authenticatedUrl}/${
                                                        it.name.trimEnd('/').trimStart('/')
                                                    }"
                                                }

                                                // 3. 直接调用，瞬间完成
                                                audioViewModel.batchScrapeAudioInfo(
                                                    audioList = list,
                                                    dataSourceType = "WEBDAV",
                                                    connectionName = webDavConnection.name
                                                        ?: "未知连接名"
                                                )
                                                MzToastManager.show(
                                                    context.getString(R.string.ui_label_added_music_in_background,list.size)
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

            else -> {
                LoadingScreen(
                    stringResource(R.string.ui_label_loading_webdav_files),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}