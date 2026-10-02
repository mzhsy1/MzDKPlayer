package org.mz.mzdkplayer.tool.logic

/**
 * 手机端视频播放页的纯逻辑（第七阶段）。
 *
 * 与 [PhoneMediaLogic] 的分工：那个管「点进去开哪个页面」，本对象管
 * 「播放页自己要算什么」——用哪个内核、连播到哪一条、进度条换算、
 * 以及「上次看到一半」的判定阈值。
 *
 * 抽出来的目的同样是能在 JVM 单测里锁住边界（尤其是「最后一条不再往下跳」与
 * 「时长未知时不给拖动」这两条，写错在界面上表现为玄学）。
 *
 * 约定：这里**只能**用 JDK API，不要引入 `android.*` / `androidx.*`。
 */
object PhonePlayerLogic {

    /**
     * 播放倍速档位，与电视端面板一致。
     *
     * 放在这里而不是各端 UI 里：档位是「产品口径」，不是布局细节，
     * 两端必须一样（`IMzPlayer.setPlaybackSpeed` 支持任意值，但入口只给这几档）。
     */
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    /** 双击屏幕左右两侧的快进 / 快退步长（秒） */
    const val GESTURE_SEEK_SECONDS = 15

    /**
     * 这些容器格式 ExoPlayer 处理不了（或处理得很差），一律交给 VLC 内核：
     * `.ts / .m2ts / .mts / .m2t` 是传输流，`.iso` 是蓝光原盘（Exo 连标题流都拿不到）。
     *
     * 与电视端 `MzDKPlayerAPP` 里「按扩展名强制 VLC」是同一份口径。
     */
    private val VLC_ONLY_EXTENSIONS = setOf("m2ts", "iso", "m2t", "mts", "ts")

    /**
     * [fileName]（给完整地址也行）是否需要用 VLC 内核。
     *
     * 先按 `?` 截断再取扩展名：HTTP 直链常带查询串，直接取 `substringAfterLast('.')`
     * 会把 `movie.mkv?token=a.b` 判成扩展名 `b`。
     */
    fun forceVlcByExtension(fileName: String): Boolean {
        val path = fileName.substringBefore('?').substringBefore('#')
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in VLC_ONLY_EXTENSIONS
    }

    /**
     * 「播放完成后播下一个」要跳的下标；没有下一个（已是最后一条、或当前不在列表里）返回 null。
     *
     * [currentIndex] 为 -1 表示当前文件不在播放列表里（例如从首页历史记录直接播放），
     * 这时不去猜「第一个是下一个」——猜错会跳到毫不相干的影片上。
     */
    fun nextPlaylistIndex(playlistSize: Int, currentIndex: Int): Int? {
        if (currentIndex < 0 || currentIndex >= playlistSize) return null
        val next = currentIndex + 1
        return next.takeIf { it < playlistSize }
    }

    /**
     * 快进 / 快退的目标位置，收敛在 `[0, durationMs]` 内。
     *
     * 时长未知（没有加载出来，或直播流）时返回 null —— 此时播放器自己也不知道能拖到哪，
     * 界面上应当直接禁用这次跳转，而不是 `seekTo` 一个越界值。
     */
    fun seekTargetMs(currentMs: Long, deltaMs: Long, durationMs: Long): Long? {
        if (durationMs <= 0L) return null
        return (currentMs + deltaMs).coerceIn(0L, durationMs)
    }

    /** 进度条比例（0..1）；时长未知时给 0，避免出现 NaN 让 Slider 直接崩 */
    fun progressOf(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    /**
     * 是否值得提示「继续播放」：只有真正看过一段（超过 [minMs]）才算。
     *
     * 阈值存在的意义是躲开片头：只看了 3 秒就退出，下次进来直接从头开始才是对的，
     * 否则每次都会被问一遍「要不要继续」。
     */
    fun shouldOfferResume(historyPositionMs: Long, minMs: Long = 5_000L): Boolean =
        historyPositionMs > minMs

    /**
     * 播放列表里当前文件的下标；不在列表里返回 -1（调用方据此禁用「下一集」）。
     */
    fun playlistIndexOf(uris: List<String>, currentUri: String): Int =
        uris.indexOfFirst { it == currentUri }
}
