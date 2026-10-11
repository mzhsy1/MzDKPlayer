package org.mz.mzdkplayer.ui.screen.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
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
import org.mz.mzdkplayer.data.model.RecentConnection
import org.mz.mzdkplayer.data.repository.RecentConnectionRepository
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.tool.Tools.toBase64
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.common.TwoArcLoading
import java.text.DateFormat
import java.util.Date

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
    // 最近访问的协议连接：写入方是五个协议各自的连接列表页，这里只订阅它的全局状态
    val recentConnections by RecentConnectionRepository.recent.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    // 主页搜索框：关键词先存在页面里，回车 / 点「搜索」时带去搜索页执行
    var searchQuery by remember { mutableStateOf("") }
    val submitSearch: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            homeNavController.navigate("SearchPage/${searchQuery.toBase64()}")
        }
    }

    val isLoading = recentlyAdded.loadState.refresh is LoadState.Loading
    val isEmpty = !isLoading && recentlyWatched.isEmpty() && recentlyAccessed.isEmpty() &&
        recentlyAdded.itemCount == 0 && recentConnections.isEmpty()

    // 内容列表的焦点请求器：主页的初始焦点落在这里的第一张卡上
    val mainFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        mainFocusRequester.requestFocus()
    }

    // 数据还没加载完时列表还不存在（显示的是加载态），加载完成后再请求一次
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
                // 顶部固定区的高度：下面的滚动列表按它让位。
                // 不写死高度是因为四种语言的文案长度 / 不同缩放下的行高都不一样
                var headerHeightPx by remember { mutableIntStateOf(0) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // 注意组合顺序：滚动列表在前、顶部搜索区在后。
                    // Compose 找「页面里第一个可聚焦控件」是按组合顺序来的，
                    // 反过来的话初始焦点会落在搜索框上 —— 搜索框一拿到焦点就会弹出软键盘
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = with(LocalDensity.current) { headerHeightPx.toDp() })
                            .focusRequester(mainFocusRequester),
                        contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp),
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

                        // 最近访问的连接 Section：点一下直接回到那个存储的文件列表
                        if (recentConnections.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = stringResource(R.string.ui_label_recent_connections),
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(recentConnections) { connection ->
                                        RecentConnectionCard(
                                            item = connection,
                                            onClick = { navController.navigate(connection.route) }
                                        )
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

                        // 最近访问的文件 Section
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

                    // 顶部固定区：搜索框 + 媒体库快捷入口。
                    // 盖在滚动列表上方（列表已按 headerHeightPx 往下让了位），内容贴着它下面滚
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .onSizeChanged { headerHeightPx = it.height }
                            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 20.dp)
                    ) {
                        HomeSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = submitSearch
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        HomeShortcutRow(homeNavController = homeNavController)
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

// ======================= 主页顶部：搜索框 + 媒体库入口 =======================

/**
 * 主页搜索框。
 *
 * 关键词只在主页暂存，回车（软键盘「搜索」）或点右侧按钮才跳去搜索页执行 ——
 * 搜索页本来就有完整的防抖 + 分页，主页不再重复实现一份。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.ui_label_search_movies_shows),
            modifier = Modifier.weight(1f),
            leadingIcon = R.drawable.baseline_search_24,
            textStyle = TextStyle(color = Color.White),
            // showKeyboardOnFocus = false：主页一进来焦点就在这个框上，别自动弹软键盘；
            // 真要输入时按遥控器「确定」，TvTextField 会把键盘叫出来
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                showKeyboardOnFocus = false
            ),
            onSubmit = onSearch
        )
        Spacer(modifier = Modifier.width(16.dp))
        MyIconButton(
            text = stringResource(R.string.ui_label_search),
            icon = R.drawable.baseline_search_24,
            // 空词没必要跳页，直接禁掉按钮（禁用后也不参与焦点遍历）
            enabled = query.isNotBlank(),
            onClick = onSearch
        )
    }
}

/** 媒体库三个分页的快捷入口：原来它们在侧边栏各占一格，现在收进主页 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeShortcutRow(
    homeNavController: NavController,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeShortcutCard(
            label = stringResource(R.string.ui_label_movies),
            icon = R.drawable.moviefileicon,
            accent = MediaLibraryTab.MOVIE.accent,
            onClick = {
                homeNavController.navigate("MediaLibraryPage?tab=${MediaLibraryTab.MOVIE.key}")
            }
        )
        HomeShortcutCard(
            label = stringResource(R.string.ui_label_series),
            icon = R.drawable.tv24dp,
            accent = MediaLibraryTab.TV.accent,
            onClick = {
                homeNavController.navigate("MediaLibraryPage?tab=${MediaLibraryTab.TV.key}")
            }
        )
        HomeShortcutCard(
            label = stringResource(R.string.ui_label_music),
            icon = R.drawable.librarymusic24dp,
            accent = MediaLibraryTab.MUSIC.accent,
            onClick = {
                homeNavController.navigate("MediaLibraryPage?tab=${MediaLibraryTab.MUSIC.key}")
            }
        )
    }
}

/** 一个小方块入口：图标底 + 名称，聚焦时整块翻成暖白底（与侧边栏选中态同一套配色） */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeShortcutCard(
    label: String,
    icon: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1B1B1F),
            contentColor = Color.White,
            focusedContainerColor = Color(0xFFFFF6E5),
            focusedContentColor = Color(0xFF16151A)
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f, pressedScale = 0.99f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, accent),
                inset = 0.dp
            )
        ),
        modifier = modifier.width(200.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = accent.copy(alpha = if (isFocused) 0.24f else 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) Color(0xFF16151A) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ======================= 主页「最近访问的连接」 =======================

/** 协议 → 图标：与各协议连接列表页用的是同一套图标 */
private fun protocolIcon(type: String): Int = when (type) {
    RecentConnection.TYPE_SMB -> R.drawable.smb
    RecentConnection.TYPE_WEBDAV -> R.drawable.svgwebdavsvg
    RecentConnection.TYPE_FTP -> R.drawable.ftp
    RecentConnection.TYPE_NFS -> R.drawable.svgnfs
    else -> R.drawable.svghttp
}

/** 协议 → 强调色：与各协议连接列表页的 `ConListAccent` 对齐 */
private fun protocolAccent(type: String): Color = when (type) {
    RecentConnection.TYPE_SMB -> Color(0xFF4FC3F7)
    RecentConnection.TYPE_WEBDAV -> Color(0xFFBA68C8)
    RecentConnection.TYPE_FTP -> Color(0xFFFFB74D)
    RecentConnection.TYPE_NFS -> Color(0xFF81C784)
    else -> Color(0xFF64B5F6)
}

/**
 * 「最近访问的连接」卡片：点一下直接回到该存储的文件列表（路由在打开时就存好了，
 * 所以这里不需要知道每个协议的路由参数怎么拼）。
 *
 * 时间用 `DateFormat` 的本地化短格式（如 2026/10/11 15:30），不额外维护一套「几分钟前」文案。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RecentConnectionCard(
    item: RecentConnection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accent = protocolAccent(item.type)
    val visitedText = remember(item.lastVisited) {
        if (item.lastVisited <= 0L) {
            ""
        } else {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(item.lastVisited))
        }
    }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1B1B1F),
            contentColor = Color.White,
            focusedContainerColor = Color(0xFFFFF6E5),
            focusedContentColor = Color(0xFF16151A)
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f, pressedScale = 0.99f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, accent),
                inset = 0.dp
            )
        ),
        modifier = modifier.width(260.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = accent.copy(alpha = if (isFocused) 0.24f else 0.16f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = protocolIcon(item.type)),
                    contentDescription = item.type,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) Color(0xFF16151A) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOf(item.type, visitedText)
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color(0xFF5A544B) else Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
