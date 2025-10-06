package org.mz.mzdkplayer.ui.audioplayer.components

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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import org.mz.mzdkplayer.tool.FtpDataSourceFactory
import org.mz.mzdkplayer.tool.NFSDataSourceFactory
import org.mz.mzdkplayer.tool.SmbDataSourceConfig

import org.mz.mzdkplayer.tool.WebDavDataSourceFactory
import kotlin.Int

@OptIn(UnstableApi::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun BuilderMzAudioPlayer(
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



                }
//                if (videoPlayerViewModel.onTracksChangedState == 0) {

//                    exoPlayer.trackSelectionParameters =exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
//                        TrackSelectionOverride(
//                            videoPlayerViewModel.mutableSetOfAudioTrackGroups[0].mediaTrackGroup,
//                            0
//                        )
//                    ).build()

  //              }

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


    LaunchedEffect(exoPlayer) {
        Log.d("exoPlayerInit", "开始初始化exoPlayer")
        //delay(150.microseconds)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberAudioPlayer(context: Context, mediaUri: String, dataSourceType: String) =
    remember(mediaUri) {

        Log.i("AVCDecoderSelector", "==============================================")

        // 配置 RenderersFactory
        val renderersFactory = DefaultRenderersFactory(context).apply {
            //setMediaCodecSelector(avcAwareCodecSelector)
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
        }

        // 根据 URI 协议选择合适的数据源工厂
        val dataSourceFactory = if (mediaUri.startsWith("smb://") && dataSourceType == "SMB") {
            // SMB 协议 1 * 1024 * 1024防止卡顿

            SmbDataSourceFactory(SmbDataSourceConfig(bufferSizeBytes = 1 * 1024 * 1024,smbBufferSizeBytes=1 * 1024 * 1024,readBufferSizeBytes = 1 * 1024 * 1024))
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
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000,  // minBufferMs: 最小缓冲时间 (例如 15秒)
                150000,  // maxBufferMs: 最大缓冲时间 (例如 60秒)
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
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }
