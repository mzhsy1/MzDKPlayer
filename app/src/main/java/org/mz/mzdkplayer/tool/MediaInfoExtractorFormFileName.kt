package org.mz.mzdkplayer.tool

/**
 * 从文件名解析出的影视信息。
 *
 * @param title      片名；已剔除年份、季集、画质、音轨、发布组等噪音
 * @param year       年份；识别不到时为空串
 * @param season     季号，两位字符串（如 "01"）；非剧集时为空串
 * @param episode    集号，两位字符串（如 "03"）；非剧集时为空串
 * @param mediaType  "movie" 或 "tv"
 * @param resolution 分辨率（如 "1080P"）；识别不到时为空串
 */
data class MediaInfo(
    val title: String,
    val year: String,
    val season: String = "",
    val episode: String = "",
    val mediaType: String = "movie",
    val resolution: String = ""
)

/**
 * 从文件名中提取影视信息（片名 / 年份 / 季 / 集 / 类型 / 分辨率）。
 *
 * 解析能力参考 lomenTV 的 `FileNameParser`，并按本项目的调用约定收敛了三点：
 *
 * 1. 只要识别出「集」，[MediaInfo.season] 必定有值（缺省补 "01"），
 *    调用点 `season.toInt()` / `episode.toInt()` 不会再抛异常；
 * 2. 合集文件名（如 `01-04`）取起始集号，同样保证可安全转 Int；
 * 3. 片名清洗后为空时回退为「去掉扩展名的原始文件名」，避免界面出现空标题。
 *
 * 只依赖 Kotlin 标准库，可直接由 JVM 单元测试覆盖。
 */
object MediaInfoExtractorFormFileName {

    // ────────────────────────────── 内部类型 ──────────────────────────────

    /** 带位置信息的匹配结果 */
    private data class MatchWithPos<T>(
        val value: T,
        /** 整段匹配的起始位置 */
        val start: Int,
        /** 整段匹配的结束位置（不含）；[titleAfterMatch] 时用于截取标题后缀 */
        val endExclusive: Int = -1,
        /** 仅「行首 01. 标题」这类写法：标题位于整段匹配之后 */
        val titleAfterMatch: Boolean = false
    )

    /** 剧集识别规则 */
    private data class EpisodeRule(
        val regex: Regex,
        /** 标题在匹配之后，如「01.师徒」的标题是「师徒」 */
        val titleAfterMatch: Boolean = false,
        /** 该规则只捕获季号，不捕获集号 */
        val seasonOnly: Boolean = false
    )

    // ────────────────────────────── 常量 ──────────────────────────────

