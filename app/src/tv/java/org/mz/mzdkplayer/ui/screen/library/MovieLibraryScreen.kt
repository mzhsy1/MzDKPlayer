package org.mz.mzdkplayer.ui.screen.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
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
import org.mz.mzdkplayer.ui.screen.common.showToast
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.net.URLEncoder

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieLibraryScreen(
    viewModel: MediaLibraryViewModel,
    navController: NavController,
    homeNavController: NavController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val movies = viewModel.pagedMovies.collectAsLazyPagingItems()
    val movieVersions by viewModel.selectedMovieVersions.collectAsState()

    var focusedMovie by remember { mutableStateOf<MediaCacheEntity?>(null) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var selectedMovieTitle by remember { mutableStateOf("") }
    var checkVersionsAfterLoad by remember { mutableStateOf(false) }
    val settingsState by settingsViewModel.uiState.collectAsState()
    val isMoviesLoading = movies.loadState.refresh == LoadState.Loading
    val isMoviesEmpty = movies.itemCount == 0
    var showEditDialog by remember { mutableStateOf(false) }

    val listFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    var isLongClickAction by remember { mutableStateOf(false) }

    LaunchedEffect(movieVersions.size, checkVersionsAfterLoad) {
        if (checkVersionsAfterLoad && movieVersions.isNotEmpty()) {
            val versionCount = movieVersions.size
            if (isLongClickAction) {
                if (versionCount == 1) {
                    showEditDialog = true
                } else {
                    showToast(
                        context,
                        context.getString(R.string.ui_label_multiple_versions_detected)
                    )
                }
                isLongClickAction = false
                checkVersionsAfterLoad = false
            } else {
                when (versionCount) {
                    1 -> {
                        val version = movieVersions.first()
                        val encodedUri = version.videoUri.toBase64()
                        val encodedFileName = version.fileName.toBase64()
                        val connectionName = version.connectionName.toBase64()
                        if (!settingsState.hideDetails) {
                            navController.navigate("MovieDetails/$encodedUri/${version.dataSourceType}/$encodedFileName/$connectionName/${version.tmdbId}")
                        } else {
                            navController.navigate("VideoPlayer/$encodedUri/${version.dataSourceType}/$encodedFileName/$connectionName")
                        }
                        checkVersionsAfterLoad = false
                        viewModel.clearSelectedMovieVersions()
                    }
                    in 2..Int.MAX_VALUE -> {
                        showVersionDialog = true
                        checkVersionsAfterLoad = false
                    }
                }
            }
        }
    }

    LaunchedEffect(isMoviesLoading, isMoviesEmpty) {
        if (!isMoviesLoading && !isMoviesEmpty) {
            runCatching { listFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(movies.itemSnapshotList.items) {
        if (focusedMovie == null && movies.itemCount > 0) {
            focusedMovie = movies.itemSnapshotList.items.firstOrNull()
        }
    }

    if (isMoviesLoading) {
        LoadingScreen(modifier = Modifier.fillMaxSize())
    } else if (isMoviesEmpty) {
        LibraryEmpty(navController = homeNavController)
    } else {
        // 使用纯黑背景，确保与导航栏无缝衔接
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // ================= 1. 沉浸式海报背景层 =================
            AnimatedContent(
                targetState = focusedMovie,
                transitionSpec = {
                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                },
                label = "BackgroundAnimation",
                modifier = Modifier.fillMaxSize()
            ) { movie ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (movie != null) {
                        val backdropUrl = movie.backdropPath ?: movie.posterPath
                        if (backdropUrl != null) {
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
                        }

                        // 多层渐变面罩，确保背景不管多亮，文字都绝对清晰
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.1f),
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
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        endX = 1100f
                                    )
                                )
                        )
                    }
                }
            }

            // ================= 2. 核心内容排版层（规范对齐） =================
            Column(modifier = Modifier.fillMaxSize()) {

                // 顶部信息展示区
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 让它自动占据列表上方的所有剩余空间
                ) {
                    AnimatedContent(
                        targetState = focusedMovie,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        },
                        label = "MovieInfoAnimation",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            // 🚀 关键调整在这里：
                            // start = 32.dp (与列表对齐，并减少离导航栏距离)
                            // top = 160.dp (把文字整体往下推，让出顶部背景空间)
                            .padding(start = 32.dp, top = 160.dp)
                            .widthIn(max = 700.dp) // 限制最大宽度，防止文字拉得太长
                    ) { movie ->
                        if (movie != null) {
                            Column {
                                // ① 元数据行 (分级 • 类型 • 年份)
                                val year = movie.releaseDate?.take(4) ?: ""
                                val typeString = if (movie.mediaType == "tv") "TV Series" else "Movie"
                                val ratingString = String.format(LocalLocale.current.platformLocale, "%.1f ★", movie.voteAverage)

                                // 🚀 修正这里：先用 map 拿到 name，再用 joinToString 拼接
                                val genresString =
                                    movie.genres.take(2).joinToString("/") { it.name }

                                // 按照截图样式：Movie · Animation/Family · 2001 · 8.5 ★
                                val metadataText = listOfNotNull(
                                    typeString,
                                    genresString.ifEmpty { "" },
                                    year.ifEmpty { null },
                                    ratingString
                                ).joinToString(" · ")

                                Text(
                                    text = metadataText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // ② 大标题 (千与千寻)
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.displaySmall, // 稍微加大了一点字号适配新图
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // ③ 简介
                                Text(
                                    text = movie.overview.ifEmpty { "No description available." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                                )
                            }
                        }
                    }
                }
                // ================= 3. 横向列表层（原生 Surface 替换 MediaCard） =================
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(1),
                    contentPadding = PaddingValues(
                        start = 32.dp,  // 调整为 32.dp，与主页一致，减少离导航栏距离
                        end = 32.dp,
                        top = 12.dp,    // 预留焦点放大扩展空间
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp) // 16:9 卡片高 100dp + 放大高度和内边距的空间
                        .focusRequester(listFocusRequester)
                ) {
                    items(movies.itemCount) { index ->
                        val movie = movies[index]
                        if (movie != null) {

                            // 使用原生 TV Clickable Surface 替换
                            Surface(
                                // 直接在这里使用官方提供的 onClick
                                onClick = {
                                    selectedMovieTitle = movie.title
                                    viewModel.clearSelectedMovieVersions()
                                    viewModel.loadMovieVersions(movie.tmdbId)
                                    checkVersionsAfterLoad = true
                                },
                                // 直接在这里使用官方提供的 onLongClick
                                onLongClick = {
                                    selectedMovieTitle = movie.title
                                    focusedMovie = movie
                                    viewModel.loadMovieVersions(movie.tmdbId)
                                    isLongClickAction = true
                                    checkVersionsAfterLoad = true
                                },
                                modifier = Modifier
                                    .width(178.dp)
                                    .height(100.dp)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            focusedMovie = movie
                                        }
                                    },
                                // 注意：因为带有点击事件，根据你给的源码，这里应该用 ClickableSurfaceDefaults
                                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
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
                                    // 优先使用横版背景图(backdrop)，没有则回退海报(poster)
                                    val cardCover = movie.backdropPath ?: movie.posterPath
                                    if (cardCover != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(org.mz.mzdkplayer.tool.Tools.formatImageUrl(cardCover, "w500"))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = movie.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // 兜底文字显示
                                        Text(
                                            text = movie.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.Center)
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 底部操作提示
                Text(
                    text = stringResource(R.string.ui_label_remote_control_tip),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                )
            }
        }
    }

    // ================= 4. 弹窗组件 (结构外移保持不变) =================
    if (showVersionDialog && movieVersions.size > 1) {
        MovieVersionSelectionDialog(
            title = selectedMovieTitle,
            versions = movieVersions,
            onDismiss = {
                showVersionDialog = false
                viewModel.clearSelectedMovieVersions()
            },
            onVersionClick = { version ->
                showVersionDialog = false
                viewModel.clearSelectedMovieVersions()

                val encodedUri = version.videoUri.toBase64()
                val encodedFileName = version.fileName.toBase64()
                val connectionName = version.connectionName.toBase64()
                if (!settingsState.hideDetails) {
                    navController.navigate("MovieDetails/$encodedUri/${version.dataSourceType}/$encodedFileName/$connectionName/${version.tmdbId}")
                } else {
                    navController.navigate("VideoPlayer/$encodedUri/${version.dataSourceType}/$encodedFileName/$connectionName")
                }
            },
            onLongClick = {
                showEditDialog = true
            }
        )
    }

    if (showEditDialog) {
        MyFileDialog(
            onDismiss = { showEditDialog = false },
            fileName = focusedMovie?.fileName,
            onEditClick = {
                showEditDialog = false
                            navController.navigate("EditTMDBInfoScreen/${focusedMovie?.videoUri?.toBase64()}")
            },
            onCloseClick = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieVersionSelectionDialog(
    title: String,
    versions: List<MediaCacheEntity>,
    onDismiss: () -> Unit,
    onVersionClick: (MediaCacheEntity) -> Unit,
    onLongClick: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .height(380.dp),
            shape = MaterialTheme.shapes.large,
            colors = SurfaceDefaults.colors(
                containerColor = Color(0xFF1E2127)
            )
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = stringResource(R.string.ui_label_select_version_for_title, title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ui_label_tip_modify_tmdb_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (versions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.ui_label_loading), color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(versions) { version ->
                            ListItem(
                                selected = false,
                                onClick = { onVersionClick(version) },
                                onLongClick = { onLongClick() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = myListItemCoverColor(),
                                overlineContent = {
                                    Text(
                                        text = "[${version.dataSourceType}] ${version.connectionName}",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        text = version.fileName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}