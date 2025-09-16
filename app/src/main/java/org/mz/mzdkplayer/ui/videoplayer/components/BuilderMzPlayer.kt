package org.mz.mzdkplayer.ui.videoplayer.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.remember

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
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import org.mz.mzdkplayer.tool.SmbDataSourceFactory
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel

import androidx.core.net.toUri

@OptIn(UnstableApi::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun BuilderMzPlayer(context: Context, mediaUri: String, exoPlayer: ExoPlayer) {
    //val pathStr = LocalContext.current.filesDir.toString()
    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()

    LaunchedEffect(Unit) {

        Log.d("播放器uri", mediaUri)
//        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
//            .buildUpon()
//            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) // 禁用文本轨道
//            .build()
//        val trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
//            .setPreferredTextLanguage("zh") // 将 "zh" 替换为你需要的默认字幕语言代码，例如 "en" 表示英语
//            .build()

        // SRT 字幕的 MIME 类型
        val mimeTypeSRT = "application/x-subrip"

        //exoPlayer.trackSelectionParameters = trackSelectionParameters

        // 根据 URI 类型处理 MediaItem 创建
        val mediaItem = if (mediaUri.startsWith("smb://") || mediaUri.startsWith("http://") || mediaUri.startsWith("https://")) {
            MediaItem.fromUri(mediaUri)
        } else {
            // 处理本地文件路径
            val uri = if (mediaUri.startsWith("file://")) {
                mediaUri.toUri()
            } else {
                // 假设是文件路径，添加 file:// 前缀
                "file://$mediaUri".toUri()
            }
            MediaItem.fromUri(uri)
        }

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // Update UI using current tracks.
                val trackGroups = exoPlayer.currentTracks.groups
                videoPlayerViewModel.mutableSetOfAudioTrackGroups.clear()
                videoPlayerViewModel.mutableSetOfVideoTrackGroups.clear()
                videoPlayerViewModel.mutableSetOfTextTrackGroups.clear()

                // 检测是否有SRT字幕轨道被选中
                var hasSrtTrackSelected = false

                for (trackGroup in trackGroups) {
                    // Group level information.
                    val trackType = trackGroup.type
                    // 音频轨
                    if (trackType == C.TRACK_TYPE_AUDIO) {
                        videoPlayerViewModel.mutableSetOfAudioTrackGroups.add(trackGroup)
                        Log.d("TRACK_TYPE_AUDIO", trackGroup.getTrackFormat(0).toString())
                    }
                    // 视频轨
                    if (trackType == C.TRACK_TYPE_VIDEO) {
                        videoPlayerViewModel.mutableSetOfVideoTrackGroups.add(trackGroup)
                    }

                    // 字幕轨
                    if (trackType == C.TRACK_TYPE_TEXT) {
                        videoPlayerViewModel.mutableSetOfTextTrackGroups.add(trackGroup)

                        if (trackGroup.isSelected) {
                            // 获取被选中轨道的格式 (循环轨道组)
                            for (i in 0 until trackGroup.length) {
                                if (trackGroup.isTrackSelected(i)) {
                                    val format = trackGroup.getTrackFormat(i)
                                    // 检查是否是SRT格式
                                    if (format.codecs == mimeTypeSRT) {
                                        hasSrtTrackSelected = true
                                        break // 找到一个SRT轨道就足够
                                    }
                                }
                            }
                        }
                    }
                    // 根据是否选中了 SRT 轨道来设置可见性
                    if (hasSrtTrackSelected) {
                        Log.d("SDS1", "SubtitleView set to GONE because SRT track is selected.")
                        videoPlayerViewModel.updateSubtitleVisibility(View.GONE)

                    } else {
                        Log.d(
                            "SDS1", "SubtitleView set to VISIBLE because no SRT track is selected."
                        )
                        videoPlayerViewModel.updateSubtitleVisibility (View.VISIBLE)

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
                    exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
                        TrackSelectionOverride(
                            videoPlayerViewModel.mutableSetOfAudioTrackGroups[0].mediaTrackGroup,
                            0
                        )
                    ).build()
                }

                for ((index, atGroup) in videoPlayerViewModel.mutableSetOfAudioTrackGroups.withIndex()) {
                    Log.d("VideoTrackGroupsID", atGroup.getTrackFormat(0).id.toString())
                    if (atGroup.isTrackSelected(0)) {
                        Log.d("sindex",index.toString())
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
        Log.d("exoPlayerInit", "开始初始化exoPlayer")
        //delay(150.microseconds)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(context: Context,mediaUri: String) = remember (mediaUri){

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
    // 根据 URI 协议选择合适的数据源工厂
    val dataSourceFactory = if (mediaUri.startsWith("smb://")) {
        // SMB 协议
        SmbDataSourceFactory()
    } else if (mediaUri.startsWith("file://") || mediaUri.startsWith("/")) {
        // 本地文件协议或绝对路径
        DefaultDataSource.Factory(context)
    } else {
        // 其他情况（如 http/https），使用默认的 HTTP 数据源
        DefaultHttpDataSource.Factory()
    }

    ExoPlayer.Builder(context).setSeekForwardIncrementMs(10000).setSeekBackIncrementMs(10000)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                dataSourceFactory
            )
        ).setRenderersFactory(renderersFactory).buildWithAssSupport( //配置ass字幕显示 LEGACY
            context,
            AssRenderType.LEGACY,
            dataSourceFactory = dataSourceFactory,
            renderersFactory = renderersFactory
        ).apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }

}