    /** 常见视频扩展名 */
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "ts", "m2ts",
        "mpg", "mpeg", "rmvb", "rm", "3gp", "vob", "asf", "ogv", "f4v", "divx"
    )

    /** 【xxx发布】这类整段噪音，先整段摘掉，避免「高清影视」混进片名 */
    private val BRACKET_SEGMENT = Regex("""【[^】]*】""")

    /** 会被当作分隔符、统一替换为空格（注意保留 '-'，以免 Spider-Man 被拆开） */
    private const val SEPARATOR_CHARS = "._【】《》（）()[]{}　"

    /**
     * 需要从片名中剔除的无意义词。
     *
     * 有意**不**收录 `final` / `complete` / `cut` / `edition` / `extended` / `internal`
     * / `limited` 这类既可能是画质标记、也可能是正经片名的词——它们出现在片名里的
     * 概率远高于作为发布组标记（真正带这些标记的文件，年份切分时通常已被截掉）。
     */
    private val FILTER_WORDS = listOf(
        // 分辨率 / 画质
        "2160p", "1080p", "720p", "480p", "360p", "4k", "8k", "2k", "uhd", "fhd", "hq", "hd",
        // 中文画质词
        "高清", "超清", "标清", "蓝光",
        // 片源
        "bluray", "blu-ray", "bdrip", "brrip", "webrip", "web-dl", "webdl", "hdtv", "hdrip",
        "dvdrip", "dvd", "remux", "hdtc", "hdts", "ts",
        // 视频编码
        "x264", "x265", "h264", "h265", "hevc", "avc", "av1", "10bit", "8bit",
        // 音频编码 / 音轨
        "dts-hd", "dts", "truehd", "dd5.1", "ddp5.1", "ddp", "dd+", "ac3", "eac3", "aac",
        "atmos", "flac", "dual audio", "multi audio",
        // HDR / 杜比
        "hdr10+", "hdr10", "hdr", "dolby vision", "dovi", "sdr",
        // 发布标记
        "proper", "repack",
        // 音轨 / 字幕
        "国语中字", "国英双语", "中英字幕", "中文字幕", "英语中字", "双语字幕", "国语", "粤语",
        "国粤", "国&粤", "中字", "中英", "简中", "繁中", "双语", "内嵌", "外挂", "字幕",
        // 站点水印
        "www", "com", "net", "org", "cc", "迅雷下载", "电影天堂", "阳光电影", "字幕组"
    )

    /**
     * 预编译的噪音词正则。
     *
     * 两点讲究：
     * * 长词优先，避免「国语」先把「国语中字」吃掉一半；
     * * 首尾是字母/数字的词加 ASCII 词边界（`国` 这类 CJK 首尾不加，否则永远匹配不上），
     *   这样 `cc` 不会把 `Accident` 砍成 `Aident`，`hd` 也不会毁掉 `Rhodes`。
     */
    private val FILTER_WORD_REGEXES: List<Regex> = FILTER_WORDS
        .distinct()
        .sortedByDescending { it.length }
        .map { word ->
            val quoted = Regex.escape(word)
            val needBoundary = word.first().isLetterOrDigit() && word.last().isLetterOrDigit()
            if (needBoundary) {
                Regex("(?<![A-Za-z0-9])$quoted(?![A-Za-z0-9])", RegexOption.IGNORE_CASE)
            } else {
                Regex(quoted, RegexOption.IGNORE_CASE)
            }
        }

    /**
     * 年份（1900-2099），前后不能紧贴其它数字。
     *
     * 末尾的 `(?![x×]\d)` 用来排除 `1920x1080` / `3840x2160` 这类分辨率数值。
     */
    private val YEAR_PATTERN = Regex("""(?<![0-9])(19\d{2}|20\d{2})(?!\d)(?![x×]\d)""")

    /** 分辨率 */
    private val RESOLUTION_PATTERN = Regex(
        """(?<![A-Za-z0-9])(2160p|1080p|1080i|720p|576p|480p|360p|8k|4k|2k)(?![A-Za-z0-9])""",
        RegexOption.IGNORE_CASE
    )

    /** 数字后面紧跟画质/音轨标记的写法，如「02 4K」「05 国语」，避免被当成标题 */
    private const val QUALITY_AFTER_NUMBER =
        """(?:(?:2160p|1080p|720p|480p|360p|4k|8k|2k|hd|x264|x265|h264|h265)\b|""" +
            """国语|粤语|国粤|国&粤|中字|中英|简中|繁中|高清|超清)"""

    /**
     * 剧集识别规则，**按顺序尝试，先命中者胜**。
     * 前两条必须在 `SxxExx` 之前，否则「01. 师徒」这类行首集号会被其它规则抢走。
     */
    private val EPISODE_RULES: List<EpisodeRule> = listOf(
        // 1) 行首「01. 师徒」「12．标题」「05、标题」
        EpisodeRule(
            Regex("""^\s*(0[1-9]|[1-9]\d{0,2})\s*[.．、]\s*"""),
            titleAfterMatch = true
        ),
        // 2) 行首「01 出身：xxx」；后面紧跟画质/音轨词时不算（那是「01 4K.国粤」）
        EpisodeRule(
            Regex("""^\s*(0[1-9]|[1-9]\d{0,2})\s+(?!\s*$QUALITY_AFTER_NUMBER)(?=\S)""", RegexOption.IGNORE_CASE),
            titleAfterMatch = true
        ),
        // 3) S01E01 / S1E1
        EpisodeRule(Regex("""[.\s_-]*[Ss](\d{1,3})[Ee](\d{1,4})(?!\d)""")),
        // 4) S01.E01 / S01 E01 / S01_E01
        EpisodeRule(Regex("""[.\s_-]*[Ss](\d{1,3})[.\s_-]*[Ee](\d{1,4})(?!\d)""")),
        // 5) Season 01 Episode 01
        EpisodeRule(
            Regex("""[.\s_-]*[Ss]eason[.\s_-]*(\d{1,3})[.\s_-]*[Ee]pisode[.\s_-]*(\d{1,4})(?!\d)""", RegexOption.IGNORE_CASE)
        ),
        // 6) 第1季第2集 / 第一季第二集
        EpisodeRule(
            Regex("""[.\s_-]*第?([0-9一二三四五六七八九十百]+)季[^0-9一二三四五六七八九十]*第?([0-9一二三四五六七八九十百]+)[集话期]""")
        ),
        // 7) 1x01
        EpisodeRule(Regex("""(?<!\d)(\d{1,3})x(\d{1,4})(?!\d)""", RegexOption.IGNORE_CASE)),
        // 8) Season 02（只有季）
        EpisodeRule(
            Regex("""[.\s_-]*[Ss]eason[.\s_-]*(\d{1,3})(?!\d)""", RegexOption.IGNORE_CASE),
            seasonOnly = true
        ),
        // 9) 第2季 / 第二季（只有季）
        EpisodeRule(Regex("""[.\s_-]*第([0-9一二三四五六七八九十百]+)季"""), seasonOnly = true),
        // 10) EP01 / EP.01 / E01（前面必须是分隔符或行首，避免 Se7en 被当成 E7）
        EpisodeRule(Regex("""(?<![A-Za-z0-9])[Ee][Pp]?[.\s_-]?(\d{1,4})(?!\d)""")),
        // 11) 第01集 / 第01话 / 第01期
        EpisodeRule(Regex("""[.\s_-]*第([0-9一二三四五六七八九十百]+)[集话期]""")),
        // 12) 夹在分隔符中间、带前导零的集号：「某剧.01.国语中字」「某剧 03 1080p」
        EpisodeRule(Regex("""[.\s_-](0\d{1,2})(?!\d)""")),
        // 13) 整串就是一个编号，如「01」「007」（1900-2099 除外，那更可能是片名本身）
        //     用 \z 而不是 $ 作结尾锚点，避免和 Kotlin 字符串模板的 `$` 打架
        EpisodeRule(Regex("""^(?!\s*(?:19|20)\d{2}\s*\z)([0-9]{1,4})\z""")),
        // 14) 夹在分隔符后的裸季号「某剧.S02」「某剧.S2」
        //     放在最后，否则会把「某剧.S01.第03集」的集号抢掉
        EpisodeRule(Regex("""[.\s_-][Ss](\d{1,2})(?!\d)"""), seasonOnly = true)
    )

    /** 合集文件名：01-04 / 12~15，只取起始集号（保证可安全 toInt） */
    private val EPISODE_RANGE_PATTERN = Regex("""(?:^|[.\s_-])(\d{1,3})\s*[-~～]\s*(\d{1,3})(?!\d)""")

    /** 行首「01 4K.国&粤」这类：编号紧跟画质/音轨 */
    private val PREFIX_EPISODE_PATTERN = Regex(
        """^\s*([0-9]{1,3})(?=[.\s_-]+$QUALITY_AFTER_NUMBER)""",
        RegexOption.IGNORE_CASE
    )

    /** 中文数字 */
    private val CHINESE_DIGITS = mapOf(
        '零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10, '百' to 100
    )

    // ────────────────────────────── 主入口 ──────────────────────────────

    /**
     * 解析文件名，返回 [MediaInfo]。
     *
     * 传入纯文件名或完整路径/URL 都可以；空串不会抛异常，只是字段全为空。
     */
    fun extract(movieName: String): MediaInfo {
        val stem = removeExtension(baseNameOf(movieName))
        if (stem.isEmpty()) return MediaInfo(title = "", year = "")

        // 1. 先摘掉「【xxx发布】」整段噪音
        val name = BRACKET_SEGMENT.replace(stem, " ").trim()

        // 2. 年份 / 季集 / 分辨率
        val yearMatch = extractYearWithPos(name)
        val episodeMatch = extractSeasonEpisodeWithPos(name)
        val year = yearMatch?.value
        val season = episodeMatch?.value?.first
        val episode = episodeMatch?.value?.second

        // 3. 划出标题区间
        val titleSource = when {
            // 「01. 师徒」：标题在集号之后
            episodeMatch != null && episodeMatch.titleAfterMatch && episodeMatch.endExclusive > 0 ->
                name.substring(episodeMatch.endExclusive)

            else -> {
                // 忽略 0：行首集号（如 S01E01、01.mkv）不该把标题截成空串
                val cut = listOfNotNull(yearMatch?.start, episodeMatch?.start)
                    .filter { it > 0 }
                    .minOrNull()
                if (cut != null) name.substring(0, cut) else name
            }
        }

        // 4. 清洗标题
        var title = cleanTitle(titleSource)
        year?.let { title = title.replace(it.toString(), " ") }
        title = finalClean(removeEpisodeInfo(title))
        if (title.isEmpty()) {
            // 兜底：整串都被判成噪音时至少保留原始文件名，避免出现空标题
            title = finalClean(cleanTitle(stem))
        }

        // 5. 输出：只要识别出季或集就算剧集，并补齐缺失的一方
        val isTv = season != null || episode != null
        return MediaInfo(
            title = title,
            year = year?.toString().orEmpty(),
            season = if (isTv) formatNumber(season ?: 1) else "",
            episode = if (isTv) formatNumber(episode ?: 1) else "",
            mediaType = if (isTv) "tv" else "movie",
            resolution = extractResolution(baseNameOf(movieName))
        )
    }

    // ────────────────────────────── 提取 ──────────────────────────────

    /** 去掉目录前缀、URL 查询串，只留文件名（只有形如 URL 的输入才裁剪 `?` / `#`） */
    private fun baseNameOf(fileName: String): String {
        val trimmed = fileName.trim()
        val withoutQuery =
            if (trimmed.contains("://")) trimmed.substringBefore('?').substringBefore('#') else trimmed
        return withoutQuery.substringAfterLast('/').substringAfterLast('\\')
    }

    /** 去掉扩展名；只有「看起来像扩展名」的 ASCII 后缀才会被去掉，`.第一季` 这种会保留 */
    private fun removeExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) return fileName
        val ext = fileName.substring(dot + 1)
        val looksLikeExtension = ext.lowercase() in VIDEO_EXTENSIONS ||
            (ext.length in 1..5 && ext.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' })
        return if (looksLikeExtension) fileName.substring(0, dot) else fileName
    }

    /**
     * 取**最后一个**年份。
     *
     * 发布名里的年份一般紧挨画质标记，而片名本身可能带年份感的数字
     * （`Blade.Runner.2049.2017`、`1917.2019`），取第一个会误判。
     */
    private fun extractYearWithPos(name: String): MatchWithPos<Int>? {
        var result: MatchWithPos<Int>? = null
        for (match in YEAR_PATTERN.findAll(name)) {
            val year = match.groupValues[1].toIntOrNull() ?: continue
            result = MatchWithPos(year, match.range.first)
        }
        return result
    }

    private fun extractSeasonEpisodeWithPos(name: String): MatchWithPos<Pair<Int?, Int?>>? {
        for (rule in EPISODE_RULES) {
            val match = rule.regex.find(name) ?: continue
            val groups = match.groupValues
            val start = match.range.first
            val end = match.range.last + 1

            if (rule.seasonOnly) {
                val season = parseNumber(groups[1]) ?: continue
                return MatchWithPos(Pair(season, null), start, end, rule.titleAfterMatch)
            }
            if (groups.size >= 3) {
                val season = parseNumber(groups[1])
                val episode = parseNumber(groups[2])
                if (season == null && episode == null) continue
                return MatchWithPos(Pair(season, episode), start, end, rule.titleAfterMatch)
            }
            val episode = parseNumber(groups.getOrNull(1).orEmpty()) ?: continue
            return MatchWithPos(Pair(null, episode), start, end, rule.titleAfterMatch)
        }

        // 合集「01-04」：取起始集号
        EPISODE_RANGE_PATTERN.find(name)?.let { match ->
            parseNumber(match.groupValues[1])?.let {
                return MatchWithPos(Pair(null, it), match.range.first, match.range.last + 1)
            }
        }

        // 行首「01 4K.国&粤」
        PREFIX_EPISODE_PATTERN.find(name)?.let { match ->
            parseNumber(match.groupValues[1])?.let {
                return MatchWithPos(Pair(null, it), match.range.first, match.range.last + 1)
            }
        }
        return null
    }

    private fun extractResolution(fileName: String): String =
        RESOLUTION_PATTERN.find(fileName)?.groupValues?.get(1)?.uppercase().orEmpty()

    // ────────────────────────────── 清洗 ──────────────────────────────

    /** 剔除噪音词 + 统一分隔符 */
    private fun cleanTitle(raw: String): String {
        if (raw.isEmpty()) return raw
        var cleaned = raw
        // 先查噪音词：此时原始分隔符还在，否则 web-dl / dd5.1 这类规则永远匹配不上
        for (regex in FILTER_WORD_REGEXES) {
            cleaned = regex.replace(cleaned, " ")
        }
        // 再统一分隔符
        val sb = StringBuilder(cleaned.length)
        for (ch in cleaned) {
            sb.append(if (SEPARATOR_CHARS.indexOf(ch) >= 0) ' ' else ch)
        }
        return sb.toString()
    }

    /** 把残留的季集信息从标题里去掉 */
    private fun removeEpisodeInfo(title: String): String {
        var cleaned = title
        for (rule in EPISODE_RULES) {
            cleaned = rule.regex.replace(cleaned, " ")
        }
        cleaned = EPISODE_RANGE_PATTERN.replace(cleaned, " ")
        cleaned = PREFIX_EPISODE_PATTERN.replace(cleaned, " ")
        return cleaned
    }

    /** 收尾：合并空白、去掉首尾空格与悬空标点 */
    private fun finalClean(raw: String): String = raw
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim('.', '-', '_', '·', '|', '/', '\\', ',', '，', '、', ':', '：', ';', '；', '&')
        .trim()

    // ────────────────────────────── 数字 ──────────────────────────────

    /** 两位字符串，保证调用方 `toInt()` 一定成功 */
    private fun formatNumber(value: Int): String = value.toString().padStart(2, '0')

    /** 解析阿拉伯数字或中文数字 */
    private fun parseNumber(str: String): Int? {
        str.toIntOrNull()?.let { return it }
        return parseChineseNumber(str)
    }

    private fun parseChineseNumber(str: String): Int? {
        if (str.isEmpty()) return null
        var total = 0
        var current = 0
        for (ch in str) {
            val value = CHINESE_DIGITS[ch] ?: return null // 出现非中文数字字符 → 放弃
            when {
                value == 100 -> {
                    if (current == 0) current = 1
                    total += current * 100
                    current = 0
                }

                value == 10 -> {
                    if (current == 0) current = 1
                    total += current * 10
                    current = 0
                }

                else -> current = value
            }
        }
        total += current
        return total.takeIf { it > 0 }
    }
}
