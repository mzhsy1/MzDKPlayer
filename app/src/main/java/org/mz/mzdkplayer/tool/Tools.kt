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

    /**
     * 将语言代码转换为中文语言名称（特别细化中文区分）
     */
    fun getFullLanguageName(languageCode: String?): String {
        if (languageCode.isNullOrEmpty() || languageCode == "und") {
            return "未知语言"
        }

        val lowerCode = languageCode.lowercase()

        // 特别处理中文的细分
        return when {
            // 简体中文
            lowerCode == "zh-hans" || lowerCode == "zh-cn" || lowerCode == "zh-sg" -> "简体中文"
            // 繁体中文
            lowerCode == "zh-hant" || lowerCode == "zh-tw" || lowerCode == "zh-hk" || lowerCode == "zh-mo" -> "繁体中文"
            // 一般中文代码（无法区分简繁体时）
            lowerCode == "zh" || lowerCode == "zho" || lowerCode == "chi" -> "中文"

            // 其他语言保持不变
            else -> getOtherLanguageName(lowerCode)
        }
    }

    /**
     * 处理其他语言的名称转换
     */
    private fun getOtherLanguageName(languageCode: String): String {
        return when (languageCode) {
            "en", "eng" -> "英语"
            "jp", "jpn", "ja" -> "日语"
            "ko", "kor" -> "韩语"
            "fr", "fra", "fre" -> "法语"
            "de", "deu", "ger" -> "德语"
            "es", "spa" -> "西班牙语"
            "it", "ita" -> "意大利语"
            "ru", "rus" -> "俄语"
            "pt", "por" -> "葡萄牙语"
            "ar", "ara" -> "阿拉伯语"
            "hi", "hin" -> "印地语"
            "tr", "tur" -> "土耳其语"
            "nl", "nld", "dut" -> "荷兰语"
            "sv", "swe" -> "瑞典语"
            "pl", "pol" -> "波兰语"
            "th", "tha" -> "泰语"
            "vi", "vie" -> "越南语"
            "id", "ind" -> "印尼语"
            "ms", "msa", "may" -> "马来语"
            "fa", "fas", "per" -> "波斯语"
            "he", "heb" -> "希伯来语"
            "el", "ell", "gre" -> "希腊语"
            "da", "dan" -> "丹麦语"
            "fi", "fin" -> "芬兰语"
            "no", "nor" -> "挪威语"
            "cs", "ces", "cze" -> "捷克语"
            "hu", "hun" -> "匈牙利语"
            "ro", "ron", "rum" -> "罗马尼亚语"
            "sk", "slk", "slo" -> "斯洛伐克语"
            "bg", "bul" -> "保加利亚语"
            "uk", "ukr" -> "乌克兰语"
            "ca", "cat" -> "加泰罗尼亚语"
            "hr", "hrv" -> "克罗地亚语"
            "sr", "srp" -> "塞尔维亚语"
            "sl", "slv" -> "斯洛文尼亚语"
            "lt", "lit" -> "立陶宛语"
            "lv", "lav" -> "拉脱维亚语"
            "et", "est" -> "爱沙尼亚语"
            "is", "isl", "ice" -> "冰岛语"
            "mt", "mlt" -> "马耳他语"
            "ga", "gle" -> "爱尔兰语"
            "gd", "gla" -> "苏格兰盖尔语"
            "cy", "cym", "wel" -> "威尔士语"
            "eu", "eus", "baq" -> "巴斯克语"
            "gl", "glg" -> "加利西亚语"
            "af", "afr" -> "南非荷兰语"
            "sw", "swa" -> "斯瓦希里语"
            "zu", "zul" -> "祖鲁语"
            "xh", "xho" -> "科萨语"
            "st", "sot" -> "南索托语"
            "tn", "tsn" -> "茨瓦纳语"
            "ss", "ssw" -> "斯威士语"
            "ve", "ven" -> "文达语"
            "ts", "tso" -> "聪加语"
            "ne", "nep" -> "尼泊尔语"
            "si", "sin" -> "僧伽罗语"
            "my", "mya", "bur" -> "缅甸语"
            "km", "khm" -> "高棉语"
            "lo", "lao" -> "老挝语"
            "mn", "mon" -> "蒙古语"
            "bo", "bod", "tib" -> "藏语"
            "ug", "uig" -> "维吾尔语"
            "sd", "snd" -> "信德语"
            "ps", "pus" -> "普什图语"
            "ku", "kur" -> "库尔德语"
            "tk", "tuk" -> "土库曼语"
            "uz", "uzb" -> "乌兹别克语"
            "kk", "kaz" -> "哈萨克语"
            "ky", "kir" -> "吉尔吉斯语"
            "tg", "tgk" -> "塔吉克语"
            "hy", "hye", "arm" -> "亚美尼亚语"
            "ka", "kat", "geo" -> "格鲁吉亚语"
            "am", "amh" -> "阿姆哈拉语"
            "ti", "tir" -> "提格里尼亚语"
            "om", "orm" -> "奥罗莫语"
            "so", "som" -> "索马里语"
            "mg", "mlg" -> "马拉加斯语"
            "yo", "yor" -> "约鲁巴语"
            "ig", "ibo" -> "伊博语"
            "ha", "hau" -> "豪萨语"
            "ff", "ful" -> "富拉语"
            "wo", "wol" -> "沃洛夫语"
            "sn", "sna" -> "绍纳语"
            "rw", "kin" -> "卢旺达语"
            "ny", "nya" -> "齐切瓦语"
            "ak", "aka" -> "阿坎语"
            "lg", "lug" -> "卢干达语"
            "mh", "mah" -> "马绍尔语"
            "sm", "smo" -> "萨摩亚语"
            "to", "ton" -> "汤加语"
            "mi", "mri", "mao" -> "毛利语"
            "fj", "fij" -> "斐济语"
            "haw" -> "夏威夷语"
            // 其他语言代码...
            else -> "${languageCode.uppercase()} (未知代码)"
        }
    }
}

