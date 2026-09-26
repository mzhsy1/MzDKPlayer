package org.mz.mzdkplayer.tool

/**
 * 手机端文件浏览里与 Android 无关的纯逻辑（第三阶段：本地 / FTP / NFS / WebDAV / HTTP）。
 *
 * 与 [FileBrowserLogic] 的分工：
 * - [FileBrowserLogic] 是五个协议**共用**的东西（目录条目过滤、各协议上一级、URL 拼接），电视端也在调；
 * - 本对象只放**手机端新引入**的判定，其中「本地文件的上一级」和「NFS 播放地址」在电视端是散在 UI 里的，
 *   抽出来是为了能在 JVM 单测里锁住边界（到根目录不再上溯、导出路径缺前导斜杠会拼出非法 URL）。
 *
 * 约定：这里**只能**用 JDK API，不要引入 `android.*` / `androidx.*`。
 */
object PhoneFileBrowserLogic {

    /**
     * 本地文件的播放地址：`file://` + 绝对路径。
     *
     * 与电视端 `LocalFileListScreen` 一致（`selectedDataSourceFactory` 认 `file://` 前缀 +
     * `dataSourceType == "LOCAL"` 这一组合）。
     */
    fun localPlaybackUri(absolutePath: String): String = "file://$absolutePath"

    /**
     * 本地目录的上一级。
     *
     * - 已经位于 [root]（或任何不该再往上走的位置）时返回 [root] 自身，调用方据此隐藏「返回上一级」；
     * - `currentPath` 为空按 [root] 处理；[root] 为空按 `"/"` 处理；
     * - 只做字符串上溯、**不**判断目录是否存在：手机上 root 之上通常是 `/storage/emulated`
     *   这类没有读取权限的目录，越界时直接夹回 [root]。
     */
    fun localParentPath(currentPath: String, root: String): String {
        val safeRoot = root.ifBlank { "/" }.trimEnd('/').ifEmpty { "/" }
        val current = currentPath.trimEnd('/').ifEmpty { safeRoot }
        if (current == safeRoot) return safeRoot

        val parent = current.substringBeforeLast('/', "").ifEmpty { "/" }
        // 注意按「目录段」比较而不是纯前缀：/storage/emulated/0extra 不是 /storage/emulated/0 的子目录
        val insideRoot = parent == safeRoot || parent.startsWith("$safeRoot/")
        return if (parent == "/" || insideRoot) parent else safeRoot
    }

    /**
     * NFS 播放地址：`nfs://<host>:<exportedPath>:<pathWithinExport>`。
     *
     * `NFSDataSource` 把 authority 之后的整段 path 按第一个 `:` 切成「导出路径 + 导出内路径」，
     * 所以导出路径（`NFSConnection.shareName`）**必须**带前导 `/`：少了会拼出
     * `nfs://192.168.1.4:export:...`，此时 authority 变成 `host:export`，端口非法、host 取不到。
     * 电视端是直接字符串拼接、没有这层兜底，这里补上（带前导 `/` 时结果与电视端逐字一致）。
     */
    fun nfsPlaybackUri(serverAddress: String, exportPath: String, pathWithinExport: String): String {
        val export = exportPath.trim().trimEnd('/').let { if (it.startsWith("/")) it else "/$it" }
        val path = pathWithinExport.trim().let { if (it.startsWith("/")) it else "/$it" }
        return "nfs://$serverAddress:$export:$path"
    }
}
