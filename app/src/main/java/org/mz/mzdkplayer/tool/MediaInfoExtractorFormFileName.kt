data class MediaInfo(
    val title: String,
    val year: String,
    val season: String = "",
    val episode: String = "",
    val mediaType: String = "movie" // "movie" 或 "tv"
)

object MediaInfoExtractorFormFileName {

    fun extract(movieName: String): MediaInfo {
        // 1. 移除扩展名（支持常见视频格式）
        var cleanName = movieName.replace(Regex("\\.\\w+$"), "").trim()
        cleanName = cleanName.replace(Regex("_"), " ")

        // 初始化 season 和 episode
        var season = ""
        var episode = ""

        // 2. 处理电视剧模式：优先匹配 SxxEyy 或 SxxSyy 格式（如 S01E03、S1E2）
        val tvPattern = Regex("S(\\d{1,2})[ES](\\d{1,2})", RegexOption.IGNORE_CASE)
        val tvMatch = tvPattern.find(cleanName)
        var isTv = false

        if (tvMatch != null) {
            season = tvMatch.groupValues[1].padStart(2, '0')
            episode = tvMatch.groupValues[2].padStart(2, '0')
            cleanName = cleanName.replace(tvMatch.value, "").trim()
            isTv = true
        } else {
            // 3. 处理合集模式：匹配 01-04 这种格式（通常用于剧集范围）
            val episodeRangePattern = Regex("(\\d{1,2})-(\\d{1,2})$")
            val rangeMatch = episodeRangePattern.find(cleanName)
            if (rangeMatch != null) {
                episode = "${rangeMatch.groupValues[1]}-${rangeMatch.groupValues[2]}"
                cleanName = cleanName.replace(rangeMatch.value, "").trim()
                isTv = true
            }
        }

        // 4. 从标题部分提取（处理 " - " 分隔符，避免残留）
        val titlePart = cleanName.split(" - ").first().trim()

        // 5. 优先处理括号年份（如 "（2018）"）
        val yearFromParentheses = Regex("（(\\d{4})）").find(titlePart)?.groupValues?.get(1)
        if (yearFromParentheses != null) {
            val title = titlePart.replace(Regex("（\\d{4}）"), "").trim()
            return MediaInfo(
                title = title.replace(".", " "),
                year = yearFromParentheses,
                season = season,
                episode = episode,
                mediaType = if (isTv) "tv" else "movie"
            )
        }

        // 6. 按原逻辑扫描年份（从后往前找四位数字）
        val parts = titlePart.split('.')
        var year: String? = null
        for (i in parts.indices.reversed()) {
            if (parts[i].matches(Regex("\\d{4}"))) {
                year = parts[i]
                break
            }
        }

        // 7. 提取标题
        val title = if (year != null) {
            parts.slice(0 until parts.indexOf(year)).joinToString(" ").replace(".", " ")
        } else {
            titlePart.substringBefore('.').replace(".", " ")
        }

        return MediaInfo(
            title = title.trim(),
            year = year ?: "",
            season = season,
            episode = episode,
            mediaType = if (isTv) "tv" else "movie"
        )
    }
}
