package org.mz.mzdkplayer.ui.screen.movie

import android.view.KeyEvent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.MovieDetails
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.Tools.getCountryName
import org.mz.mzdkplayer.di.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.DetailAccentGold
import org.mz.mzdkplayer.ui.screen.common.DetailChip
import org.mz.mzdkplayer.ui.screen.common.DetailInfoLabel
import org.mz.mzdkplayer.ui.screen.common.DetailInfoRow
import org.mz.mzdkplayer.ui.screen.common.DetailMetaRow
import org.mz.mzdkplayer.ui.screen.common.DetailPanel
import org.mz.mzdkplayer.ui.screen.common.DetailPanelTitle
import org.mz.mzdkplayer.ui.screen.common.DetailRatingBadge
import org.mz.mzdkplayer.ui.screen.common.ImmersiveBackdrop
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreenWithSub
import org.mz.mzdkplayer.ui.screen.common.LocalizedStatusText
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.tool.Tools.fromBase64
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun MovieDetailsScreen(
    videoUri: String,
    dataSourceType: String,
    fileName: String,
    connectionName: String,
    movieId: Int,
    navController: NavHostController,
    movieViewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }
) {
    val movieDetails by movieViewModel.movieDeResults.collectAsState()

    // 🟢 核心修复：传入的参数已经是 Base64 编码的，不需要再次编码
    // 这里解码出原始值用于显示或数据库查询
    val decodedUri = remember(videoUri) { videoUri.fromBase64() }
    val decodedFileName = remember(fileName) { fileName.fromBase64() }
    val decodedConnectionName = remember(connectionName) { connectionName.fromBase64() }

    LaunchedEffect(movieId) {
        if (movieId > 0) {
            // 使用解码后的 URI 调用带缓存的方法
            movieViewModel.getMovieDetailsWithCache(movieId, decodedUri, dataSourceType, decodedFileName, decodedConnectionName)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val result = movieDetails) {
            is Resource.Success -> {
                MovieContent(
                    movie = result.data,
                    onPlayClick = {
                        // 🟢 使用传入的原始编码后的字符串，避免二次编码导致播放失败
                        navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName")
                    },
                    dataSourceType = dataSourceType,
                    fileName = decodedFileName, // 传入解码后的名称用于显示
                    connectionName = decodedConnectionName // 传入解码后的名称用于显示
                )
            }

            is Resource.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LoadingScreenWithSub(
                        text = stringResource(R.string.ui_label_loading_movie_details),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f),
                        subtitle = stringResource(R.string.ui_label_tip_hide_details_page_in_settings)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MyIconButton(
                        text = stringResource(R.string.ui_label_play_now),
                        icon = R.drawable.baseline_play_arrow_24,
                        modifier = Modifier,
                        onClick = { 
                            // 🟢 同样使用原始编码字符串
                            navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName") 
                        }
                    )
                }
            }

            is Resource.Error -> ErrorView(
                message = stringResource(R.string.ui_label_loading_failed),
                onPlayAnyway = { 
                    // 🟢 同样使用原始编码字符串
                    navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName") 
                }
            )
        }
    }
}

@Composable
private fun MovieContent(
    movie: MovieDetails,
    onPlayClick: () -> Unit,
    dataSourceType: String,
    fileName: String,
    connectionName: String
) {
    // 控制详细简介弹窗的显示
    var showFullDescDialog by remember { mutableStateOf(false) }
    val titleFR = remember { FocusRequester() }
    // 使用 LazyListState 管理滚动
    val listState = rememberLazyListState()

    // 主题色
    var themeColor by remember { mutableStateOf(Color(0xFF121212)) }

    LaunchedEffect(Unit) {
       // listState.scrollToItem(0)
        titleFR.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(themeColor)) {
        // --- 背景层：全屏剧照 + 主题色渐变遮罩（主题色由剧照自动提取） ---
        ImmersiveBackdrop(
            imageUrl = movie.backdropPath
                ?.takeIf { it.isNotEmpty() }
                ?.let { org.mz.mzdkplayer.tool.Tools.formatImageUrl(it, "w1280") },
            themeColor = themeColor,
            onThemeColorExtracted = { themeColor = it }
        )

        // --- 内容滚动层 ---
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp)
        ) {
            // === 第一屏：沉浸式主信息 ===
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(1.0f) // 占满一整屏高度
                        .fillMaxWidth()
                ) {
                    // 信息区域 (位于屏幕下方1/4)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(355.dp) // 约1/4屏幕高度
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        themeColor.copy(alpha = 0.8f),
                                        themeColor
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                            .padding(start = 56.dp, end = 56.dp, bottom = 32.dp)
                    ) {
                        // 电影标题
                        Surface(
                            onClick = { onPlayClick() },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                pressedContainerColor = Color.White.copy(alpha = 0.16f)
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)))
                            ),
                            modifier = Modifier.offset(x = (-8).dp).focusRequester(titleFR)
                        ) {
                            Text(
                                text = movie.title ?: stringResource(R.string.ui_label_unknown_movie),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 6f
                                    )
                                ),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 原始标题（跨 module 的 public 属性不能 smart cast，先取本地变量）
                        val originalTitle = movie.originalTitle
                        if (!originalTitle.isNullOrEmpty() && originalTitle != movie.title) {
                            Text(
                                text = originalTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(top = 0.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 元数据行 (精简版)
                        DetailMetaRow {
                            // 评分
                            DetailRatingBadge(
                                "TMDB ${
                                    String.format(
                                        LocalLocale.current.platformLocale,
                                        "%.1f",
                                        movie.voteAverage
                                    )
                                }"
                            )

                            // 年份
                            val year = movie.releaseDate?.take(4)
                            if (!year.isNullOrEmpty()) {
                                Text(
                                    text = year,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 主要类型 (只显示前两个)
                            val mainGenres = movie.genreList.take(2).joinToString(" • ") { it.name }
                            if (mainGenres.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = mainGenres,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 电影简介 (精简版，限制宽度以获得更好的可读性)
                        Surface(
                            onClick = { showFullDescDialog = true },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                pressedContainerColor = Color.White.copy(alpha = 0.18f)
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)))
                            ),
                            modifier = Modifier.widthIn(max = 860.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(
                                    text = movie.overview,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 6f)
                                    ),
                                    color = Color.White.copy(alpha = 0.92f),
                                    lineHeight = 24.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.ui_label_press_ok_for_full_summary),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 播放按钮
                        MyIconButton(
                            text = stringResource(R.string.ui_label_play_now),
                            icon = R.drawable.baseline_play_arrow_24,
                            modifier = Modifier
                                .width(210.dp),
                            onClick = onPlayClick
                        )
                    }

                    // 向下箭头指示器
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        AnimatedDownArrow()
                    }
                }
            }

            // === 第二屏：详情信息 ===
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColor)
                        .padding(horizontal = 56.dp, vertical = 24.dp)
                ) {
                    // 电影海报卡片
                    MoviePosterSection(movie)

                    Spacer(modifier = Modifier.height(32.dp))

                    // 电影完整信息卡片
                    MovieAdditionalInfoSection(movie)

                    Spacer(modifier = Modifier.height(24.dp))

                    // 文件信息卡片
                    FileInformationSection(
                        fileName = fileName,
                        dataSourceType = dataSourceType,
                        connectionName = connectionName
                    )

                    // 底部留白
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    // 全屏简介弹窗
    if (showFullDescDialog) {
        FullDescriptionDialog(
            title = movie.title ?: "",
            overview = movie.overview,
            onDismiss = { showFullDescDialog = false }
        )
    }
}

