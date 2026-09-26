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
import org.mz.mzdkplayer.viewmodel.HTTPLinkConViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkListViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkResource
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel

/** 列目录等待上限（HTTP 目录页是一次 GET） */
private const val HTTP_LOAD_TIMEOUT_MS = 30_000L

/**
 * 把连接里存的「站点地址 + 目录路径」拼成起始目录 URL。
 *
 * 两个字段是分开存的（与电视端一致），用户可能写成 `http://host:81` + `media`、
 * `http://host:81/` + `/media/` 等各种形态，这里统一补成 `http://host:81/media/`
 * —— `HTTPLinkConViewModel` 的目录请求依赖结尾 `/`，`FileBrowserLogic.parseHttpDirectoryListing`
 * 也要求 baseUrl 带结尾 `/`，否则同级条目会被子树校验全部丢掉。
 */
internal fun httpInitialDirectory(serverAddress: String?, shareName: String?): String {
    val server = serverAddress.orEmpty().trim().trimEnd('/')
    val path = shareName.orEmpty().trim().trim('/')
    return if (path.isEmpty()) "$server/" else "$server/$path/"
}

/**
 * HTTP 目录页浏览（第三阶段；第四阶段接入刮削）。
 *
 * 复用电视端的 [HTTPLinkConViewModel]（OkHttp + `FileBrowserLogic.parseHttpDirectoryListing`）。
 * 与电视端的两点差异：
 * 1. 电视端播放地址是「当前目录 + 文件名」直接拼；这里用 [FileBrowserLogic.resolveHttpUrl]
 *    对服务端给的 href 做一次解析，编码过的中文名、以 `/` 结尾的目录链接都能对上；
 * 2. 每次换目录都重新 `connectToHTTPLink(目标目录)`，让 ViewModel 的 `baseUrl` 与页面显示的目录一致
 *    （电视端的 `listFiles` 不会更新 `baseUrl`，容易出现「显示的还是上一级」）。
 *
 * 解析出来的绝对地址同时写进条目（刮削与播放共用），保证与 `media_cache` 主键一致。
 */
@Composable
fun PhoneHttpBrowserScreen(
    connectionId: String,
    path: String,
    httpLinkListViewModel: HTTPLinkListViewModel,
    httpLinkConViewModel: HTTPLinkConViewModel,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by httpLinkListViewModel.connections.collectAsState()
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

        // 连接里存的是「站点地址 + 目录路径」，两者拼起来才是起始目录 URL
        val initialUrl = connection?.let {
            httpInitialDirectory(it.serverAddress, it.shareName)
        }.orEmpty()
        val requested = FileBrowserLogic
            .ensureTrailingSlash(path.ifBlank { initialUrl })
            .takeIf { it.startsWith("http") || it.startsWith("https") }

        if (connection == null || requested == null) {
            uiState = PhoneBrowserState.Failed(loadFailedText)
            return@LaunchedEffect
        }

        httpLinkConViewModel.connectToHTTPLink(requested)
        val settled = withTimeoutOrNull(HTTP_LOAD_TIMEOUT_MS) {
            httpLinkConViewModel.connectionStatus.first {
                it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
            }
        }

        uiState = when (settled) {
            is FileConnectionStatus.FilesLoaded -> PhoneBrowserState.Ready(
                path = requested,
                entries = httpLinkConViewModel.fileList.value
                    .mapNotNull { it.toBrowserEntry(requested) }
                    .sortedForBrowser(),
            )

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
        dataSourceType = PhoneFileProtocol.HTTP.routeValue,
        connectionName = connection?.name.orEmpty(),
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = connection?.name?.takeIf { it.isNotBlank() }
            ?: connection?.serverAddress.orEmpty(),
        subtitle = FileBrowserLogic.httpLogicalPath(loadedPath).ifEmpty { "/" },
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
                dataSourceType = PhoneFileProtocol.HTTP.routeValue,
                connectionName = connection?.name.orEmpty(),
                // 服务端给的 href 可能是相对链接，解析不出来时只能提示不支持
                directoryTarget = entry.absoluteUrl(loadedPath),
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

/** 服务端的 href 可能是相对链接，也可能带百分号编码；解析失败时返回 null（由调用方提示） */
private fun PhoneBrowserEntry.absoluteUrl(baseUrl: String): String? =
    runCatching { FileBrowserLogic.resolveHttpUrl(key, baseUrl) }.getOrNull()

private fun HTTPLinkResource.toBrowserEntry(baseUrl: String): PhoneBrowserEntry? {
    if (FileBrowserLogic.isHiddenDirEntry(name)) return null
    return PhoneBrowserEntry(
        // href 既当 key 也用于解析绝对地址（同目录内不会重复）
        key = path,
        name = name,
        isDirectory = isDirectory,
        size = if (isDirectory) null else fileSize.takeIf { it > 0L },
        playbackUri = if (!isDirectory && isPlayableMediaFileName(name)) {
            runCatching { FileBrowserLogic.resolveHttpUrl(path, baseUrl) }.getOrNull()
        } else {
            null
        },
    )
}
