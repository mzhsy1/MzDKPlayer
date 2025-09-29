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
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@OptIn(UnstableApi::class)
fun setupPlayer(player: ExoPlayer, mediaUri: String, dataSourceType: String, context: Context, onMediaInfoReady: (MutableMap<String, String>) -> Unit, onError: (String) -> Unit): ExoPlayer {
    val metadataMap = mutableMapOf<String, String>()
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
                    val extractedMap = extractAndLogMediaInfo(player)
                    // 将提取的信息合并到返回的map中
                    metadataMap.putAll(extractedMap)

                    // 打印详细日志
                    Log.i("MediaInfo", "Width: ${extractedMap["VideoWidth"]}")
                    Log.i("MediaInfo", "Height: ${extractedMap["VideoHeight"]}")
                    Log.i("MediaInfo", "Frame Rate: ${extractedMap["VideoFrameRate"]}")
                    Log.i("MediaInfo", "Bitrate: ${extractedMap["VideoBitrate"]}")
                    Log.i("MediaInfo", "Mime Type: ${extractedMap["VideoMimeType"]}")
                    Log.i("MediaInfo", "Codecs: ${extractedMap["VideoCodecs"]}")
                    Log.i("MediaInfo", "Pixel Width Height Ratio: ${extractedMap["VideoPixelWidthHeightRatio"]}")

                    Log.i("MediaInfo", "Sample Rate: ${extractedMap["AudioSampleRate"]}")
                    Log.i("MediaInfo", "Channel Count: ${extractedMap["AudioChannelCount"]}")
                    Log.i("MediaInfo", "Audio Bitrate: ${extractedMap["AudioBitrate"]}")
                    Log.i("MediaInfo", "Audio Mime Type: ${extractedMap["AudioMimeType"]}")
                    Log.i("MediaInfo", "Audio Codecs: ${extractedMap["AudioCodecs"]}")

                    Log.i("MediaInfo", "Duration (ms): ${extractedMap["DurationMs"]}")
                    Log.i("MediaInfo", "Duration (formatted): ${extractedMap["DurationFormatted"]}")
                    Log.i("MediaInfo", "Current Position (ms): ${extractedMap["CurrentPositionMs"]}")

                    // 调用回调函数，传递媒体信息
                    onMediaInfoReady(metadataMap)
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
            // 传递错误信息给调用者
            onError("Player error: ${error.message}")
        }
    })
    return player
}

/**
 * 当播放器准备就绪时，提取并返回媒体信息
 */
