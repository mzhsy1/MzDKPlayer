package org.mz.mzdkplayer.ui.screen.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.TVSeriesDetails
import org.mz.mzdkplayer.data.model.TVEpisode // 确保导入你新定义的真实数据类
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.Tools.getCountryName
import org.mz.mzdkplayer.tool.viewModelWithFactory

import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.LocalizedStatusText
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.movie.ErrorView
import org.mz.mzdkplayer.ui.screen.movie.FullDescriptionDialog
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import java.net.URLEncoder

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
    // 1. 观察 TV Series (剧集整体) 的状态
    val tvSeriesDetails by movieViewModel.tvSeriesResults.collectAsState()

    // 2. 观察 TV Episode (单集) 的状态
    val tvEpisodeDetails by movieViewModel.tvEpisodeResults.collectAsState()

    val videoUriEncoder = URLEncoder.encode(videoUri, "UTF-8")
    val fileNameEncoder = URLEncoder.encode(fileName, "UTF-8")
    val connectionNameEncoder = URLEncoder.encode(connectionName, "UTF-8")

    // 3. 获取 TV 详情 (整体)
    LaunchedEffect(seriesId) {
        if (seriesId > 0) {
            movieViewModel.getTVSeriesDetails(seriesId)
        }
    }


    // 4. 获取 TV 单集详情 (如果季数和集数有效)
    LaunchedEffect(seriesId, currentSeason, currentEpisode) {
        if (seriesId > 0 && currentSeason > 0 && currentEpisode > 0) {
            movieViewModel.getTVEpisodeDetails(
                seriesId = seriesId,
                seasonNumber = currentSeason,
                episodeNumber = currentEpisode
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 这里主要根据 Series 的加载状态决定主界面，单集加载失败不应阻塞主界面显示
        when (val result = tvSeriesDetails) {
            is Resource.Success -> {
                TVSeriesContent(
                    tvSeries = result.data,
                    tvEpisodeState = tvEpisodeDetails, // 将单集的状态传递下去
                    currentSeason = currentSeason,
                    currentEpisode = currentEpisode,
                    onPlayClick = {
                        navController.navigate("VideoPlayer/$videoUriEncoder/$dataSourceType/$fileNameEncoder/$connectionNameEncoder")
                    }
                )
            }

            is Resource.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LoadingScreen(
                        text = "正在加载剧集信息...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f),
                        subtitle = "如果你不想看到详情页，可以在设置中设置不显示详情页"
                    )
                    MyIconButton(
                        text = "立即播放",
                        icon = R.drawable.baseline_play_arrow_24,
                        modifier = Modifier,
                        onClick = { navController.navigate("VideoPlayer/$videoUriEncoder/$dataSourceType/$fileNameEncoder/$connectionNameEncoder") }
                    )


                }

            }

            is Resource.Error -> ErrorView(
                message = "剧集加载失败: ${result.message}",
                onPlayAnyway = { navController.navigate("VideoPlayer/$videoUriEncoder/$dataSourceType/$fileNameEncoder/$connectionNameEncoder") }
            )

            else -> {}
        }
    }
}

