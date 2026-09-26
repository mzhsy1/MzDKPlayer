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
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.FileBrowserLogic
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.WebDavConViewModel
import org.mz.mzdkplayer.viewmodel.WebDavFileItem
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel

/** 连接 / 列目录等待上限 */
private const val WEBDAV_LOAD_TIMEOUT_MS = 30_000L

/**
 * WebDAV 目录浏览（第三阶段；第四阶段接入刮削）。
 *
 * 复用电视端的 [WebDavConViewModel]（Sardine + 忽略证书的 OkHttp）。与电视端的两点差异：
 * 1. 电视端首次进入靠 `connectToWebDav(..., isTest = true)` 顺带列目录，之后 `listFiles`；
 *    这里统一成「连过就直接 `listFiles(目标目录)`」，切换目录不会反复重建连接；
 * 2. 播放地址 = 当前目录 URL 带上账号密码（`buildAuthenticatedUrl`，与电视端同一实现）+ 文件名，
 *    依然走 `WEBDAV` 数据源；该地址在列目录时一次算好写进条目，刮削与播放共用。
 */
@Composable
fun PhoneWebDavBrowserScreen(
    connectionId: String,
    path: String,
    webDavListViewModel: WebDavListViewModel,
    webDavConViewModel: WebDavConViewModel,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by webDavListViewModel.connections.collectAsState()
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
        val username = target?.username.orEmpty()
        val password = target?.password.orEmpty()
        // 目录 URL：路由里给的是「连接里存的 baseUrl」或点进去的子目录 URL，统一补结尾 /
        val requested = FileBrowserLogic
            .ensureTrailingSlash(path.ifBlank { target?.baseUrl.orEmpty() })
            .takeIf { it.startsWith("http") || it.startsWith("https") }

        if (target == null || requested == null) {
            uiState = PhoneBrowserState.Failed(loadFailedText)
            return@LaunchedEffect
        }

        if (!webDavConViewModel.isConnected()) {
            // isTest = true：连接成功后顺带把这个目录列出来
            webDavConViewModel.connectToWebDav(requested, username, password, isTest = true)
        } else {
            webDavConViewModel.listFiles(requested, username, password)
        }

        val settled = withTimeoutOrNull(WEBDAV_LOAD_TIMEOUT_MS) {
            webDavConViewModel.connectionStatus.first {
                it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
            }
        }

        uiState = when (settled) {
            is FileConnectionStatus.FilesLoaded -> {
                // 带账号密码的目录地址只算一次，列表里每个文件的播放地址都在它后面接文件名
                val authenticatedDir = webDavConViewModel.buildAuthenticatedUrl(
                    baseUrl = requested,
                    username = username,
                    password = password,
                )
                PhoneBrowserState.Ready(
                    path = requested,
                    entries = webDavConViewModel.fileList.value
                        .mapNotNull { it.toBrowserEntry(authenticatedDir) }
                        .sortedForBrowser(),
                )
            }

            is FileConnectionStatus.Error -> PhoneBrowserState.Failed(settled.message)
            else -> PhoneBrowserState.Failed(loadFailedText)
        }
    }

    val ready = uiState as? PhoneBrowserState.Ready
    val loadedPath = ready?.path.orEmpty()
    val entries = ready?.entries.orEmpty()
    val parentPath = FileBrowserLogic.httpParentUrl(loadedPath)

    val scrape = rememberPhoneScrapeUi(
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        dataSourceType = PhoneFileProtocol.WEBDAV.routeValue,
        connectionName = connection?.name.orEmpty(),
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = connection?.name?.takeIf { it.isNotBlank() }
            ?: connection?.baseUrl.orEmpty(),
        subtitle = loadedPath,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        state = uiState,
        // httpParentUrl 在站点根会原样返回，等于「没有上一级」
        showParent = loadedPath.isNotEmpty() && parentPath != loadedPath,
        onOpenParent = { onOpenDirectory(parentPath) },
        onRetry = { retryToken++ },
        onOpenEntry = { entry ->
            dispatchEntryClick(
                entry = entry,
                siblings = entries,
                dataSourceType = PhoneFileProtocol.WEBDAV.routeValue,
                connectionName = connection?.name.orEmpty(),
                directoryTarget = FileBrowserLogic.joinUrlDirectory(loadedPath, entry.name),
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

/** [authenticatedDirUrl] 是已经带上账号密码、且以 `/` 结尾的目录地址 */
private fun WebDavFileItem.toBrowserEntry(authenticatedDirUrl: String): PhoneBrowserEntry? {
    if (FileBrowserLogic.isHiddenDirEntry(name)) return null
    return PhoneBrowserEntry(
        key = name,
        name = name,
        isDirectory = isDirectory,
        size = if (isDirectory) null else size,
        playbackUri = if (!isDirectory && isPlayableMediaFileName(name)) {
            FileBrowserLogic.joinUrlPath(authenticatedDirUrl, name)
        } else {
            null
        },
    )
}
