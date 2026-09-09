package org.mz.mzdkplayer.ui.picviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.CirCleIconButton
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PicViewerScreen(
    mediaUri: String,
    dataSourceType: String,
    fileName: String = stringResource(R.string.ui_label_unknown_filename),
    connectionName: String,
) {
    val context = LocalContext.current

    // 1. 创建自定义的 ImageLoader
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(RemoteMediaFetcher.Factory())
            }
            .build()
    }

    // 状态管理
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 自动隐藏逻辑
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls) {
            delay(5000.milliseconds)
            showControls = false
        }
    }

    val updateInteraction = {
        lastInteractionTime = System.currentTimeMillis()
        showControls = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    rotation += rotate
                    offset += pan
                    updateInteraction()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    showControls = !showControls
                    if (showControls) updateInteraction()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 2. 使用 AsyncImage 加载图片
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(RemoteMedia(mediaUri, dataSourceType))
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = fileName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = contentScale
        )

        // 控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PicViewerControlBar(
                scale = scale,
                onScaleChange = { newScale ->
                    scale = newScale.coerceIn(0.5f, 5f)
                    updateInteraction()
                },
                onRotate = { deg ->
                    rotation += deg
                    updateInteraction()
                },
                contentScale = contentScale,
                onContentScaleChange = { newContentScale ->
                    contentScale = newContentScale
                    updateInteraction()
                },
                onReset = {
                    scale = 1f
                    rotation = 0f
                    offset = Offset.Zero
                    updateInteraction()
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PicViewerControlBar(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onRotate: (Float) -> Unit,
    contentScale: ContentScale,
    onContentScaleChange: (ContentScale) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(bottom = 24.dp)
            .widthIn(max = 600.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        colors = SurfaceDefaults.colors(
            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.75f),
            contentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩放控制
            CirCleIconButton(
                icon = painterResource(R.drawable.zoomin24dp),
                tooltip = stringResource(R.string.pic_viewer_zoom_in),
                onClick = { onScaleChange(scale * 1.2f) }
            )
            CirCleIconButton(
                icon = painterResource(R.drawable.zoomout24dp),
                tooltip = stringResource(R.string.pic_viewer_zoom_out),
                onClick = { onScaleChange(scale / 1.2f) }
            )

            // 旋转控制
            CirCleIconButton(
                icon = painterResource(R.drawable.rotateleft24dp),
                tooltip = stringResource(R.string.pic_viewer_rotate_left),
                onClick = { onRotate(-90f) }
            )
            CirCleIconButton(
                icon = painterResource(R.drawable.rotateright24dp),
                tooltip = stringResource(R.string.pic_viewer_rotate_right),
                onClick = { onRotate(90f) }
            )

            // 显示方式切换
            CirCleIconButton(
                icon = painterResource(R.drawable.aspect_ratio_24dp),
                tooltip = stringResource(R.string.pic_viewer_scale_mode),
                onClick = {
                    val nextScale = when (contentScale) {
                        ContentScale.Fit -> ContentScale.Crop
                        ContentScale.Crop -> ContentScale.FillBounds
                        else -> ContentScale.Fit
                    }
                    onContentScaleChange(nextScale)
                }
            )

            // 重置
            CirCleIconButton(
                icon = painterResource(R.drawable.zoomoutmap24dp),
                tooltip = stringResource(R.string.pic_viewer_reset),
                onClick = onReset
            )

            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
