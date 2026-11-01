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
    isFirstLoad: Boolean,
    /**
     * 用户正在用遥控器连续 seek 时压掉「正在缓冲」。
     *
     * 网络源每跳一步都会进 BUFFERING，菊花一闪一闪地盖在画面正中，和 HUD、箭头挤在一起；
     * 这段时间的缓冲本来就是 seek 的必然结果，不用提示。真卡住的话 HUD 收尾后菊花会自己回来。
     */
    suppressBuffering: Boolean = false
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
            if (!suppressBuffering) {
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
