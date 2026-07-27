package org.mz.mzdkplayer.tool


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.Base64
import android.util.Log
import org.mz.mzdkplayer.ui.screen.common.MzToastManager
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource

import androidx.media3.common.Format

import androidx.media3.common.util.UnstableApi


import org.mz.mzdkplayer.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import kotlin.math.log10
import kotlin.math.pow
import androidx.core.graphics.set
import android.net.Uri
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.net.toUri

object Tools {
    fun extractFileExtension(fileName: String?): String {
        if (fileName.isNullOrEmpty()) {
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

    @OptIn(UnstableApi::class)
    @Composable
    fun VideoBigIcon(focusedIsDir: Boolean, fileName: String?, modifier: Modifier) {

//        Icon(
//            modifier = modifier,
//            painter = painterResource(R.drawable.pcm_seeklogo__1_),
//            tint = Color.White,
//            contentDescription = null,
//        )
        Log.d("fileName", fileName.toString())
        Log.d("eFileName", extractFileExtension(fileName))
        if (focusedIsDir) {
            Image(
                modifier = modifier,
                painter = painterResource(R.drawable.foldernew),
                // tint = Color.White,
                contentDescription = null,
            )
        } else {
            when (extractFileExtension(fileName)) {
                "mkv" -> Image(
                    modifier = modifier,
                    painter = painterResource(R.drawable.mkvnew),
                    // tint = Color.White,
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

    fun containsVideoFormat(input: String): Boolean {
        val videoFormats = listOf("MP4", "MKV","M2TS", "3GP", "AVI", "MOV", "TS", "FLV","ISO")
        return videoFormats.any { format ->
            input.contains(format, ignoreCase = true)
        }
    }

    fun containsAudioFormat(input: String): Boolean {
        val audioFormats = listOf("MP3", "FLAC","WAV","AAC")
        return audioFormats.any { format ->
            input.contains(format, ignoreCase = true)
        }
    }

    fun containsImageFileExtension(input: String): Boolean {
        // Exoplayer 支持的图片格式的常见扩展名列表
        val supportedExtensions = listOf(
            "bmp",  // BMP
            "jpg",  // JPEG
            "jpeg", // JPEG
            "png",  // PNG
            "webp", // WebP
            "heif", // HEIF/HEIC
            "heic", // HEIF/HEIC
            "avif"  // AVIF (基准，需 Android 14+)
        )

        // 1. 将输入转为小写，并去掉可能的首尾空格
        val lowerInput = input.trim().lowercase()

        // 2. 检查这个字符串是否是支持的扩展名之一
        return supportedExtensions.contains(lowerInput)
    }
    /**
     * 根据音频轨道的 Format 信息推断具体的音频格式类型
     * @return 推断出的音频格式描述字符串
     */
    fun inferAudioFormatType(mimeType: String): String {
        //val mimeType = format.sampleMimeType ?: return "Unknown Audio Format (No MIME type)"

        return when (mimeType) {
            "audio/vnd.dts" -> "DTS" // DTS 家族格式判断
            "audio/vnd.dts.hd" -> "DTS HD" // DTS 家族格式判断
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
//    private fun inferDtsFormatType(format: Format): String {
//        val codecs = format.codecs?.lowercase() ?: ""
//
//        return when {
//            codecs.contains("dts-hd-ma") -> "DTS-HD Master Audio"
//            codecs.contains("dts-hd-hra") -> "DTS-HD High Resolution"
//            codecs.contains("dts-x") -> "DTS:X"
//            codecs.contains("dts-express") -> "DTS Express"
//            codecs.contains("dts") -> "DTS Core"
//
//            // 如果没有明确的 codecs 信息，则基于声道数等进行推测
//            format.channelCount >= 8 -> "DTS-HD "
//            format.channelCount == 6 -> "DTS Core "
//            else -> "DTS (未知DTS编码)"
//        }
//    }


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





    /**
     * 将语言代码转换为中文语言名称（特别细化中文区分）
     */
    /**
     * 将语言代码转换为中文语言名称（特别细化中文区分，包括字幕语言）
     */
    fun getFullLanguageName(languageCode: String?): String {
        if (languageCode.isNullOrEmpty() || languageCode == "und") {
            return "未知语言"
        }

        // 特别处理中文的细分（包括字幕语言代码）
        return when (// 简体中文（视频轨道和字幕轨道）
            val lowerCode = languageCode.lowercase()) {
            "zh-hans", "zh-cn", "zh-sg", "chs", "sc", "chi_sim" -> "简体中文"

            // 繁体中文（视频轨道和字幕轨道）
            "zh-hant", "zh-tw", "zh-hk", "zh-mo", "cht", "tc", "chi_tra" -> "繁体中文"

            // 一般中文代码（无法区分简繁体时）
            "zh", "zho", "chi" -> "中文"

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
            else -> languageCode.uppercase()
        }
    }

    /**
     * 验证连接参数
     * @param context 上下文用于显示Toast
     * @param serverAddress 服务器地址
     * @param shareName 分享文件名称
     * @return 如果验证通过返回true，否则返回false
     */
    fun validateConnectionParams(serverAddress: String, shareName: String,aliasName: String): Boolean {
        if (serverAddress.isBlank()) {
            MzToastManager.show("请输入服务器地址")
            return false
        }
        if (shareName.isBlank()) {
            MzToastManager.show("请输入分享文件名称")
            return false
        }
        if (!shareName.startsWith("/")) {
            MzToastManager.show("分享连接必须以/开头")
            return false
        }
        val pattern = Regex("^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]+\$")
        if (!pattern.matches(aliasName)) {
            MzToastManager.show("别名只能包含字母、数字、下划线、中划线和中文，不能包含特殊符号")
            return false
        }
        return true
    }

    /**
     * 验证连接参数
     * @param context 上下文用于显示Toast
     * @param serverAddress 服务器地址
     * @param shareName 分享文件名称
     * @return 如果验证通过返回true，否则返回false
     */
    fun validateSMBConnectionParams(serverAddress: String, shareName: String,aliasName:String): Boolean {
        if (serverAddress.isBlank()) {
            MzToastManager.show("请输入服务器地址")
            return false
        }
        if (shareName.isBlank()) {
            MzToastManager.show("请输入分享文件名称")
            return false
        }
        if (shareName.startsWith("/")) {
            MzToastManager.show("SMB分享名称不能以/开头")
            return false
        }
        // 验证 aliasName 不能为空
        if (aliasName.isBlank()) {
            MzToastManager.show("请输入别名")
            return false
        }

        // 使用正则表达式验证 aliasName 只允许字母、数字、下划线、中划线和中文
        val pattern = Regex("^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]+\$")
        if (!pattern.matches(aliasName)) {
            MzToastManager.show("别名只能包含字母、数字、下划线、中划线和中文，不能包含特殊符号")
            return false
        }
        return true
    }

    /**
     * 验证连接参数
     * @param context 上下文用于显示Toast
     * @param serverAddress 服务器地址
     * @return 如果验证通过返回true，否则返回false
     */
    fun validateWebConnectionParams(serverAddress: String): Boolean {
        if (serverAddress.isBlank()) {
            MzToastManager.show("请输入服务器地址")
            return false
        }

        return true
    }

    /**
     * 从 URI 字符串中提取文件名（最后一个 `/` 后的部分）
     */
    fun extractFileNameFromUri(uriString: String): String {
        return try {
            val lastSlashIndex = uriString.lastIndexOf('/')
            if (lastSlashIndex != -1 && lastSlashIndex < uriString.length - 1) {
                uriString.substring(lastSlashIndex + 1)
            } else {
                uriString // 如果没有找到 `/`，返回整个字符串
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "未知文件名" // 异常时的兜底名称
        }
    }
    fun String?.toSafeFloat(default: Float = 0f): Float {
        return if (this.isNullOrBlank()) default else try {
            this.toFloat()
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun String?.toSafeInt(default: Int = 0): Int {
        return if (this.isNullOrBlank()) default else try {
            this.toInt()
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun String?.toSafeLong(default: Long = 0L): Long {
        return if (this.isNullOrBlank()) default else try {
            this.toLong()
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun getCountryName(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "US" -> "美国"
            "CN" -> "中国"
            "JP" -> "日本"
            "KR" -> "韩国"
            "GB" -> "英国"
            "FR" -> "法国"
            "DE" -> "德国"
            "IT" -> "意大利"
            "ES" -> "西班牙"
            "RU" -> "俄罗斯"
            "CA" -> "加拿大"
            "AU" -> "澳大利亚"
            "BR" -> "巴西"
            "IN" -> "印度"
            "MX" -> "墨西哥"
            "NL" -> "荷兰"
            "SE" -> "瑞典"
            "CH" -> "瑞士"
            "BE" -> "比利时"
            "AT" -> "奥地利"
            "PL" -> "波兰"
            "TR" -> "土耳其"
            "ZA" -> "南非"
            "SG" -> "新加坡"
            "NZ" -> "新西兰"
            "TH" -> "泰国"
            "MY" -> "马来西亚"
            "PH" -> "菲律宾"
            "ID" -> "印度尼西亚"
            "VN" -> "越南"
            "HK" -> "中国香港"
            "TW" -> "中国台湾"
            "MO" -> "中国澳门"
            else -> countryCode // 或者返回 "未知"、"" 等，根据需求
        }
    }


    fun saveCoverImageToInternalStorage(context: Context, uri: String, artworkData: ByteArray?): String? {
        if (artworkData == null || artworkData.isEmpty()) return null

        try {
            val fileName = "cover_${uri.hashCode()}.webp"
            val directory = File(context.filesDir, "audio_covers")
            if (!directory.exists()) directory.mkdirs()

            val file = File(directory, fileName)
            if (file.exists()) return file.absolutePath

            // 1. 预读尺寸
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size, options)

            // 2. 初步采样缩放（减少内存占用）
            options.inSampleSize = calculateInSampleSize(options, 650, 650)
            options.inJustDecodeBounds = false
            val rawBitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size, options) ?: return null

            // 3. 执行居中裁剪并缩放到 650x650
            val finalBitmap = centerCropAndScale(rawBitmap, 650, 650)

            // 4. 保存为 WebP
            FileOutputStream(file).use { fos ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, fos)
                } else {
                    @Suppress("DEPRECATION")
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, fos)
                }
            }

            // 5. 释放内存
            if (rawBitmap != finalBitmap) rawBitmap.recycle()
            finalBitmap.recycle()

            return file.absolutePath
        } catch (e: Exception) {
            Log.e("AudioPlayerScreen", "保存裁剪后的封面失败", e)
            return null
        }
    }

    /**
     * 居中裁剪并缩放逻辑
     */
    private fun centerCropAndScale(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        // 计算缩放比例（取大的那一边，保证铺满目标区域）
        val scale =
            (targetWidth.toFloat() / sourceWidth).coerceAtLeast(targetHeight.toFloat() / sourceHeight)

        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight

        // 计算裁剪区域（在原图基础上计算居中的矩形）
        val left = (targetWidth - scaledWidth) / 2
        val top = (targetHeight - scaledHeight) / 2

        val destRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)

        // 创建目标画布
        val output = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, null, destRect, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))

        return output
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 将字节大小转换为人类可读的格式 (B, KB, MB, GB)
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return String.format(Locale.getDefault(),"%.2f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    /**
     * 尝试在指定端口范围内启动服务器
     * @param startPort 起始端口，例如 8888
     * @param maxTries 最大尝试次数，例如 10 次
     * @param onReceive 回调函数
     * @return 返回一个 Pair(启动成功的Server对象, 实际端口号)，如果失败返回 null
     */
    fun startServerOnAvailablePort(
        startPort: Int,
        maxTries: Int = 20,
        onReceive: (RemoteConfig) -> Unit
    ): Pair<RemoteInputServer, Int>? {
        var currentPort = startPort

        for (i in 0 until maxTries) {
            try {
                // 尝试创建并启动
                val server = RemoteInputServer(currentPort, onReceive)
                server.start()
                // 如果代码走到这里没有报错，说明启动成功了
                return Pair(server, currentPort)
            } catch (e: Exception) {
                // 启动失败（通常是 BindException 端口被占），打印日志并尝试下一个端口
                e.printStackTrace()
                currentPort++
            }
        }
        return null // 所有尝试都失败了
    }

    fun getLocalIpAddress(): String? {
        try {
            // 获取设备上所有的网络接口（网卡）
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(interfaces)) {
                // 排除掉回环地址（Loopback）和未启动的网卡
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                for (inetAddress in Collections.list(addresses)) {
                    // 只找 IPv4 地址，排除 IPv6（因为目前大多数局域网环境还是 IPv4 比较稳）
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val ip = inetAddress.hostAddress
                        // 过滤掉虚拟网卡或非内网网段（可选，通常直接返回第一个有效的 IPv4 即可）
                        if (ip != null && !ip.contains(":")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // 生成二维码 Bitmap
    fun generateQRCode(content: String, size: Int = 512): ImageBitmap? {
        return try {
            val bits = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bits.width
            val height = bits.height
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] =
                        if (bits[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 为播放器编码 URL。
     * 核心逻辑：
     * 1. 自动处理协议、主机、路径和查询参数。
     * 2. 对路径段进行解码再重新编码，防止“二次编码”或“部分未编码”的问题。
     * 3. 确保空格被转义为 %20 而不是 +。
     */
    fun encodeUrlForPlayer(url: String): String {
        if (url.isBlank()) return url
        
        return try {
            val uri = url.toUri()
            val scheme = uri.scheme
            val host = uri.host
            val port = uri.port
            val userInfo = uri.userInfo
            
            // 如果没有协议，可能是本地文件路径，使用原有逻辑
            if (scheme == null) {
                return url.split("/").joinToString("/") { segment ->
                    val decoded = URLDecoder.decode(segment, "UTF-8")
                    URLEncoder.encode(decoded, "UTF-8").replace("+", "%20")
                }
            }

            val builder = uri.buildUpon()
            
            // 重新构建路径，确保每个段都被正确编码
            val pathSegments = uri.pathSegments
            if (pathSegments.isNotEmpty()) {
                builder.path(null) // 清除原有路径
                pathSegments.forEach { segment ->
                    // 先解码（防止输入已经是编码过的），再编码
                    val decoded = URLDecoder.decode(segment, "UTF-8")
                    builder.appendPath(decoded)
                }
            }
            
            // 某些特殊字符在 buildUpon().build().toString() 中可能不会被编码（取决于 Android 版本）
            // 这里我们强制检查一些 VLC 敏感的字符
            val result = builder.build().toString()
            
            // 再次校验：如果仍然包含非 ASCII 字符或空格，则进行最后兜底
            // 理论上 builder.build().toString() 在现代 Android 上已经处理得很好
            result
        } catch (e: Exception) {
            Log.e("Tools", "URL 编码失败: $url", e)
            url
        }
    }

    /**
     * 将毫秒转为可读的时间格式 (例如: 01:25:30 或 05:12)
     * @param ms 毫秒数
     */
    fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"

        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            // 超过1小时，显示 HH:mm:ss
            String.format(Locale.getDefault(),"%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            // 不足1小时，显示 mm:ss
            String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
        }
    }
    /**
     * 更加人性化的时间显示
     * 如果是短视频片段显示 00:30
     * 如果是长片显示 2h 15m
     */
    fun formatFriendlyTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> formatTime(ms) // 调用上面的基础版显示 mm:ss
        }
    }
    /**
     * 将 Assets 里的字体拷贝到私有目录，并返回绝对路径
     */
    fun prepareFont(context: Context, fontName: String): String {
        val fontFile = File(context.filesDir, fontName)

        // 如果文件不存在才拷贝，避免每次启动都耗时
        if (!fontFile.exists()) {
            context.assets.open("fonts/$fontName").use { input ->
                fontFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return fontFile.absolutePath
    }

    // 编码：转为 URL 安全且不带换行符的 Base64 字符串
    fun String.toBase64(): String {
        return Base64.encodeToString(this.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    // 解码：从 Base64 还原为原始字符串
    fun String.fromBase64(): String {
        return try {
            String(Base64.decode(this, Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 处理海报/背景图 URL
     * 如果是网络全路径 (http/https) 则直接返回
     * 如果是相对路径 (以 / 开头) 则补全 TMDB 前缀
     * @param path 原始路径
     * @param size TMDB 尺寸 (例如: "w200", "w500", "w1280", "original")
     */
    fun formatImageUrl(path: String?, size: String): String? {
        if (path.isNullOrEmpty()) return null
        return if (path.startsWith("http")) {
            path
        } else {
            "https://image.tmdb.org/t/p/$size$path"
        }
    }
}



