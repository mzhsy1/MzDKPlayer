package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.tool.PlayerMediaText
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import java.util.Locale

/** 简介默认折叠到几行 */
private const val OVERVIEW_COLLAPSED_LINES = 4

/** `releaseDate` 里的年份只有 4 位数字才算数 */
private fun String?.yearOrNull(): String? = this?.take(4)?.takeIf { it.length == 4 }

/**
 * 手机端「影片详情」页（第四阶段）。
 *
 * 布局是**沉浸式**的：没有 TopAppBar，剧照直接铺到状态栏底下，返回键浮在图上，
 * 剧照底部用一条渐变收进页面背景色再接到标题 —— 视线不会被一条工具栏割断。
 * 下面是「标签胶囊 → 简介 → 播放/重新匹配 → 影片信息 → 文件信息」的单列流。
 *
 * 信息全部来自本地 `media_cache`（[MediaMetaViewModel]），页面本身不联网；
 * 只有「刮削这部影片」这一个按钮会调 [MovieViewModel.batchScrapeVideoInfo] 刮当前这一个文件。
 * 刮削完成后 [MovieViewModel.isScanning] 落下，页面自动回读一次，不用手动刷新。
 */
@Composable
fun PhoneDetailScreen(
    videoUri: String,
    dataSourceType: String,
    fileName: String,
    connectionName: String,
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    onBack: () -> Unit,
    onPlay: (sourceUri: String, dataSourceType: String, title: String) -> Unit,
    onRematch: (sourceUri: String, fileName: String, connectionName: String) -> Unit,
) {
    val meta by mediaMetaViewModel.mediaMeta.collectAsState()
    val scanning by movieViewModel.isScanning.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overviewExpanded by remember { mutableStateOf(false) }

    // media_cache 的主键就是播放地址；换文件、从匹配页回来都会重新读一次
    LaunchedEffect(videoUri) { mediaMetaViewModel.load(videoUri) }

    // 扫描结束回读一次：详情页自己刮的那个文件（或别的页面刮完再进来）会自动刷新
    var wasScanning by remember { mutableStateOf(false) }
    LaunchedEffect(scanning) {
        if (scanning) {
            wasScanning = true
        } else if (wasScanning) {
            wasScanning = false
            mediaMetaViewModel.load(videoUri)
        }
    }

    val title = PlayerMediaText.buildTitle(meta, fileName)
    val scrapeStartedText = stringResource(R.string.phone_detail_scrape_started)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 沉浸式：页面自己吃状态栏/导航栏内边距，Scaffold 不要再插一手
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            DetailHero(meta = meta, title = title, onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetaPills(meta = meta)

                if (meta == null) {
                    UnscrapedCard(
                        onScrape = {
                            scope.launch { snackbarHostState.showSnackbar(scrapeStartedText) }
                            movieViewModel.batchScrapeVideoInfo(
                                videoList = listOf(fileName to videoUri),
                                dataSourceType = dataSourceType,
                                connectionName = connectionName,
                            )
                        },
                    )
                }

                OverviewBlock(
                    overview = meta?.overview.orEmpty(),
                    expanded = overviewExpanded,
                    onToggle = { overviewExpanded = !overviewExpanded },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onPlay(videoUri, dataSourceType, title) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.phone_media_detail_play))
                    }
                    FilledTonalButton(
                        onClick = { onRematch(videoUri, fileName, connectionName) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.phone_media_detail_rematch))
                    }
                }

                movieInfoRows(meta)?.let { rows ->
                    InfoSection(title = stringResource(R.string.phone_detail_movie_info), rows = rows)
                }

                InfoSection(
                    title = stringResource(R.string.phone_detail_file),
                    rows = buildList {
                        add(stringResource(R.string.phone_detail_file_name) to fileName)
                        add(stringResource(R.string.phone_detail_source) to dataSourceType)
                        if (connectionName.isNotBlank()) {
                            add(stringResource(R.string.phone_detail_connection) to connectionName)
                        }
                    },
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 顶部剧照（16:9）+ 渐变 + 标题。
 *
 * 优先用宽幅剧照，没有就退回海报（`ContentScale.Crop` 裁成 16:9），都没有就留一块占位色。
 */
@Composable
private fun DetailHero(meta: MediaCacheEntity?, title: String, onBack: () -> Unit) {
    val heroPath = meta?.backdropPath?.takeIf { it.isNotBlank() } ?: meta?.posterPath
    // 顶部一律压一层深色（没图也压），保证返回键上的白图标在任何图上都看得清；
    // 底部渐变到 surface，下面的内容像是从剧照里"长"出来的
    val scrim = Brush.verticalGradient(
        0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
        0.4f to Color.Transparent,
        1f to MaterialTheme.colorScheme.surface,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        PhonePosterImage(
            posterPath = heroPath,
            size = "w780",
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim),
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.phone_action_back),
                tint = Color.White,
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
        )
    }
}

