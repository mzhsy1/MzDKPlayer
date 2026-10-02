package org.mz.mzdkplayer.ui.phone.component

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.logic.PhoneScrapeLogic
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserEntry
import org.mz.mzdkplayer.ui.phone.model.PhoneBrowserState
import org.mz.mzdkplayer.ui.phone.model.isAudio
import org.mz.mzdkplayer.ui.phone.model.isImage
import org.mz.mzdkplayer.ui.phone.model.isLyricSidecar
import org.mz.mzdkplayer.ui.phone.model.isVideo
import org.mz.mzdkplayer.ui.phone.screen.PhoneBrowserScrapeUi
import org.mz.mzdkplayer.ui.phone.screen.PhoneScrapeAction
import org.mz.mzdkplayer.ui.phone.screen.PhoneScrapeProgressBar

/**
 * 六个协议目录浏览页共用的外壳：顶栏（返回 / 刮削入口 / 进度）、副标题、以及
 * 「加载中 / 空 / 失败 / 缺权限 / 列表」五种状态的呈现。
 *
 * 页面只需给出标题、状态与三个回调；条目怎么列、播放地址怎么拼由各协议自己负责
 * （见 `PhoneBrowserEntry` 所在的 `model` 包）。
 */
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
                is PhoneBrowserState.Loading -> PhoneLoading()

                is PhoneBrowserState.Failed -> PhoneMessageBox(
                    title = stringResource(R.string.phone_files_load_failed),
                    message = state.message,
                    onRetry = onRetry,
                )

                is PhoneBrowserState.Blocked -> PhoneMessageBox(
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
        PhoneEmptyState(message = stringResource(R.string.phone_browser_directory_empty))
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
