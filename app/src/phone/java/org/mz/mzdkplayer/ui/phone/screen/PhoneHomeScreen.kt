package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaHistoryEntity
import org.mz.mzdkplayer.data.model.HistoryWithMetadata
import org.mz.mzdkplayer.tool.PlayerMediaText
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel

private val HistoryCardWidth = 200.dp
private val PosterCardWidth = 124.dp

/**
 * 手机端首页（第四阶段：接上真实数据）。
 *
 * 结构对着电视端 `HomeScreen` 来：**最近观看 / 最近添加 / 最近访问**三段，
 * 数据同样出自 `MediaLibraryViewModel`（Room 的 `media_cache` 与 `media_history`），
 * 刮削过的条目直接显示海报、评分与季集信息，没有刮削的退回文件名。
 *
 * 与电视端的差异：电视端点了海报先进「详情页」，手机端没有详情页，点卡片直接播放；
 * 另外把电视端放在侧边栏的「文件浏览 / 设置」入口做成首页底部的两张快捷卡片。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneHomeScreen(
    libraryViewModel: MediaLibraryViewModel,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlay: (sourceUri: String, dataSourceType: String, title: String) -> Unit,
) {
    val recentlyWatched by libraryViewModel.recentlyWatched.collectAsState()
    val recentlyAccessed by libraryViewModel.recentlyAccessedFiles.collectAsState()
    val recentlyAdded = libraryViewModel.recentlyAdded.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isLoading = recentlyAdded.loadState.refresh is LoadState.Loading
    val isEmpty = !isLoading &&
            recentlyWatched.isEmpty() &&
            recentlyAccessed.isEmpty() &&
            recentlyAdded.itemCount == 0

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                subtitle = { Text(stringResource(R.string.phone_home_subtitle)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                isLoading -> item { HomeLoading() }

                isEmpty -> item { WelcomeCard(onOpenFiles = onOpenFiles) }

                else -> {
                    if (recentlyWatched.isNotEmpty()) {
                        item {
                            SectionTitle(stringResource(R.string.ui_label_recently_watched))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(recentlyWatched, key = { "watched-${it.history.mediaUri}" }) { item ->
                                    HistoryCard(
                                        item = item,
                                        onClick = {
                                            val meta = item.metadata
                                            onPlay(
                                                item.history.mediaUri,
                                                item.history.protocolName,
                                                PlayerMediaText.buildTitle(meta, item.history.fileName),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (recentlyAdded.itemCount > 0) {
                        item {
                            SectionTitle(stringResource(R.string.ui_label_recently_added))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(
                                    count = recentlyAdded.itemCount,
                                    key = { index -> "added-${recentlyAdded.peek(index)?.videoUri ?: index}" },
                                ) { index ->
                                    recentlyAdded[index]?.let { media ->
                                        AddedMediaCard(
                                            media = media,
                                            onClick = {
                                                onPlay(
                                                    media.videoUri,
                                                    media.dataSourceType,
                                                    PlayerMediaText.buildTitle(media, media.fileName),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (recentlyAccessed.isNotEmpty()) {
                        item {
                            SectionTitle(stringResource(R.string.ui_label_recently_visited))
                        }
                        items(
                            items = recentlyAccessed,
                            key = { "accessed-${it.mediaUri}" },
                        ) { file ->
                            RecentFileRow(
                                file = file,
                                onClick = { onPlay(file.mediaUri, file.protocolName, file.fileName) },
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.phone_home_quick_actions))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickActionCard(
                        icon = PhoneIcons.Folder,
                        title = stringResource(R.string.ui_label_file_browsing),
                        supporting = stringResource(R.string.phone_home_files_entry_sub),
                        onClick = onOpenFiles,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Settings,
                        title = stringResource(R.string.ui_label_settings),
                        supporting = stringResource(R.string.phone_home_settings_entry_sub),
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingIndicator()
    }
}

/** 一条内容都没有时的引导卡：直接把人送去文件页 */
@Composable
private fun WelcomeCard(onOpenFiles: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.phone_home_welcome_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.phone_home_welcome_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(
                onClick = onOpenFiles,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.ui_label_file_browsing))
            }
        }
    }
}

/** 最近观看：16:9 剧照 + 播放进度条 + 刮削标题 */
@Composable
private fun HistoryCard(item: HistoryWithMetadata, onClick: () -> Unit) {
    // 跨 module 的 public 属性不能 smart cast，先取本地变量
    val meta = item.metadata
    val artwork = meta?.backdropPath ?: meta?.posterPath

    Column(modifier = Modifier.width(HistoryCardWidth)) {
        ArtworkBox(
            imagePath = artwork,
            imageSize = "w500",
            onClick = onClick,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                )
            }
            PlaybackProgressBar(
                fraction = item.history.getPlaybackPercentage() / 100f,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = PlayerMediaText.buildTitle(meta, item.history.fileName),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = mediaSubtitle(
                mediaType = meta?.mediaType,
                seasonNumber = meta?.seasonNumber ?: 0,
                episodeNumber = meta?.episodeNumber ?: 0,
                year = meta?.releaseDate?.take(4),
                fallback = item.history.protocolName,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 最近添加：刮削入库的文件，用 2:3 海报 */
@Composable
private fun AddedMediaCard(media: MediaCacheEntity, onClick: () -> Unit) {
    Column(modifier = Modifier.width(PosterCardWidth)) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (!PhonePosterImage(
                        posterPath = media.posterPath,
                        size = "w342",
                        modifier = Modifier.fillMaxSize(),
                    )
                ) {
                    Icon(
                        imageVector = PhoneIcons.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = media.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = mediaSubtitle(
                mediaType = media.mediaType,
                seasonNumber = media.seasonNumber,
                episodeNumber = media.episodeNumber,
                year = media.releaseDate?.take(4),
                fallback = media.dataSourceType,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 最近访问：没刮削过的文件记录，一行一条 */
@Composable
private fun RecentFileRow(file: MediaHistoryEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PhoneIcons.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = file.protocolName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = "${file.getPlaybackPercentage()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 16:9 的剧照容器；没有图时给一块占位底色，`content` 用来叠进度条 / 播放按钮 */
@Composable
private fun ArtworkBox(
    imagePath: String?,
    imageSize: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        Box {
            PhonePosterImage(
                posterPath = imagePath,
                size = imageSize,
                modifier = Modifier.fillMaxSize(),
            )
            content()
        }
    }
}

@Composable
private fun PlaybackProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** 副标题：剧集给 `SxxExx`，电影给年份，都没有就退回协议名 */
private fun mediaSubtitle(
    mediaType: String?,
    seasonNumber: Int,
    episodeNumber: Int,
    year: String?,
    fallback: String,
): String = when {
    mediaType == "tv" && (seasonNumber > 0 || episodeNumber > 0) ->
        String.format(java.util.Locale.US, "S%02dE%02d", seasonNumber, episodeNumber)

    !year.isNullOrBlank() && year.length == 4 -> year
    else -> fallback
}
