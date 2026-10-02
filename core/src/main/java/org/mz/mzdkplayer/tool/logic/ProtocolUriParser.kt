package org.mz.mzdkplayer.tool.logic

/**
 * 各协议 URI 里「路径 / 端口」的纯字符串解析。
 *
 * 与凭证解析分开：`userInfo` → 账号密码已经由 [FileTimeParse.credentials] 负责，
 * 本对象只管路径结构。抽出来的动机是 `SmbUtils` 与 `FtpDataSource` 各自写了一份
 * 「SMB 的共享名取第几段」「URI 不写端口时算几」。
 *
 * 只接收 `String`（不接收 `android.net.Uri`），所以能在 JVM 单测里直接跑。
 */
internal object ProtocolUriParser {

    /** URI 未显式写端口时，FTP 的默认端口。 */
    const val DEFAULT_FTP_PORT = 21

    /** SMB 路径拆出的「共享名 + 共享内路径」。 */
    data class SmbPath(val shareName: String, val filePath: String)

    /**
     * `/共享名/目录/影片.mkv` → `SmbPath("共享名", "目录/影片.mkv")`。
     *
     * - 开头多余的 `/` 与连续的空路径段都被忽略（`//share//a.mkv` → `("share", "a.mkv")`）
     * - 只有共享名、没有文件时 [SmbPath.filePath] 是空串（连整个共享会用到）
     * - 一个路径段都没有（空串 / `/` / `///`）时返回 null，调用方据此报
     *   `Invalid SMB URI: no share or path`
     */
    fun parseSmbPath(path: String): SmbPath? {
        val segments = path.split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null
        return SmbPath(segments.first(), segments.drop(1).joinToString("/"))
    }

    /**
     * `Uri.port` → 真实端口：Android 在 URI 未写端口时给的是 `-1`，
     * 此时落到 [DEFAULT_FTP_PORT]。
     */
    fun ftpPort(uriPort: Int): Int = if (uriPort == -1) DEFAULT_FTP_PORT else uriPort
}
