package org.mz.mzdkplayer.tool

/**
 * 「伴生文件路径推导」的纯字符串逻辑 —— 由视频路径推出同名侧车文件的路径
 * （弹幕 `.xml`、刮削信息 `.nfo`）。
 *
 * 抽出来的原因：这件事在项目里原本有**四份各自为政的实现**
 * （`SmbUtils.getDanmakuSmbUri` / `SmbUtils.getDanmakuNfsUri` /
 * `NfoReader.constructNfoUri` / `NfoReader.getNfoNfsUri`），
 * 「没有扩展名时怎么办」四处口径不同，且 NFS 那套 `/导出目录:导出内路径` 的冒号处理最容易写错。
 * 现在四处都委托到这里，规则集中、可被 `SidecarPathTest` 覆盖。
 *
 * 本对象只做字符串处理，不依赖 Android / 网络。
 */
internal object SidecarPathLogic {

    /**
     * **统一的伴生文件命名口径**：把路径末尾的扩展名换成 [suffix]，得到同目录、同基名的侧车文件。
     *
     * 判定「末尾有扩展名」的条件是最后一个 `.` 落在**最后一段路径内、且不是该段的首字符**：
     * - `/share/Movies/影片.mkv` → `/share/Movies/影片.xml`
     * - `/m/a.b.c` → `/m/a.b.xml`（只换最后一段）
     * - `/movies.v2/影片` → `/movies.v2/影片.xml`（点落在目录名上，不算扩展名）
     * - `.hidden` → `.hidden.xml`（点开头的隐藏文件不当作扩展名）
     * - `/share/Movies/影片` → `/share/Movies/影片.xml`（没有扩展名就追加）
     *
     * 也就是说：**任何输入都不会返回 null，也绝不会把原文件名丢掉**。
     * 这是历史四份实现的公共收敛点 —— 它们此前的行为分别是「退化成根目录的 `.xml`」
     * 「抛异常」「放弃查询」「在末尾追加」，后两种才符合预期。
     */
    fun withExtension(path: String, suffix: String): String {
        val lastDotIndex = path.lastIndexOf('.')
        val lastSlashIndex = path.lastIndexOf('/')
        val hasExtension = lastDotIndex > lastSlashIndex + 1
        return if (hasExtension) path.substring(0, lastDotIndex) + suffix else path + suffix
    }

    /**
     * 目录部分（含结尾的 `/`）；没有 `/` 时返回空串。
     * 例如 `/movies/a.mkv` → `/movies/`。
     */
    fun directoryOf(path: String): String {
        val lastSlashIndex = path.lastIndexOf('/')
        return if (lastSlashIndex != -1) path.take(lastSlashIndex + 1) else ""
    }

    /**
     * 文件名部分（最后一个 `/` 之后）；没有 `/` 时整串就是文件名。
     * 例如 `/movies/a.mkv` → `a.mkv`。
     */
    fun fileNameOf(path: String): String {
        val lastSlashIndex = path.lastIndexOf('/')
        return if (lastSlashIndex != -1) path.substring(lastSlashIndex + 1) else path
    }

    /**
     * 拆分 NFS 路径里的「导出目录」与「导出内路径」。
     *
     * 形如 `/fs/1000/nfs:/movies/a.mkv` → `("fs/1000/nfs", "/movies/a.mkv")`。
     * 与 [FileTimeParse.splitNfsPath] 的区别：本函数**原样返回**，不补齐导出内路径的前导 `/`，
     * 也不因导出目录为空而返回 null —— 空值判定交给调用方（各协议的错误策略不同）。
     *
     * 找不到分隔冒号时返回 null。
     */
    fun splitNfsRaw(path: String): Pair<String, String>? {
        val colonIndex = path.indexOf(':', 1)
        if (colonIndex == -1) return null
        return path.substring(1, colonIndex) to path.substring(colonIndex + 1)
    }

    /**
     * 把「导出目录」与「导出内路径」拼回 NFS 路径形态（`/导出目录:导出内路径`，含前导 `/`）。
     *
     * 手动拼接而不是用 `Uri.Builder`，因为 `Uri.Builder.path()` 会把冒号编码成 `%3A`。
     */
    fun joinNfsPath(exportedPath: String, pathWithinExport: String): String =
        "/$exportedPath:$pathWithinExport"
}
