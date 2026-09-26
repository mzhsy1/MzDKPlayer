package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.tool.PhoneScrapeLogic
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.phone.PhoneIcons

/**
 * 各协议目录浏览页共用的模型与 UI（第三阶段：本地 / FTP / NFS / WebDAV / HTTP，
 * 第四阶段起 SMB 也走这一套，同时补上刮削展示）。
 *
 * 各协议在 UI 层的差异只有「条目怎么列」和「播放地址怎么拼」，列表的样子、加载中、
 * 失败重试、返回上一级、刮削进度完全一致，所以把这一层抽成一套组件，
 * 各页面只负责「把协议的条目映射成 [PhoneBrowserEntry]」和「把点击事件接回协议」。
 */
internal data class PhoneBrowserEntry(
    val key: String,
    val name: String,
    val isDirectory: Boolean,
    /** 文件大小；目录或取不到大小时为 null */
    val size: Long? = null,
    /**
     * 该条目的媒体地址（目录与不支持的格式为 null）。
     *
     * 刮削与播放**共用**它：`media_cache` 的主键就是这个地址，两边必须逐字一致，
     * 否则会出现「列表显示了海报、点进去播放页又变成文件名」。
     *
     * 第五阶段起视频 / 音频 / 图片**都**会填这个字段（音频与图片不参与刮削，
     * 见 `PhoneScrapeLogic.scrapeTargets` 只挑视频）。
     */
    val playbackUri: String? = null,
)

internal sealed interface PhoneBrowserState {
    data object Loading : PhoneBrowserState

    /**
     * [path] 是**真正加载出来的**目录（不一定等于路由里请求的那个）：
     * 上级目录与播放地址都基于它计算，避免拿路由参数和实际结果对不上。
     */
    data class Ready(val path: String, val entries: List<PhoneBrowserEntry>) : PhoneBrowserState

    data class Failed(val message: String) : PhoneBrowserState

    /** 缺系统权限（存储 / 局域网）：[blockedActions] 由页面提供「去授权 / 重试」按钮 */
    data class Blocked(val title: String, val message: String) : PhoneBrowserState
}

/** 目录在前，其次按名称忽略大小写排序；电视端列表页没有排序，手机端统一加一层。 */
internal fun List<PhoneBrowserEntry>.sortedForBrowser(): List<PhoneBrowserEntry> =
    sortedWith(compareByDescending<PhoneBrowserEntry> { it.isDirectory }.thenBy { it.name.lowercase() })

/** 文件名是不是手机端能播的视频（口径与电视端一致：扩展名子串匹配） */
internal fun isVideoFileName(name: String): Boolean =
    Tools.containsVideoFormat(Tools.extractFileExtension(name))

/** 能播的视频条目（扩展名在白名单里） */
internal val PhoneBrowserEntry.isVideo: Boolean
    get() = !isDirectory && isVideoFileName(name)

/** 音频条目的判定口径与电视端一致（`Tools.containsAudioFormat`，同样是子串匹配） */
internal fun isAudioFileName(name: String): Boolean =
    Tools.containsAudioFormat(Tools.extractFileExtension(name))

internal fun isImageFileName(name: String): Boolean =
    Tools.containsImageFileExtension(Tools.extractFileExtension(name))

internal val PhoneBrowserEntry.isAudio: Boolean
    get() = !isDirectory && isAudioFileName(name)

internal val PhoneBrowserEntry.isImage: Boolean
    get() = !isDirectory && isImageFileName(name)

/** 交给 `:core` 的纯逻辑做分流（视频不归它管，这里只用到音频 / 图片两种） */
internal fun PhoneBrowserEntry.toMediaItem(): PhoneMediaLogic.Item = PhoneMediaLogic.Item(
    name = name,
    playbackUri = playbackUri,
    isDirectory = isDirectory,
)

/**
 * 点击本条目的分流结果：目录、视频、无地址与不支持的格式都返回 null，由调用方自己处理
 * （目录进下一层、视频走播放路由、其余弹「不支持」）。
 */
