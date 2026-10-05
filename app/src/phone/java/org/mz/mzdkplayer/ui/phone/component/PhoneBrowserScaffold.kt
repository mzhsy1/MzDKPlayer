package org.mz.mzdkplayer.ui.phone.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // 当前目录路径作为顶栏副标题（展开态可见，滚动收起后隐藏）
    val subtitleContent: (@Composable () -> Unit)? = if (!subtitle.isNullOrBlank()) {
        { Text(text = subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    } else {
        null
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                MediumFlexibleTopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    subtitle = subtitleContent,
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
                    scrollBehavior = scrollBehavior,
                )
                if (scrape != null) PhoneScrapeProgressBar(ui = scrape)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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

                // 列表支持下拉刷新：重新加载当前目录（进入 Loading 态后由 PhoneLoading 接管）
                is PhoneBrowserState.Ready -> PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = onRetry,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PhoneBrowserList(
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
}

/** 列表行统一的高度下限：Expressive 倾向更大的触控目标（≥48dp） */
private val BrowserRowMinHeight = 68.dp

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

    // 不再用分割线：Expressive 里用「圆角容器块 + 间距」表达分组
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showParent) {
            item(key = "parent") {
                PhoneParentRow(onClick = onOpenParent)
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
        }
    }
}

/** Expressive 列表行：大圆角色块 + 更大触控目标，取代原来的「ListItem + 分割线」 */
@Composable
private fun BrowserRow(
    containerColor: Color,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.largeIncreased,
        color = containerColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = BrowserRowMinHeight)
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading()
            Column(modifier = Modifier.weight(1f)) { content() }
            trailing?.invoke()
        }
    }
}

/** 「返回上一级」：与文件行同款卡片，用 tertiary 色块说明「这不是文件」 */
@Composable
private fun PhoneParentRow(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    BrowserRow(
        containerColor = scheme.surfaceContainer,
        onClick = onClick,
        leading = {
            LeadingIconBox(
                icon = Icons.Filled.KeyboardArrowUp,
                container = scheme.tertiaryContainer,
                content = scheme.onTertiaryContainer,
            )
        },
    ) {
        Text(
            text = stringResource(R.string.phone_smb_parent_directory),
            style = MaterialTheme.typography.bodyLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

    BrowserRow(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
        leading = { EntryLeading(entry = entry, meta = meta) },
        trailing = if (onShowDetails != null) {
            {
                IconButton(onClick = onShowDetails) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.phone_media_detail_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 行首图标：刮削到海报的视频用海报缩略图，其余用「饱和色块 + 类型图标」。
 *
 * 把文件类型做成一块色块，是 Expressive 里最直接的信息分区手段
 * （目录 = secondary、视频 = primary、音频 = tertiary）。
 */
@Composable
private fun EntryLeading(entry: PhoneBrowserEntry, meta: PhoneScrapeLogic.Meta?) {
    if (!entry.isDirectory && meta != null) {
        PosterThumb(posterPath = meta.posterPath, width = 44.dp, height = 60.dp)
        return
    }
    val scheme = MaterialTheme.colorScheme
    when {
        entry.isDirectory -> LeadingIconBox(PhoneIcons.Folder, scheme.secondaryContainer, scheme.onSecondaryContainer)
        entry.isVideo -> LeadingIconBox(PhoneIcons.Movie, scheme.primaryContainer, scheme.onPrimaryContainer)
        entry.isAudio -> LeadingIconBox(PhoneIcons.Music, scheme.tertiaryContainer, scheme.onTertiaryContainer)
        entry.isImage -> LeadingIconBox(PhoneIcons.Image, scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        else -> LeadingIconBox(PhoneIcons.Movie, scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
    }
}

/** 44dp 圆角图标容器（Expressive 形状对比：小元素用 medium 圆角，大容器用 large+） */
@Composable
private fun LeadingIconBox(icon: ImageVector, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = content,
        )
    }
}
