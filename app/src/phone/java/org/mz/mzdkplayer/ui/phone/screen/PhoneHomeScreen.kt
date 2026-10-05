@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaHistoryEntity
import org.mz.mzdkplayer.data.model.HistoryWithMetadata
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.ui.phone.component.PhonePosterImage
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel

private val HistoryCardWidth = 208.dp
private val PosterCardWidth = 124.dp

/*
 * M3 Expressive 的「多种形状」：这里用 `MaterialShapes` 里的多边形给每类内容配一个
 * 专属形状，同一屏里靠形状差异区分区块，而不是靠颜色深浅。
 *
 * `RoundedPolygon.toShape()` 本身是 `@Composable` 且内部带 `remember`，所以写成
 * `@Composable get()` 的属性即可 —— 重组不会重复建 Shape 对象。
 */
private val ContinueBulletShape: Shape
    @Composable get() = MaterialShapes.Cookie4Sided.toShape()

private val AddedBulletShape: Shape
    @Composable get() = MaterialShapes.Cookie6Sided.toShape()

private val VisitedBulletShape: Shape
    @Composable get() = MaterialShapes.Cookie9Sided.toShape()

private val QuickBulletShape: Shape
    @Composable get() = MaterialShapes.Sunny.toShape()

private val FilesBadgeShape: Shape
    @Composable get() = MaterialShapes.Cookie6Sided.toShape()

private val SettingsBadgeShape: Shape
    @Composable get() = MaterialShapes.Clover4Leaf.toShape()

private val RowBadgeShape: Shape
    @Composable get() = MaterialShapes.Clover4Leaf.toShape()

private val WelcomeBadgeShape: Shape
    @Composable get() = MaterialShapes.Clover8Leaf.toShape()

/**
 * 手机端首页（第四阶段：接上真实数据 / 第九阶段：Material 3 Expressive 视觉重构）。
 *
 * 结构对着电视端 `HomeScreen` 来：**继续播放 / 最近观看 / 最近添加 / 最近访问**四段，
 * 数据同样出自 `MediaLibraryViewModel`（Room 的 `media_cache` 与 `media_history`），
 * 刮削过的条目直接显示海报、评分与季集信息，没有刮削的退回文件名。
 *
 * 表达性（Expressive）落点：
 * - 首屏一张「继续播放」Hero 卡：图 + 渐隐遮罩 + 表达性字阶 + 波浪进度条；
 * - 区块标题用 `titleLargeEmphasized`，前面缀一个 `MaterialShapes` 多边形（形状即分区）；
 * - 所有进度条从手搓 `Box` 换成 [LinearWavyProgressIndicator]（表达性组件）；
 * - 卡片圆角走 `extraLarge / extraExtraLarge`、图标底座换成表达性形状；
 * - 进度条生长动画取 `MaterialTheme.motionScheme` 的空间曲线，与主题动效一致。
 *
 * 与电视端的差异：电视端点了海报先进「详情页」，手机端没有详情页，点卡片直接播放；
 * 另外把电视端放在侧边栏的「文件浏览 / 设置」入口做成首页底部的两张快捷卡片。
 */