@OptIn(UnstableApi::class)
private fun extractAndLogMediaInfo(player: ExoPlayer): MutableMap<String, String> {
    val metadataMap = mutableMapOf<String, String>()
    val videoFormat = player.videoFormat
    val audioFormat = player.audioFormat

    if (videoFormat != null) {
        Log.i("MediaInfo", "=== Video Information ===")

        putIfNotNull(metadataMap, "VideoWidth", videoFormat.width.toString())
        Log.i("MediaInfo", "Width: ${videoFormat.width}")

        putIfNotNull(metadataMap, "VideoHeight", videoFormat.height.toString())
        Log.i("MediaInfo", "Height: ${videoFormat.height}")

        putIfNotNull(metadataMap, "VideoFrameRate", videoFormat.frameRate?.toString())
        Log.i("MediaInfo", "Frame Rate: ${videoFormat.frameRate}")

        putIfNotNull(metadataMap, "VideoBitrate", videoFormat.bitrate.toString())
        Log.i("MediaInfo", "Bitrate: ${videoFormat.bitrate}")

        putIfNotNull(metadataMap, "VideoMimeType", videoFormat.sampleMimeType)
        Log.i("MediaInfo", "Mime Type: ${videoFormat.sampleMimeType}")

        putIfNotNull(metadataMap, "VideoCodecs", videoFormat.codecs)
        Log.i("MediaInfo", "Codecs: ${videoFormat.codecs}")

        putIfNotNull(metadataMap, "VideoPixelWidthHeightRatio", videoFormat.pixelWidthHeightRatio.toString())
        Log.i("MediaInfo", "Pixel Width Height Ratio: ${videoFormat.pixelWidthHeightRatio}")
    } else {
        Log.i("MediaInfo", "No video track found.")
        putIfNotNull(metadataMap, "VideoTrackFound", "false")
    }

    if (audioFormat != null) {
        Log.i("MediaInfo", "=== Audio Information ===")

        putIfNotNull(metadataMap, "AudioSampleRate", audioFormat.sampleRate.toString())
        Log.i("MediaInfo", "Sample Rate: ${audioFormat.sampleRate}")

        putIfNotNull(metadataMap, "AudioChannelCount", audioFormat.channelCount.toString())
        Log.i("MediaInfo", "Channel Count: ${audioFormat.channelCount}")

        putIfNotNull(metadataMap, "AudioBitrate", audioFormat.bitrate.toString())
        Log.i("MediaInfo", "Bitrate: ${audioFormat.bitrate}")

        putIfNotNull(metadataMap, "AudioMimeType", audioFormat.sampleMimeType)
        Log.i("MediaInfo", "Mime Type: ${audioFormat.sampleMimeType}")

        putIfNotNull(metadataMap, "AudioCodecs", audioFormat.codecs)
        Log.i("MediaInfo", "Codecs: ${audioFormat.codecs}")
    } else {
        Log.i("MediaInfo", "No audio track found.")
        putIfNotNull(metadataMap, "AudioTrackFound", "false")
    }

    Log.i("MediaInfo", "=== General Information ===")
    putIfNotNull(metadataMap, "DurationMs", player.duration.toString())
    Log.i("MediaInfo", "Duration (ms): ${player.duration}")

    val formattedDuration = formatTime(player.duration)
    putIfNotNull(metadataMap, "DurationFormatted", formattedDuration)
    Log.i("MediaInfo", "Duration (formatted): $formattedDuration")

    putIfNotNull(metadataMap, "CurrentPositionMs", player.currentPosition.toString())
    Log.i("MediaInfo", "Current Position (ms): ${player.currentPosition}")

    // 元数据信息（如 ID3、EBML 等）会在 onMetadata 回调中提供
    Log.i(
        "MediaInfo",
        "Metadata information will be logged in the onMetadata callback if available."
    )

    return metadataMap
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@OptIn(UnstableApi::class)
fun builderPlayer(mediaUri: String, context: Context, dataSourceType: String): ExoPlayer{
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

    // 针对错误信息中的Dolby Vision编码问题，配置渲染器
    val renderersFactory = DefaultRenderersFactory(context).apply {
        //setMediaCodecSelector(avcAwareCodecSelector)
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON) // 启用扩展渲染器模式，可能有助于处理特殊编码
    }

    return ExoPlayer.Builder(context)
        .setSeekForwardIncrementMs(10000)
        .setSeekBackIncrementMs(10000)
        //.setLoadControl(loadControl)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                dataSourceFactory
            )
        )
        .setRenderersFactory(renderersFactory)
        .build()
}

private fun putIfNotNull(map: MutableMap<String, String>, key: String, value: String?) {
    if (value != null) {
        map[key] = value
    }
}

// 使用示例
//fun exampleUsage(focusedIsDir: Boolean, focusedFileName: String, focusedMediaUri: String, context: Context) {
//    if (!focusedIsDir && Tools.containsVideoFormat(
//            Tools.extractFileExtension(focusedFileName)
//        )) {
//        Log.d("focusedIsDir", false.toString())
//        Log.d("focusedIsDir","获取媒体信息")
//        val exoPlayer = builderPlayer(mediaUri = focusedMediaUri, context, dataSourceType = "SMB")
//
//        // 使用回调方式获取媒体信息
//        setupPlayer(
//            exoPlayer,
//            focusedMediaUri,
//            "SMB",
//            context,
//            onMediaInfoReady = { mediaInfoMap ->
//                // 在这里处理获取到的媒体信息
//                Log.d("focusedIsDir", mediaInfoMap.toString())
//
//                // 可以在这里使用媒体信息进行后续操作
//                // 例如更新UI、保存到数据库等
//            },
//            onError = { errorMessage ->
//                // 在发生错误时处理
//                Log.e("focusedIsDir", "Error occurred: $errorMessage")
//
//                // 可以返回一个包含错误信息的map或执行其他错误处理
//                val errorMap = mutableMapOf<String, String>()
//                errorMap["Error"] = errorMessage
//                Log.d("focusedIsDir", errorMap.toString())
//            }
//        )
//    }
//}
//
//// 模拟的Tools类，用于编译
//object Tools {
//    fun containsVideoFormat(extension: String): Boolean {
//        val videoExtensions = listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v")
//        return videoExtensions.contains(extension.lowercase())
//    }
//
//    fun extractFileExtension(fileName: String): String {
//        return fileName.substringAfterLast('.', "")
//    }
//}
//
//// 模拟的数据源工厂类，用于编译
//class SmbDataSourceFactory
//class WebDavDataSourceFactory
//class FtpDataSourceFactory
//class NFSDataSourceFactory



