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

import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel

import androidx.core.net.toUri
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.TsExtractor
import org.mz.mzdkplayer.tool.FtpDataSourceFactory

import org.mz.mzdkplayer.tool.NFSDataSourceFactory


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
// ⚠️ 重点：获取与 rememberPlayer 中使用的相同的 DataSourceFactory 逻辑
    val dataSourceFactory =
        remember { selectedDataSourceFactory(mediaUri, dataSourceType, context) }
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
                ) || mediaUri.startsWith("ftp://") || mediaUri.startsWith("nfs://")
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
        // 2. 检查是否是需要特殊处理的 .m2ts 文件
        val isM2ts = mediaUri.endsWith(".m2ts", ignoreCase = true) ||
                mediaUri.endsWith(".mts", ignoreCase = true)

        val mediaSource = if (isM2ts) {
            Log.w("M2TS_EXTRACTOR", "M2TS file detected. Forcing usage of TsExtractor.")

            // 内部匿名类实现的 ExtractorsFactory，只返回 TsExtractor
            val forcedTsExtractorFactory = ExtractorsFactory {
                arrayOf<Extractor>(
                    TsExtractor(
                        SubtitleParser.Factory.UNSUPPORTED
                    )
                )
            }

            // ⭐️ 修正后的 Media3 v1.8.0 逻辑：将 ExtractorsFactory 传入构造函数
            ProgressiveMediaSource.Factory(
                dataSourceFactory,
                forcedTsExtractorFactory
            ).createMediaSource(mediaItem)

        } else {
            // 否则，使用 ExoPlayer 默认的 DefaultMediaSourceFactory
            Log.d("M2TS_EXTRACTOR", "Standard file. Using default MediaSourceFactory.")
            DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
        }
        exoPlayer.setMediaSource(mediaSource)
        //exoPlayer.setMediaItem(mediaItem)
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
                var hasPGSTrackSelected = false
                var hasASSTrackSelected = false
                var hasVTTTrackSelected = false
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
                                    if (format.codecs == "application/pgs") {
                                        hasPGSTrackSelected = true
                                        break // 找到一个SRT轨道就足够
                                    }
                                    if (format.codecs == "text/x-ssa") {
                                        hasASSTrackSelected = true
                                        break // 找到一个SRT轨道就足够
                                    }
                                    if (format.codecs == "text/vtt") {
                                        hasVTTTrackSelected = true
                                        break // 找到一个SRT轨道就足够
                                    }
                                }
                            }
                        }
                    }
                    // 根据是否选中了 SRT 或者 PGS轨道来设置可见性
                    if (hasSrtTrackSelected || hasPGSTrackSelected || hasVTTTrackSelected) {
                        Log.d("SDS1", "SubtitleView set to GONE because SRT PGS track is selected.")
                        videoPlayerViewModel.updateSubtitleVisibility(View.GONE)
                        videoPlayerViewModel.updateCusSubtitleVisibility(true)

                    } else if (hasASSTrackSelected) {
                        videoPlayerViewModel.updateSubtitleVisibility(View.GONE)
                        Log.d("SDS1", "SubtitleView set to VISIBLE because ASS track is selected.")
                        videoPlayerViewModel.updateCusSubtitleVisibility(true)
                    } else {
                        Log.d(
                            "SDS1", "SubtitleView set to VISIBLE because no SRT track is selected."
                        )
                        videoPlayerViewModel.updateCusSubtitleVisibility(false)
                        videoPlayerViewModel.updateSubtitleVisibility(View.VISIBLE)

                    }
                }
                if (videoPlayerViewModel.onTracksChangedState == 0) {
                    if (videoPlayerViewModel.mutableSetOfVideoTrackGroups.isNotEmpty()) {
                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
                                TrackSelectionOverride(
                                    videoPlayerViewModel.mutableSetOfVideoTrackGroups[0].mediaTrackGroup,
                                    0
                                )
                            ).build()
                    }
