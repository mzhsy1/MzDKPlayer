package org.mz.mzdkplayer.ui.phone.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserEntry
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserState
import org.mz.mzdkplayer.ui.phone.model.dispatchEntryClick
import org.mz.mzdkplayer.ui.phone.screen.rememberPhoneScrapeUi
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel

/**
 * 一个来源（协议 / 本地）在「目录浏览」这件事上的差异点。
 *
 * 六个浏览页原本把这些东西连同加载流程、状态机、Scaffold 一起各抄了一遍，
 * 现在只保留这一份声明 + 一个 [PhoneBrowserPage.load] 挂起函数。
 */
internal class PhoneBrowserSpec(
    /** 播放页与刮削共用的来源标记（`LOCAL` / `SMB` / `FTP` / …） */
    val dataSourceType: String,
    /** 顶栏标题（连接名，取不到时退回地址） */
    val title: String,
    /** 写进 `media_cache` 的连接名 */
    val connectionName: String,
    /** 副标题：把「真正加载出来的目录」转成给用户看的路径 */
    val subtitleOf: (loadedPath: String) -> String = { it.ifEmpty { "/" } },
    /** 上一级目录；返回空串表示没有上一级 */
    val parentPathOf: (loadedPath: String) -> String = { "" },
    /** 是否显示「返回上一级」：默认「有加载出来的目录且上一级与当前不同」 */
    val showParentOf: (loadedPath: String, parentPath: String) -> Boolean =
        { loadedPath, parentPath -> loadedPath.isNotEmpty() && parentPath != loadedPath },
    /**
     * 点目录行时要去加载的路径；返回 null 表示这一行（例如 HTTP 解析失败的目录）不支持。
     */
    val directoryTargetOf: (entry: PhoneBrowserEntry, loadedPath: String) -> String? = { _, _ -> null },
)

/**
 * 六个来源共用的目录浏览骨架：加载 → 状态机（加载中 / 空 / 失败 / 缺权限 / 就绪）
 * → 刮削会话 → 列表渲染。
 *
 * [load] 是唯一的协议差异实现：它自己去连、去列目录，返回 [PhoneBrowserState]。
 * [connectionKey] / [requestPath] 变化（换连接、换目录、点重试）会重新执行 [load]。
 */
@Composable
internal fun PhoneBrowserPage(
    spec: PhoneBrowserSpec,
    connectionKey: Any?,
    requestPath: String,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    autoScrape: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    /** [connectionName] 由本骨架带上（各协议页自己那份），播放页写播放历史要用 */
    onPlayVideo: (sourceUri: String, name: String, connectionName: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onOpenDetail: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
    /**
     * 缺权限（存储 / 局域网）时 `PhoneBrowserState.Blocked` 里显示的按钮。
     * 参数是「重试」动作（与失败态的「重试」同一个，会重跑 [load]），「去授权」按钮按需调用。
     */
    blockedActions: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
    /** 点「重试」时的额外动作（例如重置「权限已经问过一次」的标记，允许再弹一次申请） */
    onRetryExtra: () -> Unit = {},
    load: suspend () -> PhoneBrowserState,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var retryToken by remember { mutableIntStateOf(0) }
    var uiState by remember(requestPath) { mutableStateOf<PhoneBrowserState>(PhoneBrowserState.Loading) }
    val unsupportedText = stringResource(R.string.phone_player_unsupported)

    val retryAction: () -> Unit = {
        onRetryExtra()
        retryToken++
    }
    val actions = blockedActions
    val blockedContent: (@Composable () -> Unit)? = if (actions != null) {
        { actions(retryAction) }
    } else {
        null
    }

    // load 每次重组都是新 lambda（捕获了最新的 connection / path），用 State 包一层，
    // 这样 LaunchedEffect 只在 key 变化时重启，不会因为 lambda 换了身份而反复加载
    val currentLoad by rememberUpdatedState(load)

    LaunchedEffect(connectionKey, requestPath, retryToken) {
        uiState = PhoneBrowserState.Loading
        uiState = currentLoad()
    }

    val ready = uiState as? PhoneBrowserState.Ready
    val loadedPath = ready?.path.orEmpty()
    val entries = ready?.entries.orEmpty()
    val parentPath = spec.parentPathOf(loadedPath)

    val scrape = rememberPhoneScrapeUi(
        movieViewModel = movieViewModel,
        mediaMetaViewModel = mediaMetaViewModel,
        dataSourceType = spec.dataSourceType,
        connectionName = spec.connectionName,
        autoScrape = autoScrape,
        entries = entries,
        snackbarHostState = snackbarHostState,
    )

    PhoneBrowserScaffold(
        title = spec.title,
        subtitle = spec.subtitleOf(loadedPath),
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        state = uiState,
        showParent = spec.showParentOf(loadedPath, parentPath),
        onOpenParent = { onOpenDirectory(parentPath) },
        onRetry = retryAction,
        onOpenEntry = { entry ->
            dispatchEntryClick(
                entry = entry,
                siblings = entries,
                dataSourceType = spec.dataSourceType,
                connectionName = spec.connectionName,
                directoryTarget = spec.directoryTargetOf(entry, loadedPath),
                onOpenDirectory = onOpenDirectory,
                onPlayVideo = onPlayVideo,
                onOpenMedia = onOpenMedia,
                onUnsupported = { scope.launch { snackbarHostState.showSnackbar(unsupportedText) } },
            )
        },
        blockedActions = blockedContent,
        scrape = scrape,
        onOpenDetailEntry = { entry ->
            entry.playbackUri?.let { onOpenDetail(it, entry.name, spec.connectionName) }
        },
    )
}
