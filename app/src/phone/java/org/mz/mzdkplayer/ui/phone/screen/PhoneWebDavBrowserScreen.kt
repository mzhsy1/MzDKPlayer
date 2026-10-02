package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.tool.logic.FileBrowserLogic
import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.component.PhoneBrowserPage
import org.mz.mzdkplayer.ui.phone.component.PhoneBrowserSpec
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserEntry
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserState
import org.mz.mzdkplayer.ui.phone.model.isPlayableMediaFileName
import org.mz.mzdkplayer.ui.phone.model.sortedForBrowser
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.WebDavConViewModel
import org.mz.mzdkplayer.viewmodel.WebDavFileItem
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel
import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol

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
    onPlayVideo: (sourceUri: String, name: String, connectionName: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by webDavListViewModel.connections.collectAsState()
    val connection = remember(connections, connectionId) {
        connections.firstOrNull { it.id == connectionId }
    }

    val loadFailedText = stringResource(R.string.phone_files_load_failed)

    PhoneBrowserPage(
        spec = PhoneBrowserSpec(
            dataSourceType = PhoneFileProtocol.WEBDAV.routeValue,
            title = connection?.name?.takeIf { it.isNotBlank() }
                ?: connection?.baseUrl.orEmpty(),
            connectionName = connection?.name.orEmpty(),
            subtitleOf = { it },
            parentPathOf = { FileBrowserLogic.httpParentUrl(it) },
            directoryTargetOf = { entry, loadedPath ->
                FileBrowserLogic.joinUrlDirectory(loadedPath, entry.name)
            },
        ),
        connectionKey = connection,
        requestPath = path,
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        autoScrape = autoScrape,
        onBack = onBack,
        onOpenDirectory = onOpenDirectory,
        onPlayVideo = onPlayVideo,
        onOpenMedia = onOpenMedia,
        onOpenDetail = onOpenDetail,
        load = { loadWebDavDirectory(connection, path, webDavConViewModel, loadFailedText) },
    )
}

/** 必要时先连上（连接时顺带列目录），否则直接列 [path] 目录 */
private suspend fun loadWebDavDirectory(
    connection: WebDavConnection?,
    path: String,
    viewModel: WebDavConViewModel,
    loadFailedText: String,
): PhoneBrowserState {
    val username = connection?.username.orEmpty()
    val password = connection?.password.orEmpty()
    // 目录 URL：路由里给的是「连接里存的 baseUrl」或点进去的子目录 URL，统一补结尾 /
    val requested = FileBrowserLogic
        .ensureTrailingSlash(path.ifBlank { connection?.baseUrl.orEmpty() })
        .takeIf { it.startsWith("http") || it.startsWith("https") }

    if (connection == null || requested == null) return PhoneBrowserState.Failed(loadFailedText)

    if (!viewModel.isConnected()) {
        // isTest = true：连接成功后顺带把这个目录列出来
        viewModel.connectToWebDav(requested, username, password, isTest = true)
    } else {
        viewModel.listFiles(requested, username, password)
    }

    val settled = withTimeoutOrNull(WEBDAV_LOAD_TIMEOUT_MS) {
        viewModel.connectionStatus.first {
            it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
        }
    }

    return when (settled) {
        is FileConnectionStatus.FilesLoaded -> {
            // 带账号密码的目录地址只算一次，列表里每个文件的播放地址都在它后面接文件名
            val authenticatedDir = viewModel.buildAuthenticatedUrl(
                baseUrl = requested,
                username = username,
                password = password,
            )
            PhoneBrowserState.Ready(
                path = requested,
                entries = viewModel.fileList.value
                    .mapNotNull { it.toBrowserEntry(authenticatedDir) }
                    .sortedForBrowser(),
            )
        }

        is FileConnectionStatus.Error -> PhoneBrowserState.Failed(settled.message)
        else -> PhoneBrowserState.Failed(loadFailedText)
    }
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
