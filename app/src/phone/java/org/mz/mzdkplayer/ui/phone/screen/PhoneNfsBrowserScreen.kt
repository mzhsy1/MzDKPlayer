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
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.tool.FileBrowserLogic
import org.mz.mzdkplayer.tool.PhoneFileBrowserLogic
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.NFSConViewModel
import org.mz.mzdkplayer.viewmodel.NFSListViewModel

/** 连接 / 列目录等待上限（NFS 走 RPC，超时统一放宽到 30 秒） */
private const val NFS_LOAD_TIMEOUT_MS = 30_000L

/**
 * NFS 目录浏览（第三阶段；第四阶段接入刮削）。
 *
 * 复用电视端的 [NFSConViewModel]（nfs-client + `FileBrowserLogic` 路径口径）。与电视端的三点差异：
 * 1. 电视端进目录时直接把 `nfs://server:share:path` 拼在 UI 里；这里统一走
 *    [PhoneFileBrowserLogic.nfsPlaybackUri]（导出路径缺前导 `/` 时补上，否则 URI 解析不出 host）；
 * 2. 连接与列目录拆成两步（连接时 `isTest = false`），这样「要看的目录」由页面显式指定，
 *    不会被连接时顺手列出的根目录盖掉；
 * 3. 播放地址在列目录时一次算好写进条目（刮削与播放共用，保证与 `media_cache` 主键一致）。
 */
@Composable
fun PhoneNfsBrowserScreen(
    connectionId: String,
    path: String,
    nfsListViewModel: NFSListViewModel,
    nfsConViewModel: NFSConViewModel,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by nfsListViewModel.connections.collectAsState()
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
        val server = target?.serverAddress.orEmpty()
        if (target == null || server.isBlank()) {
            uiState = PhoneBrowserState.Failed(loadFailedText)
            return@LaunchedEffect
        }

        // NFS 路径一律绝对路径；路由里 "" 表示挂载根
        val requested = path.trim().ifEmpty { "/" }.let { if (it.startsWith("/")) it.trimEnd('/') else "/$it" }
            .ifEmpty { "/" }

        if (!nfsConViewModel.isConnected()) {
            nfsConViewModel.connectToNFS(target, isTest = false)
            val connected = withTimeoutOrNull(NFS_LOAD_TIMEOUT_MS) {
                nfsConViewModel.connectionStatus.first {
                    it is FileConnectionStatus.Connected || it is FileConnectionStatus.Error
                }
            }
            if (connected !is FileConnectionStatus.Connected) {
                uiState = PhoneBrowserState.Failed(
                    (connected as? FileConnectionStatus.Error)?.message ?: loadFailedText
                )
                return@LaunchedEffect
            }
        }

        nfsConViewModel.listFiles(requested)
        val listed = withTimeoutOrNull(NFS_LOAD_TIMEOUT_MS) {
            nfsConViewModel.connectionStatus.first {
                it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
            }
        }

        uiState = when (listed) {
            is FileConnectionStatus.FilesLoaded -> PhoneBrowserState.Ready(
                path = requested,
                // isDirectory / length 会走 RPC 且会抛 IOException，放到 IO 线程并逐个兜底
                entries = withContext(Dispatchers.IO) {
                    nfsConViewModel.fileList.value
                        .mapNotNull { file -> runCatching { file.toBrowserEntry(target, requested) }.getOrNull() }
                        .sortedForBrowser()
                },
            )

            is FileConnectionStatus.Error -> PhoneBrowserState.Failed(listed.message)
            else -> PhoneBrowserState.Failed(loadFailedText)
        }
    }

    val ready = uiState as? PhoneBrowserState.Ready
    val loadedPath = ready?.path.orEmpty()
    val entries = ready?.entries.orEmpty()
    // 挂载根（"" 或 "/"）返回 ""，表示没有上一级
    val parentPath = FileBrowserLogic.nfsParentPath(loadedPath)

    val scrape = rememberPhoneScrapeUi(
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        dataSourceType = PhoneFileProtocol.NFS.routeValue,
        connectionName = connection?.name.orEmpty(),
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = connection?.name?.takeIf { it.isNotBlank() }
            ?: connection?.serverAddress.orEmpty(),
        subtitle = loadedPath.ifEmpty { "/" },
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        state = uiState,
        showParent = parentPath.isNotEmpty(),
        onOpenParent = { onOpenDirectory(parentPath) },
        onRetry = { retryToken++ },
        onOpenEntry = { entry ->
            dispatchEntryClick(
                entry = entry,
                siblings = entries,
                dataSourceType = PhoneFileProtocol.NFS.routeValue,
                connectionName = connection?.name.orEmpty(),
                directoryTarget = FileBrowserLogic.nfsChildPath(loadedPath, entry.name),
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

private fun Nfs3File.toBrowserEntry(
    connection: NFSConnection,
    currentPath: String,
): PhoneBrowserEntry? {
    val fileName = name ?: return null
    if (FileBrowserLogic.isHiddenDirEntry(fileName)) return null
    val isDir = isDirectory
    return PhoneBrowserEntry(
        key = fileName,
        name = fileName,
        isDirectory = isDir,
        size = if (isDir) null else length(),
        playbackUri = if (!isDir && isPlayableMediaFileName(fileName)) {
            PhoneFileBrowserLogic.nfsPlaybackUri(
                serverAddress = connection.serverAddress.orEmpty(),
                exportPath = connection.shareName.orEmpty(),
                pathWithinExport = FileBrowserLogic.nfsChildPath(currentPath, fileName),
            )
        } else {
            null
        },
    )
}
