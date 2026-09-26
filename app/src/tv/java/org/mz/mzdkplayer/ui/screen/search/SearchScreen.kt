package org.mz.mzdkplayer.ui.screen.search

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import org.mz.mzdkplayer.di.RepositoryProvider.createSearchViewModel
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.viewmodel.SearchViewModel


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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems

import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.MediaCard
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.library.EpisodeSelectionDialog // 假设你已将其提取或放在同一包下
import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.net.URLEncoder

@Composable
fun SearchScreen(
    navController: NavController, // 需要传入 NavController 进行跳转
    viewModel: SearchViewModel =  viewModelWithFactory {
        createSearchViewModel()
    }
) {
    val query by viewModel.searchQuery.collectAsState()
    val pagedItems = viewModel.searchResults.collectAsLazyPagingItems()

    // --- TV 弹窗状态管理 ---
    val episodes by viewModel.selectedSeriesEpisodes.collectAsState()
    var showEpisodeDialog by remember { mutableStateOf(false) }
    var selectedSeriesName by remember { mutableStateOf("") }
    // 👇 添加一个 FocusRequester
    val buttonFocusRequester = remember { FocusRequester() }

    // 👇 关键：页面加载后，主动把焦点丢给按钮
    LaunchedEffect(Unit) {
        buttonFocusRequester.requestFocus()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, top = 24.dp, end = 48.dp)
    ) {
        // --- 顶部搜索栏 (保持不变) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = stringResource(R.string.ui_label_search_movies_shows),
                modifier = Modifier.weight(1f).focusRequester(buttonFocusRequester),
                colors = myTTFColor(),
                textStyle = TextStyle(color = Color.White),
            )
            Spacer(modifier = Modifier.width(16.dp))
            MyIconButton(
                text = if (query.isNotEmpty()) stringResource(R.string.ui_label_clear) else stringResource(R.string.ui_label_search),
                icon = if (query.isNotEmpty()) R.drawable.baseline_search_24 else R.drawable.baseline_search_24,
                onClick = { if (query.isNotEmpty()) viewModel.onSearchQueryChanged("") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 结果展示区域 ---
        if (pagedItems.itemCount == 0 && query.isNotEmpty() && pagedItems.loadState.refresh is LoadState.NotLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ui_label_no_results_found), color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pagedItems.itemCount) { index ->
                    val media = pagedItems[index]
                    if (media != null) {
                        // 提取年份逻辑
                        val releaseDate = media.releaseDate
                        val episodeAirDate = media.episodeAirDate
                        val year = when {
                            !releaseDate.isNullOrEmpty() -> releaseDate.take(4)
                            !episodeAirDate.isNullOrEmpty() -> episodeAirDate.take(4)
                            else -> ""
                        }

                        MediaCard(
                            title = media.title,
                            posterPath = media.posterPath,
                            year = year,
                            onClick = {
                                // === 核心逻辑修改：区分 TV 和 电影 ===
                                if (media.mediaType == "tv") {
                                    // 1. 如果是电视剧，设置名称，加载集数，显示弹窗
                                    selectedSeriesName = media.title
                                    viewModel.loadEpisodes(media.tmdbId)
                                    showEpisodeDialog = true
                                } else {
                                    // 2. 如果是电影，直接跳转到详情页或播放页
                                    // 这里沿用你的 URI 编码逻辑，但针对电影结构可能略有不同
                                    // 假设电影也跳一样的详情页，或者你有单独的 MovieDetails
                                    val encodedUri = media.videoUri.toBase64()
                                    val encodedFileName = media.fileName.toBase64()
                                    val connectionName = media.connectionName.toBase64()

                                    // 注意：电影没有 Season/Episode，传 0 或其他默认值
                                    navController.navigate(
                                        "MovieDetails/$encodedUri/${media.dataSourceType}/$encodedFileName/$connectionName/${media.tmdbId}"
                                    )
                                }
                            },
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }

    // --- 整合 TV 集数选择弹窗 ---
    if (showEpisodeDialog) {
        // 直接复用你现有的 EpisodeSelectionDialog 组件
        EpisodeSelectionDialog(
            title = selectedSeriesName,
            episodes = episodes,
            onDismiss = {
                showEpisodeDialog = false
                viewModel.clearSelectedEpisodes()
            },
            onEpisodeClick = { episode ->
                showEpisodeDialog = false
                viewModel.clearSelectedEpisodes() // 跳转前清理

                // 执行跳转逻辑 (与 TvLibraryScreen 一致)
                            val encodedUri = episode.videoUri.toBase64()
                val encodedFileName = episode.fileName.toBase64()
                val connectionName = episode.connectionName.toBase64()

                navController.navigate(
                    "TVSeriesDetails/$encodedUri/${episode.dataSourceType}/$encodedFileName/$connectionName/${episode.tmdbId}/${episode.seasonNumber}/${episode.episodeNumber}"
                )
            },
            navController = navController
        )
    }
}