package org.mz.mzdkplayer.ui.screen.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
// 👇 新增 FocusRequester 相关的导入
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.AudioCacheEntity
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.ui.screen.common.LibraryEmpty
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    viewModel: AudioViewModel,
    homeNavController: NavController,
    mainNavController: NavHostController
) {
    val audioList by viewModel.allAudio.collectAsState()
    var focusedAudio by remember { mutableStateOf<AudioCacheEntity?>(null) }
    val context = LocalContext.current

    // 👇 1. 为音频列表创建 FocusRequester
    val listFocusRequester = remember { FocusRequester() }

    // 预定义缺省图 Painter
    val placeholderPainter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.baseline_music_note_24))

    LaunchedEffect(audioList) {
        if (focusedAudio == null && audioList.isNotEmpty()) {
            focusedAudio = audioList.first()
        }
    }

    // 👇 2. 监听音频列表状态，非空时自动请求焦点
    LaunchedEffect(audioList.isNotEmpty()) {
        if (audioList.isNotEmpty()) {
            runCatching { listFocusRequester.requestFocus() }
        }
    }

    if (audioList.isEmpty()) {
        LibraryEmpty(navController = homeNavController, type = "music")
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // --- 1. 沉浸式动态背景 ---
            AnimatedContent(
                targetState = focusedAudio?.localCoverPath,
                transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                label = "BgAnim"
            ) { path ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (path != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(path)).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.5f)
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                    ))
                }
            }

            // --- 2. 主内容布局 ---
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧预览区
                Column(
                    modifier = Modifier.weight(0.4f).padding(end = 64.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                    ) {
                        AsyncImage(
                            model = focusedAudio?.localCoverPath?.let { File(it) },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = placeholderPainter,
                            placeholder = placeholderPainter
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = focusedAudio?.title ?: "未知歌曲",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(255, 248, 240),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = focusedAudio?.artist ?: "未知艺术家",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(255, 248, 240).copy(0.7f),
                        maxLines = 1
                    )

                    if (!focusedAudio?.album.isNullOrBlank()) {
                        Text(
                            text = "${focusedAudio?.album}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(255, 248, 240).copy(0.4f),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // 右侧列表区 (紧凑单行模式)
                LazyColumn(
                    modifier = Modifier
                        .weight(0.6f)
                        .focusGroup()
                        .focusRestorer()
                        .focusRequester(listFocusRequester), // 👇 3. 绑定焦点请求器到 LazyColumn
                    contentPadding = PaddingValues(vertical = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(audioList) { index, audio ->
                        val isFocused = focusedAudio?.audioUri == audio.audioUri

                        ListItem(
                            selected = false,
                            onClick = {
                                val encodedUri = audio.audioUri.toBase64()
                                val encodedFn = audio.fileName.toBase64()
                                val encodedConn = audio.connectionName.toBase64()
                                val audioItems = audioList.map { AudioItem(it.audioUri, it.fileName, it.dataSourceType) }
                                AudioPlaylistRepository.setPlaylist(audioItems)
                                mainNavController.navigate("AudioPlayer/$encodedUri/${audio.dataSourceType}/$encodedFn/$encodedConn/$index")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) focusedAudio = audio },
                            colors = MyFileListItemColor(),
                            shape = ListItemDefaults.shape(RoundedCornerShape(8.dp)),
                            leadingContent = {
                                Text(
                                    text = String.format(LocalLocale.current.platformLocale,"%02d", index + 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isFocused) Color.Black.copy(0.5f) else Color.Gray
                                )
                            },
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = audio.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (audio.artist.isNotBlank()) {
                                        Text(
                                            text = " - ${audio.artist}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                if (audio.duration > 0) {
                                    Text(
                                        text = formatMillis(audio.duration),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}