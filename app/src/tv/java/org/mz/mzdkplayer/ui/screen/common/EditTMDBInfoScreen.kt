package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.viewmodel.MovieViewModel

// 假设 MediaInfoExtractorFormFileName 已经在同一个包或正确导入
import org.mz.mzdkplayer.tool.metadata.MediaInfoExtractorFormFileName
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.di.viewModelWithFactory
import org.mz.mzdkplayer.tool.logic.ScrapeSourcePolicy
import org.mz.mzdkplayer.ui.common.formatSearchSource
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor
import org.mz.mzdkplayer.ui.theme.mySideFilterChipColor

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EditTMDBInfoScreen(
    mediaUri: String,
    navController: NavHostController,
    movieViewModel: MovieViewModel = viewModelWithFactory { RepositoryProvider.createMovieViewModel() }// 确保获取到的是同一个 ViewModel 实例 (或者通过 Hilt注入)
) {
    val context = LocalContext.current
    val searchResults by movieViewModel.manualSearchResults.collectAsState()

    // 1. 解析文件名和初始化状态
    val decodedUri = mediaUri
    // 从 URI 中提取纯文件名 (假设 URI 结构是 smb://.../filename.ext)
    val fileName = remember(decodedUri) {
        decodedUri.substringAfterLast("/")
    }

    // 使用提取器获取初始信息
    val initialInfo = remember(fileName) {
        MediaInfoExtractorFormFileName.extract(fileName)
    }

    // UI 状态
    var searchKeyword by remember { mutableStateOf(initialInfo.title) }
    var isSearchMovie by remember { mutableStateOf(initialInfo.mediaType == "movie") }
    // 搜索源默认跟随设置里选的刮削数据源，用户可以单独切成另一个源
    var searchSource by remember { mutableStateOf(movieViewModel.defaultSearchSource()) }

    // TV 专属状态：季号和集号
    // 如果提取结果为空，默认给 "1"
    var seasonText by remember { mutableStateOf(initialInfo.season.ifEmpty { "1" }) }
    var episodeText by remember { mutableStateOf(initialInfo.episode.ifEmpty { "1" }) }

    // 自动触发一次搜索（可选，如果不想一进来就搜索可以注释掉）
    // 换搜索源也重搜一次：结果列表必须对应当前选中的源，否则点选会用错 id 空间
    LaunchedEffect(searchSource) {
        if (searchKeyword.isNotEmpty()) {
            movieViewModel.searchMediaManual(searchKeyword, isSearchMovie, searchSource)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // 深色背景
            .padding(24.dp)
    ) {
        // --- 左侧：控制面板 (40% 宽度) ---
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_label_correct_matching_info),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.ui_label_file_placeholder, fileName),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 1. 搜索关键词输入
            Text(stringResource(R.string.ui_label_search_keyword_name), color = Color.LightGray, fontSize = 12.sp)
            TvTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = stringResource(R.string.ui_label_enter_movie_series_name),
            )

            // 2. 类型切换 (电影 / 电视剧)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = isSearchMovie,
                    onClick = { isSearchMovie = true },
                    colors = mySideFilterChipColor(),
                    leadingIcon = {if (isSearchMovie) Icon(painterResource(R.drawable.check24dp), contentDescription = null) },
                    modifier = Modifier.padding(end = 8.dp)
                ){
                    Text(stringResource(R.string.ui_label_movies))
                }
                FilterChip(
                    selected = !isSearchMovie,
                    onClick = { isSearchMovie = false },
                    colors = mySideFilterChipColor(),
                    leadingIcon = { if (!isSearchMovie) Icon(painterResource(R.drawable.check24dp), contentDescription = null) },
                    modifier = Modifier.padding(end = 8.dp)
                ){
                    Text(stringResource(R.string.ui_label_series))
                }
            }

            // 2.1 搜索源 (TMDB / 豆瓣)：豆瓣的候选不分电影剧集，类型由上面的芯片决定
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ui_label_search_source),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                for (source in listOf(ScrapeSourcePolicy.TMDB, ScrapeSourcePolicy.DOUBAN)) {
                    val selected = searchSource == source
                    FilterChip(
                        selected = selected,
                        onClick = { searchSource = source },
                        colors = mySideFilterChipColor(),
                        leadingIcon = { if (selected) Icon(painterResource(R.drawable.check24dp), contentDescription = null) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(formatSearchSource(source))
                    }
                }
            }

            // 3. 剧集专属设置 (如果是剧集模式)
            if (!isSearchMovie) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(stringResource(R.string.ui_label_season), color = Color.LightGray, fontSize = 12.sp)
                        TvTextField(
                            value = seasonText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) seasonText = it },
                            placeholder = "1",
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(stringResource(R.string.ui_label_episode), color = Color.LightGray, fontSize = 12.sp)
                        TvTextField(
                            value = episodeText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) episodeText = it },
                            placeholder = "1",
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.ui_label_specify_season_episode_note),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. 搜索按钮（按上面选中的数据源搜）
            MyIconButton(
                text = stringResource(R.string.ui_label_search),
                icon = R.drawable.baseline_search_24, // 假设你有这个图标，如果没有可以用默认的
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (searchKeyword.isNotBlank()) {
                        movieViewModel.searchMediaManual(searchKeyword, isSearchMovie, searchSource)
                    } else {
                        showToast(context, context.getString(R.string.ui_label_please_enter_keyword))
                    }
                }
            )
        }

        // --- 右侧：结果列表 (60% 宽度) ---
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            when (val result = searchResults) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.ui_label_searching), color = Color.White)
                    }
                }
                is Resource.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.ui_label_search_failed, result.message),
                            color = Color.Red
                        )
                    }
                }
                is Resource.Success -> {
                    val list = result.data
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.ui_label_no_related_results_found), color = Color.Gray)
                        }
                    } else {
                        Text(
                            stringResource(R.string.ui_label_search_results_confirm),
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(list) { item ->
                                ListItem(
                                    selected = false,
                                    onClick = {
                                        // 核心保存逻辑
                                        val sNum = seasonText.toIntOrNull() ?: 1
                                        val eNum = episodeText.toIntOrNull() ?: 1

                                        movieViewModel.updateMediaMapping(
                                            videoUri = decodedUri,
                                            selectedMedia = item,
                                            seasonNumber = sNum,
                                            episodeNumber = eNum,
                                            originalFileName = fileName
                                        )

                                        showToast(context, context.getString(R.string.ui_label_corrected_to,item.title))
                                        //navController.popBackStack() // 返回上一页
                                    },
                                    colors = myListItemCoverColor(),
                                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                                    leadingContent = {
                                        AsyncImage(
                                            model = org.mz.mzdkplayer.tool.Tools.formatImageUrl(item.posterPath, "w200"),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(60.dp)
                                                .background(Color.Gray)
                                        )
                                    },
                                    headlineContent = {
                                        item.title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    },
                                    supportingContent = {
                                        val date = item.releaseDate ?: stringResource(R.string.ui_label_unknown_date)
                                        val overview = item.overview.ifBlank {stringResource(R.string.ui_label_no_introduction_yet) }
                                        Column {
                                            Text(date, fontSize = 12.sp)
                                            Text(overview, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}