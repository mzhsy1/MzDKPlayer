package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.MediaItem
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.tool.MediaInfoExtractorFormFileName
import org.mz.mzdkplayer.tool.PhoneScrapeLogic
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel

/** 写库是异步的，返回上一页前留一点时间，免得列表还读到旧记录 */
private const val AFTER_MATCH_DELAY_MS = 800L

/**
 * 手机端「手动匹配」页（第四阶段）。
 *
 * 对应电视端的 `EditTMDBInfoScreen`：文件名解析出关键词与类型后自动搜一次，
 * 用户改关键词 / 换电影与剧集 / 填季集，点结果即调用
 * [MovieViewModel.updateMediaMapping] 覆盖 `media_cache` 里该文件的那一条。
 *
 * 与电视端的差异：不做分栏大屏布局，改成「当前匹配 + 搜索条件 + 结果列表」的纵向单列。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneMatchScreen(
    videoUri: String,
    dataSourceType: String,
    fileName: String,
    connectionName: String,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    onBack: () -> Unit,
) {
    val searchResults by movieViewModel.manualSearchResults.collectAsState()
    val cachedMeta by mediaMetaViewModel.mediaMeta.collectAsState()

    val initialInfo = remember(fileName) { MediaInfoExtractorFormFileName.extract(fileName) }

    var keyword by remember { mutableStateOf(initialInfo.title) }
    var isMovie by remember { mutableStateOf(initialInfo.mediaType != "tv") }
    var seasonText by remember { mutableStateOf(initialInfo.season.ifEmpty { "1" }) }
    var episodeText by remember { mutableStateOf(initialInfo.episode.ifEmpty { "1" }) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val matchedText = stringResource(R.string.phone_match_saved)

    // 一进来先查一次当前匹配，再按解析出的关键词自动搜一次
    LaunchedEffect(videoUri) { mediaMetaViewModel.load(videoUri) }
    LaunchedEffect(Unit) {
        if (keyword.isNotBlank()) {
            movieViewModel.searchMediaManual(keyword, isMovie)
        }
    }

    val search: () -> Unit = {
        if (keyword.isNotBlank()) {
            movieViewModel.searchMediaManual(keyword, isMovie)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_label_correct_matching_info)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.phone_action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            CurrentMatchCard(meta = cachedMeta?.toScrapeMeta())

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(stringResource(R.string.ui_label_search_keyword_name)) },
                placeholder = { Text(stringResource(R.string.ui_label_enter_movie_series_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                trailingIcon = {
                    IconButton(onClick = search) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.ui_label_search),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isMovie,
                    onClick = { isMovie = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {},
                    label = { Text(stringResource(R.string.ui_label_movies)) },
                )
                SegmentedButton(
                    selected = !isMovie,
                    onClick = { isMovie = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {},
                    label = { Text(stringResource(R.string.ui_label_series)) },
                )
            }

            if (!isMovie) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        value = seasonText,
                        onValueChange = { seasonText = it },
                        label = stringResource(R.string.ui_label_season),
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = episodeText,
                        onValueChange = { episodeText = it },
                        label = stringResource(R.string.ui_label_episode),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Button(onClick = search, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_label_search))
            }

            // 结果列表吃掉剩余高度；Box 是为了能在这里用 ColumnScope 的 weight
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                SearchResults(
                    results = searchResults,
                    onPick = { item ->
                        movieViewModel.updateMediaMapping(
                            videoUri = videoUri,
                            selectedMedia = item,
                            seasonNumber = seasonText.toIntOrNull() ?: 0,
                            episodeNumber = episodeText.toIntOrNull() ?: 0,
                            originalFileName = fileName,
                            dataSourceType = dataSourceType,
                            connectionName = connectionName,
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar(matchedText)
                            delay(AFTER_MATCH_DELAY_MS)
                            onBack()
                        }
                    },
                )
            }
        }
    }
}

/** 当前文件在 `media_cache` 里的那一条，没有就是「还没刮削」 */
@Composable
private fun CurrentMatchCard(meta: PhoneScrapeLogic.Meta?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PosterThumb(
                posterPath = meta?.posterPath,
                width = 48.dp,
                height = 72.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.phone_match_current),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = meta?.title ?: stringResource(R.string.phone_match_none),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                meta?.year?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        // 季集号只收数字，与电视端 `EditTMDBInfoScreen` 同一口径
        onValueChange = { input -> if (input.all { it.isDigit() }) onValueChange(input) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SearchResults(
    results: Resource<List<MediaItem>>,
    onPick: (MediaItem) -> Unit,
) {
    when (results) {
        is Resource.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.ui_label_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is Resource.Error -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = results.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        is Resource.Success -> {
            val items = results.data
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.phone_match_no_result),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ResultRow(item = item, onClick = { onPick(item) })
                }
            }
        }
    }
}

@Composable
private fun ResultRow(item: MediaItem, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        leadingContent = {
            PosterThumb(posterPath = item.posterPath, width = 40.dp, height = 56.dp)
        },
        supportingContent = {
            val year = item.releaseDate?.take(4).orEmpty()
            if (year.isNotEmpty()) Text(year)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = item.title.orEmpty(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
