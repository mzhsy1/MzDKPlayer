package org.mz.mzdkplayer.ui.screen.tv

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import androidx.palette.graphics.Palette
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.BitmapImage
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.TVEpisode
import org.mz.mzdkplayer.data.model.TVSeriesDetails
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.Tools.getCountryName
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.LoadingScreenWithSub
import org.mz.mzdkplayer.ui.screen.common.LocalizedStatusText
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.movie.ErrorView
import org.mz.mzdkplayer.ui.screen.movie.FullDescriptionDialog
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.tool.Tools.fromBase64
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.net.URLEncoder
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun TVSeriesDetailsScreen(
    videoUri: String,
    dataSourceType: String,
    fileName: String,
    connectionName: String,
    seriesId: Int,
    currentSeason: Int,
    currentEpisode: Int,
    navController: NavHostController,
    movieViewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }
) {
    val tvSeriesDetails by movieViewModel.tvSeriesResults.collectAsState()
    val tvEpisodeDetails by movieViewModel.tvEpisodeResults.collectAsState()

    // 🟢 核心修复：传入的参数已经是 Base64 编码的，不需要再次编码
    val decodedUri = remember(videoUri) { videoUri.fromBase64() }
    val decodedFileName = remember(fileName) { fileName.fromBase64() }
    val decodedConnectionName = remember(connectionName) { connectionName.fromBase64() }

    LaunchedEffect(seriesId, currentSeason, currentEpisode) {
        if (seriesId > 0 && currentSeason > 0 && currentEpisode > 0) {
            movieViewModel.getTVDetailsWithCache(
                seriesId = seriesId,
                season = currentSeason,
                episode = currentEpisode,
                videoUri = decodedUri,
                dataSourceType,
                decodedFileName,
                decodedConnectionName
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val result = tvSeriesDetails) {
            is Resource.Success -> {
                TVSeriesContent(
                    tvSeries = result.data,
                    tvEpisodeState = tvEpisodeDetails,
                    currentSeason = currentSeason,
                    currentEpisode = currentEpisode,
                    onPlayClick = {
                        // 🟢 使用传入的原始编码后的字符串
                        navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName")
                    },
                    dataSourceType = dataSourceType,
                    fileName = decodedFileName,
                    connectionName = decodedConnectionName
                )
            }

            is Resource.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LoadingScreenWithSub(
                        text = stringResource(R.string.ui_label_loading_tv_series_info),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f),
                        subtitle = stringResource(R.string.ui_label_tip_hide_details_page_in_settings)
                    )
                    MyIconButton(
                        text =stringResource(R.string.ui_label_play_now),
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
                message = stringResource(R.string.ui_label_tv_series_loading_failed,result.message),
                onPlayAnyway = { 
                    // 🟢 同样使用原始编码字符串
                    navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName") 
                }
            )

        }
    }
}

