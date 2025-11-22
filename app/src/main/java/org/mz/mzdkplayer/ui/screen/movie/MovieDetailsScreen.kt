package org.mz.mzdkplayer.ui.screen.movie

import android.view.KeyEvent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
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
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.LocalizedStatusText
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import java.net.URLEncoder

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
    val tvSeriesDetails by movieViewModel.tvSeriesResults.collectAsState()
    // [新增] 解码 URI 用于数据库查询
    val decodedUri = remember(videoUri) {
        java.net.URLDecoder.decode(videoUri, "UTF-8")
    }
    LaunchedEffect(movieId) {
        if (movieId > 0) {
            // [修改] 调用带缓存的方法
            movieViewModel.getMovieDetailsWithCache(movieId, decodedUri,dataSourceType,fileName,connectionName)
        }
    }
    val videoUriEncoder = URLEncoder.encode(videoUri, "UTF-8")
    val fileNameEncoder = URLEncoder.encode(fileName, "UTF-8")
    val connectionNameEncoder = URLEncoder.encode(connectionName, "UTF-8")
    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        when (val result = movieDetails) {
            is Resource.Success -> {
                MovieContent(
                    movie = result.data,
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
                        text = "正在加载电影详情...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f),
                        subtitle = "如果你不想看到详情页，可以在设置中设置不显示详情页"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MyIconButton(
                        text = "立即播放",
                        icon = R.drawable.baseline_play_arrow_24,
                        modifier = Modifier,
                        onClick = { navController.navigate("VideoPlayer/$videoUriEncoder/$dataSourceType/$fileNameEncoder/$connectionNameEncoder") }
                    )
                }
            }

            is Resource.Error -> ErrorView(
                message = "加载失败",
                onPlayAnyway = { navController.navigate("VideoPlayer/$videoUriEncoder/$fileNameEncoder/$fileName/$connectionNameEncoder") }
            )

            else -> {}
        }
    }
}

@Composable
private fun MovieContent(
    movie: MovieDetails,
    onPlayClick: () -> Unit
) {
    // 控制详细简介弹窗的显示
    var showFullDescDialog by remember { mutableStateOf(false) }
    val watchButtonsFR = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        watchButtonsFR.requestFocus()
    }
    // 1. 背景层
    Box(modifier = Modifier.fillMaxSize()) {
        if (!movie.backdropPath.isNullOrEmpty()) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/original${movie.backdropPath}",
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
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
            if (!movie.posterPath.isNullOrEmpty()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                    contentDescription = movie.title,
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
            // 标题
            item {
                Text(
                    text = movie.title ?: "未知标题",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!movie.originalTitle.isNullOrEmpty() && movie.originalTitle != movie.title) {
                    Text(
                        text = movie.originalTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 元数据
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEDA33D), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TMDB ${String.format("%.1f", movie.voteAverage)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = movie.releaseDate?.take(4) ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "|", color = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    LocalizedStatusText(movie.status)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val extraInfo = listOfNotNull(
                    movie.originCountry.joinToString(", ") { getCountryName(it) }
                        .takeIf { it.isNotEmpty() },
                    movie.genreList.joinToString(" / ") { it.name }.takeIf { it.isNotEmpty() }
                ).joinToString("  •  ")
                if (extraInfo.isNotEmpty()) {
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // === 简介区域 (可点击) ===
            item {
                // 使用 TV Material3 的 Surface 作为可点击容器
                Surface(
                    onClick = { showFullDescDialog = true },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f), // 获取焦点时微亮
                        pressedContainerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 28.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        // 可选：提示文本
                        Text(
                            text = "点击阅读全文",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 按钮区域
            item {
                MyIconButton(
                    text = "立即播放",
                    icon = R.drawable.baseline_play_arrow_24,
                    modifier = Modifier
                        .width(160.dp)
                        .focusRequester(watchButtonsFR),
                    onClick = onPlayClick
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // 3. 全屏简介弹窗
    if (showFullDescDialog) {
        FullDescriptionDialog(
            title = movie.title ?: "",
            overview = movie.overview,
            onDismiss = { showFullDescDialog = false }
        )
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
                .fillMaxSize() // 或者指定大小，如 .fillMaxWidth(0.8f).fillMaxHeight(0.8f)
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // === 核心修改部分开始 ===
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                var isFocused by remember { mutableStateOf(false) }

                // 我们给文本容器加一个 Box，让这个 Box 负责获焦和滚动处理
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // 1. 添加边框指示：当获焦时显示边框，告诉用户这里可以操作
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        // 2. 允许获焦
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        // 3. 拦截按键事件
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        // 向下滚动
                                        coroutineScope.launch {
                                            scrollState.animateScrollBy(200f) // 每次滚动的像素量，可调整
                                        }
                                        true // 返回 true 表示事件已消费，不再传递给系统（防止焦点跳走）
                                    }

                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        // 向上滚动
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
                        // 4. 绑定 scrollState
                        .verticalScroll(scrollState)
                        .padding(8.dp) // 内边距，防止文字贴边框
                ) {
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        lineHeight = 32.sp,
                        fontSize = 18.sp
                    )
                }
                // === 核心修改部分结束 ===

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MyIconButton(
                        text = "关闭",
                        icon = R.drawable.baseline_play_arrow_24, // 这里为了防止报错用了你有的图标，记得换成关闭图标
                        modifier = Modifier.width(120.dp),
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
                text = "尝试直接播放",
                icon = R.drawable.baseline_play_arrow_24,
                modifier = Modifier.width(180.dp),
                onClick = onPlayAnyway
            )
        }
    }
}

// 辅助 Helper 以避免 Border 导入冲突
// 在 Compose 中，BorderStroke 来自 androidx.compose.foundation
// 而 Border 对象来自 androidx.tv.material3
// 上面的代码已处理导入，这里只是说明。