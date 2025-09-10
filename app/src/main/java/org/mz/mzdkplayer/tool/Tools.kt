package org.mz.mzdkplayer.tool


import androidx.annotation.OptIn

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi

import org.mz.mzdkplayer.R
import java.util.Locale

object Tools {
    fun extractFileExtension(fileName: String?): String {
        if (fileName == null || fileName.isEmpty()) {
            return ""
        }
        // 处理可能以点结尾的文件名或隐藏文件（无扩展名）
        val lastDotIndex = fileName.lastIndexOf('.')
        if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            // 确保点不在字符串的开头（隐藏文件）且不是最后一个字符
            return fileName.substring(lastDotIndex + 1).lowercase(Locale.getDefault())
        }
        return "" // 没有扩展名

    }

    fun containsVideoFormat(input: String): Boolean {
        val videoFormats = listOf("MP4", "MKV", "M2TS", "3GP", "AVI", "MOV", "TS", "FLV")
        return videoFormats.any { format ->
            input.contains(format, ignoreCase = true)
        }
    }

    /**
     * 根据音频轨道的 Format 信息推断具体的音频格式类型
     * @param format 音频轨道的 Format 对象
     * @return 推断出的音频格式描述字符串
     */
    fun inferAudioFormatType(format: Format): String {
        val mimeType = format.sampleMimeType ?: return "Unknown Audio Format (No MIME type)"

        return when (mimeType) {
            "audio/vnd.dts" -> inferDtsFormatType(format) // DTS 家族格式判断

            // Dolby 家族格式判断
            "audio/true-hd" -> "Dolby TrueHD"
            "audio/ac3" -> "Dolby Digital (AC3)"
            "audio/eac3" -> "Dolby Digital Plus (E-AC3)"
            "audio/eac3-joc" -> "Dolby Digital Plus with Atmos (E-AC3 JOC)"

            // AAC 格式
            "audio/mp4a-latm" -> "AAC (Advanced Audio Coding)"

            // OPUS 格式
            "audio/opus" -> "Opus"

            // Vorbis 格式
            "audio/vorbis" -> "Vorbis"

            // FLAC 格式
            "audio/flac" -> "FLAC (Free Lossless Audio Codec)"

            // PCM 格式
            "audio/raw" -> "PCM (Uncompressed)"
            "audio/wav" -> "WAV (PCM)"
            "audio/x-wav" -> "WAV (PCM)"

            // MP3 格式
            "audio/mpeg" -> "MP3 (MPEG-1 Audio Layer III)"
            "audio/mp3" -> "MP3 (MPEG-1 Audio Layer III)"

            // 其他已知格式
            else -> mimeType.removePrefix("audio/").uppercase()
        }
    }
    /**
     * 专门推断 DTS 家族具体格式的辅助方法
     */
    private fun inferDtsFormatType(format: Format): String {
        val codecs = format.codecs?.lowercase() ?: ""

        return when {
            codecs.contains("dts-hd-ma") -> "DTS-HD Master Audio"
            codecs.contains("dts-hd-hra") -> "DTS-HD High Resolution"
            codecs.contains("dts-x") -> "DTS:X"
            codecs.contains("dts-express") -> "DTS Express"
            codecs.contains("dts") -> "DTS Core"

            // 如果没有明确的 codecs 信息，则基于声道数等进行推测
            format.channelCount >= 8 -> "DTS-HD "
            format.channelCount == 6 -> "DTS Core "
            else -> "DTS (未知DTS编码)"
        }
    }


    fun audioFormatIconType(format: Format): Int {
        val mimeType = format.sampleMimeType ?: return   R.drawable.noradudio

        return when (mimeType) {
            "audio/vnd.dts" -> inferDtsFormatTypeIcon(format) // DTS 家族格式判断

            // Dolby 家族格式判断
            "audio/true-hd" -> R.drawable.logo_dolby_audio
            "audio/ac3" -> R.drawable.logo_dolby_audio
            "audio/eac3" -> R.drawable.logo_dolby_audio
            "audio/eac3-joc" -> R.drawable.dolby_atmos

            // AAC 格式
            "audio/mp4a-latm" -> R.drawable.aac

            // OPUS 格式
            "audio/opus" ->   R.drawable.noradudio

            // Vorbis 格式
            "audio/vorbis" ->    R.drawable.noradudio

            // FLAC 格式
            "audio/flac" -> R.drawable.hei

            // PCM 格式
            "audio/raw" -> R.drawable.pcm_seeklogo__1_
            "audio/wav" -> R.drawable.pcm_seeklogo__1_
            "audio/x-wav" -> R.drawable.pcm_seeklogo__1_

            // MP3 格式
            "audio/mpeg" ->  R.drawable.mp3_seeklogo
            "audio/mp3" -> R.drawable.mp3_seeklogo

            // 其他已知格式
            else ->   R.drawable.noradudio
        }
    }


    /**
     * 专门推断 DTS 家族具体格式的辅助方法
     */
    private fun inferDtsFormatTypeIcon(format: Format): Int {
        val codecs = format.codecs?.lowercase() ?: ""

        return when {
            codecs.contains("dts-hd-ma") -> R.drawable.dts_hd_master_audio_seeklogo
            codecs.contains("dts-hd-hra") -> R.drawable.dts_hd_master_audio_seeklogo
            codecs.contains("dts-x") -> R.drawable.dts_hd_master_audio_seeklogo
            codecs.contains("dts-express") -> R.drawable.dts_1
            codecs.contains("dts") -> R.drawable.dts_1

            // 如果没有明确的 codecs 信息，则基于声道数等进行推测
            format.channelCount >= 8 -> R.drawable.dts_hd_master_audio_seeklogo
            format.channelCount == 6 ->  R.drawable.dts_1
            else -> R.drawable.dts_1
        }
    }

    /**
     * 获取音频轨道的详细技术信息
     */
    @OptIn(UnstableApi::class)
    fun getAudioTrackTechnicalDetails(format: Format): Map<String, Any> {
        return mapOf(
            "format" to inferAudioFormatType(format),
            "mimeType" to (format.sampleMimeType ?: "null"),
            "codecs" to (format.codecs ?: "null"),
            "channels" to format.channelCount,
            "sampleRate" to if (format.sampleRate != Format.NO_VALUE) "${format.sampleRate}Hz" else "unknown",
            "bitrate" to if (format.bitrate != Format.NO_VALUE) "${format.bitrate / 1000} kbps" else "unknown",
            "language" to (format.language ?: "und"),
            "id" to (format.id ?: "null")
        )
    }
}

