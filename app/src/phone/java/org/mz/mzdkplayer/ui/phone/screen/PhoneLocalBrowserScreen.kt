package org.mz.mzdkplayer.ui.phone.screen

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.PhoneFileBrowserLogic
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.PhoneStoragePermission
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import java.io.File

/**
 * 手机端「浏览本机文件」的根目录。
 *
 * 电视端在 `LocalFileTypeScreen` 里给了 内部存储 / USB / /mnt / / 四个入口；
 * 手机上 `/mnt`、`/` 基本不可读，外接存储也不稳定，所以只保留内部存储这一个根，
 * 并且不允许往上越过它（见 [PhoneFileBrowserLogic.localParentPath]）。
 */
internal fun localBrowserRoot(): String = Environment.getExternalStorageDirectory().absolutePath

/**
 * 本地文件浏览（第三阶段；第四阶段接入刮削）。
 *
 * 与电视端的差异：
 * - 数据只走 `java.io.File`，不再先查 MediaStore（那一步是电视端为了刮削/媒体库做的，
 *   手机上「所有文件访问」拿到后直接列目录就够）；
 * - 目录权限用 [PhoneStoragePermission] 判定，未授权时给「去授权 / 重试」两个按钮；
 * - 视频条目的 `file://` 播放地址在列目录时算好，刮削与播放共用同一个地址。
 */
@Composable
fun PhoneLocalBrowserScreen(
    path: String,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val context = LocalContext.current
    val root = remember { localBrowserRoot() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(PhoneStoragePermission.isGranted(context)) }
    var retryToken by remember { mutableIntStateOf(0) }
    var uiState by remember(path) { mutableStateOf<PhoneBrowserState>(PhoneBrowserState.Loading) }

    val permissionTitle = stringResource(R.string.phone_files_local_permission_title)
    val permissionMessage = stringResource(R.string.phone_files_local_permission_message)
    val unreadableMessage = stringResource(R.string.phone_files_directory_unreadable)
    val unsupportedText = stringResource(R.string.phone_player_unsupported)

    // 「所有文件访问」授权页返回后重新判定；低版本走运行时权限
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { granted = PhoneStoragePermission.isGranted(context) }
    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted = PhoneStoragePermission.isGranted(context) }

    LaunchedEffect(path, retryToken, granted) {
        if (!granted) {
            uiState = PhoneBrowserState.Blocked(
                title = permissionTitle,
                message = permissionMessage,
            )
            return@LaunchedEffect
        }
        uiState = PhoneBrowserState.Loading
        val entries = withContext(Dispatchers.IO) { listLocalDirectory(path) }
        uiState = if (entries == null) {
            // 读不到（目录被删、或只授了媒体权限时访问受限目录）
            PhoneBrowserState.Failed(unreadableMessage)
        } else {
            PhoneBrowserState.Ready(path, entries.sortedForBrowser())
        }
    }

    val parentPath = PhoneFileBrowserLogic.localParentPath(path, root)
    // 真正加载出来的条目（不是路由里请求的那个目录），点击分流与刮削都用它
    val entries = (uiState as? PhoneBrowserState.Ready)?.entries.orEmpty()

    val scrape = rememberPhoneScrapeUi(
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        dataSourceType = PhoneFileProtocol.LOCAL.routeValue,
        // 本机文件没有连接概念，连接名留空（电视端本地列表也是这么写的）
        connectionName = "",
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = stringResource(R.string.ui_label_local_files),
        subtitle = path,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        state = uiState,
        showParent = parentPath != path.trimEnd('/'),
        onOpenParent = { onOpenDirectory(parentPath) },
        onRetry = { retryToken++ },
        onOpenEntry = { entry ->
            dispatchEntryClick(
                entry = entry,
                siblings = entries,
                dataSourceType = PhoneFileProtocol.LOCAL.routeValue,
                // 本机文件没有连接概念，与 `media_cache` / 播放历史里写的那一条保持一致
                connectionName = "",
                directoryTarget = entry.key,
                onOpenDirectory = onOpenDirectory,
                onPlayVideo = onPlayVideo,
                onOpenMedia = onOpenMedia,
                onUnsupported = { scope.launch { snackbarHostState.showSnackbar(unsupportedText) } },
            )
        },
        blockedActions = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                FilledTonalButton(
                    onClick = {
                        if (PhoneStoragePermission.needsAllFilesAccess) {
                            settingsLauncher.launch(PhoneStoragePermission.allFilesAccessIntent(context))
                        } else {
                            runtimeLauncher.launch(PhoneStoragePermission.runtimePermission)
                        }
                    },
                ) {
                    Text(stringResource(R.string.phone_action_grant))
                }
                TextButton(onClick = { retryToken++ }) {
                    Text(stringResource(R.string.phone_action_retry))
                }
            }
        },
        scrape = scrape,
        onOpenDetailEntry = { entry ->
            // 本机文件没有连接名，与 `media_cache` 里写的那一条保持一致
            entry.playbackUri?.let { onOpenDetail(it, entry.name, "") }
        },
    )
}

/** 列出目录；目录不存在或没有读取权限时返回 null（由调用方显示失败态） */
private fun listLocalDirectory(path: String): List<PhoneBrowserEntry>? {
    val dir = File(path)
    if (!dir.isDirectory) return null
    val files = dir.listFiles() ?: return null
    return files.map { file ->
        val isDirectory = file.isDirectory
        PhoneBrowserEntry(
            key = file.absolutePath,
            name = file.name,
            isDirectory = isDirectory,
            size = if (isDirectory) null else file.length(),
            // 视频 / 音频 / 图片都在这里一次算好地址（图片查看与音频播放共用同一个口径）
            playbackUri = if (!isDirectory && isPlayableMediaFileName(file.name)) {
                PhoneFileBrowserLogic.localPlaybackUri(file.absolutePath)
            } else {
                null
            },
        )
    }
}
