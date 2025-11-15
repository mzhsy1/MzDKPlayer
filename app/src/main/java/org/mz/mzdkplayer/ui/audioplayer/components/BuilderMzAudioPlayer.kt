package org.mz.mzdkplayer.ui.audioplayer.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import org.mz.mzdkplayer.tool.SmbDataSourceFactory
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.tool.FtpDataSourceFactory
import org.mz.mzdkplayer.tool.NFSDataSourceFactory
import org.mz.mzdkplayer.tool.SmbDataSourceConfig
import org.mz.mzdkplayer.tool.WebDavDataSourceFactory
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel

@OptIn(UnstableApi::class)
@SuppressLint("SuspiciousIndentation")
@Composable
fun BuilderMzAudioPlayer(
    context: Context,
    mediaUri: String,
    exoPlayer: ExoPlayer,
    dataSourceType: String,
    extraList: List<AudioItem>,
    currentIndex: String,
    audioPlayerViewModel: AudioPlayerViewModel
) {


    LaunchedEffect(Unit) {
        Log.d("播放器uri", mediaUri)

//        // 创建媒体项列表
        val mediaItems = mutableListOf<MediaItem>()

        // 添加当前播放的媒体项
//        val currentMediaItem = if (mediaUri.startsWith("smb://") || mediaUri.startsWith("http://") || mediaUri.startsWith(
//                "https://"
//            ) || mediaUri.startsWith("ftp://") || mediaUri.startsWith("nfs://")
//        ) {
//            MediaItem.fromUri(mediaUri)
//        } else {
//            // 处理本地文件路径
//            val uri = if (mediaUri.startsWith("file://")) {
//                mediaUri.toUri()
//            } else {
//                // 假设是文件路径，添加 file:// 前缀
//                "file://$mediaUri".toUri()
//            }
//            Log.d("MediaItemUri", uri.toString())
//            MediaItem.fromUri(uri)
//        }
//        mediaItems.add(currentMediaItem)

        // 添加额外的媒体项到播放列表
        for (audioItem in extraList) {
            val extraMediaItem = if (audioItem.uri.startsWith("smb://") || audioItem.uri.startsWith("http://") || audioItem.uri.startsWith(
                    "https://"
                ) || audioItem.uri.startsWith("ftp://") || audioItem.uri.startsWith("nfs://")
            ) {
                MediaItem.fromUri(audioItem.uri)
            } else {
                val uri = if (audioItem.uri.startsWith("file://")) {
                    audioItem.uri.toUri()
                } else {
                    "file://${audioItem.uri}".toUri()
                }
                Log.d("ExtraMediaItemUri", uri.toString())
                MediaItem.fromUri(uri)
            }
            mediaItems.add(extraMediaItem)
        }

        // 清除现有的播放列表并添加新的媒体项
        exoPlayer.clearMediaItems()
        exoPlayer.addMediaItems(mediaItems)
        exoPlayer.seekTo(currentIndex.toInt(), 0) // ⭐ 关键：跳到指定位置
        audioPlayerViewModel.selectedAtIndex = currentIndex.toInt()
        exoPlayer.prepare()
        exoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {


            }

        })
    }

    LaunchedEffect(exoPlayer) {
        Log.d("exoPlayerInit", "开始初始化exoPlayer")
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberAudioPlayer(context: Context, mediaUri: String, dataSourceType: String) =
    remember(mediaUri) {

        // 配置 RenderersFactory
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
            setEnableAudioFloatOutput(true)
            setEnableDecoderFallback(true)
        }

        // 根据 URI 协议选择合适的数据源工厂
        val dataSourceFactory = if (mediaUri.startsWith("smb://") && dataSourceType == "SMB") {
            // SMB 协议 1 * 1024 * 1024防止卡顿
            SmbDataSourceFactory(SmbDataSourceConfig(bufferSizeBytes = 1 * 1024 * 1024, smbBufferSizeBytes = 1 * 1024 * 1024, readBufferSizeBytes = 1 * 1024 * 1024))
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
                30000,  // minBufferMs: 最小缓冲时间
                120000, // maxBufferMs: 最大缓冲时间 (减少到 2 分钟)
                2000,   // bufferForPlaybackMs: 开始播放前缓冲时间
                5000    // bufferForPlaybackAfterRebufferMs: 重新缓冲后恢复播放前缓冲时间
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(5000, true) // 添加回退缓冲，允许 seek 回退 5 秒
            .build()


        ExoPlayer.Builder(context)
            .setSeekForwardIncrementMs(60000)
            .setSeekBackIncrementMs(60000)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
            )
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF // 设置为循环播放整个播放列表
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA) // 媒体播放
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC) // 音乐内容
                    .build()
                setAudioAttributes(audioAttributes, true) // true 表示处理音频焦点
            }
    }



