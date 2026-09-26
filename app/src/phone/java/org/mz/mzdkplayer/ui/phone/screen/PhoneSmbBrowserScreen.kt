package org.mz.mzdkplayer.ui.phone.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.FileBrowserLogic
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.PhoneLocalNetworkPermission
import org.mz.mzdkplayer.ui.phone.SMB_ROOT_PATH
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.SMBConViewModel
import org.mz.mzdkplayer.viewmodel.SMBConfig
import org.mz.mzdkplayer.viewmodel.SMBFileItem
import org.mz.mzdkplayer.viewmodel.SMBListViewModel

/** SMB 连接/列目录的等待上限 */
private const val SMB_CONNECT_TIMEOUT_MS = 20_000L
private const val SMB_LIST_TIMEOUT_MS = 30_000L

/**
 * SMB 目录浏览。
 *
 * 连接与列目录复用电视端的 [SMBConViewModel]（smbj 实现 + `FileBrowserLogic` 路径口径），
 * 这里只做手机端列表渲染与「目录下钻 / 视频交给播放页」的分发。
 *
 * 第四阶段起改用公共的 `PhoneBrowserScaffold`（第三阶段它是自己一套状态与文案），
 * 于是刮削展示、失败重试、空目录提示与其余五个协议完全一致；
 * 独有的「Android 17 本地网络权限」引导改挂到公共组件的 `Blocked` 状态上。
 *
 * ViewModel 由 `PhoneApp` 挂在 Activity 作用域上，所以一层层点进子目录不会反复重连 SMB。
 */
