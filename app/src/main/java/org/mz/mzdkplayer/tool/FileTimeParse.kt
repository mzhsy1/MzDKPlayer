package org.mz.mzdkplayer.tool

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * [FileTimeResolver] 中与 Android / 网络无关的纯字符串解析逻辑。
 *
 * 单独抽出来是为了让这些易错的解析分支能在 JVM 单元测试里直接覆盖，
 * 实现与原内联版本保持一致。
 */
internal object FileTimeParse {

    /** HTTP 头里常见的几种日期写法，按顺序尝试 */
    private val HTTP_DATE_FORMATS = arrayOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz", // RFC 1123，如 Wed, 21 Oct 2015 07:28:00 GMT
        "EEE, dd MMM yyyy HH:mm:ss Z",   // 时区写成偏移量，如 +0800
        "EEE, dd MMM yy HH:mm:ss zzz",   // 两位年份
        "EEE MMM d HH:mm:ss yyyy"        // asctime 风格，如 Wed Oct 21 07:28:00 2015
    )

    /**
     * 解析 HTTP 响应头里的日期字符串（如 `Last-Modified`）。
     *
     * 依次尝试 [HTTP_DATE_FORMATS]；为空、无法识别或结果非正数时返回 null。
     */
    fun parseHttpDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        for (pattern in HTTP_DATE_FORMATS) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(trimmed)?.time
            }.getOrNull()
            if (parsed != null && parsed > 0L) return parsed
        }
        return null
    }

    /**
     * 由地址的 scheme 推断数据源类型，用于 dataSourceType 缺失或未知时的兜底。
     *
     * 匹配不到已知协议时返回 null，由调用方按本地文件处理。
     */
    fun inferDataSourceType(mediaUri: String): String? = when {
        mediaUri.startsWith("smb://", true) -> "SMB"
        mediaUri.startsWith("ftp://", true) -> "FTP"
        mediaUri.startsWith("nfs://", true) -> "NFS"
        mediaUri.startsWith("http://", true) || mediaUri.startsWith("https://", true) -> "HTTP"
        else -> null
    }

    /**
     * 从 URI 的 userInfo 段拆出账号密码。
     *
     * `user:pass` → `("user", "pass")`；`user` → `("user", "")`；
     * `null` → `(defaultUser, "")`。
     */
    fun credentials(userInfo: String?, defaultUser: String): Pair<String, String> =
        (userInfo?.substringBefore(':') ?: defaultUser) to (userInfo?.substringAfter(':', "") ?: "")

    /**
     * 拆分 NFS 地址里的「导出目录」与「导出内路径」。
     *
     * 形如 `/fs/1000/nfs:/movies/a.mkv` → `("fs/1000/nfs", "/movies/a.mkv")`。
     * 注意导出目录**不含**前导 `/`（与 [NFSDataSource] 的解析保持一致）。
     *
     * 第二个冒号不存在、或导出目录为空（冒号在索引 0/1）时返回 null；
     * 导出内路径统一补齐前导 `/`。
     */
    fun splitNfsPath(path: String): Pair<String, String>? {
        val colonIndex = path.indexOf(':', 1)
        if (colonIndex <= 1) return null

        val exportedPath = path.substring(1, colonIndex)
        val pathWithinExport = path.substring(colonIndex + 1)
        val nfsFilePath = if (pathWithinExport.startsWith("/")) {
            pathWithinExport
        } else {
            "/$pathWithinExport"
        }
        return exportedPath to nfsFilePath
    }
}