@Composable
private fun AnimatedDownArrow() {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "arrowOffset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = dy.dp)
    ) {
        Text(
            text = stringResource(R.string.ui_label_more_information),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Scroll Down",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
    }
}

// 电影海报卡片
@Composable
private fun MoviePosterSection(movie: MovieDetails) {
    DetailPanel(contentPadding = PaddingValues(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 海报图
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                if (!movie.posterPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = org.mz.mzdkplayer.tool.Tools.formatImageUrl(movie.posterPath, "w500"),
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.ui_label_no_poster),
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // 右侧信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title ?: stringResource(R.string.ui_label_unknown_movie),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val originalTitle = movie.originalTitle
                if (!originalTitle.isNullOrEmpty() && originalTitle != movie.title) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = originalTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.55f),
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 评分 / 年份
                val ratingText =
                    "TMDB " + String.format(
                        LocalLocale.current.platformLocale,
                        "%.1f",
                        movie.voteAverage
                    )
                DetailMetaRow {
                    DetailRatingBadge(text = ratingText)

                    val year = movie.releaseDate?.take(4)
                    if (!year.isNullOrEmpty()) {
                        Text(
                            text = year,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 类型标签
                if (movie.genreList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        movie.genreList.take(4).forEach { genre ->
                            DetailChip(text = genre.name)
                        }
                    }
                }
            }
        }
    }
}

// 电影附加信息部分
@Composable
private fun MovieAdditionalInfoSection(movie: MovieDetails) {
    DetailPanel {
        DetailPanelTitle(text = stringResource(R.string.ui_label_movie_details))
        Spacer(modifier = Modifier.height(20.dp))

        // 完整类型列表
        val genres = movie.genreList.joinToString(" • ") { it.name }
        if (genres.isNotEmpty()) {
            DetailInfoRow(label = stringResource(R.string.ui_label_genre), value = genres)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 制作国家
        val countries = movie.originCountry.joinToString(", ") { getCountryName(it) }
        if (countries.isNotEmpty()) {
            DetailInfoRow(
                label = stringResource(R.string.ui_label_countries_of_origin),
                value = countries
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            DetailInfoLabel(text = stringResource(R.string.ui_label_status))
            LocalizedStatusText(movie.status)
        }

        // 上映日期
        val releaseDate = movie.releaseDate
        if (!releaseDate.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailInfoRow(
                label = stringResource(R.string.ui_label_release_date),
                value = releaseDate
            )
        }
    }
}

@Composable
private fun FileInformationSection(
    fileName: String,
    dataSourceType: String,
    connectionName: String
) {
    DetailPanel {
        DetailPanelTitle(text = stringResource(R.string.ui_label_media_source_file))
        Spacer(modifier = Modifier.height(20.dp))

        DetailInfoRow(
            label = stringResource(R.string.ui_label_file_name),
            value = fileName,
            labelWidth = 88.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        DetailInfoRow(
            label = stringResource(R.string.ui_label_data_source),
            value = dataSourceType,
            labelWidth = 88.dp
        )
        if (connectionName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailInfoRow(
                label = stringResource(R.string.ui_label_connection_name),
                value = connectionName,
                labelWidth = 88.dp
            )
        }
    }
}

@Composable
fun FullDescriptionDialog(
    title: String,
    overview: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF262A31), Color(0xFF14161A))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(36.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 标题下的强调线
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DetailAccentGold)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 滚动文本区域
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                var isFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(200f)
                                        }
                                        true
                                    }

                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(-200f)
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                ) {
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 34.sp,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MyIconButton(
                        text = stringResource(R.string.ui_label_close),
                        icon = R.drawable.baseline_close_24,
                        modifier = Modifier.width(140.dp),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onPlayAnyway: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            MyIconButton(
                text = stringResource(R.string.ui_label_try_direct_play),
                icon = R.drawable.baseline_play_arrow_24,
                onClick = onPlayAnyway,
            )
        }
    }
}