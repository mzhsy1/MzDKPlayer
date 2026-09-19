package org.mz.mzdkplayer.tool

/**
 * 同名字幕匹配的纯字符串逻辑，从 [SubtitleScanner] 中抽出，不依赖 Android / 网络。
 *
 * 抽出的目的有两个：
 * 1. 让「哪些文件算同名字幕」这条规则可以被 JVM 单元测试直接覆盖（见 `SubtitleMatchTest`）；
 * 2. 五个协议（本地 / SMB / NFS / FTP / WebDAV / HTTP）的扫描流程共用同一份匹配与拼装口径，
 *    避免各分支各写一遍 filter + map 而出现细微差异。
 */
internal object SubtitleMatchLogic {

    /**
     * 常见字幕文件扩展名（统一小写）。
     * 只放播放器真正能加载的类型，其余（lrc / idx / ttml 等）不参与同名匹配。
     */
    val SUBTITLE_EXTENSIONS = setOf(
        "srt", "ass", "ssa", "vtt", "sub",
        "sup", "pgs",
    )

    /**
     * 判断文件名是否为「视频名」的同名字幕。
     *
     * 规则：以「视频名.」开头（覆盖 `.srt` / `.chs.srt` / `.国语.ass` 等变体），
     * 且扩展名属于字幕集合，且不等于视频自身。
     *
     * @param candidateName 目录里的候选文件名（如 `影片.chs.srt`）
     * @param videoName 视频文件名（如 `影片.mkv`）
     */
    fun isSameNameSubtitle(candidateName: String, videoName: String): Boolean {
        val baseName = videoName.substringBeforeLast('.', videoName)
        if (baseName.isBlank()) return false
        if (candidateName == videoName) return false
        if (!candidateName.startsWith("$baseName.")) return false
        val ext = candidateName.substringAfterLast('.', "").lowercase()
        return ext in SUBTITLE_EXTENSIONS
    }

    /**
     * 从目录文件名列表里筛出同名字幕，去重并按文件名排序。
     *
     * @return 命中的字幕文件名列表（不含目录前缀）
     */
    fun matchSameNameSubtitleNames(videoName: String, dirNames: List<String>): List<String> =
        dirNames
            .filter { isSameNameSubtitle(it, videoName) }
            .distinct()
            .sorted()

    /**
     * 筛出同名字幕并拼成 (字幕URI, 字幕文件名) 列表，供播放器直接加载。
     *
     * 字幕 URI 复用视频 URI 的目录前缀，因此账号密码、端口等信息天然保持一致。
     */
    fun buildSubtitlePairs(
        videoUri: String,
        videoName: String,
        dirNames: List<String>
    ): List<Pair<String, String>> {
        val prefix = dirPrefixOf(videoUri)
        return matchSameNameSubtitleNames(videoName, dirNames).map { prefix + it to it }
    }

    /**
     * 提取视频 URI 的目录前缀（保留协议 / 凭证 / host / 目录，以 `/` 结尾）。
     * URI 里没有 `/` 时返回空串。
     */
    fun dirPrefixOf(videoUri: String): String {
        val idx = videoUri.lastIndexOf('/')
        return if (idx >= 0) videoUri.substring(0, idx + 1) else ""
    }
}
