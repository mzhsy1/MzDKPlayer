package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.tool.PhoneScrapeLogic
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel

/** 扫描中定期回读数据库的间隔：刮到一条就能在列表里立刻看到海报 */
private const val SCRAPE_REFRESH_INTERVAL_MS = 1_500L

/** 刮削结果 → 列表展示形态（只取列表要用的字段，方便纯逻辑单测） */
internal fun MediaCacheEntity.toScrapeMeta(): PhoneScrapeLogic.Meta = PhoneScrapeLogic.Meta(
    title = title,
    year = releaseDate?.take(4)?.takeIf { it.length == 4 && it.toIntOrNull() != null },
    voteAverage = voteAverage,
    mediaType = mediaType,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    posterPath = posterPath,
    backdropPath = backdropPath,
    overview = overview,
    genres = genres.map { it.name },
)

/**
 * 浏览器页的刮削状态。
 *
 * 由 [rememberPhoneScrapeUi] 组装，页面只需要把它交给 `PhoneBrowserScaffold`：
 * 顶栏会出现「刮削本目录」按钮与进度，行内会显示海报与刮削标题。
 */
internal class PhoneBrowserScrapeUi(
    private val metaByUri: Map<String, PhoneScrapeLogic.Meta>,
    val scanning: Boolean,
    val scannedCount: Int,
    val scanTotal: Int,
    /** 本目录还没刮削过的视频数量（0 表示没得刮） */
    val pendingCount: Int,
    val onScrapeAll: () -> Unit = {},
) {
    fun metaOf(uri: String?): PhoneScrapeLogic.Meta? = uri?.let { metaByUri[it] }

    /** 扫描进度（0..1），没开始时为 0 */
    val progress: Float
        get() = PhoneScrapeLogic.progressPercent(scannedCount, scanTotal) / 100f
}

/**
 * 组装刮削会话。
 *
 * 三件事：
 * 1. 目录变化时批量读一次库里已有的刮削记录（[MediaMetaViewModel.loadMany]）；
 * 2. 开了自动刮削（手机端设置里的开关，默认关）、且本目录还有没刮过的视频时，
 *    交给 [MovieViewModel.batchScrapeVideoInfo] 串行去刮（它自带 1.5 秒间隔，避免打爆 TMDB）；
 * 3. 扫描期间每 1.5 秒回读一次数据库，刮到一条列表里就出现一条。
 *
 * [dataSourceType] / [connectionName] 会写进 `media_cache`，口径与播放页一致。
 */
