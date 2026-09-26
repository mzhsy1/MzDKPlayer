package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.commons.net.ftp.FTPFile
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.FileBrowserLogic
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.viewmodel.FTPConViewModel
import org.mz.mzdkplayer.viewmodel.FTPListViewModel
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel

/** FTP 默认端口，与电视端 `FTPConScreen` 一致 */
private const val FTP_DEFAULT_PORT = 21

/** 连接（含首页目录）等待上限 */
private const val FTP_LOAD_TIMEOUT_MS = 30_000L

/**
 * FTP 目录浏览（第三阶段；第四阶段接入刮削）。
 *
 * 连接与列目录复用电视端的 [FTPConViewModel]（commons-net 实现 + `FileBrowserLogic` 路径口径），
 * 这里只做手机端列表渲染与「目录下钻 / 视频交给播放页」的分发。
 *
 * 两条与电视端**有意**不同的口径：
 * 1. 连接时把**当前要看的目录**当作起始目录传给 `connectToFTP`（电视端只传连接里配置的起始目录后再逐级下钻），
 *    这样「重试 / 换目录」都只走一次请求，也不会出现「路由说 A 目录、实际列的是 B 目录」；
 * 2. 播放地址用 [FileBrowserLogic.buildFtpResourceUrl] 按当前目录显式拼接，
 *    不依赖 ViewModel 里那份 `currentPath` 状态（等价，但不受并发刷新顺序影响）。
 *    这个地址同时写进 `media_cache`，所以刮削结果与播放地址天然对得上。
 */
@Composable
fun PhoneFtpBrowserScreen(
    connectionId: String,
    path: String,
    ftpListViewModel: FTPListViewModel,
    ftpConViewModel: FTPConViewModel,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by ftpListViewModel.connections.collectAsState()
    val connection = remember(connections, connectionId) {
        connections.firstOrNull { it.id == connectionId }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var retryToken by remember { mutableIntStateOf(0) }
    var uiState by remember(path) { mutableStateOf<PhoneBrowserState>(PhoneBrowserState.Loading) }

    val loadFailedText = stringResource(R.string.phone_files_load_failed)
    val unsupportedText = stringResource(R.string.phone_player_unsupported)

    LaunchedEffect(connection, path, retryToken) {
        uiState = PhoneBrowserState.Loading

        val target = connection
        val server = target?.ip.orEmpty()
        if (target == null || server.isBlank()) {
            uiState = PhoneBrowserState.Failed(loadFailedText)
            return@LaunchedEffect
        }

        val port = target.port ?: FTP_DEFAULT_PORT
        // 显示路径（不带前导 /）："" 表示共享根目录；请求前统一由 normalizeFtpDirectory 规整
        val requested = FileBrowserLogic.ftpDisplayPath(FileBrowserLogic.normalizeFtpDirectory(path))

        if (!ftpConViewModel.isConnected()) {
            ftpConViewModel.connectToFTP(
                server = server,
                port = port,
                username = target.username.orEmpty(),
                password = target.password.orEmpty(),
                shareName = requested,
            )
        } else {
            ftpConViewModel.listFiles(requested)
        }

        val settled = withTimeoutOrNull(FTP_LOAD_TIMEOUT_MS) {
            ftpConViewModel.connectionStatus.first {
                it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
            }
        }

        uiState = when (settled) {
            is FileConnectionStatus.FilesLoaded -> PhoneBrowserState.Ready(
                path = requested,
                entries = ftpConViewModel.fileList.value
                    .mapNotNull { it.toBrowserEntry(target, requested, port) }
                    .sortedForBrowser(),
            )

            is FileConnectionStatus.Error -> PhoneBrowserState.Failed(settled.message)
            else -> PhoneBrowserState.Failed(loadFailedText)
        }
    }

    val ready = uiState as? PhoneBrowserState.Ready
    val loadedPath = ready?.path.orEmpty()
    val entries = ready?.entries.orEmpty()
    val parentPath = FileBrowserLogic.ftpParentPath(loadedPath)

    val scrape = rememberPhoneScrapeUi(
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        dataSourceType = PhoneFileProtocol.FTP.routeValue,
        connectionName = connection?.name.orEmpty(),
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = connection?.name?.takeIf { it.isNotBlank() } ?: connection?.ip.orEmpty(),
        subtitle = loadedPath.ifEmpty { "/" },
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        state = uiState,
        showParent = loadedPath.isNotEmpty(),
        onOpenParent = { onOpenDirectory(parentPath) },
        onRetry = { retryToken++ },
        onOpenEntry = { entry ->
            dispatchEntryClick(
                entry = entry,
                siblings = entries,
                dataSourceType = PhoneFileProtocol.FTP.routeValue,
                connectionName = connection?.name.orEmpty(),
                directoryTarget = if (loadedPath.isEmpty()) {
                    entry.name
                } else {
                    "$loadedPath/${entry.name}"
                },
                onOpenDirectory = onOpenDirectory,
                onPlayVideo = onPlayVideo,
                onOpenMedia = onOpenMedia,
                onUnsupported = { scope.launch { snackbarHostState.showSnackbar(unsupportedText) } },
            )
        },
        scrape = scrape,
        onOpenDetailEntry = { entry ->
            entry.playbackUri?.let { onOpenDetail(it, entry.name, connection?.name.orEmpty()) }
        },
    )
}

private fun FTPFile.toBrowserEntry(
    connection: FTPConnection,
    currentPath: String,
    defaultPort: Int,
): PhoneBrowserEntry? {
    val fileName = name ?: return null
    if (FileBrowserLogic.isHiddenDirEntry(fileName)) return null
    return PhoneBrowserEntry(
        key = fileName,
        name = fileName,
        isDirectory = isDirectory,
        size = if (isDirectory) null else size,
        playbackUri = if (isPlayableMediaFileName(fileName)) {
            FileBrowserLogic.buildFtpResourceUrl(
                server = connection.ip.orEmpty(),
                port = connection.port ?: defaultPort,
                username = connection.username.orEmpty(),
                password = connection.password.orEmpty(),
                currentPath = currentPath,
                resourceName = fileName,
            )
        } else {
            null
        },
    )
}
