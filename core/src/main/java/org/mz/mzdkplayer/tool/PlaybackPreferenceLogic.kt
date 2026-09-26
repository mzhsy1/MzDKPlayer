package org.mz.mzdkplayer.tool

/**
 * 轨道引用：轨道 id 是首选的身份标识，下标只作为兜底。
 *
 * 为什么两个都要存：Exo 的 id 来自容器（`Format.id`），VLC 的 id 是轨道号，
 * 绝大多数情况下同一文件重复播放都稳定；但个别容器不给 id，实现里会退化成
 * 「组下标」这种跟顺序有关的标识，所以下标记下来可以做兜底。
 */
data class PlaybackTrackRef(
    val id: String,
    val index: Int
)

/**
 * 单个文件的播放偏好。四个字段都可以为空 —— 为空表示「这一项没有记忆」。
 */
data class PlaybackPreference(
    val audio: PlaybackTrackRef? = null,
    val subtitle: PlaybackTrackRef? = null,
    val playbackSpeed: Float? = null,
    val aspectRatio: String? = null
) {
    /** 四项全空等价于「这个文件没有需要记忆的东西」 */
    val isEmpty: Boolean
        get() = audio == null && subtitle == null && playbackSpeed == null && aspectRatio == null
}

/**
 * 「按文件记住播放偏好」的纯逻辑：编解码、存储键、轨道匹配、索引淘汰。
 *
 * 只依赖 JDK，可直接跑 JVM 单测；落盘由 [org.mz.mzdkplayer.data.repository.PlaybackPreferenceRepository] 负责。
 * 值格式为 `a:0:2/3|s:1:-1|p:1.5|r:ZOOM`，每个字段都是 `名字:下标:id`，
 * 名字用单字符以省空间（a=音轨、s=字幕、p=倍速、r=画面比例）。
 */
internal object PlaybackPreferenceLogic {

    /** 最多记住多少个文件，超出后按「最近使用」淘汰最旧的 */
    const val MAX_ENTRIES = 200

    /** SharedPreferences 里存「最近使用顺序」的键 */
    const val INDEX_KEY = "playback_pref_index"

    /** 每个文件一条记录的键前缀，后面直接拼视频 URI */
    private const val ENTRY_PREFIX = "playback_pref_"

    private const val FIELD_SEPARATOR = "|"
    private const val NAME_VALUE_SEPARATOR = ":"

    private const val FIELD_AUDIO = "a"
    private const val FIELD_SUBTITLE = "s"
    private const val FIELD_SPEED = "p"
    private const val FIELD_RATIO = "r"

    /** 轨道 id 里如果出现分隔符会破坏格式，做一次最小编码 */
    private const val ESCAPED_PERCENT = "%25"
    private const val ESCAPED_PIPE = "%7C"

    fun storageKey(videoUri: String): String = ENTRY_PREFIX + videoUri

    fun encode(pref: PlaybackPreference): String {
        val parts = mutableListOf<String>()
        pref.audio?.let { parts += encodeTrack(FIELD_AUDIO, it) }
        pref.subtitle?.let { parts += encodeTrack(FIELD_SUBTITLE, it) }
        pref.playbackSpeed?.let { parts += FIELD_SPEED + NAME_VALUE_SEPARATOR + it }
        pref.aspectRatio?.takeIf { it.isNotEmpty() }
            ?.let { parts += FIELD_RATIO + NAME_VALUE_SEPARATOR + it }
        return parts.joinToString(FIELD_SEPARATOR)
    }

    /** 解析失败或解析后四项全空都返回 null，调用方据此走默认行为 */
    fun decode(raw: String?): PlaybackPreference? {
        if (raw.isNullOrEmpty()) return null

        var audio: PlaybackTrackRef? = null
        var subtitle: PlaybackTrackRef? = null
        var speed: Float? = null
        var ratio: String? = null

        raw.split(FIELD_SEPARATOR).forEach { field ->
            val name = field.substringBefore(NAME_VALUE_SEPARATOR)
            val rest = field.substringAfter(NAME_VALUE_SEPARATOR, "")
            when (name) {
                FIELD_AUDIO -> audio = decodeTrack(rest)
                FIELD_SUBTITLE -> subtitle = decodeTrack(rest)
                // 非正数的倍速没有意义，直接当没存过
                FIELD_SPEED -> speed = rest.toFloatOrNull()?.takeIf { it > 0f }
                FIELD_RATIO -> ratio = rest.takeIf { it.isNotEmpty() }
            }
        }

        return PlaybackPreference(audio, subtitle, speed, ratio).takeIf { !it.isEmpty }
    }

    /**
     * 在轨道列表里找保存的那一条：**先按 id 精确匹配，匹配不到再按下标兜底**。
     * 返回列表下标，找不到返回 -1（调用方应保持原有选择，不要乱切）。
     */
    fun selectIndex(tracks: List<PlaybackTrackRef>, saved: PlaybackTrackRef?): Int {
        if (saved == null) return -1
        if (saved.id.isNotEmpty()) {
            val byId = tracks.indexOfFirst { it.id == saved.id }
            if (byId >= 0) return byId
        }
        return if (saved.index in tracks.indices) saved.index else -1
    }

    /**
     * 把 [key] 移到最近使用列表的最前面并裁剪到 [maxEntries]，
     * 返回 `(新的顺序, 需要删掉的旧键)`。
     */
    fun touchIndex(
        index: List<String>,
        key: String,
        maxEntries: Int = MAX_ENTRIES
    ): Pair<List<String>, List<String>> {
        val limit = maxEntries.coerceAtLeast(0)
        val ordered = listOf(key) + index.filter { it != key }
        return ordered.take(limit) to ordered.drop(limit)
    }

    /**
     * 画面比例要不要按文件记忆：锁定全局比例时一切以全局设置为准，
     * 既不记录也不恢复，否则「锁定」就失去意义了。
     */
    fun shouldRememberAspectRatio(lockGlobalRatio: Boolean): Boolean = !lockGlobalRatio

    private fun encodeTrack(name: String, ref: PlaybackTrackRef): String =
        name + NAME_VALUE_SEPARATOR + ref.index + NAME_VALUE_SEPARATOR + escape(ref.id)

    private fun decodeTrack(rest: String): PlaybackTrackRef? {
        val index = rest.substringBefore(NAME_VALUE_SEPARATOR).toIntOrNull() ?: return null
        val id = unescape(rest.substringAfter(NAME_VALUE_SEPARATOR, ""))
        return PlaybackTrackRef(id, index)
    }

    private fun escape(raw: String): String =
        raw.replace("%", ESCAPED_PERCENT).replace(FIELD_SEPARATOR, ESCAPED_PIPE)

    private fun unescape(raw: String): String =
        raw.replace(ESCAPED_PIPE, FIELD_SEPARATOR).replace(ESCAPED_PERCENT, "%")
}