@Composable
internal fun rememberPhoneScrapeUi(
    movieViewModel: MovieViewModel,
    mediaMetaViewModel: MediaMetaViewModel,
    dataSourceType: String,
    connectionName: String,
    autoScrape: Boolean,
    entries: List<PhoneBrowserEntry>,
    snackbarHostState: SnackbarHostState,
): PhoneBrowserScrapeUi {
    val cache by mediaMetaViewModel.mediaMetaMap.collectAsState()
    val scanning by movieViewModel.isScanning.collectAsState()
    val scannedCount by movieViewModel.currentScanIndex.collectAsState()
    val scanTotal by movieViewModel.totalScanCount.collectAsState()

    val items = remember(entries) {
        entries.map { PhoneScrapeLogic.Item(name = it.name, playbackUri = it.playbackUri) }
    }
    val allTargets = remember(items) { PhoneScrapeLogic.scrapeTargets(items) }
    val videoUris = remember(allTargets) { allTargets.map { it.second } }
    val pending = remember(items, cache) { PhoneScrapeLogic.pendingTargets(items, cache.keys) }

    val metaByUri = remember(cache) { cache.mapValues { it.value.toScrapeMeta() } }

    // 目录换了先读一次库里已有的结果，别让新目录顶着上一个目录的海报
    LaunchedEffect(videoUris) { mediaMetaViewModel.loadMany(videoUris) }

    // 自动刮削：同一个目录只自动发起一次（刮不到的条目不要反复重试）
    var autoRequested by remember(videoUris) { mutableStateOf(false) }
    LaunchedEffect(pending, autoScrape, autoRequested) {
        if (autoScrape && !autoRequested && pending.isNotEmpty() && !movieViewModel.isScanning.value) {
            autoRequested = true
            movieViewModel.batchScrapeVideoInfo(pending, dataSourceType, connectionName)
        }
    }

    // 扫描中滚动回读：扫描状态是 ViewModel 的 StateFlow，循环里直接读最新值
    LaunchedEffect(scanning, videoUris) {
        if (!scanning) return@LaunchedEffect
        while (movieViewModel.isScanning.value) {
            delay(SCRAPE_REFRESH_INTERVAL_MS)
            mediaMetaViewModel.loadMany(videoUris)
        }
        mediaMetaViewModel.loadMany(videoUris)
    }

    val scope = rememberCoroutineScope()
    val nothingToScrapeText = stringResource(R.string.phone_scrape_none)
    val startedText = stringResource(R.string.phone_scrape_started)

    // 每次都新建：它只是一份「当前状态 + 一个动作」，不持有需要跨重组保留的东西
    return PhoneBrowserScrapeUi(
        metaByUri = metaByUri,
        scanning = scanning,
        scannedCount = scannedCount,
        scanTotal = scanTotal,
        pendingCount = pending.size,
        onScrapeAll = {
            if (allTargets.isEmpty()) {
                scope.launch { snackbarHostState.showSnackbar(nothingToScrapeText) }
            } else {
                // 手动点的时候不跳过已有的，`batchScrapeVideoInfo` 自己会跳过「详情已加载」的条目
                scope.launch { snackbarHostState.showSnackbar(startedText.format(allTargets.size)) }
                movieViewModel.batchScrapeVideoInfo(allTargets, dataSourceType, connectionName)
            }
        },
    )
}

/**
 * 顶栏的刮削入口：待刮数量做成角标，扫描中改成进度文字。
 *
 * 扫描中不显示按钮（`batchScrapeVideoInfo` 本来也会忽略重复请求），避免用户以为没反应。
 */
@Composable
internal fun PhoneScrapeAction(ui: PhoneBrowserScrapeUi, modifier: Modifier = Modifier) {
    if (ui.scanning) {
        Text(
            text = stringResource(R.string.phone_scrape_scanning, ui.scannedCount, ui.scanTotal),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(end = 12.dp),
        )
        return
    }
    if (ui.pendingCount <= 0) return

    IconButton(onClick = ui.onScrapeAll, modifier = modifier) {
        BadgedBox(
            badge = {
                Badge { Text(ui.pendingCount.toString()) }
            },
        ) {
            Icon(
                imageVector = PhoneIcons.Scrape,
                contentDescription = stringResource(R.string.phone_scrape_action),
            )
        }
    }
}

/** 扫描中的细进度条，接在顶栏下面 */
@Composable
internal fun PhoneScrapeProgressBar(ui: PhoneBrowserScrapeUi, modifier: Modifier = Modifier) {
    if (!ui.scanning) return
    LinearProgressIndicator(
        progress = { ui.progress },
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * 海报 / 剧照。
 *
 * `posterPath` 为空（没刮到）时返回 false，由调用方回退到图标 —— 这样调用方
 * 不用在 Compose 里做 `if (url == null) Icon() else AsyncImage()` 的重复分支。
 */
@Composable
internal fun PhonePosterImage(
    posterPath: String?,
    modifier: Modifier = Modifier,
    size: String = "w200",
): Boolean {
    val url = Tools.formatImageUrl(posterPath, size) ?: return false
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
    return true
}

/** 列表行 / 详情页共用的海报缩略图，没有图就用占位底色 + 影片图标 */
@Composable
internal fun PosterThumb(
    posterPath: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!PhonePosterImage(posterPath = posterPath, modifier = Modifier.size(width, height))) {
            Icon(
                imageVector = PhoneIcons.Movie,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
