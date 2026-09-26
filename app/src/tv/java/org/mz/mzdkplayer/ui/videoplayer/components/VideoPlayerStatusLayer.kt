package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.viewmodel.VideoPlayerStatus

@Composable
fun BoxScope.VideoPlayerStatusLayer(
    playerStatus: VideoPlayerStatus,
    isFirstLoad: Boolean
) {
    when (playerStatus) {
        is VideoPlayerStatus.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .background(Color.Black)
            ) {
                VAErrorScreen(playerStatus.toString())
            }
        }

        is VideoPlayerStatus.BUFFERING -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                LoadingScreen(
                    stringResource(id = R.string.ui_label_buffering_now),
                    modifier = Modifier
                        .width(95.dp)
                        .height(95.dp)
                        .align(Alignment.Center)
                        .offset(y = (-68).dp)
                        .background(
                            Color.Black.copy(0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    fontSize = 16,
                    36
                )
            }
        }

        else -> {}
    }

    if (isFirstLoad) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            LoadingScreen(
                stringResource(R.string.ui_label_loading_do_not_operate),
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}
