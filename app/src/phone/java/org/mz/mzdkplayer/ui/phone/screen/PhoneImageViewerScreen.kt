package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.common.RemoteMedia
import org.mz.mzdkplayer.ui.common.rememberRemoteMediaImageLoader
import org.mz.mzdkplayer.ui.phone.PhoneIcons

/** 工具栏自动隐藏时间：手机上一直挂着三条半透明控件会挡住图片 */
private const val CONTROLS_HIDE_DELAY_MS = 4_000L

/** 缩放范围与电视端一致 */
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 6f

/**
 * 手机端图片查看页（第五阶段）。
 *
 * 与电视端 [org.mz.mzdkplayer.ui.picviewer.PicViewerScreen] 的关系：
 * 缩放 / 旋转 / 平移的手势口径一致（`detectTransformGestures` + `graphicsLayer`，范围 1x–6x），
 * 差别在手机上补了两件事 —— **左右滑动切换同目录的其它图片**（电视端是单张查看），
 * 以及把控件压成顶部标题 + 底部一排按钮，4 秒后自动隐藏（点一下屏幕再唤出）。
 *
 * 远程协议的图片靠 [RemoteMedia] + `RemoteMediaFetcher` 读，普通 String 地址
 * Coil 不认识 `smb:// / nfs://` 这些 scheme。放大到 1x 以上时禁用翻页手势，
 * 否则拖动图片会被 pager 抢走。
 */
@Composable
fun PhoneImageViewerScreen(
    uris: List<String>,
    initialIndex: Int,
    dataSourceType: String,
    onBack: () -> Unit,
) {
    if (uris.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val context = LocalContext.current
    val imageLoader = rememberRemoteMediaImageLoader()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, uris.lastIndex),
        pageCount = { uris.size },
    )

    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showControls by remember { mutableStateOf(true) }

    // 换张图就回到原始姿态：上一张放大到的位置套在新图上是错的
    LaunchedEffect(pagerState.settledPage) {
        scale = 1f
        rotation = 0f
        offset = Offset.Zero
    }

    // 工具栏自动隐藏；任何交互都会把 showControls 翻回 true 从而重新计时
    LaunchedEffect(showControls, pagerState.settledPage) {
        if (showControls) {
            delay(CONTROLS_HIDE_DELAY_MS)
            showControls = false
        }
    }

    val currentUri = uris[pagerState.currentPage.coerceIn(0, uris.lastIndex)]
    val currentName = Tools.extractFileNameFromUri(currentUri)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            // 放大之后手势留给图片平移，翻页只在不放大时可用
            userScrollEnabled = scale <= MIN_SCALE,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { showControls = !showControls }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        rotation += rotate
                        // 没放大时不让图片被拖走（会变成"空白 + 图跑到角上"）
                        offset = if (scale > MIN_SCALE) offset + pan else Offset.Zero
                        showControls = false
                    }
                },
        ) { page ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(RemoteMedia(uris[page], dataSourceType))
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = Tools.extractFileNameFromUri(uris[page]),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        // 只有当前页带变换，左右两页保持原始姿态
                        scaleX = if (page == pagerState.currentPage) scale else 1f,
                        scaleY = if (page == pagerState.currentPage) scale else 1f,
                        rotationZ = if (page == pagerState.currentPage) rotation else 0f,
                        translationX = if (page == pagerState.currentPage) offset.x else 0f,
                        translationY = if (page == pagerState.currentPage) offset.y else 0f,
                    ),
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.phone_action_back),
                        tint = Color.White,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.phone_image_counter,
                            pagerState.currentPage + 1,
                            uris.size,
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImageControlButton(
                    icon = PhoneIcons.ZoomOut,
                    label = stringResource(R.string.pic_viewer_zoom_out),
                    onClick = { scale = (scale / 1.25f).coerceAtLeast(MIN_SCALE) },
                )
                ImageControlButton(
                    icon = PhoneIcons.ZoomIn,
                    label = stringResource(R.string.pic_viewer_zoom_in),
                    onClick = { scale = (scale * 1.25f).coerceAtMost(MAX_SCALE) },
                )
                ImageControlButton(
                    icon = PhoneIcons.RotateLeft,
                    label = stringResource(R.string.pic_viewer_rotate_left),
                    onClick = { rotation -= 90f },
                )
                ImageControlButton(
                    icon = PhoneIcons.RotateRight,
                    label = stringResource(R.string.pic_viewer_rotate_right),
                    onClick = { rotation += 90f },
                )
                ImageControlButton(
                    icon = PhoneIcons.Image,
                    label = stringResource(R.string.pic_viewer_reset),
                    onClick = {
                        scale = 1f
                        rotation = 0f
                        offset = Offset.Zero
                    },
                )
            }
        }
    }
}

/** 底部工具栏的一个按钮：压成圆形半透明底，`contentDescription` 直接复用电视端的 `pic_viewer_*` 文案 */
@Composable
private fun ImageControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}
