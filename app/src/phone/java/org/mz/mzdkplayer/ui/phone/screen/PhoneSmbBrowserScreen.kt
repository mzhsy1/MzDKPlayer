package org.mz.mzdkplayer.ui.phone.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.model.SMBConnection
import org.mz.mzdkplayer.tool.logic.FileBrowserLogic
import org.mz.mzdkplayer.tool.logic.PhoneFileBrowserLogic
import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.PhoneLocalNetworkPermission
import org.mz.mzdkplayer.ui.phone.SMB_ROOT_PATH
import org.mz.mzdkplayer.ui.phone.component.PhoneBrowserPage
import org.mz.mzdkplayer.ui.phone.component.PhoneBrowserSpec
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserEntry
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserState
import org.mz.mzdkplayer.ui.phone.model.isPlayableMediaFileName
import org.mz.mzdkplayer.ui.phone.model.sortedForBrowser
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.SMBConViewModel
import org.mz.mzdkplayer.viewmodel.SMBConfig
import org.mz.mzdkplayer.viewmodel.SMBFileItem
import org.mz.mzdkplayer.viewmodel.SMBListViewModel
import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol

/** SMB 连接/列目录的等待上限 */
private const val SMB_CONNECT_TIMEOUT_MS = 20_000L
private const val SMB_LIST_TIMEOUT_MS = 30_000L

/**
 * SMB 目录浏览。
 *
 * 连接与列目录复用电视端的 [SMBConViewModel]（smbj 实现 + `FileBrowserLogic` 路径口径），
 * 页面只声明差异点 + 一个加载实现，其余交给 `PhoneBrowserPage`。
 *
 * 第四阶段起改用公共的浏览外壳（第三阶段它是自己一套状态与文案），
 * 于是刮削展示、失败重试、空目录提示与其余五个协议完全一致；
 * 独有的「Android 17 本地网络权限」引导挂到 `PhoneBrowserState.Blocked` 上。
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
    onPlayVideo: (sourceUri: String, name: String, connectionName: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val connections by smbListViewModel.connections.collectAsState()
    val connection = remember(connections, connectionId) {
        connections.firstOrNull { it.id == connectionId }
    }

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

    PhoneBrowserPage(
        spec = PhoneBrowserSpec(
            dataSourceType = PhoneFileProtocol.SMB.routeValue,
            title = connection?.name?.takeIf { it.isNotBlank() } ?: connection?.ip.orEmpty(),
            connectionName = connection?.name.orEmpty(),
            subtitleOf = { it.ifEmpty { SMB_ROOT_PATH } },
            parentPathOf = { PhoneFileBrowserLogic.smbParentPath(it) },
            showParentOf = { loadedPath, parentPath -> parentPath != loadedPath },
            directoryTargetOf = { entry, _ -> entry.key },
        ),
        // 授权结果回来会把 localNetworkGranted 翻成 true，key 变化后本页自动接着连接
        connectionKey = connection to localNetworkGranted,
        requestPath = path,
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        autoScrape = autoScrape,
        onBack = onBack,
        onOpenDirectory = onOpenDirectory,
        onPlayVideo = onPlayVideo,
        onOpenMedia = onOpenMedia,
        onOpenDetail = onOpenDetail,
        // 重试：允许再弹一次授权申请（首次被划掉/拒绝之后还有机会重来）
        onRetryExtra = { localNetworkAsked = false },
        blockedActions = { retry ->
            Column(modifier = Modifier.padding(top = 16.dp)) {
                FilledTonalButton(onClick = retry) {
                    Text(stringResource(R.string.phone_action_grant))
                }
            }
        },
        load = {
            if (!localNetworkGranted) {
                if (!localNetworkAsked) {
                    localNetworkAsked = true
                    localNetworkLauncher.launch(PhoneLocalNetworkPermission.PERMISSION)
                }
                PhoneBrowserState.Blocked(
                    title = localNetworkTitle,
                    message = localNetworkMessage,
                )
            } else {
                loadSmbDirectory(connection, path, smbConViewModel, loadFailedText)
            }
        },
    )
}

/** 必要时先连上，再列 [path] 目录；两种失败都收成 `Failed` 返回给骨架 */
private suspend fun loadSmbDirectory(
    connection: SMBConnection?,
    path: String,
    viewModel: SMBConViewModel,
    loadFailedText: String,
): PhoneBrowserState {
    val server = connection?.ip.orEmpty()
    val share = connection?.shareName.orEmpty()
    if (connection == null || server.isBlank() || share.isBlank()) {
        return PhoneBrowserState.Failed(loadFailedText)
    }

    if (!viewModel.isConnected()) {
        viewModel.connectToSMB(
            ip = server,
            username = connection.username.orEmpty(),
            password = connection.password.orEmpty(),
            shareName = share,
        )
        val settled = withTimeoutOrNull(SMB_CONNECT_TIMEOUT_MS) {
            viewModel.connectionStatus.first {
                it is FileConnectionStatus.Connected || it is FileConnectionStatus.Error
            }
        }
        if (settled !is FileConnectionStatus.Connected) {
            return PhoneBrowserState.Failed(
                (settled as? FileConnectionStatus.Error)?.message ?: loadFailedText
            )
        }
    }

    viewModel.listSMBFiles(
        SMBConfig(
            server = server,
            share = share,
            path = path,
            username = connection.username.orEmpty(),
            password = connection.password.orEmpty(),
        )
    )
    val listed = withTimeoutOrNull(SMB_LIST_TIMEOUT_MS) {
        viewModel.connectionStatus.first {
            it is FileConnectionStatus.FilesLoaded || it is FileConnectionStatus.Error
        }
    }
    return when (listed) {
        is FileConnectionStatus.FilesLoaded -> PhoneBrowserState.Ready(
            path = path,
            entries = viewModel.fileList.value
                .map { it.toBrowserEntry() }
                .sortedForBrowser(),
        )

        is FileConnectionStatus.Error -> PhoneBrowserState.Failed(listed.message)
        else -> PhoneBrowserState.Failed(loadFailedText)
    }
}

private fun SMBFileItem.toBrowserEntry(): PhoneBrowserEntry = PhoneBrowserEntry(
    key = fullPath,
    name = name,
    isDirectory = isDirectory,
    size = if (isDirectory) null else fileSize.takeIf { it > 0L },
    playbackUri = if (!isDirectory && isPlayableMediaFileName(name)) playbackUri() else null,
)

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