@Composable
private fun TVSeriesContent(
    tvSeries: TVSeriesDetails,
    tvEpisodeState: Resource<TVEpisode>,
    currentSeason: Int,
    currentEpisode: Int,
    onPlayClick: () -> Unit,
    dataSourceType: String,
    fileName: String,
    connectionName: String
) {
    var showFullDescDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val titleFR = remember { FocusRequester() }

    // 主题色
    var themeColor by remember { mutableStateOf(Color(0xFF121212)) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        //listState.scrollToItem(0)
        //titleFR.requestFocus()
    }

    // 背景图URL
    val bgImageUrl = remember(tvEpisodeState, tvSeries) {
        if (tvEpisodeState is Resource.Success && !tvEpisodeState.data.stillPath.isNullOrEmpty()) {
            org.mz.mzdkplayer.tool.Tools.formatImageUrl(tvEpisodeState.data.stillPath, "w1280")
        } else if (!tvSeries.backdropPath.isNullOrEmpty()) {
            org.mz.mzdkplayer.tool.Tools.formatImageUrl(tvSeries.backdropPath, "w1280")
        } else {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(themeColor)) {
        // --- 背景层 ---
        if (bgImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bgImageUrl)
                    .allowHardware(false)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { result ->
                    val image = result.result.image
                    val bitmap = (image as? BitmapImage)?.bitmap
                        ?: image.asDrawable(context.resources).toBitmap()
                    bitmap.let { bmp ->
                        Palette.from(bmp).generate { palette ->
                            val dominant = palette?.darkVibrantSwatch?.rgb ?: palette?.dominantSwatch?.rgb
                            if (dominant != null) {
                                val original = Color(dominant)
                                // 压暗主题色
                                themeColor = Color(
                                    red = original.red * 0.25f,
                                    green = original.green * 0.25f,
                                    blue = original.blue * 0.25f,
                                    alpha = 1f
                                )
                            }
                        }
                    }
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        // --- 优化的渐变遮罩系统 ---
        // 1. 主垂直渐变 (更平滑的过渡)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,                    // 0%
                            Color.Transparent,                    // 50% 保持透明
                            themeColor.copy(alpha = 0.3f),       // 65% 开始渐变
                            themeColor.copy(alpha = 0.6f),       // 75% 加强
                            themeColor.copy(alpha = 0.85f),      // 85% 接近不透明
                            themeColor                           // 100% 完全覆盖
                        ),
                        startY = 0f,
                        endY = 1800f // 更大的渐变范围
                    )
                )
        )

        // 2. 左侧电影感暗角 (改善文字可读性)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            themeColor.copy(alpha = 0.4f)
                        ),
                        center = Offset(-0.3f, 0.5f), // 偏左中心
                        radius = 1.8f
                    )
                )
        )

        // --- 内容滚动层 ---
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
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
                            .height(380.dp) // 约1/4屏幕高度
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
                            .padding(start = 56.dp, end = 56.dp, bottom = 30.dp)
                    ) {
                        // 剧集标题
                        Surface(
                            onClick = { onPlayClick() },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                pressedContainerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)))
                            ),
                            modifier = Modifier.offset(x = (-8).dp)
                        ) {
                            Text(
                                text = "${tvSeries.name} S${currentSeason}·E${currentEpisode}",
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).focusRequester(titleFR),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 元数据行 (精简版)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 评分
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFC200), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TMDB ${String.format(LocalLocale.current.platformLocale, "%.1f", tvSeries.voteAverage)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 年份
                            Text(
                                text = tvSeries.firstAirDate?.take(4) ?: "",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "•", color = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))

                            // 主要类型 (只显示前两个)
                            val mainGenres = tvSeries.genreList.take(2).joinToString(" • ") { it.name }
                            if (mainGenres.isNotEmpty()) {
                                Text(
                                    text = mainGenres,
                                    color = Color(0xFFCCCCCC),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 剧集简介 (精简版)
                        Surface(
                            onClick = { showFullDescDialog = true },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                pressedContainerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)))
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = tvSeries.overview ?: stringResource(R.string.ui_label_no_introduction_yet),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        shadow = Shadow(color = Color.Black, blurRadius = 4f)
                                    ),
                                    color = Color.White.copy(alpha = 0.95f),
                                    lineHeight = 24.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.ui_label_press_ok_for_full_summary),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 播放按钮
                        val buttonText = if (currentSeason > 0 && currentEpisode > 0) {
                            // 传入 ID 和两个参数：Season 和 Episode
                            stringResource(R.string.ui_label_play_season_episode, currentSeason, currentEpisode)
                        } else {
                            stringResource(R.string.ui_label_play_now)
                        }

                        MyIconButton(
                            text = buttonText,
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
                    // 当前播放信息
                    Text(
                        text = stringResource(R.string.ui_label_current_playback_info),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // 当前单集详情卡片
                    if (currentSeason > 0 && currentEpisode > 0) {
                        when (tvEpisodeState) {
                            is Resource.Success -> {
                                CurrentEpisodeInfoSection(
                                    season = currentSeason,
                                    episode = currentEpisode,
                                    details = tvEpisodeState.data,
                                    containerColor = Color.White.copy(alpha = 0.08f)
                                )
                            }
                            is Resource.Loading -> {
                                Text(stringResource(R.string.ui_label_loading_episode_details), color = Color.Gray)
                            }
                            else -> {}
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 剧集完整信息卡片 (将次要信息移到这里)
                    SeriesAdditionalInfoSection(tvSeries)

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

    if (showFullDescDialog) {
        FullDescriptionDialog(
            title = tvSeries.name ?: "",
            overview = tvSeries.overview ?:stringResource(R.string.ui_label_no_content_available),
            onDismiss = { showFullDescDialog = false }
        )
    }
}

// 新增：剧集附加信息部分
@Composable
private fun SeriesAdditionalInfoSection(tvSeries: TVSeriesDetails) {
    Surface(
        onClick = { },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            pressedContainerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_label_tv_series_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCCCCCC)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 完整季数/集数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ui_label_seasons_episodes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = stringResource(
                        id = R.string.ui_label_seasons_episodes_summary,
                        tvSeries.numberOfSeasons?:0,
                        tvSeries.numberOfEpisodes?:0
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 完整类型列表
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = stringResource(R.string.ui_label_genre),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = tvSeries.genreList.joinToString(" • ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 制作国家
            val countries = tvSeries.originCountry.joinToString(", ") { getCountryName(it) }
            if (countries.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = stringResource(R.string.ui_label_countries_of_origin),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.width(100.dp)
                    )
                    Text(
                        text = countries,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }

            // 状态
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ui_label_status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.width(100.dp)
                )
                LocalizedStatusText(tvSeries.status, )
            }
        }
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

@Composable
private fun FileInformationSection(
    fileName: String,
    dataSourceType: String,
    connectionName: String
) {
    Surface(
        onClick = { },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            pressedContainerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_label_media_source_file),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCCCCCC)
            )
            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(label = stringResource(R.string.ui_label_file_name), value = fileName)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label =  stringResource(R.string.ui_label_data_source), value = dataSourceType)
            if (connectionName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = stringResource(R.string.ui_label_connection_name), value = connectionName)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
private fun CurrentEpisodeInfoSection(
    season: Int,
    episode: Int,
    details: TVEpisode,
    containerColor: Color
) {
    var isSpoilerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ui_label_playing_now),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF12EA89),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "S${season} E${episode}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // 单集缩略图
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (!details.stillPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = org.mz.mzdkplayer.tool.Tools.formatImageUrl(details.stillPath, "w500"),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.ui_label_no_image), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 单集文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 1. 获取基础日期文案
                val airDateText = stringResource(R.string.ui_label_air_date, details.airDate ?: "N/A")

                // 2. 根据条件获取运行时文案（如果 runtime 为空则为空字符串）
                // 跨 module 的 public 属性不能 smart cast，先取本地变量
                val runtime = details.runtime
                val runtimeSuffix = if (runtime != null && runtime > 0) {
                    stringResource(R.string.ui_label_runtime_suffix, runtime)
                } else {
                    ""
                }

                // 3. 在 Text 中显示
                Text(
                    text = "$airDateText$runtimeSuffix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 防剧透简介逻辑
                if (details.overview.isNotEmpty()) {
                    if (isSpoilerVisible) {
                        Text(
                            text = details.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray,
                            lineHeight = 24.sp
                        )
                    } else {
                        Surface(
                            onClick = { isSpoilerVisible = true },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.ui_label_show_episode_summary_spoiler),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.ui_label_no_episode_summary_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}