//                    exoPlayer.trackSelectionParameters =exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
//                        TrackSelectionOverride(
//                            videoPlayerViewModel.mutableSetOfAudioTrackGroups[0].mediaTrackGroup,
//                            0
//                        )
//                    ).build()
                    if (videoPlayerViewModel.mutableSetOfTextTrackGroups.isNotEmpty()) {
                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
                                TrackSelectionOverride(
                                    videoPlayerViewModel.mutableSetOfTextTrackGroups[0].mediaTrackGroup,
                                    0
                                )
                            ).build()
                    }
                }

                if (videoPlayerViewModel.mutableSetOfAudioTrackGroups.isNotEmpty()) {
                    for ((index, atGroup) in videoPlayerViewModel.mutableSetOfAudioTrackGroups.withIndex()) {
                        Log.d("VideoTrackGroupsID", atGroup.getTrackFormat(0).id.toString())
                        if (atGroup.isTrackSelected(0)) {
                            Log.d("sindex", index.toString())
                            videoPlayerViewModel.selectedAtIndex = index
                        }
                    }
                }
                if (videoPlayerViewModel.mutableSetOfVideoTrackGroups.isNotEmpty()) {
                    for ((index, vtGroup) in videoPlayerViewModel.mutableSetOfVideoTrackGroups.withIndex()) {
                        if (vtGroup.isTrackSelected(0)) {
                            videoPlayerViewModel.selectedVtIndex = index
                        }
                    }
                }
                if (videoPlayerViewModel.mutableSetOfTextTrackGroups.isNotEmpty()) {
                    for ((index, vtGroup) in videoPlayerViewModel.mutableSetOfTextTrackGroups.withIndex()) {
                        if (vtGroup.isTrackSelected(0)) {
                            videoPlayerViewModel.selectedStIndex = index
                        }
                    }
                }
                videoPlayerViewModel.onTracksChangedState = 1
            }


        })
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(context: Context, mediaUri: String, dataSourceType: String) =
    remember(mediaUri) {

        // 配置 RenderersFactory
        val renderersFactory =
            DefaultRenderersFactory(context).forceEnableMediaCodecAsynchronousQueueing().apply {
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
        } else if ((mediaUri.startsWith("nfs://")) && dataSourceType == "NFS") {
            NFSDataSourceFactory()
        } else if ((mediaUri.startsWith("http://") || mediaUri.startsWith("https://")) && dataSourceType == "HTTP") {
            DefaultHttpDataSource.Factory()
        } else {
            // 其他情况（如 http/https），使用默认的 HTTP 数据源
            DefaultHttpDataSource.Factory()
        }

//        val loadControl = DefaultLoadControl.Builder()
//            .setBufferDurationsMs(
//                30000,  // minBufferMs: 最小缓冲时间
//                120000, // maxBufferMs: 最大缓冲时间 (减少到 2 分钟)
//                2000,   // bufferForPlaybackMs: 开始播放前缓冲时间
//                5000    // bufferForPlaybackAfterRebufferMs: 重新缓冲后恢复播放前缓冲时间
//            )
//            .setTargetBufferBytes(C.LENGTH_UNSET)
//            .setPrioritizeTimeOverSizeThresholds(true)
//            .setBackBuffer(5000, true) // 添加回退缓冲，允许 seek 回退 5 秒
//            .build()
        ExoPlayer.Builder(context).setSeekForwardIncrementMs(60000).setSeekBackIncrementMs(60000)
            //.setLoadControl(loadControl)

            .setRenderersFactory(renderersFactory).build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE

            }
    }

//            .buildWithAssSupport(context = context, renderType = AssRenderType.LEGACY,
//                renderersFactory=renderersFactory, dataSourceFactory = dataSourceFactory).apply {
fun selectedDataSourceFactory(
    mediaUri: String,
    dataSourceType: String,
    context: Context
): DataSource.Factory {
    return if (mediaUri.startsWith("smb://") && dataSourceType == "SMB") {
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
    } else if ((mediaUri.startsWith("nfs://")) && dataSourceType == "NFS") {
        NFSDataSourceFactory()
    } else if ((mediaUri.startsWith("http://") || mediaUri.startsWith("https://")) && dataSourceType == "HTTP") {
        DefaultHttpDataSource.Factory()
    } else {
        // 其他情况（如 http/https），使用默认的 HTTP 数据源
        DefaultHttpDataSource.Factory()
    }
}