@Composable
fun PhoneSmbBrowserScreen(
    connectionId: String,
    path: String,
    smbListViewModel: SMBListViewModel,
    smbConViewModel: SMBConViewModel,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by smbListViewModel.connections.collectAsState()
    val connection = remember(connections, connectionId) {
        connections.firstOrNull { it.id == connectionId }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var retryToken by remember { mutableIntStateOf(0) }
    // key 里带 path：进入新目录时天然回到 Loading，不会闪一下上级目录的内容
    var uiState by remember(path) { mutableStateOf<PhoneBrowserState>(PhoneBrowserState.Loading) }

    // Android 17 起访问局域网要运行时授权；没有它就去连 SMB 只会白等 5 秒然后超时
    val context = LocalContext.current
    var localNetworkGranted by remember {
        mutableStateOf(PhoneLocalNetworkPermission.isGranted(context))
    }
    // 申请过一次就不再自动弹窗（连续拒绝后系统会直接返回失败），由「去授权」再发起
    var localNetworkAsked by remember { mutableStateOf(false) }
    val localNetworkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        localNetworkGranted = granted
    }

    val loadFailedText = stringResource(R.string.phone_files_load_failed)
    val localNetworkTitle = stringResource(R.string.phone_smb_local_network_title)
    val localNetworkMessage = stringResource(R.string.phone_smb_local_network_message)
    val unsupportedText = stringResource(R.string.phone_player_unsupported)

    LaunchedEffect(connection, path, retryToken, localNetworkGranted) {
        uiState = PhoneBrowserState.Loading

        if (!localNetworkGranted) {
            // 授权结果回来会把 localNetworkGranted 翻成 true，本 effect 跟着重跑并接着连接
            uiState = PhoneBrowserState.Blocked(
                title = localNetworkTitle,
                message = localNetworkMessage,
            )
            if (!localNetworkAsked) {
                localNetworkAsked = true
                localNetworkLauncher.launch(PhoneLocalNetworkPermission.PERMISSION)
            }
            return@LaunchedEffect
        }

        val target = connection
        val server = target?.ip.orEmpty()
        val share = target?.shareName.orEmpty()
        if (target == null || server.isBlank() || share.isBlank()) {
            uiState = PhoneBrowserState.Failed(loadFailedText)
            return@LaunchedEffect
        }

        if (!smbConViewModel.isConnected()) {
            smbConViewModel.connectToSMB(
                ip = server,
                username = target.username.orEmpty(),
                password = target.password.orEmpty(),
                shareName = share,
            )
            val settled = withTimeoutOrNull(SMB_CONNECT_TIMEOUT_MS) {
                smbConViewModel.connectionStatus.first {
                    it is FileConnectionStatus.Connected || it is FileConnectionStatus.Error
                }
            }
            if (settled !is FileConnectionStatus.Connected) {
                uiState = PhoneBrowserState.Failed(
                    (settled as? FileConnectionStatus.Error)?.message ?: loadFailedText
                )
                return@LaunchedEffect
            }
        }

        smbConViewModel.listSMBFiles(
            SMBConfig(
                server = server,
                share = share,
                path = path,
                username = target.username.orEmpty(),
                password = target.password.orEmpty(),
            )
        )
        val listed = withTimeoutOrNull(SMB_LIST_TIMEOUT_MS) {
            smbConViewModel.connectionStatus.first {
                it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
            }
        }
        uiState = when (listed) {
            is FileConnectionStatus.FilesLoaded -> PhoneBrowserState.Ready(
                path = path,
                entries = smbConViewModel.fileList.value
                    .map { it.toBrowserEntry() }
                    .sortedForBrowser(),
            )

            is FileConnectionStatus.Error -> PhoneBrowserState.Failed(listed.message)
            else -> PhoneBrowserState.Failed(loadFailedText)
        }
    }

    // 重试：允许再弹一次授权申请（首次被划掉/拒绝之后还有机会重来）
    val onRetry: () -> Unit = {
        localNetworkAsked = false
        retryToken++
    }

    val ready = uiState as? PhoneBrowserState.Ready
    val loadedPath = ready?.path.orEmpty()
    val entries = ready?.entries.orEmpty()

    val scrape = rememberPhoneScrapeUi(
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        dataSourceType = PhoneFileProtocol.SMB.routeValue,
        connectionName = connection?.name.orEmpty(),
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = connection?.name?.takeIf { it.isNotBlank() } ?: connection?.ip.orEmpty(),
        subtitle = loadedPath.ifEmpty { SMB_ROOT_PATH },
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        state = uiState,
        showParent = smbParentPath(loadedPath) != loadedPath,
        onOpenParent = { onOpenDirectory(smbParentPath(loadedPath)) },
        onRetry = onRetry,
        onOpenEntry = { entry ->
            dispatchEntryClick(
                entry = entry,
                siblings = entries,
                dataSourceType = PhoneFileProtocol.SMB.routeValue,
                connectionName = connection?.name.orEmpty(),
                directoryTarget = entry.key,
                onOpenDirectory = onOpenDirectory,
                onPlayVideo = onPlayVideo,
                onOpenMedia = onOpenMedia,
                // 不支持的格式（字幕等）与其余协议一致：给一句提示
                onUnsupported = { scope.launch { snackbarHostState.showSnackbar(unsupportedText) } },
            )
        },
        blockedActions = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(R.string.phone_action_grant))
                }
            }
        },
        scrape = scrape,
        onOpenDetailEntry = { entry ->
            entry.playbackUri?.let { onOpenDetail(it, entry.name, connection?.name.orEmpty()) }
        },
    )
}

/** 目录在前，其次按名称（忽略大小写）排序 */
private fun SMBFileItem.toBrowserEntry(): PhoneBrowserEntry = PhoneBrowserEntry(
    key = fullPath,
    name = name,
    isDirectory = isDirectory,
    size = if (isDirectory) null else fileSize.takeIf { it > 0L },
    playbackUri = if (!isDirectory && isPlayableMediaFileName(name)) playbackUri() else null,
)

/** 上一级目录；共享根目录返回自身 */
internal fun smbParentPath(path: String): String {
    if (path.isEmpty() || path == "/") return "/"
    return path.removeSuffix("/").substringBeforeLast("/", "").ifBlank { "/" }
}

/**
 * 交给播放器的 SMB 地址。
 *
 * 用 `buildSmbUrlWithCredentials` 而不是 `buildSmbUrl`：播放地址**始终**要带 userInfo 段，
 * 这两个函数口径不同（见 `FileBrowserLogic` 注释），不要合并。
 */
internal fun SMBFileItem.playbackUri(): String =
    FileBrowserLogic.buildSmbUrlWithCredentials(
        server = server,
        share = share,
        path = fullPath,
        username = username,
        password = password,
    )