internal fun PhoneBrowserEntry.mediaOpen(
    siblings: List<PhoneBrowserEntry>,
    dataSourceType: String,
    connectionName: String,
): PhoneMediaLogic.MediaOpen? = PhoneMediaLogic.openFor(
    item = toMediaItem(),
    siblings = siblings.map { it.toMediaItem() },
    dataSourceType = dataSourceType,
    connectionName = connectionName,
)

/**
 * 列目录时要**提前算好媒体地址**的扩展名：视频 / 音频 / 图片。
 *
 * 刮削只认视频（见 `PhoneScrapeLogic.scrapeTargets`），但地址本身是三种都要的 ——
 * 播放、音频、图片共用同一个「列目录时算好」的口径，避免点击时再拼一遍。
 */
internal fun isPlayableMediaFileName(name: String): Boolean =
    isVideoFileName(name) || isAudioFileName(name) || isImageFileName(name)

/**
 * 同目录歌词文件（`.lrc`）。
 *
 * 这种文件**不列进浏览页**：它是音频的伴生文件，点开也没有播放器，
 * 而音频页会自己按同名规则去找它（见 `PhoneMediaLogic.lyricSiblingName`）。
 */
internal val PhoneBrowserEntry.isLyricSidecar: Boolean
    get() = !isDirectory && Tools.extractFileExtension(name).equals("lrc", ignoreCase = true)

/**
 * 六个协议浏览页共用的「点击一行」分流。
 *
 * [directoryTarget] 由各协议自己算（各自的目录路径口径不同：本地是绝对路径、FTP 是不带前导 `/`
 * 的显示路径、WebDAV/HTTP 是完整目录 URL……），为 null 表示这一行（HTTP 解析失败的目录）
 * 只能提示不支持。
 */
internal fun dispatchEntryClick(
    entry: PhoneBrowserEntry,
    siblings: List<PhoneBrowserEntry>,
    dataSourceType: String,
    connectionName: String,
    directoryTarget: String?,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onUnsupported: () -> Unit,
) {
    val uri = entry.playbackUri
    when {
        entry.isDirectory -> if (directoryTarget != null) {
            onOpenDirectory(directoryTarget)
        } else {
            onUnsupported()
        }

        entry.isVideo && uri != null -> onPlayVideo(uri, entry.name)

        else -> {
            val open = entry.mediaOpen(siblings, dataSourceType, connectionName)
            if (open != null) onOpenMedia(open) else onUnsupported()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhoneBrowserScaffold(
    title: String,
    subtitle: String?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    state: PhoneBrowserState,
    showParent: Boolean,
    onOpenParent: () -> Unit,
    onOpenEntry: (PhoneBrowserEntry) -> Unit,
    onRetry: () -> Unit,
    blockedActions: (@Composable () -> Unit)? = null,
    /** 刮削会话；为 null 时列表退化成「只有文件名」（第三阶段的样子） */
    scrape: PhoneBrowserScrapeUi? = null,
    /**
     * 视频行右侧「详情」按钮的去向（跳 `phone/detail/...` 那个沉浸式详情页）。
     * 为 null 时不显示该按钮。
     */
    onOpenDetailEntry: ((PhoneBrowserEntry) -> Unit)? = null,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.phone_action_back),
                            )
                        }
                    },
                    actions = {
                        if (scrape != null) PhoneScrapeAction(ui = scrape)
                    },
                )
                if (scrape != null) PhoneScrapeProgressBar(ui = scrape)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            when (state) {
                is PhoneBrowserState.Loading -> PhoneBrowserLoading()

                is PhoneBrowserState.Failed -> PhoneBrowserMessage(
                    title = stringResource(R.string.phone_files_load_failed),
                    message = state.message,
                    onRetry = onRetry,
                )

                is PhoneBrowserState.Blocked -> PhoneBrowserMessage(
                    title = state.title,
                    message = state.message,
                    onRetry = null,
                    actions = blockedActions,
                )

                is PhoneBrowserState.Ready -> PhoneBrowserList(
                    state = state,
                    showParent = showParent,
                    onOpenParent = onOpenParent,
                    onOpenEntry = onOpenEntry,
                    onOpenDetails = onOpenDetailEntry,
                    scrape = scrape,
                )
            }
        }
    }
}

