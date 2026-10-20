package org.mz.mzdkplayer.ui.screen.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.MediaItem
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.common.VideoBigIcon
import org.mz.mzdkplayer.tool.Tools.formatFileSize
import org.mz.mzdkplayer.ui.common.RemoteMedia
import org.mz.mzdkplayer.ui.common.rememberRemoteMediaImageLoader
import kotlin.Boolean

@Composable
fun FileSize(isDirectory: Boolean = true,fileSize: Long = 1L){

    if (!isDirectory) {
        Box(
            modifier = Modifier.widthIn(min = 72.dp), // 设置最小宽度，确保单位对齐
            contentAlignment = Alignment.CenterEnd    // 文字靠右对齐
        )
        {
            Text(
                text = formatFileSize(fileSize),
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    color = Color(0xFF9A96A6)
                )
            )
        }
    } else {
        // 目录项右侧只给一个淡箭头，和文件的体量文字区分开
        Icon(
            painter = painterResource(R.drawable.arrowright24dp),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF6E6A78)
        )
    }
}

@Composable
fun FileName(fileName: String=""){
    Text(
        fileName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
    )
}
@Composable
fun MediaInfoLoading(){
    Box(
        modifier = Modifier
            .widthIn(200.dp)
            .fillMaxHeight(0.6f)
            .background(Color(0xFF1A1A21))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.ui_label_loading_in_progress),
            color = Color(0xFF918D9C),
            fontSize = 13.sp
        )
    }
}
@Composable
fun MediaTitle(title: String?){
    Text(
        title?: stringResource(R.string.ui_label_unknown_title),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )

}
@Composable
fun MediaReleaseDate(releaseDate: String?){
    Text(
        text = releaseDate?.substring(0, 4) ?: "N/A",
        color = Color(0xFF9A96A6),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MediaFocusedFileName(focusedFileName: String?){
    Text(
        focusedFileName?:stringResource(R.string.ui_label_unknown_filename),
        color = Color(0xFFF4F1EA),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
@Composable
fun FileIcon(isDirectory: Boolean = true,fileExtension: String="mp4"){
    Icon(
        painter = when {
            isDirectory -> painterResource(R.drawable.baseline_folder_24)
            Tools.containsVideoFormat(fileExtension) -> painterResource(R.drawable.moviefileicon)
            Tools.containsAudioFormat(fileExtension) -> painterResource(R.drawable.baseline_music_note_24)
            Tools.containsImageFileExtension(fileExtension) -> painterResource(R.drawable.image24dp)
            else -> painterResource(R.drawable.baseline_insert_drive_file_24)
        },
        contentDescription = null,
    )
}

@Composable
fun CirCleIconButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    tooltip: String = "",
    contentDescription: String? = null,
    onClick: () -> Unit = {},
    enable: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // 1. 外层 Box 仅作为容器，不包裹 Popup 逻辑
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 2. 这里的 Surface 必须强制固定 size
        Surface(
            modifier = Modifier.size(36.dp), // 显式声明 36dp，防止被拉伸成条状
            onClick = onClick,
            interactionSource = interactionSource,
            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.DarkGray,
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = Color.Black
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        ) {
            Icon(
                painter = icon,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentDescription = contentDescription,
            )
        }

        // 3. Popup 逻辑独立出来
        if (isFocused && tooltip.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -70)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2D2D2D).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tooltip,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MediaPreviewSection(
    focusedMovie: Resource<MediaItem?>,
    focusedFileName: String?,
    focusedIsDir: Boolean,
    onMediaIdResolved: (Int) -> Unit, // 新增回调：当解析出电影 ID 时通知外部
    modifier: Modifier = Modifier,
    focusedMediaUri: String? = null, // 新增：当前焦点的媒体 URI
    focusedDataSourceType: String = "LOCAL", // 新增：数据源类型，用于远程图片预览
) {
        Column(
            modifier = modifier, // 使用传入的 modifier
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 预览区不再铺灰色底板：海报/图片直接浮在黑底上，只保留圆角，
            // 否则竖版海报左右会露出两条灰边，和整页纯黑背景割裂
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                // 判断是否是图片文件
                val isImage = focusedFileName?.let {
                    Tools.containsImageFileExtension(Tools.extractFileExtension(it))
                } == true
                // 图片 + 有效 URI 才走图片预览
                val previewUri = focusedMediaUri?.takeIf { isImage && it.isNotBlank() }

                Log.d("FileBrowserCom", "focusedFileName: $focusedFileName, isImage: $isImage, focusedMediaUri: $focusedMediaUri")

                when (focusedMovie) {
                    is Resource.Success -> {
                        val movie = focusedMovie.data
                        if (movie != null) {
                            // 关键点：通过 SideEffect 或直接在逻辑中触发回调
                            // 这样父组件的 mediaId 就会保持同步
                            LaunchedEffect(movie.id) {
                                onMediaIdResolved(movie.id)
                            }

                            when {
                                // 图片文件优先显示图片本身，避免被同名电影的刮削海报"抢"走
                                previewUri != null -> ImagePreview(previewUri, focusedFileName, focusedDataSourceType)
                                movie.posterPath != null -> AsyncImage(
                                    model = org.mz.mzdkplayer.tool.Tools.formatImageUrl(movie.posterPath, "w500"),
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                else -> VideoBigIcon(focusedIsDir, focusedFileName, Modifier.fillMaxWidth().height(200.dp))
                            }
                        } else {
                            // 如果没有匹配到电影，通知父组件重置 mediaId
                            LaunchedEffect(Unit) { onMediaIdResolved(-1) }
                            if (previewUri != null) {
                                ImagePreview(previewUri, focusedFileName, focusedDataSourceType)
                            } else {
                                VideoBigIcon(focusedIsDir, focusedFileName, Modifier.fillMaxWidth().height(200.dp))
                            }
                        }
                    }
                    is Resource.Loading -> {
                        // 图片是本地/远程直读，不需要等待 TMDB 刮削结果
                        if (previewUri != null) {
                            ImagePreview(previewUri, focusedFileName, focusedDataSourceType)
                        } else {
                            MediaInfoLoading()
                        }
                    }
                    is Resource.Error -> {
                        LaunchedEffect(Unit) { onMediaIdResolved(-1) }
                        if (previewUri != null) {
                            ImagePreview(previewUri, focusedFileName, focusedDataSourceType)
                        } else {
                            VideoBigIcon(focusedIsDir, focusedFileName, Modifier.fillMaxWidth().height(200.dp))
                        }
                    }
                }
                }
            }

            // --- 文字信息区 ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val movie = (focusedMovie as? Resource.Success)?.data
                if (movie != null) {
                    MediaTitle(movie.title)
                    Spacer(modifier = Modifier.height(6.dp))
                    MediaReleaseDate(movie.releaseDate)
                } else {
                    MediaFocusedFileName(focusedFileName)
                    // 文件名下方补一行格式标签，空态也不再只有光秃秃一行名字
                    val extension = Tools.extractFileExtension(focusedFileName ?: "").uppercase()
                    if (!focusedIsDir && extension.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = extension,
                            color = Color(0xFF8C88A0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

}

@Composable
fun ImagePreview(uri: String, fileName: String?, dataSourceType: String = "LOCAL") {
    val context = LocalContext.current
    // 必须使用带 RemoteMediaFetcher 的 ImageLoader，否则 SMB/FTP/NFS/WEBDAV 等
    // 自定义 scheme 的 URI 无法被 Coil 解析，预览会一直空白
    val imageLoader = rememberRemoteMediaImageLoader()

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(RemoteMedia(uri, dataSourceType))
            .crossfade(true)
            .build(),
        imageLoader = imageLoader,
        contentDescription = fileName,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clip(RoundedCornerShape(14.dp))
    )
}
