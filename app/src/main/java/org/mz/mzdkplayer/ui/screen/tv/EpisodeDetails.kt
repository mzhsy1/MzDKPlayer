package org.mz.mzdkplayer.ui.screen.tv//package org.mz.mzdkplayer.ui.screen.tv
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.aspectRatio
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.tv.material3.Border
//import androidx.tv.material3.ClickableSurfaceDefaults
//import androidx.tv.material3.MaterialTheme
//import androidx.tv.material3.Surface
//import androidx.tv.material3.Text
//import coil3.compose.AsyncImage
//import org.mz.mzdkplayer.R
//import org.mz.mzdkplayer.data.model.TVSeriesDetails // 确保导入你的数据类
//import org.mz.mzdkplayer.data.repository.Resource
//import org.mz.mzdkplayer.di.RepositoryProvider
//import org.mz.mzdkplayer.tool.viewModelWithFactory
//import org.mz.mzdkplayer.ui.screen.movie.ErrorView
//import org.mz.mzdkplayer.ui.screen.movie.FullDescriptionDialog
//import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
//import org.mz.mzdkplayer.ui.screen.common.MyIconButton
//import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
//
//// === 临时 Mock 数据类，用于当前集数详情 ===
//@Preview
//@Composable
//fun TVSeriesDetailsScreenMock(
////    videoUri: String,
////    dataSourceType: String,
////    fileName: String,
////    connectionName: String,
////    seriesId: Int,          // 替换 movieId
////    currentSeason: Int,     // 新增参数
////    currentEpisode: Int,    // 新增参数
////    navController: NavHostController,
//
//) {
//    val movieViewModel: MovieViewModel = viewModelWithFactory {
//        RepositoryProvider.createMovieViewModel()
//    }
//    // 1. 观察 TV Series 的状态
//    val tvSeriesDetails by movieViewModel.tvSeriesResults.collectAsState()
//
//    val tvEpisodeDetails by movieViewModel.tvEpisodeResults.collectAsState()
//
//    // 2. 获取 TV 详情
////    LaunchedEffect(seriesId) {
////        if (seriesId > 0) {
////            movieViewModel.getTVSeriesDetails(seriesId)
////        }
////    }
//    LaunchedEffect(Unit) {
//
//            movieViewModel.getTVSeriesDetails(75865)
//
//    }
//
//    LaunchedEffect(Unit) {
//
//        movieViewModel.getTVEpisodeDetails(75865, 1,1)
//
//    }
//
//    Box(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        when (val result = tvSeriesDetails) {
//            is Resource.Success -> {
//                TVSeriesContent(
//                    tvSeries = result.data,
//                    currentSeason = 1,
//                    currentEpisode = 1,
//                    onPlayClick = {
//                        //navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName")
//                    }
//                )
//            }
//            is Resource.Loading -> LoadingScreen(text = "正在加载剧集信息...", modifier = Modifier.fillMaxSize())
//            is Resource.Error -> ErrorView(
//                message = "剧集加载失败: ${result.message}",
//                onPlayAnyway = { //navController.navigate("VideoPlayer/$videoUri/$dataSourceType/$fileName/$connectionName")
//                     }
//            )
//            else -> {}
//        }
//    }
//}
//
//@Composable
//private fun TVSeriesContent(
//    tvSeries: TVSeriesDetails,
//    currentSeason: Int,
//    currentEpisode: Int,
//    onPlayClick: () -> Unit
//) {
//    // 控制剧集总体简介弹窗
//    var showFullDescDialog by remember { mutableStateOf(false) }
//
//    // 获取 Mock 的单集数据 (实际项目中应从 API 获取)
//    val currentEpisodeDetails = remember(currentSeason, currentEpisode) {
//        if (currentSeason > 0 && currentEpisode > 0) {
//            getMockEpisodeDetails(currentSeason, currentEpisode)
//        } else null
//    }
//
//    // 1. 背景层 (与 Movie 保持一致)
//    Box(modifier = Modifier.fillMaxSize()) {
//        if (!tvSeries.backdropPath.isNullOrEmpty()) {
//            AsyncImage(
//                model = "https://image.tmdb.org/t/p/original${tvSeries.backdropPath}",
//                contentDescription = null,
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop,
//                alpha = 0.6f
//            )
//        }
//        // 渐变遮罩
//        Box(
//            modifier = Modifier.fillMaxSize().background(
//                Brush.horizontalGradient(
//                    colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.7f), Color.Transparent),
//                    startX = 0f, endX = 1600f
//                )
//            )
//        )
//        Box(
//            modifier = Modifier.fillMaxSize().background(
//                Brush.verticalGradient(
//                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), // 底部加深一点以便阅读
//                    startY = 0f
//                )
//            )
//        )
//    }
//
//    // 2. 内容主体
//    Row(
//        modifier = Modifier.fillMaxSize(),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // 左侧海报
//        Box(
//            modifier = Modifier
//                .padding(start = 48.dp, top = 32.dp, bottom = 32.dp)
//                .width(320.dp)
//                .aspectRatio(2f / 3f),
//            contentAlignment = Alignment.Center
//        ) {
//            if (!tvSeries.posterPath.isNullOrEmpty()) {
//                AsyncImage(
//                    model = "https://image.tmdb.org/t/p/w500${tvSeries.posterPath}",
//                    contentDescription = tvSeries.name,
//                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
//                    contentScale = ContentScale.Crop
//                )
//            } else {
//                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.Gray))
//            }
//        }
//
//        Spacer(modifier = Modifier.width(40.dp))
//
//        // 右侧信息列表
//        LazyColumn(
//            modifier = Modifier.weight(1f).fillMaxSize(),
//            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, end = 48.dp),
//            verticalArrangement = Arrangement.Center
//        ) {
//            // === 剧集标题 ===
//            item {
//                Text(
//                    text = tvSeries.name ?: "未知剧集",
//                    style = MaterialTheme.typography.displayMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = Color.White
//                )
//                if (!tvSeries.originalName.isNullOrEmpty() && tvSeries.originalName != tvSeries.name) {
//                    Text(
//                        text = tvSeries.originalName,
//                        style = MaterialTheme.typography.titleMedium,
//                        color = Color.LightGray,
//                        fontStyle = FontStyle.Italic,
//                        modifier = Modifier.padding(top = 4.dp)
//                    )
//                }
//                Spacer(modifier = Modifier.height(16.dp))
//            }
//
//            // === 元数据 (针对 TV 调整) ===
//            item {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Box(
//                        modifier = Modifier
//                            .background(Color(0xFFFFC200), RoundedCornerShape(4.dp))
//                            .padding(horizontal = 6.dp, vertical = 2.dp)
//                    ) {
//                        Text(
//                            text = "TMDB ${String.format("%.1f", tvSeries.voteAverage)}",
//                            style = MaterialTheme.typography.labelMedium,
//                            color = Color.Black,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(12.dp))
//
//                    // 显示年份范围
//                    val yearInfo = if (!tvSeries.lastAirDate.isNullOrEmpty() && tvSeries.firstAirDate?.take(4) != tvSeries.lastAirDate.take(4)) {
//                        "${tvSeries.firstAirDate?.take(4)} - ${tvSeries.lastAirDate.take(4)}"
//                    } else {
//                        tvSeries.firstAirDate?.take(4) ?: ""
//                    }
//                    Text(text = yearInfo, color = Color.White, style = MaterialTheme.typography.bodyMedium)
//
//                    Spacer(modifier = Modifier.width(12.dp))
//                    Text(text = "|", color = Color.Gray)
//                    Spacer(modifier = Modifier.width(12.dp))
//
//                    // 显示总季数和集数
//                    Text(
//                        text = "共 ${tvSeries.numberOfSeasons} 季 / ${tvSeries.numberOfEpisodes} 集",
//                        color = Color(0xFF64B5F6), // 淡蓝色突出
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//
//                val extraInfo = listOfNotNull(
//                    tvSeries.originCountry.joinToString(", ").takeIf { it.isNotEmpty() },
//                    tvSeries.genreList.joinToString(" / ") { it.name }.takeIf { it.isNotEmpty() }
//                ).joinToString("  •  ")
//
//                if (extraInfo.isNotEmpty()) {
//                    Text(text = extraInfo, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
//                }
//                Spacer(modifier = Modifier.height(24.dp))
//            }
//
//            // === 剧集总体简介 (可点击) ===
//            item {
//                Surface(
//                    onClick = { showFullDescDialog = true },
//                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
//                    colors = ClickableSurfaceDefaults.colors(
//                        containerColor = Color.Transparent,
//                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
//                        pressedContainerColor = Color.White.copy(alpha = 0.2f)
//                    ),
//                    border = ClickableSurfaceDefaults.border(
//                        focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)))
//                    ),
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Column(modifier = Modifier.padding(8.dp)) {
//                        Text(
//                            text = tvSeries.overview ?: "暂无简介",
//                            style = MaterialTheme.typography.bodyLarge,
//                            color = Color.White.copy(alpha = 0.9f),
//                            lineHeight = 28.sp,
//                            maxLines = 3,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                        Text(
//                            text = "点击查看完整剧集简介",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = Color.Gray,
//                            modifier = Modifier.padding(top = 4.dp)
//                        )
//                    }
//                }
//                Spacer(modifier = Modifier.height(24.dp))
//            }
//
//            // === 当前集数信息板块 (如果参数 > 0) ===
//            if (currentEpisodeDetails != null) {
//                item {
//                    CurrentEpisodeInfoSection(
//                        season = currentSeason,
//                        episode = currentEpisode,
//                        details = currentEpisodeDetails
//                    )
//                    Spacer(modifier = Modifier.height(24.dp))
//                }
//            }
//
//            // === 按钮区域 ===
//            item {
//                MyIconButton(
//                    text = if (currentEpisodeDetails != null) "播放 S${currentSeason} E${currentEpisode}" else "立即播放",
//                    icon = R.drawable.baseline_play_arrow_24,
//                    modifier = Modifier.width(220.dp), // 稍微宽一点以容纳文字
//                    onClick = onPlayClick
//                )
//                Spacer(modifier = Modifier.height(48.dp))
//            }
//        }
//    }
//
//    // 保持使用原有的 FullDescriptionDialog (代码复用)
//    if (showFullDescDialog) {
//        FullDescriptionDialog(
//            title = tvSeries.name ?: "",
//            overview = tvSeries.overview ?: "暂无内容",
//            onDismiss = { showFullDescDialog = false }
//        )
//    }
//}
//
///**
// * 显示当前集的具体信息
// * 包含：标题、防剧透简介、缩略图
// */
//@Composable
//private fun CurrentEpisodeInfoSection(
//    season: Int,
//    episode: Int,
//    details: EpisodeDetails
//) {
//    var isSpoilerVisible by remember { mutableStateOf(false) }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Color(0xFF1E1E1E).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
//            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
//            .padding(16.dp)
//    ) {
//        Text(
//            text = "正在播放",
//            style = MaterialTheme.typography.labelSmall,
//            color = Color(0xFF12EA89)
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Row(modifier = Modifier.fillMaxWidth()) {
//            // 单集缩略图
//            Box(
//                modifier = Modifier
//                    .width(160.dp)
//                    .aspectRatio(16f / 9f)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(Color.Black)
//            ) {
//                if (!details.stillPath.isNullOrEmpty()) {
//                    AsyncImage(
//                        model = details.stillPath,
//                        contentDescription = null,
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                }
//                // 集数标签覆盖
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.BottomStart)
//                        .background(Color.Black.copy(alpha = 0.7f))
//                        .padding(4.dp)
//                ) {
//                    Text(text = "S${season} E${episode}", color = Color.White, fontSize = 10.sp)
//                }
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            // 单集文字信息
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = details.name,
//                    style = MaterialTheme.typography.titleMedium,
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                    text = "放送日期: ${details.airDate} | 时长: ${details.runtime}分钟",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = Color.Gray
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // 防剧透简介逻辑
//                if (isSpoilerVisible) {
//                    Text(
//                        text = details.overview,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = Color.LightGray,
//                        maxLines = 4,
//                        overflow = TextOverflow.Ellipsis,
//                        lineHeight = 20.sp
//                    )
//                } else {
//                    // 防剧透按钮
//                    Surface(
//                        onClick = { isSpoilerVisible = true },
//                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
//                        colors = ClickableSurfaceDefaults.colors(
//                            containerColor = Color.White.copy(alpha = 0.1f),
//                            focusedContainerColor = Color.White.copy(alpha = 0.2f)
//                        ),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Row(
//                            modifier = Modifier.padding(8.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.Center
//                        ) {
//                            Text(
//                                text = "显示单集简介 (可能涉及剧透)",
//                                color = Color.Gray,
//                                style = MaterialTheme.typography.labelSmall
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//// === Mock 数据生成方法 ===
//// 模拟日本番剧（类似鬼灭之刃/咒术回战风格）的单集信息
//private fun getMockEpisodeDetails(season: Int, episode: Int): EpisodeDetails {
//    // 这里使用静态图片作为 Mock，实际应为 API 获取
//    // 这里的图片是一个通用的 Anime 风景图
//    val mockImage = "https://image.tmdb.org/t/p/w500/mHviD0JqaC4eAkY6gFVEhQX2qWL.jpg"
//
//    return EpisodeDetails(
//        name = "橡皮／值日／鬼脸／百元", // 模拟日式标题
//        overview = "在橡皮擦上写上喜欢的人的名字，用完以后便能两情相悦。上课时高木向西片微笑着借橡皮擦，并好奇橡皮上会写着什么，此时西片紧张不已……西片的橡皮上到底写着什么？而高木的橡皮上写的文字又是……",
//        stillPath = mockImage,
//        airDate = "2024-11-12",
//        runtime = 24
//    )
//}
//
//// 注意：FullDescriptionDialog 和 ErrorView 复用你之前代码中的定义即可，这里不需要重复粘贴
//// 除非它们是 private 且不在同一个文件中。如果在同一个文件中，直接使用即可。