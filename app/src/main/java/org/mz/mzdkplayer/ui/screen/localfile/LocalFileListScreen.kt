package org.mz.mzdkplayer.ui.screen.localfile

import NoSearchResult
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import org.mz.mzdkplayer.tool.Tools.toBase64
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.data.model.LocalFileLoadStatus
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
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.common.showToast


import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.vm.AudioViewModel
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun LocalFileListScreen(path: String?, navController: NavHostController, settingsViewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<File>() }
    var status by remember { mutableStateOf<LocalFileLoadStatus>(LocalFileLoadStatus.LoadingFile) }
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var seaText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val filteredFiles = remember(files, seaText) {
        if (seaText.isBlank()) {
            files
        } else {
            files.filter { file ->
                file.name.contains(seaText, ignoreCase = true)
            }
        }
    }
    val movieViewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }// 新增：获取MovieViewModel
    // 新增：电影信息状态
    val focusedMovie by movieViewModel.focusedMovie.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val isScanning by movieViewModel.isScanning.collectAsState()
    val currentScanIndex by movieViewModel.currentScanIndex.collectAsState() // 新增：引入当前进度
    val totalScanCount by movieViewModel.totalScanCount.collectAsState() // 新增：引入总数
    // 获取 AudioViewModel
    val audioViewModel: AudioViewModel = viewModelWithFactory {
        RepositoryProvider.createAudioViewModel() // 不需要 context 了
    }
    val decodedPath = path ?: ""
    val isAudioScanning by audioViewModel.isScanning.collectAsState()
    var mediaId by remember { mutableIntStateOf(-1) }
    var focusedIsVideo by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
    LaunchedEffect(path) {
        status = LocalFileLoadStatus.LoadingFile
        files.clear()
        delay(300.milliseconds)

        val decodedPath = path ?: ""

        if (decodedPath.isEmpty()) {
            status = LocalFileLoadStatus.Error(context.getString(R.string.ui_label_path_is_empty))
            return@LaunchedEffect
        }

        try {
            // 1. 尝试 MediaStore 查询
            val mediaStoreFiles = queryMediaStore(context, decodedPath)

            if (mediaStoreFiles.isNotEmpty()) {
                files.addAll(mediaStoreFiles)
                status = LocalFileLoadStatus.FilesLoaded
                return@LaunchedEffect
            }

            // 2. 降级到文件系统 API
            val dir = File(decodedPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.let {
                    files.addAll(it.toList())
                    status = LocalFileLoadStatus.FilesLoaded
                } ?: run {
                    status = LocalFileLoadStatus.Error(context.getString(R.string.ui_label_failed_to_read_directory_content))
                }
            } else {
                status = LocalFileLoadStatus.Error(context.getString(R.string.ui_label_directory_not_exist_or_unaccessible))
            }
        } catch (e: Exception) {
            Log.e("LocalFileListScreen", "加载文件失败", e)
            status = LocalFileLoadStatus.Error(e.message ?: context.getString(R.string.ui_label_unknown_error))
        }
    }
    // 处理焦点变化和媒体播放
    LaunchedEffect(focusedFileName, focusedIsDir, focusedIsVideo, settingsState.local) {
        // 1. 基础校验：如果是目录、无文件名、或者不是视频，直接清空信息
        if (focusedFileName == null || focusedIsDir || !focusedIsVideo) {
            movieViewModel.clearFocusedMovie()
            return@LaunchedEffect
        }

        // 2. 根据设置决定策略
        // settingsState.local 为 false 代表 "禁止自动刮削/仅本地数据" (防止重复入库)
        if (settingsState.local) {
            // === 模式 A：自动刮削 (主数据源) ===
            // 场景：这是用户的主要观看路径，允许自动联网获取信息。
            Log.d("LocalFileListScreen", "自动模式: 触发搜索/刮削: $focusedFileName")
            movieViewModel.searchFocusedMovie(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "LOCAL",
                connectionName = "本地文件"
            )

        } else {
            // === 模式 B：仅查询数据库 (防重复) ===
            // 场景：用户不希望此协议自动产生新数据，但如果之前"手动批量扫描"过，这里应该显示出来。
            Log.d("LocalFileListScreen", "禁止自动刮削模式: 仅查询数据库: $focusedFileName")
            movieViewModel.getFocusedInfo(
                movieName = focusedFileName,
                isDirectory = false,
                videoUri = focusedMediaUri,
                dataSourceType = "LOCAL",
                connectionName = "本地文件"
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
    {
        // 根据状态显示不同 UI
        when (status) {
            is LocalFileLoadStatus.LoadingFile -> {
                LoadingScreen(
                    stringResource(R.string.ui_label_loading_local_files), Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            is LocalFileLoadStatus.Error -> {
                val error = status as LocalFileLoadStatus.Error
                VAErrorScreen("${stringResource(R.string.ui_label_loading_failed)} ${error.message}",)
            }

            LocalFileLoadStatus.FilesLoaded -> {
                if (files.isEmpty()) {
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
                                // 目录本身为空（未搜索时）
                                else -> {
                                    items(filteredFiles) { file ->
                                        val isDirectory = file.isDirectory
                                        val fileName = file.name
                                        val fullPath = file.absolutePath // 获取文件的绝对路径
                                        // 本地文件 URI 格式通常是 file:///full/path/to/file
                                        val fullFileUri = "file://$fullPath"

                                        val encodedFileUri = fullFileUri.toBase64()
                                        val encodedFileName = fileName.toBase64()
                                        val encodedLocalFile = "本地文件".toBase64()

                                        ListItem(
                                            selected = false,
                                            onClick = {
                                                coroutineScope.launch {
                                                    // --- 统一编码和错误处理 ---

                                                    // 1. 尝试编码完整路径 (作为 URI 使用)

                                                    val fileExtension =
                                                        Tools.extractFileExtension(fileName)

                                                    when {
                                                        isDirectory -> {
                                                            // --- 目录点击处理 ---
                                                            // 对新的路径（完整路径）进行编码，用于导航
                                                            val encodedNewPath = fullPath.toBase64()

                                                            Log.d(
                                                                "LocalFileListScreen",
                                                                "Navigating to subdirectory: $fullPath"
                                                            )
                                                            // 导航到子目录
                                                            navController.navigate("LocalFileListScreen/$encodedNewPath")
                                                        }

                                                        Tools.containsVideoFormat(fileExtension) -> {
                                                            // 构建视频播放列表
                                                            val videoFiles = filteredFiles.filter { localFile ->
                                                                !localFile.isDirectory && Tools.containsVideoFormat(Tools.extractFileExtension(localFile.name))
                                                            }
                                                            val videoItems = videoFiles.map { localFile ->
                                                                VideoItem(
                                                                    uri = "file://${localFile.absolutePath}",
                                                                    fileName = localFile.name,
                                                                    dataSourceType = "LOCAL",
                                                                    connectionName = "本地文件"
                                                                )
                                                            }
                                                            VideoPlaylistRepository.setPlaylist(videoItems)

                                                            // 检查是否有媒体信息（mediaId > 0, 且是焦点文件, 且未隐藏详情）
                                                            if (mediaId > 0 && focusedFileName == file.name && !settingsState.hideDetails) {
                                                                val mediaInfoFN =
                                                                    MediaInfoExtractorFormFileName.extract(
                                                                        file.name
                                                                    )
                                                                val route =
                                                                    if (mediaInfoFN.mediaType == "movie") {
                                                                        "MovieDetails/$encodedFileUri/LOCAL/$encodedFileName/$encodedLocalFile/$mediaId"
                                                                    } else {
                                                                        // 注意：这里假设 season/episode 可以安全地转换为 Int
                                                                        "TVSeriesDetails/$encodedFileUri/LOCAL/$encodedFileName/$encodedLocalFile/$mediaId/${mediaInfoFN.season.toInt()}/${mediaInfoFN.episode.toInt()}"
                                                                    }
                                                                navController.navigate(route)
                                                            } else {
                                                                navController.navigate(
                                                                    "VideoPlayer/$encodedFileUri/LOCAL/$encodedFileName/$encodedLocalFile" // connectionName 留空
                                                                )
                                                            }
                                                        }

                                                        Tools.containsAudioFormat(fileExtension) -> {
                                                            // 构建音频文件列表（只包含音频文件）
                                                            // 注意：这里使用 fileList 而不是 filteredFiles，以获取完整列表
                                                            val audioFiles =
                                                                files.filter { localFile ->
                                                                    !localFile.isDirectory && Tools.containsAudioFormat(
                                                                        Tools.extractFileExtension(
                                                                            localFile.name
                                                                        )
                                                                    )
                                                                }

                                                            // 快速查找索引
                                                            val currentAudioIndex =
                                                                audioFiles.withIndex()
                                                                    .firstOrNull { it.value.absolutePath == fullPath }?.index
                                                                    ?: -1

                                                            if (currentAudioIndex == -1) {
                                                                Log.e(
                                                                    "LocalFileListScreen",
                                                                    "未找到文件在音频列表中: $fileName"
                                                                )
                                                                return@launch
                                                            }

                                                            // 构建播放列表
                                                            val audioItems =
                                                                audioFiles.map { localFile ->
                                                                    AudioItem(
                                                                        uri = "file://${localFile.absolutePath}",
                                                                        fileName = localFile.name,
                                                                        dataSourceType = "LOCAL"
                                                                    )
                                                                }

                                                            // 设置数据
                                                            AudioPlaylistRepository.setPlaylist(
                                                                audioItems
                                                            )

                                                            // 导航到音频播放器
                                                            navController.navigate(
                                                                "AudioPlayer/$encodedFileUri/LOCAL/$encodedFileName/$encodedLocalFile/$currentAudioIndex" // connectionName 留空
                                                            )
                                                        }

                                                        Tools.containsImageFileExtension(
                                                            fileExtension
                                                        ) -> {
                                                            navController.navigate(
                                                                "PicViewer/$encodedFileUri/LOCAL/$encodedFileName/$encodedLocalFile" // connectionName 留空
                                                            )
                                                        }

                                                        else -> {
                                                            showToast(
                                                                context,
                                                                context.getString(R.string.ui_label_unsupported_format_with_extension,fileExtension)
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
                                                        focusedFileName = file.name
                                                        focusedIsDir = file.isDirectory
                                                        mediaId = -1
                                                        focusedIsVideo =
                                                            Tools.containsVideoFormat(
                                                                Tools.extractFileExtension(
                                                                    file.name
                                                                )
                                                            )
                                                        focusedMediaUri =
                                                            "file://${file.absolutePath}"
                                                    }
                                                },
                                            scale = ListItemDefaults.scale(
                                                scale = 1.0f,
                                                focusedScale = 1.02f
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
                                    horizontalArrangement = Arrangement.spacedBy(
                                        16.dp,
                                        Alignment.CenterHorizontally
                                    ),
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
                                            if (!settingsState.local) {
                                                showToast(
                                                    context,
                                                    context.getString(R.string.ui_label_scraping_not_enabled)
                                                )
                                            } else {
                                                // 1. 获取要扫描的视频文件列表
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val scanList = if (settingsState.recursiveScanLevel > 0) {
                                                        // 递归扫描
                                                        val dir = File(decodedPath)
                                                        dir.walkTopDown()
                                                            .maxDepth(settingsState.recursiveScanLevel)
                                                            .filter { file ->
                                                                !file.isDirectory && Tools.containsVideoFormat(Tools.extractFileExtension(file.name))
                                                            }
                                                            .map { file ->
                                                                file.name to "file://${file.absolutePath}"
                                                            }
                                                            .toList()
                                                    } else {
                                                        // 非递归，只取当前层级
                                                        files.filter { file ->
                                                            !file.isDirectory && Tools.containsVideoFormat(Tools.extractFileExtension(file.name))
                                                        }.map { file ->
                                                            file.name to "file://${file.absolutePath}"
                                                        }
                                                    }

                                                    withContext(Dispatchers.Main) {
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
                                                                dataSourceType = "LOCAL",
                                                                connectionName = "本地文件"
                                                            )
                                                        }
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
                                            if (!settingsState.local) {
                                                showToast(
                                                    context,
                                                    context.getString(R.string.ui_label_scraping_not_enabled)
                                                )
                                            } else {
                                                // 1. 过滤音频文件
                                                val audioFiles = files.filter {
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
                                                    it.name to "file://${it.absolutePath}"
                                                }

                                                // 3. 直接调用，瞬间完成
                                                audioViewModel.batchScrapeAudioInfo(
                                                    audioList = list,
                                                    dataSourceType = "LOCAL",
                                                    connectionName = "本地文件"
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
                    }
                }
            }
        }
    }
}

private fun queryMediaStore(context: Context, path: String): List<File> {
    val normalizedPath = if (path.endsWith("/")) path else "$path/"
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }
    val selection = """
        ${MediaStore.Files.FileColumns.DATA} LIKE ? 
        AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE ?
    """.trimIndent()
    val selectionArgs = arrayOf("$normalizedPath%", "$normalizedPath%/%")

    return context.contentResolver.query(
        collection,
        arrayOf(MediaStore.Files.FileColumns.DATA),
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }
            .mapNotNull { it -> File(it).takeIf { it.exists() } }
            .toList()
    } ?: emptyList()
}
//navOptions {
//                        popUpTo("FilePage/${URLEncoder.encode(pathUri, "UTF-8")}") {
//                            inclusive = true
//                            saveState = false
//                        }
//                        launchSingleTop = true
//                        restoreState = false
//                    }