@Composable
private fun TVSeriesContent(
    tvSeries: TVSeriesDetails,
    tvEpisodeState: Resource<TVEpisode>, // 接收 Resource 状态
    currentSeason: Int,
    currentEpisode: Int,
    onPlayClick: () -> Unit
) {
    // 控制剧集总体简介弹窗
    var showFullDescDialog by remember { mutableStateOf(false) }
    val watchButtonsFR = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        watchButtonsFR.requestFocus()
    }
    // 1. 背景层 (与 Movie 保持一致)
    Box(modifier = Modifier.fillMaxSize()) {
        if (!tvSeries.backdropPath.isNullOrEmpty()) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original${tvSeries.backdropPath}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
        }
        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        startX = 0f, endX = 1600f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 0f
                    )
                )
        )
    }

    // 2. 内容主体
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧海报
        Box(
            modifier = Modifier
                .padding(start = 48.dp, top = 32.dp, bottom = 32.dp)
                .widthIn(300.dp, 320.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!tvSeries.posterPath.isNullOrEmpty()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${tvSeries.posterPath}",
                    contentDescription = tvSeries.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.width(40.dp))

        // 右侧信息列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, end = 48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // === 剧集标题 ===
            item {
                Surface(
                    onClick = { },
                    modifier = Modifier.offset(x = (-8).dp),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        pressedContainerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)))
                    ),
                ) {
                    Text(
                        text = tvSeries.name ?: "未知剧集",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                        color = Color.White
                    )
                }

                if (!tvSeries.originalName.isNullOrEmpty() && tvSeries.originalName != tvSeries.name) {
                    Text(
                        text = tvSeries.originalName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // === 元数据 ===
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFC200), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TMDB ${String.format("%.1f", tvSeries.voteAverage)}",
                            style = MaterialTheme.typography.labelMedium,

                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // 显示年份范围
                    val yearInfo =
                        if (!tvSeries.lastAirDate.isNullOrEmpty() && tvSeries.firstAirDate?.take(4) != tvSeries.lastAirDate.take(
                                4
                            )
                        ) {
                            "${tvSeries.firstAirDate?.take(4)} - ${tvSeries.lastAirDate.take(4)}"
                        } else {
                            tvSeries.firstAirDate?.take(4) ?: ""
                        }
                    Text(
                        text = yearInfo,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "|", color = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))

                    // 显示总季数和集数
                    Text(
                        text = "共 ${tvSeries.numberOfSeasons} 季 / ${tvSeries.numberOfEpisodes} 集",
                        color = Color(0xFF64B5F6),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "|", color = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))

                    LocalizedStatusText(tvSeries.status)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val extraInfo = listOfNotNull(
                    tvSeries.originCountry.joinToString(", ") { getCountryName(it) }
                        .takeIf { it.isNotEmpty() },
                    tvSeries.genreList
                        .joinToString(" / ") { it.name }
                        .takeIf { it.isNotEmpty() }
                ).joinToString("  •  ")

                if (extraInfo.isNotEmpty()) {
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // === 剧集总体简介 ===
            item {
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
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = tvSeries.overview ?: "暂无简介",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 28.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "点击查看完整剧集简介",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // === 当前集数信息板块 (真实数据) ===
            // 只有当 Loading 结束且 Success 时才显示，或者你可以添加 Loading 时的骨架屏
            if (currentSeason > 0 && currentEpisode > 0) {
                when (tvEpisodeState) {
                    is Resource.Success -> {
                        item {
                            CurrentEpisodeInfoSection(
                                season = currentSeason,
                                episode = currentEpisode,
                                details = tvEpisodeState.data
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    is Resource.Loading -> {
                        // 可选：显示一个简单的加载提示，或者占位符
                        item {
                            Text(
                                "正在加载第 $currentEpisode 集信息...",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    // Error 状态下不显示该板块或显示重试，这里暂时隐藏以免影响美观
                    else -> {}
                }
            }

            // === 按钮区域 ===
            item {
                val buttonText = if (currentSeason > 0 && currentEpisode > 0)
                    "播放 S${currentSeason} E${currentEpisode}"
                else
                    "立即播放"

                MyIconButton(
                    text = buttonText,
                    icon = R.drawable.baseline_play_arrow_24,
                    modifier = Modifier
                        .width(220.dp)
                        .focusRequester(watchButtonsFR),
                    onClick = onPlayClick
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showFullDescDialog) {
        FullDescriptionDialog(
            title = tvSeries.name ?: "",
            overview = tvSeries.overview ?: "暂无内容",
            onDismiss = { showFullDescDialog = false }
        )
    }
}

/**
 * 显示当前集的具体信息 (使用真实 TVEpisode 数据类)
 */
@Composable
private fun CurrentEpisodeInfoSection(
    season: Int,
    episode: Int,
    details: TVEpisode
) {
    var isSpoilerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "正在播放",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF12EA89)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // 单集缩略图
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (!details.stillPath.isNullOrEmpty()) {
                    AsyncImage(
                        // 必须添加 TMDB 图片前缀
                        model = "https://image.tmdb.org/t/p/w500${details.stillPath}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 没有图片时的占位
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No Image",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // 集数标签覆盖
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(4.dp)
                ) {
                    Text(text = "S${season} E${episode}", color = Color.White, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 单集文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 处理可能为空的 runtime
                val runtimeText = if (details.runtime != null && details.runtime > 0)
                    " | 时长: ${details.runtime}分钟" else ""

                Text(
                    text = "放送日期: ${details.airDate}$runtimeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 防剧透简介逻辑
                if (details.overview.isNotEmpty()) {
                    if (isSpoilerVisible) {
                        Text(
                            text = details.overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    } else {
                        // 防剧透按钮
                        Surface(
                            onClick = { isSpoilerVisible = true },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "显示单集简介 (可能涉及剧透)",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "暂无本集简介",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}