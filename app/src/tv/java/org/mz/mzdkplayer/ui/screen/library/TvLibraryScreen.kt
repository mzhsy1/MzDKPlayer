package org.mz.mzdkplayer.ui.screen.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.ui.screen.common.LibraryEmpty
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.MyFileDialog
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor
import org.mz.mzdkplayer.tool.Tools.toBase64

// === 电视剧屏幕 (原生 Box 实现沉浸式列表) ===
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLibraryScreen(
    viewModel: MediaLibraryViewModel,
    navController: NavController,
    homeNavController: NavController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val tvSeriesList = viewModel.pagedTVSeries.collectAsLazyPagingItems()
    val episodes by viewModel.selectedSeriesEpisodes.collectAsState()

    // 状态：当前获得焦点的剧集 (用于更新背景)
    var focusedTvShow by remember { mutableStateOf<MediaCacheEntity?>(null) }
    val isTvSeriesLoading = tvSeriesList.loadState.refresh == LoadState.Loading
    val isTvSeriesEmpty = tvSeriesList.itemCount == 0
    // 控制弹窗显示
    var showEpisodeDialog by remember { mutableStateOf(false) }
    var selectedSeriesName by remember { mutableStateOf("") }
    val settingsState by settingsViewModel.uiState.collectAsState()
    // 控制弹窗显示
    var showEditDialog by remember { mutableStateOf(false) }
    // 为电视剧列表创建 FocusRequester
    val listFocusRequester = remember { FocusRequester() }
    // 如果刚进入页面没有焦点，默认尝试获取列表第一个作为背景
    LaunchedEffect(tvSeriesList.itemSnapshotList.items) {
        if (focusedTvShow == null && tvSeriesList.itemCount > 0) {
            focusedTvShow = tvSeriesList.itemSnapshotList.items.firstOrNull()
        }
    }
    // 监听加载状态，加载完成且非空时自动请求焦点
    LaunchedEffect(isTvSeriesLoading, isTvSeriesEmpty) {
        if (!isTvSeriesLoading && !isTvSeriesEmpty) {
            runCatching { listFocusRequester.requestFocus() }
        }
    }
    if (isTvSeriesLoading) {
        LoadingScreen(modifier = Modifier.fillMaxSize())
    } else if (isTvSeriesEmpty) {
        LibraryEmpty(navController = homeNavController, type = "tv")
    } else {
        // 使用纯黑背景，确保与导航栏无缝衔接
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // ================= 1. 沉浸式海报背景层 =================
            AnimatedContent(
                targetState = focusedTvShow,
                transitionSpec = {
                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                },
                label = "BackgroundAnimation",
                modifier = Modifier.fillMaxSize()
            ) { tvShow ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val backdropUrl = tvShow?.backdropPath ?: tvShow?.posterPath
                    if (tvShow != null && !backdropUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(org.mz.mzdkplayer.tool.Tools.formatImageUrl(backdropUrl, "w1280"))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.75f
                        )
                    } else {
                        // 空状态背景：铺一层深灰，避免闪白
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF111111))
                        )
                    }

                    // 多层渐变遮罩，确保背景不管多亮，文字都绝对清晰
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black,
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent
                                    ),
                                    endX = 1100f // 左侧信息区域加深
                                )
                            )
                    )
                }
            }

            // ================= 2. 核心内容排版层（规范对齐） =================
            Column(modifier = Modifier.fillMaxSize()) {

                // 顶部信息展示区（自动占据列表上方的所有剩余空间）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = focusedTvShow,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        },
                        label = "TvShowInfoAnimation",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 32.dp, top = 150.dp)
                            .widthIn(max = 720.dp) // 限制最大宽度，防止文字拉得太长
                    ) { tvShow ->
                        Column {
                            // ① 元数据行：类型 · 类型标签/年份 · 评分 · 季数
                            val year = tvShow?.releaseDate?.take(4) ?: ""
                            val seasons = tvShow?.numberOfSeasons ?: 0
                            val ratingString = String.format(
                                LocalLocale.current.platformLocale,
                                "%.1f ★",
                                tvShow?.voteAverage ?: 0.0
                            )
                            val genresString = tvShow?.genres?.take(2)?.joinToString("/") { it.name } ?: ""
                            val metadataText = listOfNotNull(
                                "TV Series",
                                genresString.ifEmpty { null },
                                year.ifEmpty { null },
                                ratingString,
                                if (seasons > 0) stringResource(R.string.ui_label_season_count, seasons) else null
                            ).joinToString(" · ")

                            Text(
                                text = metadataText,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ② 大标题
                            Text(
                                text = tvShow?.title.orEmpty(),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // ③ 简介
                            Text(
                                text = tvShow?.overview.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                            )
                        }
                    }
                }

                // ================= 3. 横向剧集列表（横版封面卡片） =================
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(1),
                    contentPadding = PaddingValues(
                        start = 32.dp,   // 与信息区左对齐
                        end = 32.dp,
                        top = 12.dp,     // 预留焦点放大空间
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp) // 16:9 卡片 + 放大与内边距空间
                        .focusRequester(listFocusRequester)
                ) {
                    items(tvSeriesList.itemCount) { index ->
                        val tvShow = tvSeriesList[index]
                        if (tvShow != null) {
                            TvShowCard(
                                tvShow = tvShow,
                                onFocused = { focusedTvShow = tvShow },
                                onClick = {
                                    // 点击时，加载该剧集下的所有文件，并显示弹窗
                                    selectedSeriesName = tvShow.title
                                    viewModel.loadEpisodes(tvShow.tmdbId, tvShow.source)
                                    showEpisodeDialog = true
                                }
                            )
                        }
                    }
                }

                // 底部操作提示
                Text(
                    text = stringResource(R.string.ui_label_scroll_right_for_more),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                )
            }
        }
    }
    // === 集数选择弹窗 ===
    if (showEpisodeDialog) {
        EpisodeSelectionDialog(
            title = selectedSeriesName,
            episodes = episodes,
            navController = navController, // 传入 navController
            onDismiss = {
                showEpisodeDialog = false
                viewModel.clearSelectedEpisodes()
            },
            onEpisodeClick = { episode ->
                showEpisodeDialog = false
                // 跳转到现有的 TVSeriesDetailsScreen
                val encodedUri = episode.videoUri.toBase64()
                val encodedFileName = episode.fileName.toBase64()
                val connectionName = episode.connectionName.toBase64()
                // 核心：这里将具体的 Uri 和 Season/Episode 传给详情页
                if (!settingsState.hideDetails) {
                    navController.navigate(
                        "TVSeriesDetails/$encodedUri/${episode.dataSourceType}/$encodedFileName/$connectionName/${episode.tmdbId}/${episode.seasonNumber}/${episode.episodeNumber}"
                    )
                } else {
                    navController.navigate("VideoPlayer/$encodedUri/${episode.dataSourceType}/$encodedFileName/$connectionName")
                }
            }
        )
    }
    if (showEditDialog) {
        MyFileDialog(
            onDismiss = { showEditDialog = false },
            fileName = focusedTvShow?.fileName,
            onEditClick = {
                showEditDialog = false
                navController.navigate("EditTMDBInfoScreen/${focusedTvShow?.videoUri?.toBase64()}")
            },
            onCloseClick = { showEditDialog = false }
        )
    }
}

