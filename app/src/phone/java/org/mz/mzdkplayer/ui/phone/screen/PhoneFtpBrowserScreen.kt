package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.commons.net.ftp.FTPFile
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.logic.FileBrowserLogic
import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.component.PhoneBrowserPage
import org.mz.mzdkplayer.ui.phone.component.PhoneBrowserSpec
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserEntry
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserState
import org.mz.mzdkplayer.ui.phone.model.isPlayableMediaFileName
import org.mz.mzdkplayer.ui.phone.model.sortedForBrowser
import org.mz.mzdkplayer.viewmodel.FTPConViewModel
import org.mz.mzdkplayer.viewmodel.FTPListViewModel
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol

/** FTP 默认端口，与电视端 `FTPConScreen` 一致 */
private const val FTP_DEFAULT_PORT = 21

/** 连接（含首页目录）等待上限 */
private const val FTP_LOAD_TIMEOUT_MS = 30_000L

/**
 * FTP 目录浏览（第三阶段；第四阶段接入刮削）。
 *
 * 连接与列目录复用电视端的 [FTPConViewModel]（commons-net 实现 + `FileBrowserLogic` 路径口径），
 * 页面只声明「FTP 与其它来源不同的那几点」，其余交给 `PhoneBrowserPage`。
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
    onPlayVideo: (sourceUri: String, name: String, connectionName: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by ftpListViewModel.connections.collectAsState()
    val connection = remember(connections, connectionId) {
        connections.firstOrNull { it.id == connectionId }
    }

    val loadFailedText = stringResource(R.string.phone_files_load_failed)
    val port = connection?.port ?: FTP_DEFAULT_PORT
    // 显示路径（不带前导 /）："" 表示共享根目录；请求前统一由 normalizeFtpDirectory 规整
    val requested = FileBrowserLogic.ftpDisplayPath(FileBrowserLogic.normalizeFtpDirectory(path))

    PhoneBrowserPage(
        spec = PhoneBrowserSpec(
            dataSourceType = PhoneFileProtocol.FTP.routeValue,
            title = connection?.name?.takeIf { it.isNotBlank() } ?: connection?.ip.orEmpty(),
            connectionName = connection?.name.orEmpty(),
            subtitleOf = { it.ifEmpty { "/" } },
            parentPathOf = { FileBrowserLogic.ftpParentPath(it) },
            directoryTargetOf = { entry, loadedPath ->
                if (loadedPath.isEmpty()) entry.name else "$loadedPath/${entry.name}"
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
        load = {
            val target = connection
            val server = target?.ip.orEmpty()
            if (target == null || server.isBlank()) {
                PhoneBrowserState.Failed(loadFailedText)
            } else {
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

                when (settled) {
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
