package org.mz.mzdkplayer.player.core


import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.TimestampAdjuster
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.extractor.ts.TsExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA
import androidx.media3.extractor.ts.TsExtractor.MODE_MULTI_PMT
import androidx.media3.extractor.ts.TsPayloadReader
import org.mz.mzdkplayer.tool.FtpDataSourceFactory



import org.mz.mzdkplayer.tool.NFSDataSourceFactory
import org.mz.mzdkplayer.tool.SmbDataSourceFactory
import org.mz.mzdkplayer.tool.WebDavDataSourceFactory
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.viewmodel.VideoPlayerViewModel


@OptIn(UnstableApi::class)
fun selectedDataSourceFactory(
    mediaUri: String,
    dataSourceType: String,
    context: Context
): DataSource.Factory {
    return if (mediaUri.startsWith("smb://") && dataSourceType == "SMB") {
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
        DefaultHttpDataSource.Factory().setReadTimeoutMs(30000).setConnectTimeoutMs(30000)
    } else {
        // 其他情况（如 http/https），使用默认的 HTTP 数据源
        DefaultHttpDataSource.Factory().setReadTimeoutMs(30000).setConnectTimeoutMs(30000)
    }
}


@OptIn(UnstableApi::class)
fun findExternalSubtitles(mediaUri: String): List<MediaItem.SubtitleConfiguration> {
    val subtitles = mutableListOf<MediaItem.SubtitleConfiguration>()

    // 1. 获取基础 URI 部分（不含扩展名）
    val lastDotIndex = mediaUri.lastIndexOf('.')
    if (lastDotIndex == -1) return subtitles // 没有扩展名，无法处理

    val baseUri = mediaUri.take(lastDotIndex)

    // 定义要查找的字幕扩展名和对应的 MIME 类型
    val subtitleExtensions = mapOf(
        ".srt" to "application/x-subrip", // SRT
        ".ass" to "text/x-ssa",          // ASS/SSA
        ".ssa" to "text/x-ssa",          // ASS/SSA (另一种扩展名)
        ".vtt" to "text/vtt"             // WebVTT
    )

    // 2. 遍历可能的字幕文件
    for ((ext, mimeType) in subtitleExtensions) {
        // 构造潜在的字幕文件 URI
        val subtitleUriString = baseUri + ext
        val subtitleUri = subtitleUriString.toUri()
        val fileName = subtitleUriString.substringAfterLast('/').substringAfterLast('\\')
        // 由于 Media3/ExoPlayer 在加载 MediaItem 时会处理 URI 的有效性
        // 对于远程文件（如HTTP/SMB），只需要构造URI即可。
        // 对于本地文件，理论上你需要检查文件是否存在（但对于跨平台/网络，最简单是直接交给播放器处理）
        // 播放器会尝试加载这个URI，如果失败，它会跳过这个字幕，不会影响视频播放。

        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(mimeType)
            // 你可能需要根据字幕文件的语言约定来设置语言，这里暂时留空或设置为默认
            // 例如：如果文件名是 "video.zh.srt"，则可以尝试解析 "zh"
            // 为了简单起见，我们暂时设置一个默认的 SelectionFlag
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).setLabel("[外部加载]$fileName")
            .setLanguage("zh")
            .build()

        subtitles.add(subtitleConfig)
        Log.d("SubtitleLoader", "Candidate subtitle: $subtitleUriString ($mimeType)")
    }

    return subtitles
}

fun finalUri(mediaUri: String) =
    if (mediaUri.startsWith("smb://") || mediaUri.startsWith("http://") || mediaUri.startsWith(
            "https://"
        ) || mediaUri.startsWith("ftp://") || mediaUri.startsWith("nfs://")
    ) {
        mediaUri.toUri()
    } else {
        // 处理本地文件路径
        if (mediaUri.startsWith("file://")) {
            mediaUri.toUri()
        } else {
            "file://$mediaUri".toUri()
        }
    }
