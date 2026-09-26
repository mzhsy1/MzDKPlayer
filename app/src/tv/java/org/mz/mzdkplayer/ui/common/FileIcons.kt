package org.mz.mzdkplayer.ui.common

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.media3.common.util.UnstableApi
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools

/**
 * 电视端的文件 / 音轨图标映射。
 *
 * 这两个函数原本挂在 `Tools` object 上，但它们依赖 app 的 `R.drawable`，
 * 是 core → app 的反向依赖，所以留在 app 侧（手机端暂不需要，故放在 tv sourceSet）。
 */

@OptIn(UnstableApi::class)
@Composable
fun VideoBigIcon(focusedIsDir: Boolean, fileName: String?, modifier: Modifier) {
    Log.d("fileName", fileName.toString())
    Log.d("eFileName", Tools.extractFileExtension(fileName))
    if (focusedIsDir) {
        Image(
            modifier = modifier,
            painter = painterResource(R.drawable.foldernew),
            contentDescription = null,
        )
    } else {
        when (Tools.extractFileExtension(fileName)) {
            "mkv" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.mkvnew),
                contentDescription = null,
            )

            "mp4" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.mp4new),
                contentDescription = null,
            )

            "flv" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.flvnew),
                contentDescription = null,
            )

            "3gp" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.n3gpnew),
                contentDescription = null,
            )

            "ts" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.tsnew),
                contentDescription = null,
            )

            "m2ts" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.m2tsnew),
                contentDescription = null,
            )

            "mov" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.movnew),
                contentDescription = null,
            )

            "mp3" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.mp3big),
                contentDescription = null,
            )

            "wav" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.wavbig),
                contentDescription = null,
            )

            "flac" -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.flacnew),
                contentDescription = null,
            )

            else -> Image(
                modifier = modifier,
                painter = painterResource(R.drawable.filenew),
                contentDescription = null,
            )
        }
    }
}

fun audioFormatIconType(mimeType: String?): Int {
    if (mimeType == null) return R.drawable.noradudio

    // 统一转小写，去掉空格，方便匹配
    val type = mimeType.lowercase().trim()

    return when {
        // --- 1. Dolby 家族 ---
        // Atmos 优先级最高
        type.contains("eac3-joc") || type.contains("atmos") -> {
            R.drawable.dolby_atmos
        }
        // TrueHD (ExoPlayer: audio/true-hd | VLC: TrueHD Audio)
        type.contains("true-hd") || type.contains("truehd") -> {
            R.drawable.logo_dolby_audio
        }
        // AC3 / A52 (ExoPlayer: audio/ac3 | VLC: A52 Audio (aka AC3))
        type.contains("ac3") || type.contains("ac-3") || type.contains("a52") -> {
            R.drawable.logo_dolby_audio
        }
        // E-AC3 / DD+
        type.contains("eac3") || type.contains("ec-3") -> {
            R.drawable.logo_dolby_audio
        }

        // --- 2. DTS 家族 ---
        // DTS-HD (ExoPlayer: audio/vnd.dts.hd | VLC 如果包含特定标识)
        type.contains("dts-hd") || type.contains("dtshd") || type.contains("master audio") -> {
            R.drawable.dts_hd_master_audio
        }
        // 普通 DTS (ExoPlayer: audio/vnd.dts | VLC: DTS Audio)
        type.contains("dts") -> {
            R.drawable.dts_1
        }

        // --- 3. 其他常见格式 ---
        // AAC (ExoPlayer: audio/mp4a-latm | VLC: AAC Audio)
        type.contains("mp4a") || type.contains("aac") -> {
            R.drawable.aac
        }
        // FLAC
        type.contains("flac") -> {
            R.drawable.hei
        }
        // MP3 (ExoPlayer: audio/mpeg | VLC: MPEG Audio layer 3)
        type.contains("mpeg") || type.contains("mp3") || type.contains("layer 3") -> {
            R.drawable.mp3
        }
        // PCM / WAV
        type.contains("pcm") || type.contains("raw") || type.contains("wav") -> {
            R.drawable.pcm_seeklogo__1_
        }

        // 默认返回
        else -> R.drawable.noradudio
    }
}