/**
 * 剧集卡片：横版 16:9 封面（优先剧照 backdrop，回退海报），
 * 底部叠加剧名，右上角显示季数角标，焦点态白色描边 + 轻微放大。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvShowCard(
    tvShow: MediaCacheEntity,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = {},
        modifier = Modifier
            .width(190.dp)
            .height(107.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocused()
            },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, Color.White),
                inset = 0.dp
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF252830)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val cover = tvShow.backdropPath ?: tvShow.posterPath
            if (!cover.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(org.mz.mzdkplayer.tool.Tools.formatImageUrl(cover, "w500"))
                        .crossfade(true)
                        .build(),
                    contentDescription = tvShow.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 兜底文字显示
                Text(
                    text = tvShow.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                )
            }

            // 底部渐变 + 剧名，让卡片自带辨识度
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            Text(
                text = tvShow.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
            )

            // 右上角季数角标
            val seasons = tvShow.numberOfSeasons ?: 0
            if (seasons > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ui_label_season_count, seasons),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 集数选择弹窗：左侧剧集海报 + 右侧剧集列表
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeSelectionDialog(
    title: String,
    episodes: List<MediaCacheEntity>,
    onDismiss: () -> Unit,
    onEpisodeClick: (MediaCacheEntity) -> Unit,
    navController: NavController
) {
    // 状态：当前选中的、准备编辑的单集
    var selectedEpisodeForEdit by remember { mutableStateOf<MediaCacheEntity?>(null) }
    // 弹窗打开后主动把焦点交给第一条剧集，省去用户再按一次方向键
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(episodes) {
        if (episodes.isNotEmpty()) {
            runCatching { firstEpisodeFocusRequester.requestFocus() }
        }
    }
    // usePlatformDefaultWidth = false：解除平台对 Dialog 宽度的限制，才能按屏幕比例铺开
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.82f),
            shape = MaterialTheme.shapes.large,
            colors = SurfaceDefaults.colors(
                containerColor = Color(0xFF15171C)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                // 左侧：剧集海报（有封面时才展示，避免空框）
                val poster = episodes.firstOrNull()?.posterPath
                if (!poster.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(2f / 3f)
                            .background(Color(0xFF252830), RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(org.mz.mzdkplayer.tool.Tools.formatImageUrl(poster, "w500"))
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }

                // 右侧：标题 + 提示 + 剧集列表
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ui_label_select_episode_for_title, title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ui_label_tip_long_press_edit_tmdb_info),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (episodes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.ui_label_loading), color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(episodes) { index, episode ->
                                ListItem(
                                    selected = false, // 焦点控制由 TV 框架自动处理
                                    onClick = { onEpisodeClick(episode) },
                                    onLongClick = { selectedEpisodeForEdit = episode },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (index == 0) {
                                                Modifier.focusRequester(firstEpisodeFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    colors = myListItemCoverColor(),
                                    // headlineContent: 显示季集序号
                                    headlineContent = {
                                        // 1. 先准备好剧集名称后缀（如果有名字，就加上 " · 名字"）
                                        val episodeNameSuffix = if (!episode.episodeName.isNullOrEmpty()) {
                                            " · ${episode.episodeName}"
                                        } else {
                                            ""
                                        }
                                        // 2. 使用 stringResource 填充
                                        Text(
                                            text = stringResource(
                                                R.string.ui_label_episode_format,
                                                episode.seasonNumber,
                                                episode.episodeNumber,
                                                episodeNameSuffix
                                            ),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    // overlineContent: 显示数据源与文件名
                                    overlineContent = {
                                        Text(
                                            "${episode.dataSourceType} · ${episode.fileName}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // --- 长按后的二级弹窗 ---
    if (selectedEpisodeForEdit != null) {
        MyFileDialog(
            onDismiss = { selectedEpisodeForEdit = null },
            fileName = selectedEpisodeForEdit?.fileName,
            onEditClick = {
                val episode = selectedEpisodeForEdit
                if (episode != null) {
                    val encodedUri = episode.videoUri.toBase64()
                    // 跳转到修改页面
                    navController.navigate("EditTMDBInfoScreen/$encodedUri")
                }
                selectedEpisodeForEdit = null // 关闭菜单
                onDismiss() // 如果需要，跳转后可以关闭剧集选择弹窗
            },
            onCloseClick = { selectedEpisodeForEdit = null }
        )
    }
}
