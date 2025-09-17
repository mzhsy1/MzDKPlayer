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
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import org.mz.mzdkplayer.MzDkPlayerApplication

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
            Log.d("MediaItemUri",uri.toString())
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
    // 创建针对雷鸟鹤6 Pro (联发科MT9653平台) 的MediaCodecSelector
    val mediaTekAwareCodecSelector =
        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val allDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )

            // 首先过滤出所有硬件解码器
            val hardwareDecoders = allDecoders.filter { info ->
                info.hardwareAccelerated // 系统标识的硬件解码器
            }

            // 优先选择联发科（MediaTek）的硬件解码器，特别是支持杜比视界的
            val mediaTekDecoders = hardwareDecoders.filter { info ->
                // 联发科解码器在名称中通常包含 "mediatek", "mtk", 或芯片型号如 "mt9653", "pentonic"
                info.name.contains("mediatek", ignoreCase = true) ||
                        info.name.contains("mtk", ignoreCase = true) ||
                        info.name.contains("mt9653", ignoreCase = true) ||
                        info.name.contains("pentonic", ignoreCase = true) ||
                        // 同时保留之前针对杜比视界的筛选逻辑（如果仍需特定格式优先）
                        (mimeType.startsWith("video/") && info.name.contains(
                            "dolby",
                            ignoreCase = true
                        ))
            }

            Log.d("MediaCodecSelector", "For MIME type: $mimeType")
            Log.d(
                "MediaCodecSelector",
                "All hardware decoders: ${hardwareDecoders.map { it.name }}"
            )
            Log.d(
                "MediaCodecSelector",
                "MediaTek prioritized decoders: ${mediaTekDecoders.map { it.name }}"
            )

            // 优先返回联发科解码器，如果没有则返回所有硬件解码器，最后再fallback到所有解码器
            return@MediaCodecSelector when {
                mediaTekDecoders.isNotEmpty() -> mediaTekDecoders
                hardwareDecoders.isNotEmpty() -> hardwareDecoders
                else -> allDecoders // 最终回退到所有解码器（理论上应该总有软件解码器）
            }
        }
    // 配置 RenderersFactory
    val renderersFactory = DefaultRenderersFactory(context).apply {
        //setMediaCodecSelector(amlogicAwareCodecSelector)
        setMediaCodecSelector(mediaTekAwareCodecSelector)
    }
    // 根据 URI 协议选择合适的数据源工厂
    val dataSourceFactory = if (mediaUri.startsWith("smb://")) {
        // SMB 协议


        val cache = MzDkPlayerApplication.downloadCache

         CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory( SmbDataSourceFactory())
            .setCacheWriteDataSinkFactory(
                CacheDataSink.Factory().setCache(cache)
                .setFragmentSize(10 * 1024 * 1024)
                .setBufferSize(64 * 1024)) // 使用默认
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
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
        ).setRenderersFactory(renderersFactory)
        .buildWithAssSupport( //配置ass字幕显示 LEGACY
            context,
            AssRenderType.LEGACY,
            dataSourceFactory = dataSourceFactory,
            renderersFactory = renderersFactory
        ).apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
}
