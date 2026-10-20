package org.mz.mzdkplayer.tool.logic

import java.util.Locale

/**
 * 文件列表排序字段。
 *
 * 电视端六个文件浏览页（本地 / SMB / FTP / NFS / HTTP / WebDAV）共用这一套口径：
 * 用户只需要在任意一页选一次排序，其它协议、下一次进 App 都沿用。
 */
enum class FileSortField {
    NAME,
    TIME,
    SIZE,
    TYPE,
}

/**
 * 一条排序规则：按哪个字段、升序还是降序、文件夹是否置顶。
 *
 * 「文件夹优先」之所以是独立开关而不是一个字段值：用户常见的两种诉求
 * ——「先看文件夹再看文件」与「按时间把刚放进去的东西翻出来」——可以叠加，
 * 之前 WebDAV / SMB 的 `sortedBy { it.name }` 把所有条目混在一起排，正是被吐槽的点。
 */
data class FileSortOption(
    val field: FileSortField = FileSortField.NAME,
    val descending: Boolean = false,
    val foldersFirst: Boolean = true,
)

/**
 * 文件列表排序纯逻辑（无 Android 依赖，可在 `:core` 直接跑 JVM 单测）。
 *
 * 设计要点：
 * 1. 排序键**每个条目只算一次**。NFS 的 `getAttributes()`、`length()` 都是一次 RPC，
 *    如果按比较器里的 lambda 现取，n log n 次比较就是几百次网络往返，列表会卡死。
 *    因此这里先把条目装饰成 [SortEntry] 再排。
 * 2. 只在真正用到时才取时间/大小（按名称排序不会触发任何额外 IO）。
 * 3. 主键相同的条目统一再按名称升序兜底，保证每次进入目录顺序稳定
 *    （服务端返回顺序不保证一致，尤其是时间/大小相同的情况）。
 */
object FileSortLogic {

    /** 默认排序：文件夹优先 + 名称升序（最接近传统文件管理器，也是修复「文件与文件夹混排」的默认值） */
    val DEFAULT = FileSortOption()

    /**
     * 排序菜单里的固定档位（顺序即菜单顺序）。
     *
     * 「文件夹优先」是菜单顶部的独立开关，所以这里每一档都只描述「字段 + 方向」，
     * 选中某一档时保留当前开关状态（见 [FileSortDialog] 调用点）。
     */
    val PRESETS: List<FileSortOption> = listOf(
        FileSortOption(FileSortField.NAME, descending = false),
        FileSortOption(FileSortField.NAME, descending = true),
        FileSortOption(FileSortField.TIME, descending = true),
        FileSortOption(FileSortField.TIME, descending = false),
        FileSortOption(FileSortField.SIZE, descending = true),
        FileSortOption(FileSortField.SIZE, descending = false),
        FileSortOption(FileSortField.TYPE, descending = false),
    )

    private const val ASC = "asc"
    private const val DESC = "desc"
    private const val FOLDERS_FIRST = "dirs"
    private const val MIXED = "mixed"

    /** 存进 SharedPreferences 的字符串，形如 `name-asc-dirs`（可读、可手改、加字段时不会撞号） */
    fun encode(option: FileSortOption): String {
        val field = option.field.name.lowercase(Locale.ROOT)
        val direction = if (option.descending) DESC else ASC
        val folders = if (option.foldersFirst) FOLDERS_FIRST else MIXED
        return "$field-$direction-$folders"
    }

    /** 读设置时用：脏数据/老版本值一律收敛回默认档，不让界面拿到半截状态 */
    fun normalize(raw: String?): String = encode(decode(raw))

    fun decode(raw: String?): FileSortOption {
        if (raw.isNullOrBlank()) return DEFAULT
        val parts = raw.trim().lowercase(Locale.ROOT).split('-')
        val field = when (parts.getOrNull(0)) {
            "name" -> FileSortField.NAME
            "time" -> FileSortField.TIME
            "size" -> FileSortField.SIZE
            "type" -> FileSortField.TYPE
            else -> return DEFAULT
        }
        val descending = when (parts.getOrNull(1)) {
            DESC -> true
            ASC -> false
            else -> return DEFAULT
        }
        val foldersFirst = when (parts.getOrNull(2)) {
            FOLDERS_FIRST -> true
            MIXED -> false
            else -> return DEFAULT
        }
        return FileSortOption(field, descending, foldersFirst)
    }

    /** 设置页「点一下换一档」：按 [PRESETS] 顺序循环，保留文件夹优先开关 */
    fun next(option: FileSortOption): FileSortOption {
        val index = PRESETS.indexOfFirst { it.field == option.field && it.descending == option.descending }
        val nextPreset = if (index < 0) PRESETS.first() else PRESETS[(index + 1) % PRESETS.size]
        return nextPreset.copy(foldersFirst = option.foldersFirst)
    }

    /**
     * 按 [option] 排序。
     *
     * @param nameOf 条目显示名（大小写不敏感比较）
     * @param isDirectoryOf 是否文件夹（「文件夹优先」用）
     * @param modifiedOf 最后修改时间（毫秒，拿不到给 0）；只在按时间排序时调用
     * @param sizeOf 文件大小（字节，目录给 0）；只在按大小排序时调用
     */
    fun <T> sort(
        items: List<T>,
        option: FileSortOption,
        nameOf: (T) -> String,
        isDirectoryOf: (T) -> Boolean,
        modifiedOf: (T) -> Long = { 0L },
        sizeOf: (T) -> Long = { 0L },
    ): List<T> {
        if (items.size < 2) return items

        val needTime = option.field == FileSortField.TIME
        val needSize = option.field == FileSortField.SIZE
        val needType = option.field == FileSortField.TYPE

        // 一次遍历把排序需要的东西全部取好（网络协议下这就是「只发一次 RPC」的关键）
        val entries = items.map { item ->
            val name = nameOf(item).lowercase(Locale.ROOT)
            SortEntry(
                item = item,
                name = name,
                isDirectory = isDirectoryOf(item),
                modified = if (needTime) modifiedOf(item) else 0L,
                size = if (needSize) sizeOf(item) else 0L,
                extension = if (needType) extensionOf(name) else "",
            )
        }

        val byName = Comparator<SortEntry<T>> { a, b -> a.name.compareTo(b.name) }
        val primary: Comparator<SortEntry<T>> = when (option.field) {
            FileSortField.NAME -> byName
            FileSortField.TIME -> compareBy<SortEntry<T>> { it.modified }
            FileSortField.SIZE -> compareBy<SortEntry<T>> { it.size }
            FileSortField.TYPE -> compareBy<SortEntry<T>> { it.extension }.then(byName)
        }
        val ordered = if (option.descending) primary.reversed() else primary
        val comparator = if (option.foldersFirst) {
            // true 排在前面：目录整体置顶，目录内部与文件内部再按所选规则排
            compareByDescending<SortEntry<T>> { it.isDirectory }.then(ordered)
        } else {
            ordered
        }

        return entries.sortedWith(comparator.then(byName)).map { it.item }
    }

    /** 扩展名（不含点），无扩展名（含 `.gitignore` 这种）返回空串 */
    private fun extensionOf(lowercaseName: String): String {
        val dot = lowercaseName.lastIndexOf('.')
        return if (dot <= 0 || dot == lowercaseName.length - 1) {
            ""
        } else {
            lowercaseName.substring(dot + 1)
        }
    }

    private data class SortEntry<T>(
        val item: T,
        val name: String,
        val isDirectory: Boolean,
        val modified: Long,
        val size: Long,
        val extension: String,
    )
}
