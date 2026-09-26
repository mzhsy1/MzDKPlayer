package org.mz.mzdkplayer.ui.screen.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaHistoryEntity
import org.mz.mzdkplayer.data.model.HistoryWithMetadata
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.tool.Tools.toBase64
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.TwoArcLoading

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediaLibraryViewModel,
    navController: NavController,
    homeNavController: NavController,
    settingsViewModel: SettingsViewModel
) {
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()
    val recentlyAdded = viewModel.recentlyAdded.collectAsLazyPagingItems()
    val recentlyAccessed by viewModel.recentlyAccessedFiles.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val isLoading = recentlyAdded.loadState.refresh is LoadState.Loading
    val isEmpty = !isLoading && recentlyWatched.isEmpty() && recentlyAccessed.isEmpty() && recentlyAdded.itemCount == 0

    // 👇 添加一个 FocusRequester
    val mainFocusRequester = remember { FocusRequester() }

    // 👇 关键：页面加载后，立即请求焦点，防止焦点落在侧边栏
    LaunchedEffect(Unit) {
        mainFocusRequester.requestFocus()
    }

    // 当加载完成后，再次尝试请求焦点以确保落在内容列表上
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            mainFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            isLoading -> {
                LoadingHomeState(Modifier.focusRequester(mainFocusRequester))
            }
            isEmpty -> {
                EmptyHomeState(homeNavController, mainFocusRequester)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().focusRequester(mainFocusRequester),
                    contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // 最近观看 Section
                    if (recentlyWatched.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.ui_label_recently_watched),
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recentlyWatched) { item ->
                                    HistoryCard(item, navController, settingsState.hideDetails)
                                }
                            }
                        }
                    }

                    // 最近添加 Section
                    if (recentlyAdded.itemCount > 0) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.ui_label_recently_added),
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recentlyAdded.itemCount) { index ->
                                    recentlyAdded[index]?.let { movie ->
                                        MediaCard(movie, navController, settingsState.hideDetails)
                                    }
                                }
                            }
                        }
                    }

                    // 最近访问 Section
                    if (recentlyAccessed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.ui_label_recently_visited),
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recentlyAccessed) { item ->
                                    FileHistoryCard(item, navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingHomeState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable(), // 让加载界面也具有捕获焦点的能力
        contentAlignment = Alignment.Center
    ) {
        TwoArcLoading(modifier = Modifier.size(64.dp))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyHomeState(
    homeNavController: NavController,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            colors = SurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.05f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.tvoff24dp),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.2f)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.ui_label_no_content),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.ui_label_go_to_file_section_to_add),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(40.dp))
        MyIconButton(
            text = stringResource(R.string.ui_label_file_browsing),
            icon = R.drawable.baseline_folder_24,
            modifier = Modifier.focusRequester(focusRequester),
            onClick = { homeNavController.navigate("FileHomePage") }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryCard(
    item: HistoryWithMetadata,
    navController: NavController,
    hideDetails: Boolean
) {
    val context = LocalContext.current
    val backdrop = item.metadata?.backdropPath ?: item.metadata?.posterPath
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Card(
            onClick = {
                val encodedUri = item.history.mediaUri.toBase64()
                val encodedFileName = item.history.fileName.toBase64()
                val connectionName = item.history.connectionName.toBase64()
                // 跨 module 的 public 属性不能 smart cast，先取本地变量
                val meta = item.metadata
                if (meta != null && !hideDetails) {
                    if (meta.mediaType == "movie") {
                        navController.navigate("MovieDetails/$encodedUri/${meta.dataSourceType}/$encodedFileName/$connectionName/${meta.tmdbId}")
                    } else {
                        navController.navigate("TVSeriesDetails/$encodedUri/${meta.dataSourceType}/$encodedFileName/$connectionName/${meta.tmdbId}/${meta.seasonNumber}/${meta.episodeNumber}")
                    }
                } else {
                    navController.navigate("VideoPlayer/$encodedUri/${item.history.protocolName}/$encodedFileName/$connectionName")
                }
            },
            shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    inset = 0.dp
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.1f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (backdrop != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(org.mz.mzdkplayer.tool.Tools.formatImageUrl(backdrop, "w500"))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "没有图像",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // 显示进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                        .align(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.history.getPlaybackPercentage() / 100f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.metadata?.title ?: item.history.fileName,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        val meta = item.metadata
        val subtitleText = when (meta?.mediaType) {
            "tv" -> "S${meta.seasonNumber} E${meta.episodeNumber}"
            "movie" -> meta.releaseDate?.take(4) ?: ""
            else -> item.history.protocolName
        }
        if (subtitleText.isNotEmpty()) {
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    movie: MediaCacheEntity,
    navController: NavController,
    hideDetails: Boolean
) {
    val backdrop = movie.backdropPath ?: movie.posterPath
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Card(
            onClick = {
                val encodedUri = movie.videoUri.toBase64()
                val encodedFileName = movie.fileName.toBase64()
                val connectionName = movie.connectionName.toBase64()
                if (!hideDetails) {
                    if (movie.mediaType == "movie") {
                        navController.navigate("MovieDetails/$encodedUri/${movie.dataSourceType}/$encodedFileName/$connectionName/${movie.tmdbId}")
                    } else {
                        navController.navigate("TVSeriesDetails/$encodedUri/${movie.dataSourceType}/$encodedFileName/$connectionName/${movie.tmdbId}/${movie.seasonNumber}/${movie.episodeNumber}")
                    }
                } else {
                    navController.navigate("VideoPlayer/$encodedUri/${movie.dataSourceType}/$encodedFileName/$connectionName")
                }
            },
            shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    inset = 0.dp
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.1f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (backdrop != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(org.mz.mzdkplayer.tool.Tools.formatImageUrl(backdrop, "w500"))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "没有图像",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        val subtitleText = if (movie.mediaType == "tv") {
            "S${movie.seasonNumber} E${movie.episodeNumber}"
        } else {
            movie.releaseDate?.take(4) ?: ""
        }
        if (subtitleText.isNotEmpty()) {
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileHistoryCard(item: MediaHistoryEntity, navController: NavController) {
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        Card(
            onClick = {
                val encodedUri = item.mediaUri.toBase64()
                val encodedFileName = item.fileName.toBase64()
                val connectionName = item.connectionName.toBase64()
                navController.navigate("VideoPlayer/$encodedUri/${item.protocolName}/$encodedFileName/$connectionName")
            },
            shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, Color.White),
                    inset = 0.dp
                )
            ),
            scale = CardDefaults.scale(focusedScale = 1.1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.protocolName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.fileName,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = item.protocolName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
