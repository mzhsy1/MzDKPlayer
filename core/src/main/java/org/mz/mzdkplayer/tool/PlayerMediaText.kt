package org.mz.mzdkplayer.tool

import org.mz.mzdkplayer.data.local.MediaCacheEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 播放界面标题 / 日期的文本组装工具。
 *
 * 标题规则：优先使用刮削后的标题；电视剧额外带上第几季第几集；再带上年份。
 * 没有刮削信息时，原样显示文件名。
 */
object PlayerMediaText {

    private const val TV_SEASON_EPISODE_FORMAT = "S%02dE%02d"
    private const val DATE_FORMAT = "yyyy/MM/dd"

    /**
     * 构建播放界面展示的标题。
     *
     * @param meta 该文件在 media_cache 里的刮削记录，可能为 null
     * @param fileName 原始文件名，作为兜底
     */
    fun buildTitle(meta: MediaCacheEntity?, fileName: String): String {
        if (meta == null || meta.title.isBlank()) return fileName

        val title = StringBuilder(meta.title.trim())
        if (isTv(meta)) {
            val season = meta.seasonNumber
            val episode = meta.episodeNumber
            // 只有季集信息有效时才拼接，避免出现 S00E00
            if (season > 0 || episode > 0) {
                title.append(' ').append(String.format(Locale.US, TV_SEASON_EPISODE_FORMAT, season, episode))
            }
        }
        yearOf(meta)?.let { title.append(" (").append(it).append(')') }
        return title.toString()
    }

    /**
     * 把文件时间（毫秒时间戳）格式化成播放界面展示的日期。
     * 时间无效时返回空字符串，界面上会自动省略这一段。
     */
    fun buildFileDateText(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return runCatching {
            SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date(epochMillis))
        }.getOrDefault("")
    }

    private fun isTv(meta: MediaCacheEntity): Boolean =
        meta.mediaType.equals("tv", ignoreCase = true)

    /** 电影取上映年份；电视剧优先取剧集首播年份，其次取单集播出年份 */
    private fun yearOf(meta: MediaCacheEntity): String? {
        val candidates = if (isTv(meta)) {
            listOf(meta.releaseDate, meta.episodeAirDate)
        } else {
            listOf(meta.releaseDate)
        }
        return candidates.firstNotNullOfOrNull { it?.trim()?.takeYear() }
    }

    private fun String.takeYear(): String? {
        if (length < 4) return null
        val year = take(4)
        return year.takeIf { it.all(Char::isDigit) }
    }
}