@Composable
fun PhoneHomeScreen(
    libraryViewModel: MediaLibraryViewModel,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    /** 三个区块是否显示（「设置 → 界面与首页 → 首页显示」里可关，默认全开） */
    showRecentlyWatched: Boolean = true,
    showRecentlyAdded: Boolean = true,
    showRecentlyVisited: Boolean = true,
    /** [fileName] 是原始文件名（播放页自己拼刮削标题）、[connectionName] 用于写播放历史 */
    onPlay: (
        sourceUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String,
    ) -> Unit,
) {
    val recentlyWatched by libraryViewModel.recentlyWatched.collectAsState()
    val recentlyAccessed by libraryViewModel.recentlyAccessedFiles.collectAsState()
    val recentlyAdded = libraryViewModel.recentlyAdded.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isLoading = recentlyAdded.loadState.refresh is LoadState.Loading
    // 判断「有没有内容」只看数据本身，不看区块开关：全部关掉区块时应该只留快捷入口，
    // 而不是弹一张「还没有内容」的引导卡（那是给真的没有数据的人看的）
    val isEmpty = !isLoading &&
            recentlyWatched.isEmpty() &&
            recentlyAccessed.isEmpty() &&
            recentlyAdded.itemCount == 0

    // Hero 拿最近的一条记录；下面那条「最近观看」横向列表只放剩下的，
    // 否则同一部片子会在首屏连着出现两次。开关关掉时 Hero 一起消失。
    val hero = if (showRecentlyWatched) recentlyWatched.firstOrNull() else null
    val watchedRail = if (hero != null) recentlyWatched.drop(1) else recentlyWatched

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
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                isLoading -> item(key = "loading") { HomeLoading() }

                isEmpty -> item(key = "welcome") { WelcomeCard(onOpenFiles = onOpenFiles) }

                else -> {
                    if (hero != null) {
                        item(key = "hero") {
                            ContinueWatchingHero(
                                item = hero,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    onPlay(
                                        hero.history.mediaUri,
                                        hero.history.protocolName,
                                        hero.history.fileName,
                                        hero.history.connectionName,
                                    )
                                },
                            )
                        }
                    }

                    if (showRecentlyWatched && watchedRail.isNotEmpty()) {
                        item(key = "section-watched") {
                            Column(modifier = Modifier.animateItem()) {
                                SectionTitle(
                                    text = stringResource(R.string.ui_label_recently_watched),
                                    bullet = ContinueBulletShape,
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(
                                        items = watchedRail,
                                        key = { "watched-${it.history.mediaUri}" },
                                    ) { item ->
                                        HistoryCard(
                                            item = item,
                                            onClick = {
                                                onPlay(
                                                    item.history.mediaUri,
                                                    item.history.protocolName,
                                                    item.history.fileName,
                                                    item.history.connectionName,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showRecentlyAdded && recentlyAdded.itemCount > 0) {
                        item(key = "section-added") {
                            Column(modifier = Modifier.animateItem()) {
                                SectionTitle(
                                    text = stringResource(R.string.ui_label_recently_added),
                                    bullet = AddedBulletShape,
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(
                                        count = recentlyAdded.itemCount,
                                        key = { index ->
                                            "added-${recentlyAdded.peek(index)?.videoUri ?: index}"
                                        },
                                    ) { index ->
                                        recentlyAdded[index]?.let { media ->
                                            AddedMediaCard(
                                                media = media,
                                                onClick = {
                                                    onPlay(
                                                        media.videoUri,
                                                        media.dataSourceType,
                                                        media.fileName,
                                                        media.connectionName,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showRecentlyVisited && recentlyAccessed.isNotEmpty()) {
                        item(key = "section-visited-title") {
                            SectionTitle(
                                text = stringResource(R.string.ui_label_recently_visited),
                                bullet = VisitedBulletShape,
                                modifier = Modifier.animateItem(),
                            )
                        }
                        items(
                            items = recentlyAccessed,
                            key = { "accessed-${it.mediaUri}" },
                        ) { file ->
                            RecentFileRow(
                                file = file,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    onPlay(
                                        file.mediaUri,
                                        file.protocolName,
                                        file.fileName,
                                        file.connectionName,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item(key = "quick-actions") {
                Column(modifier = Modifier.animateItem()) {
                    SectionTitle(
                        text = stringResource(R.string.phone_home_quick_actions),
                        bullet = QuickBulletShape,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuickActionCard(
                            icon = PhoneIcons.Folder,
                            shape = FilesBadgeShape,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = stringResource(R.string.ui_label_file_browsing),
                            supporting = stringResource(R.string.phone_home_files_entry_sub),
                            onClick = onOpenFiles,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionCard(
                            icon = Icons.Filled.Settings,
                            shape = SettingsBadgeShape,
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
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
}

/**
 * 区块标题：表达性字阶 + 一枚 `MaterialShapes` 多边形。
 *
 * 颜色不再整段染成 `primary`（那样四个区块标题都在抢注意力），改成「标题用 `onSurface`
 * 保证可读、颜色只落在前面那枚形状上」——表达性里颜色是用来突出重点的，不是用来铺量的。
 */
@Composable
private fun SectionTitle(
    text: String,
    bullet: Shape,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShapeBlob(
            shape = bullet,
            color = MaterialTheme.colorScheme.primary,
            size = 14.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 表达性形状的色块底座：形状本体可以旋转，里面的内容不跟着转 */
@Composable
private fun ShapeBlob(
    shape: Shape,
    color: Color,
    size: Dp,
    rotation: Float = 0f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotate(rotation)
                .clip(shape)
                .background(color)
        )
        content()
    }
}

@Composable
private fun HomeLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LoadingIndicator()
        // 表达性进度条：首屏加载时的等待也有形状
        LinearWavyProgressIndicator(modifier = Modifier.width(128.dp))
    }
}

/** 一条内容都没有时的引导卡：直接把人送去文件页 */
@Composable
private fun WelcomeCard(onOpenFiles: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraExtraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ShapeBlob(
                shape = WelcomeBadgeShape,
                color = MaterialTheme.colorScheme.primary,
                size = 96.dp,
                rotation = -12f,
            ) {
                Icon(
                    imageVector = PhoneIcons.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                text = stringResource(R.string.phone_home_welcome_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.phone_home_welcome_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onOpenFiles) {
                Text(stringResource(R.string.ui_label_file_browsing))
            }
        }
    }
}

/**
 * 首屏 Hero：「继续播放」最近一条记录。
 *
 * 有剧照/海报时铺满整卡，底部压一层从透明到 `scrim` 的竖向渐变保证文字可读；
 * 没图时退回 `surfaceContainerHighest` 纯色卡（这时文字用主题色，不能再用白色）。
 * 底部横着一条 [LinearWavyProgressIndicator] 表示看到哪儿了 —— 波浪就是表达性的记号。
 */
@Composable
private fun ContinueWatchingHero(
    item: HistoryWithMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 跨 module 的 public 属性不能 smart cast，先取本地变量
    val meta = item.metadata
    val artwork = meta?.backdropPath ?: meta?.posterPath
    val title = cardTitle(meta, item.history.fileName)

    val target = (item.history.getPlaybackPercentage() / 100f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "heroPlaybackProgress",
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraExtraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(16f / 10f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val hasArtwork = PhonePosterImage(
                posterPath = artwork,
                size = "w780",
                modifier = Modifier.fillMaxSize(),
            )
            if (hasArtwork) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.42f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f),
                            )
                        )
                )
            }

            val titleColor = if (hasArtwork) Color.White else MaterialTheme.colorScheme.onSurface
            val subColor =
                if (hasArtwork) Color.White.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant

            // 左上角：说明这张卡是干嘛的
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.ui_label_continue_playing),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = titleColor,
                            maxLines = 2,
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
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (hasArtwork) Color.White else MaterialTheme.colorScheme.primary,
                    trackColor =
                        if (hasArtwork) Color.White.copy(alpha = 0.32f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

/** 最近观看：16:9 剧照 + 波浪播放进度 + 刮削标题 */
@Composable
private fun HistoryCard(item: HistoryWithMetadata, onClick: () -> Unit) {
    // 跨 module 的 public 属性不能 smart cast，先取本地变量
    val meta = item.metadata
    val artwork = meta?.backdropPath ?: meta?.posterPath

    val target = (item.history.getPlaybackPercentage() / 100f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "railPlaybackProgress",
    )

    Column(modifier = Modifier.width(HistoryCardWidth)) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PhonePosterImage(
                    posterPath = artwork,
                    size = "w500",
                    modifier = Modifier.fillMaxSize(),
                )
                // 图片太亮时白字压不住，底部垫一层薄渐变
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                            )
                        )
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                // 底部留出圆角内缩的量，否则进度条两端会被 extraLarge 的圆角切掉
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = cardTitle(meta, item.history.fileName),
            style = MaterialTheme.typography.titleSmallEmphasized,
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
            shape = MaterialTheme.shapes.extraLargeIncreased,
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = media.title,
            style = MaterialTheme.typography.titleSmallEmphasized,
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
private fun RecentFileRow(
    file: MediaHistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShapeBlob(
                shape = RowBadgeShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                size = 44.dp,
                rotation = 10f,
            ) {
                Icon(
                    imageVector = PhoneIcons.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmallEmphasized,
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
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * 快捷入口卡。
 *
 * 两张卡结构一样，靠**容器色**（`primaryContainer` / `tertiaryContainer`）区分，
 * 底座形状也各给一个 `MaterialShapes` 多边形 —— 表达性里颜色与形状都承担信息。
 */
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    shape: Shape,
    container: Color,
    onContainer: Color,
    title: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShapeBlob(
                shape = shape,
                color = container,
                size = 48.dp,
                rotation = -14f,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
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

/**
 * 卡片标题：刮削到就用**原始**刮削标题，否则退回文件名。
 *
 * 这里故意不用播放页那套 `PlayerMediaText.buildTitle()` —— 它会拼上季集与年份，
 * 而首页把季集/年份放在第二行，用 `buildTitle()` 会出现「标题 (2010) / 副标题 2010」
 * 这种上下重复。首页三处卡片（继续播放 / 最近观看 / 最近添加）口径一致。
 */
private fun cardTitle(meta: MediaCacheEntity?, fileName: String): String =
    meta?.title?.takeIf { it.isNotBlank() } ?: fileName

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
