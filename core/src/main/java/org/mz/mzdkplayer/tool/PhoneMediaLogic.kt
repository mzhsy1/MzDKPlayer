package org.mz.mzdkplayer.tool

import org.mz.mzdkplayer.data.model.AudioItem
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * 手机端「媒体条目怎么分流」的纯逻辑（第五阶段：音乐播放与图片查看）。
 *
 * 与 [PhoneScrapeLogic] 的分工：那个管「哪些条目要刮削、刮完怎么显示」，本对象管
 * 「点下去应该开哪个页面，以及那个页面需要哪些上下文」。
 *
 * 抽出来的目的同样是能在 JVM 单测里锁住边界：目录与非媒体文件不能参与分流、
 * 音频播放列表只收本目录里的音频且顺序与列表一致、当前曲目的下标必须落在列表内。
 *
 * 约定：这里**只能**用 JDK API（外加 [Tools] 与 [AudioItem] 这种纯数据），
 * 不要引入 `android.*` / `androidx.*`。
 */
object PhoneMediaLogic {

    /** 手机端能就地打开的媒体类型 */
    enum class Kind { VIDEO, AUDIO, IMAGE, OTHER }

    /**
     * 目录里的一个条目在「打开媒体」视角下需要的最小信息。
     *
     * 与 `PhoneBrowserEntry`（app 侧）保持字段一致，由调用方做映射 —— `:core` 不认识 UI 类型。
     */
    data class Item(
        val name: String,
        val playbackUri: String?,
        val isDirectory: Boolean = false,
    )

    /**
     * 点击一个条目后要打开的页面所需的全部参数。
     *
     * [kind] 只有 `AUDIO` / `IMAGE` 两种 —— 视频走原有的播放路由，不分流到这里。
     * 音频带 [audioItems] + [currentIndex]，图片带 [imageUris] + [currentIndex]，
     * 两者都只包含**同目录**的兄弟文件，这样「下一首 / 左右滑动」不会跳出去。
     */
    data class MediaOpen(
        val kind: Kind,
        val title: String,
        /** 该来源的协议标记（`LOCAL / SMB / FTP / NFS / WEBDAV / HTTP`），播放地址与它配套使用 */
        val dataSourceType: String,
        /** 连接名（本机文件为空串），只有音频会写进播放历史 */
        val connectionName: String = "",
        val audioItems: List<AudioItem> = emptyList(),
        val imageUris: List<String> = emptyList(),
        val currentIndex: Int = 0,
    )

    /** 按扩展名判定媒体类型（目录由调用方先排除掉） */
    fun kindOf(fileName: String): Kind {
        val extension = Tools.extractFileExtension(fileName)
        return when {
            Tools.containsVideoFormat(extension) -> Kind.VIDEO
            Tools.containsAudioFormat(extension) -> Kind.AUDIO
            Tools.containsImageFileExtension(extension) -> Kind.IMAGE
            else -> Kind.OTHER
        }
    }

    /** 条目视角的类型：目录、没有媒体地址的条目一律算 [Kind.OTHER]（不可打开） */
    fun kindOf(item: Item): Kind {
        if (item.isDirectory) return Kind.OTHER
        if (item.playbackUri.isNullOrBlank()) return Kind.OTHER
        return kindOf(item.name)
    }

    /**
     * 点击 [item] 时要打开的页面；返回 null 表示这条不该由本对象接管
     * （目录、视频、无地址或不支持的扩展名）。
     *
     * 播放地址就是各协议列目录时算好的那一个，与刮削用的 `media_cache` 主键一致。
     */
    fun openFor(
        item: Item,
        siblings: List<Item>,
        dataSourceType: String,
        connectionName: String,
    ): MediaOpen? = when (kindOf(item)) {
        Kind.AUDIO -> {
            val playable = siblings.filter { kindOf(it) == Kind.AUDIO }
            val index = playable.indexOfFirst { it.playbackUri == item.playbackUri }
            if (index < 0) {
                null
            } else {
                MediaOpen(
                    kind = Kind.AUDIO,
                    title = item.name,
                    dataSourceType = dataSourceType,
                    connectionName = connectionName,
                    audioItems = playable.map {
                        AudioItem(
                            uri = it.playbackUri.orEmpty(),
                            fileName = it.name,
                            dataSourceType = dataSourceType,
                        )
                    },
                    currentIndex = index,
                )
            }
        }

        Kind.IMAGE -> {
            val images = siblings.filter { kindOf(it) == Kind.IMAGE }
            val index = images.indexOfFirst { it.playbackUri == item.playbackUri }
            if (index < 0) {
                null
            } else {
                MediaOpen(
                    kind = Kind.IMAGE,
                    title = item.name,
                    dataSourceType = dataSourceType,
                    imageUris = images.map { it.playbackUri.orEmpty() },
                    currentIndex = index,
                )
            }
        }

        Kind.VIDEO, Kind.OTHER -> null
    }

