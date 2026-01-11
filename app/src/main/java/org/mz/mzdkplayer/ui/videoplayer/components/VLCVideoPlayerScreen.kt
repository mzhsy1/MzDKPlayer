package org.mz.mzdkplayer.ui.videoplayer.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun VLCVideoPlayerScreen(
    mediaUri: String,
    fileName: String
) {
    val context = LocalContext.current

    // 1. 优化 LibVLC 参数配置
    val libVLC = remember {
        val options = arrayListOf(
            "-vvv",
            // 尝试更激进的硬件解码策略，或者在某些烂设备上可能需要去掉这行用软解
            "--mediacodec-dr",
            "--vout=android_display",
            // "--audio-resampler=soxr", // <--- 【删除】绝对不要在直通时用重采样器
            "--no-audio-time-stretch",   // 禁用音频时间伸缩，直通必须禁用

            // 尝试强制 S/PDIF (某些旧版本或设备需要显式开启直通)
            // "--spdif",

            // 缓存设置
            "--file-caching=6000",
            "--network-caching=6000",
            "--live-caching=6000"
        )
        LibVLC(context, options)
    }

    val mediaPlayer = remember { MediaPlayer(libVLC) }

    // 2. 状态管理
    var isSurfaceReady by remember { mutableStateOf(false) }

    // 3. 播放逻辑
    LaunchedEffect(isSurfaceReady, mediaUri) {
        if (isSurfaceReady && mediaUri.isNotEmpty()) {
            try {
                val uri = mediaUri.toUri()
                val media = Media(libVLC, uri).apply {
                    // 硬件解码设置：
                    // 第一个 true: 启用硬件解码
                    // 第二个 true: 强制硬解 (force). 如果设备硬解太烂容易崩，建议改成 false 试试
                    setHWDecoderEnabled(true, false)

                    // 针对部分流媒体优化
                    //addOption(":demux=avformat")

                    // 尝试减少延迟
                    addOption(":clock-jitter=0")
                    addOption(":clock-synchro=0")
                }

                mediaPlayer.media = media
                // 释放 Media 对象，因为 MediaPlayer 已经持有了引用
                media.release()

                // 尝试强制数字音频输出 (Passthrough)
                // 注意：这取决于设备是否能正确握手 HDMI
                mediaPlayer.setAudioDigitalOutputEnabled(true)

                mediaPlayer.play()
            } catch (e: Exception) {
                Log.e("VLCPlayer", "Error loading media", e)
            }
        }
    }

    // 4. 生命周期清理 (DisposableEffect 必须在 AndroidView 之前定义，保证清理逻辑正确)
    DisposableEffect(Unit) {
        onDispose {
            // 先停止，再 detach，最后 release
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVLC.release()
        }
    }

    // 5. 视图层
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            VLCVideoLayout(ctx).apply {
                // Attach 时不需要过早，VLCVideoLayout 会自动处理 Surface
                mediaPlayer.attachViews(this, null, true, false)

                // 确保布局加载完成后再通知播放，避免 Surface 还没这就开始推流导致的报错
                post { isSurfaceReady = true }
            }
        }
    )
}