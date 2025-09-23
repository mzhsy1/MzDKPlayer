package org.mz.mzdkplayer.ui.videoplayer.components

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodecList
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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import org.mz.mzdkplayer.tool.FtpDataSourceFactory
import org.mz.mzdkplayer.tool.WebDavDataSource
import org.mz.mzdkplayer.tool.WebDavDataSourceFactory

@OptIn(UnstableApi::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun BuilderMzPlayer(
    context: Context,
    mediaUri: String,
    exoPlayer: ExoPlayer,
    dataSourceType: String
) {
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
        val mediaItem =
            if (mediaUri.startsWith("smb://") || mediaUri.startsWith("http://") || mediaUri.startsWith(
                    "https://"
                ) || mediaUri.startsWith("ftp://")
            ) {
                MediaItem.fromUri(mediaUri)
            } else {
                // 处理本地文件路径
                val uri = if (mediaUri.startsWith("file://")) {
                    mediaUri.toUri()
                } else {
                    // 假设是文件路径，添加 file:// 前缀
                    "file://$mediaUri".toUri()
                }
                Log.d("MediaItemUri", uri.toString())
                MediaItem.fromUri(uri)
            }
        Log.e("AVCDecoderSelector", "==============================================")
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
                        videoPlayerViewModel.updateSubtitleVisibility(View.VISIBLE)

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
                        Log.d("sindex", index.toString())
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
fun rememberPlayer(context: Context, mediaUri: String, dataSourceType: String) =
    remember(mediaUri) {
        val codecInfos = MediaCodecList(MediaCodecList.ALL_CODECS)
        for (info in codecInfos.codecInfos) {
            if (info.isEncoder) continue
            for (type in info.supportedTypes) {
                if (type.contains("avc")) {
                    Log.i("CODEC", "Name: ${info.name}, Type: $type")
                    // 进一步可以查询 Capabilities
                }
            }
        }
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
//    val mediaTekAwareCodecSelector =
//        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
//            val allDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
//                mimeType,
//                requiresSecureDecoder,
//                requiresTunnelingDecoder
//            )
//
//            // 首先过滤出所有硬件解码器
//            val hardwareDecoders = allDecoders.filter { info ->
//                info.hardwareAccelerated // 系统标识的硬件解码器
//            }
//
//            // 优先选择联发科（MediaTek）的硬件解码器，特别是支持杜比视界的
//            val mediaTekDecoders = hardwareDecoders.filter { info ->
//                // 联发科解码器在名称中通常包含 "mediatek", "mtk", 或芯片型号如 "mt9653", "pentonic"
//                info.name.contains("mediatek", ignoreCase = true) ||
//                        info.name.contains("mtk", ignoreCase = true) ||
//                        info.name.contains("mt9653", ignoreCase = true) ||
//                        info.name.contains("pentonic", ignoreCase = true) ||
//                        // 同时保留之前针对杜比视界的筛选逻辑（如果仍需特定格式优先）
//                        (mimeType.startsWith("video/") && info.name.contains(
//                            "dolby",
//                            ignoreCase = true
//                        ))
//            }
//
//            Log.d("MediaCodecSelector", "For MIME type: $mimeType")
//            Log.d(
//                "MediaCodecSelector",
//                "All hardware decoders: ${hardwareDecoders.map { it.name }}"
//            )
//            Log.d(
//                "MediaCodecSelector",
//                "MediaTek prioritized decoders: ${mediaTekDecoders.map { it.name }}"
//            )
//
//            // 优先返回联发科解码器，如果没有则返回所有硬件解码器，最后再fallback到所有解码器
//            return@MediaCodecSelector when {
//                mediaTekDecoders.isNotEmpty() -> mediaTekDecoders
//                hardwareDecoders.isNotEmpty() -> hardwareDecoders
//                else -> allDecoders // 最终回退到所有解码器（理论上应该总有软件解码器）
//            }
//        }
        Log.i("AVCDecoderSelector", "==============================================")
//    val avcAwareCodecSelector =
//        MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
//            // 获取所有支持当前MIME类型的解码器信息
//            val allDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
//                mimeType,
//                requiresSecureDecoder,
//                requiresTunnelingDecoder
//            )
//
//            // 首先，详细记录所有解码器信息
//            Log.i("AVCDecoderSelector", "==============================================")
//            Log.i("AVCDecoderSelector", "Querying decoders for MIME type: '$mimeType'")
//            Log.i("AVCDecoderSelector", "Requires Secure: $requiresSecureDecoder, Requires Tunneling: $requiresTunnelingDecoder")
//            Log.i("AVCDecoderSelector", "All available decoders (${allDecoders.size} found):")
//            allDecoders.forEachIndexed { index, decoderInfo ->
//                Log.i("AVCDecoderSelector", "  [$index] Name: '${decoderInfo.name}', " +
//                        "HardwareAccelerated: ${decoderInfo.hardwareAccelerated}, " +
//                        "CodecMimeType: '${decoderInfo.codecMimeType}', " +
//                        "Capabilities: ${decoderInfo.capabilities}")
//            }
//
//            // 特别关注 video/avc (H.264) 的情况
//            if (mimeType.startsWith("video/avc") || mimeType.startsWith("video/avc")) {
//                // 分离硬件和软件解码器以便分析
//                val hardwareDecoders = allDecoders.filter { it.hardwareAccelerated }
//                val softwareDecoders = allDecoders.filterNot { it.hardwareAccelerated }
//
//                Log.i("AVCDecoderSelector", "--- H.264/AVC Specific Analysis ---")
//                Log.i("AVCDecoderSelector", "Total Hardware decoders: ${hardwareDecoders.size}")
//                hardwareDecoders.forEachIndexed { index, decoderInfo ->
//                    Log.i("AVCDecoderSelector", "  Hardware [$index]: '${decoderInfo.name}'")
//                }
//                Log.i("AVCDecoderSelector", "Total Software decoders: ${softwareDecoders.size}")
//                softwareDecoders.forEachIndexed { index, decoderInfo ->
//                    Log.i("AVCDecoderSelector", "  Software [$index]: '${decoderInfo.name}'")
//                }
//
//                // 在这里，我们可以自定义优先级逻辑。例如，优先选择硬件解码器：
//                val prioritizedList = if (hardwareDecoders.isNotEmpty()) {
//                    Log.i("AVCDecoderSelector", "Prioritizing hardware decoders.")
//                    hardwareDecoders
//                } else {
//                    Log.i("AVCDecoderSelector", "No hardware decoder found, falling back to software decoders.")
//                    softwareDecoders
//                }
//
//                // 如果你想强制使用某个特定的解码器（用于测试），可以在这里操作：
//                 val forcedDecoderName = "OMX.google.h264.decoder" // 示例
//                //OMX.google.h264.decoder 软件
//                 val forcedDecoder = allDecoders.find { it.name == forcedDecoderName }
//                 val finalList = forcedDecoder?.let { listOf(it) } ?: prioritizedList
//
//                Log.i("AVCDecoderSelector", "Final list of decoders to use (${prioritizedList.size}): ${prioritizedList.map { it.name }}")
//                prioritizedList // 返回优先选择的列表
//            } else {
//                // 对于非AVC格式，可以返回所有解码器或应用其他逻辑
//                Log.i("AVCDecoderSelector", "Non-AVC MIME type, returning all decoders.")
//                allDecoders
//            }
//        }
        // 配置 RenderersFactory
        val renderersFactory = DefaultRenderersFactory(context).apply {
            //setMediaCodecSelector(avcAwareCodecSelector)
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
        }

        // 根据 URI 协议选择合适的数据源工厂
        val dataSourceFactory = if (mediaUri.startsWith("smb://") && dataSourceType == "SMB") {
            // SMB 协议

            SmbDataSourceFactory()
            //val cache = MzDkPlayerApplication.downloadCache

//         CacheDataSource.Factory()
//            .setCache(cache)
//            .setUpstreamDataSourceFactory( SmbDataSourceFactory())
//            .setCacheWriteDataSinkFactory(
//                CacheDataSink.Factory().setCache(cache)
//                .setFragmentSize(20 * 1024 * 1024)
//                .setBufferSize(8 * 1024 * 1024)) // 使用16MB缓冲
            //       .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else if ((mediaUri.startsWith("file://") || mediaUri.startsWith("/")) && dataSourceType == "LOCAL") {
            // 本地文件协议或绝对路径
            DefaultDataSource.Factory(context)
        } else if ((mediaUri.startsWith("http://") || mediaUri.startsWith("https://")) && dataSourceType == "WEBDAV") {
            WebDavDataSourceFactory()
        } else if ((mediaUri.startsWith("ftp://")) && dataSourceType == "FTP") {
            FtpDataSourceFactory()
        } else {
            // 其他情况（如 http/https），使用默认的 HTTP 数据源
            DefaultHttpDataSource.Factory()
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000,  // minBufferMs: 最小缓冲时间 (例如 15秒)
                120000,  // maxBufferMs: 最大缓冲时间 (例如 60秒)
                5000,   // bufferForPlaybackMs: 开始播放前至少要缓冲的时间 (例如 2.5秒)
                5000    // bufferForPlaybackAfterRebufferMs: 重新缓冲后恢复播放前至少要缓冲的时间 (例如 5秒)
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // 不使用字节数限制
            .setPrioritizeTimeOverSizeThresholds(true) // 优先时间阈值
            .build()
        ExoPlayer.Builder(context).setSeekForwardIncrementMs(10000).setSeekBackIncrementMs(10000)
            .setLoadControl(loadControl)
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
