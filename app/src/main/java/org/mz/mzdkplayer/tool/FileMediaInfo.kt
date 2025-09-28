package org.mz.mzdkplayer.tool

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.ui.text.intl.Locale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType


@OptIn(UnstableApi::class)
fun setupPlayer(player: ExoPlayer, mediaUri: String, dataSourceType: String, context: Context) {

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


    player.setMediaItem(mediaItem)
    player.prepare()
    // 7. 添加 Player.EventListener 来监听准备完成等事件
    player.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    Log.d("MediaInfo", "Player is ready. Extracting information...")
                    extractAndLogMediaInfo(player)
                }

                Player.STATE_ENDED -> {
                    Log.d("MediaInfo", "Playback ended.")
                }

                Player.STATE_BUFFERING -> {
                    Log.d("MediaInfo", "Player is buffering.")
                }

                Player.STATE_IDLE -> {
                    Log.d("MediaInfo", "Player is idle.")
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("MediaInfo", "Player error: ${error.message}", error)
        }
    })
}

/**
 * 当播放器准备就绪时，提取并记录媒体信息
 */
@OptIn(UnstableApi::class)
private fun extractAndLogMediaInfo(player: ExoPlayer) {
    val videoFormat = player.videoFormat
    val audioFormat = player.audioFormat

    if (videoFormat != null) {
        Log.i("MediaInfo", "=== Video Information ===")
        Log.i("MediaInfo", "Width: ${videoFormat.width}")
        Log.i("MediaInfo", "Height: ${videoFormat.height}")
        Log.i("MediaInfo", "Frame Rate: ${videoFormat.frameRate}")
        Log.i("MediaInfo", "Bitrate: ${videoFormat.bitrate}")
        Log.i("MediaInfo", "Mime Type: ${videoFormat.sampleMimeType}")
        Log.i("MediaInfo", "Codecs: ${videoFormat.codecs}")
        Log.i("MediaInfo", "Pixel Width Height Ratio: ${videoFormat.pixelWidthHeightRatio}")
    } else {
        Log.i("MediaInfo", "No video track found.")
    }

    if (audioFormat != null) {
        Log.i("MediaInfo", "=== Audio Information ===")
        Log.i("MediaInfo", "Sample Rate: ${audioFormat.sampleRate}")
        Log.i("MediaInfo", "Channel Count: ${audioFormat.channelCount}")
        Log.i("MediaInfo", "Bitrate: ${audioFormat.bitrate}")
        Log.i("MediaInfo", "Mime Type: ${audioFormat.sampleMimeType}")
        Log.i("MediaInfo", "Codecs: ${audioFormat.codecs}")
    } else {
        Log.i("MediaInfo", "No audio track found.")
    }

    Log.i("MediaInfo", "=== General Information ===")
    Log.i("MediaInfo", "Duration (ms): ${player.duration}")
    Log.i("MediaInfo", "Duration (formatted): ${formatTime(player.duration)}")
    Log.i("MediaInfo", "Current Position (ms): ${player.currentPosition}")

    // 元数据信息（如 ID3、EBML 等）会在 onMetadata 回调中提供
    Log.i(
        "MediaInfo",
        "Metadata information will be logged in the onMetadata callback if available."
    )

}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@OptIn(UnstableApi::class)
 fun builderPlayer(mediaUri: String, context: Context,dataSourceType: String): ExoPlayer{
    val dataSourceFactory = if (mediaUri.startsWith("smb://") && dataSourceType == "SMB") {
        // SMB 协议
        SmbDataSourceFactory()
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
    val renderersFactory = DefaultRenderersFactory(context).apply {
        //setMediaCodecSelector(avcAwareCodecSelector)
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
    }
    return ExoPlayer.Builder(context).setSeekForwardIncrementMs(10000).setSeekBackIncrementMs(10000)
        //.setLoadControl(loadControl)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                dataSourceFactory
            )
        ).build()
}