/** 标签胶囊：类型 / 年份 / 评分 / 季集 / 类型标签，自动换行 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaPills(meta: MediaCacheEntity?) {
    if (meta == null) return

    val isTv = meta.mediaType == "tv"
    val year = meta.releaseDate.yearOrNull()

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(text = stringResource(if (isTv) R.string.ui_label_series else R.string.phone_media_detail_movie))
        year?.let { Pill(text = it) }
        if (meta.voteAverage > 0.0) {
            Pill(text = String.format(Locale.US, "%.1f", meta.voteAverage), withStar = true)
        }
        if (isTv && (meta.seasonNumber > 0 || meta.episodeNumber > 0)) {
            Pill(text = String.format(Locale.US, "S%02dE%02d", meta.seasonNumber, meta.episodeNumber))
        }
        meta.genres.forEach { Pill(text = it.name) }
    }
}

@Composable
private fun Pill(text: String, withStar: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (withStar) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** 没刮削过：说清「为什么只有文件名」，并给一条就地刮削的路 */
@Composable
private fun UnscrapedCard(onScrape: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.phone_detail_unscraped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onScrape) {
                Text(stringResource(R.string.phone_detail_scrape_this))
            }
        }
    }
}

/** 简介：折叠时被截断了才给「展开 / 收起」 */
@Composable
private fun OverviewBlock(overview: String, expanded: Boolean, onToggle: () -> Unit) {
    if (overview.isBlank()) return

    var truncated by remember(overview) { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.phone_detail_overview),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = overview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else OVERVIEW_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) truncated = result.hasVisualOverflow },
        )
        if (expanded || truncated) {
            TextButton(
                onClick = onToggle,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.phone_detail_collapse else R.string.phone_detail_expand
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 「影片信息」的行；没刮削过时返回 null（整段不显示） */
@Composable
private fun movieInfoRows(meta: MediaCacheEntity?): List<Pair<String, String>>? {
    if (meta == null) return null

    val isTv = meta.mediaType == "tv"
    return buildList {
        meta.releaseDate?.takeIf { it.isNotBlank() }?.let {
            add(
                stringResource(
                    if (isTv) R.string.phone_detail_first_air else R.string.phone_detail_release
                ) to it
            )
        }
        // status 非空（刮削成功才会有值，默认是「未知状态」）
        meta.status.takeIf { it.isNotBlank() }?.let {
            add(stringResource(R.string.phone_detail_status) to it)
        }
        if (isTv) {
            meta.numberOfSeasons?.takeIf { it > 0 }?.let {
                add(stringResource(R.string.phone_detail_seasons) to it.toString())
            }
            meta.numberOfEpisodes?.takeIf { it > 0 }?.let {
                add(stringResource(R.string.phone_detail_episodes) to it.toString())
            }
            meta.episodeName?.takeIf { it.isNotBlank() }?.let {
                add(stringResource(R.string.phone_detail_episode_title) to it)
            }
            meta.episodeRuntime?.takeIf { it > 0 }?.let {
                add(
                    stringResource(R.string.phone_detail_runtime) to
                            stringResource(R.string.phone_detail_minutes, it)
                )
            }
        }
        meta.originCountry.takeIf { it.isNotEmpty() }?.let {
            add(stringResource(R.string.phone_detail_region) to it.joinToString(" / "))
        }
    }
}

/** 一段信息卡：小标题 + 「标签 : 值」若干行（值右对齐，长文本自动换行） */
@Composable
private fun InfoSection(title: String, rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                rows.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(76.dp),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
