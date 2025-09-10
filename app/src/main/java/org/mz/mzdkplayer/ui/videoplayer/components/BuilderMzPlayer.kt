package org.mz.mzdkplayer.ui.videoplayer.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.remember

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.yourpackage.smbplayer.SmbDataSource
import com.yourpackage.smbplayer.SmbDataSourceFactory
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel

import kotlin.time.Duration.Companion.microseconds

@OptIn(UnstableApi::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun BuilderMzPlayer(context: Context, smbUri: String, exoPlayer: ExoPlayer) {
    val pathStr = LocalContext.current.filesDir.toString()
    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
    LaunchedEffect(Unit) {
        exoPlayer.setMediaItem(MediaItem.fromUri(smbUri))
        exoPlayer.prepare()
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    // Update UI using current tracks.
                    val trackGroups = exoPlayer.currentTracks.groups
                    videoPlayerViewModel.mutableSetOfAudioTrackGroups.clear()
                    videoPlayerViewModel.mutableSetOfVideoTrackGroups.clear()
                    for (trackGroup in trackGroups) {
                        // Group level information.
                        val trackType = trackGroup.type
                        if (trackType == C.TRACK_TYPE_AUDIO) {
                            videoPlayerViewModel.mutableSetOfAudioTrackGroups.add(trackGroup)

                            Log.d("TRACK_TYPE_AUDIO", trackGroup.getTrackFormat(0).toString())

                        }
                        if (trackType == C.TRACK_TYPE_VIDEO) {
                            videoPlayerViewModel.mutableSetOfVideoTrackGroups.add(trackGroup)
                        }
                    }
                    if (videoPlayerViewModel.onTracksChangedState == 0) {
                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
                                TrackSelectionOverride(
                                    videoPlayerViewModel.mutableSetOfVideoTrackGroups[0].mediaTrackGroup,
                                    0
                                )
                            ).build()
                    }

                    for ((index, atGroup) in videoPlayerViewModel.mutableSetOfAudioTrackGroups.withIndex()) {
                        Log.d("VideoTrackGroupsID", atGroup.getTrackFormat(0).id.toString())
                        if (atGroup.isTrackSelected(0)) {
                            videoPlayerViewModel.selectedAtIndex = index
                        }
                    }
                    for ((index, vtGroup) in videoPlayerViewModel.mutableSetOfVideoTrackGroups.withIndex()) {
                        if (vtGroup.isTrackSelected(0)) {
                            videoPlayerViewModel.selectedVtIndex = index
                        }
                    }
                    videoPlayerViewModel.onTracksChangedState = 1
                }

            })
    }


    LaunchedEffect(exoPlayer) {
        Log.d("sd", "开始初始化mpd")
        delay(150.microseconds)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(context: Context) = remember {
    val trackSelector = DefaultTrackSelector(context)

    val licenseRequestHeaders =
        mapOf(
            "Referer" to "https://www.bilibili.com/",
            "origin" to "https://www.bilibili.com/video/",
            "user-agent" to " Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0",
            //"cookie" to "buvid3=E748DE9B-4009-D8C4-0F14-B9CB71AC0C6B77255infoc; b_nut=1742997777; _uuid=A2E64452-7C46-BF99-31C3-4B4EB7314861077570infoc; buvid_fp=fb6c2f21a45b2c8717572ac3ec89c6f1; buvid4=D7D418BD-F75D-58B9-CD03-9FA73F4E065778299-025032614-TROyH1x%2B%2FJR0buFxePjtbtvud5oW4HwuBvWe1reTm7bs9QojOxvlqcBaneemMhF%2F; header_theme_version=CLOSE; enable_web_push=DISABLE; enable_feed_channel=ENABLE; home_feed_column=4; rpdid=|(Y|RRJ|~)k0J'u~RJuYu|~|; DedeUserID=301934906; DedeUserID__ckMd5=87a7b3a52103cdc2; CURRENT_QUALITY=120; LIVE_BUVID=AUTO1917457500589924; PVID=4; b_lsid=16E444B3_197069C0E27; bili_ticket=eyJhbGciOiJIUzI1NiIsImtpZCI6InMwMyIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NDg0MjE3NzgsImlhdCI6MTc0ODE2MjUxOCwicGx0IjotMX0.fq5X9Zol_kigKCXr22CpEv9Txa8c245rW36Ztox7Bv4; bili_ticket_expires=1748421718; SESSDATA=20974aa5%2C1763714578%2Ceacd2%2A51CjBkQf6MVcNy2YwUBGxEpfxqFkJ2MvHth4h9-b4U3c4283Z1RI5AUzraGB86LhIZsEYSVk5rQWZlVWtldnFnTWpuLWNfaUVVdFlkeTRvYjBfdkFybkJpY3FSMUtLazFsMmVDSjVVRmxpLWpNU3dZZnJSamVURnJMV3dnaXRuUjFDSGFmVnE4eTRnIIEC; bili_jct=e8f2ab82c807a29c0b6d2c70207abee8; bp_t_offset_301934906=1070839194907049984; sid=4l2wqcdf; CURRENT_FNVAL=2000; browser_resolution=516-744",
            "sec-ch-ua-platform" to "Windows",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "cross-site"

        )
// 创建 SMB DataSource Factory
    // val smbDataSourceFactory = SmbDataSource().

    // 创建一个专门的 MediaSource Factory，使用我们的 SMB DataSource Factory
    //val mediaSourceFactory = DefaultMediaSourceFactory(context,smbDataSourceFactory)
    // 创建针对 Amlogic 芯片的 MediaCodecSelector
//    val amlogicAwareCodecSelector =
//        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
//            val allDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
//                mimeType,
//                requiresSecureDecoder,
//                requiresTunnelingDecoder
//            )
//            // 优先选择 Amlogic 的杜比视界解码器
//            val amlogicDecoders = allDecoders.filter { info ->
//                info.name.contains("amlogic", ignoreCase = true) &&
//                        info.name.contains("dolby", ignoreCase = true)
//            }
//            Log.d("amlogicDecoders",amlogicDecoders.toString())
//            return@MediaCodecSelector amlogicDecoders.ifEmpty { allDecoders }
//        }
    // 配置 RenderersFactory
    val renderersFactory = DefaultRenderersFactory(context).apply {
        //setMediaCodecSelector(amlogicAwareCodecSelector)
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    }
    val dataSourceFactory =
        SmbDataSourceFactory()

    ExoPlayer.Builder(context)
        .setSeekForwardIncrementMs(10000)
        .setSeekBackIncrementMs(10000)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                dataSourceFactory
            )
        ).setRenderersFactory(renderersFactory)
        .build()
        .apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }

}