@Composable
private fun PhoneBrowserList(
    state: PhoneBrowserState.Ready,
    showParent: Boolean,
    onOpenParent: () -> Unit,
    onOpenEntry: (PhoneBrowserEntry) -> Unit,
    onOpenDetails: ((PhoneBrowserEntry) -> Unit)?,
    scrape: PhoneBrowserScrapeUi?,
) {
    val entries = state.entries.filterNot { it.isLyricSidecar }
    if (entries.isEmpty() && !showParent) {
        PhoneBrowserEmpty()
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (showParent) {
            item(key = "parent") {
                ListItem(
                    onClick = onOpenParent,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    Text(stringResource(R.string.phone_smb_parent_directory))
                }
                HorizontalDivider()
            }
        }
        // key 统一加前缀：条目的 key 可能是裸文件名（FTP / NFS），
        // 万一真有叫 "parent" 的文件，也不会和「返回上一级」那行撞 key 导致崩溃
        items(entries, key = { "entry-${it.key}" }) { entry ->
            val openDetails = onOpenDetails
            PhoneBrowserEntryRow(
                entry = entry,
                meta = scrape?.metaOf(entry.playbackUri),
                onClick = { onOpenEntry(entry) },
                onShowDetails = if (entry.isVideo && openDetails != null) {
                    { openDetails(entry) }
                } else {
                    null
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun PhoneBrowserEntryRow(
    entry: PhoneBrowserEntry,
    meta: PhoneScrapeLogic.Meta?,
    onClick: () -> Unit,
    onShowDetails: (() -> Unit)?,
) {
    // 刮削到了就用刮削标题；副标题优先「年份 · 评分」，没刮削才退回文件大小
    val title = PhoneScrapeLogic.displayTitle(meta, entry.name)
    val sizeText = entry.size
        ?.takeIf { !entry.isDirectory && it > 0L }
        ?.let { Tools.formatFileSize(it) }
    val supporting = PhoneScrapeLogic.displaySubtitle(meta) ?: sizeText

    ListItem(
        onClick = onClick,
        leadingContent = { EntryLeading(entry = entry, meta = meta) },
        supportingContent = {
            if (supporting != null) {
                Text(text = supporting, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = {
            val showDetails = onShowDetails
            if (showDetails != null) {
                IconButton(onClick = showDetails) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.phone_media_detail_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 行首图标：刮削到海报的视频用海报缩略图，其余用文件夹 / 影片图标。
 *
 * 海报高度固定 56dp（与 `ListItem` 的最小行高一致），列表行高度不会被撑得参差不齐。
 */
@Composable
private fun EntryLeading(entry: PhoneBrowserEntry, meta: PhoneScrapeLogic.Meta?) {
    if (!entry.isDirectory && meta != null) {
        PosterThumb(posterPath = meta.posterPath, width = 40.dp, height = 56.dp)
        return
    }
    val (icon, playable) = when {
        entry.isDirectory -> PhoneIcons.Folder to true
        entry.isVideo -> PhoneIcons.Movie to true
        entry.isAudio -> PhoneIcons.Music to true
        entry.isImage -> PhoneIcons.Image to true
        else -> PhoneIcons.Movie to false
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = if (playable) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhoneBrowserLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingIndicator()
        Text(
            text = stringResource(R.string.ui_label_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** 失败 / 缺权限共用的提示块：[onRetry] 为空时不显示重试按钮，改用 [actions] 里的自定义按钮 */
@Composable
private fun PhoneBrowserMessage(
    title: String,
    message: String,
    onRetry: (() -> Unit)?,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (onRetry != null) {
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.phone_action_retry))
            }
        }
        actions?.invoke()
    }
}

@Composable
private fun PhoneBrowserEmpty() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.phone_browser_directory_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