    /**
     * 音频播放列表里的下标；找不到时返回 0（调用方据此退化成「从第一首开始」而不是崩掉）。
     *
     * 播放列表是写进 `AudioPlaylistRepository` 之后由播放页读回来的，
     * 中间可能隔着进程重建，所以要允许「当前地址不在列表里」。
     */
    fun indexOfUri(items: List<AudioItem>, uri: String): Int {
        val index = items.indexOfFirst { it.uri == uri }
        return if (index < 0) 0 else index
    }

    /**
     * 同目录同名歌词文件的候选名：`歌曲.flac` → `歌曲.lrc`。
     *
     * 只认 `.lrc`，不认 `.txt` —— 目录里常有说明文件、目录清单，同名 txt 被当成歌词的
     * 风险大于收益。
     */
    fun lyricSiblingName(audioFileName: String): String =
        audioFileName.substringBeforeLast('.', audioFileName) + LYRIC_EXTENSION

    /**
     * 把 [uri] 末尾的文件名换成 [newFileName]，得到同目录兄弟文件的地址。
     *
     * 各协议的「同目录」在地址上都表现为「最后一个 `/` 之前的部分相同」，
     * 所以统一按字符串换尾段，不需要按协议分支。返回 null 表示这个地址没法安全替换
     * （空串、没有路径段、末尾就是 `/`）。
     */
    fun siblingUri(uri: String, newFileName: String): String? {
        if (uri.isBlank() || newFileName.isBlank()) return null
        // HTTP 直链可能带 `?`，换文件名时要把它去掉，否则会拼出 `歌.lrc?token=xxx` 这种怪地址
        val base = uri.substringBefore('?')
        val slash = base.lastIndexOf('/')
        if (slash < 0 || slash == base.lastIndex) return null
        return base.substring(0, slash + 1) + newFileName
    }

    /**
     * 歌词文件的字节 → 文本。
     *
     * 顺序是「BOM → 严格 UTF-8 → GBK」：带 BOM 的按 BOM 走；没有 BOM 的先按 UTF-8 严格解，
     * 只要出现一个非法字节就退回 GBK —— 国内下载的 .lrc 相当一部分是 GBK 编码。
     */
    fun decodeLyricText(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val bom = when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                Charsets.UTF_8 to 3

            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE to 2
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE to 2
            else -> null
        }
        val text = when {
            bom != null -> runCatching {
                String(bytes, bom.second, bytes.size - bom.second, bom.first)
            }.getOrNull()

            else -> decodeStrictUtf8(bytes) ?: runCatching { String(bytes, GBK) }.getOrNull()
        }
        return text?.removePrefix("\uFEFF")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun decodeStrictUtf8(bytes: ByteArray): String? = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()

    /**
     * 把「以某个字符为分隔符的一串 Base64 值」拼/拆开（图片序列走路由传递时用）。
     *
     * 之所以固定用 `,`：Base64 的 URL_SAFE 字符集是 `A-Za-z0-9-_`，不会出现逗号，
     * 所以不需要额外转义，也就不会因为转义把路由撑长。
     */
    fun joinArgs(values: List<String>): String = values.joinToString(ARG_SEPARATOR)

    fun splitArgs(raw: String?): List<String> =
        raw.orEmpty().split(ARG_SEPARATOR).filter { it.isNotEmpty() }

    private const val ARG_SEPARATOR = ","

    /** 同目录歌词只认这个后缀 */
    private const val LYRIC_EXTENSION = ".lrc"

    /** GBK 是 `.lrc` 最常见的非 UTF-8 编码，Android 与 JVM 都自带 */
    private val GBK: Charset = Charset.forName("GBK")
}
