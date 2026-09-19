package org.mz.mzdkplayer.tool

import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * 文件浏览模块（SMB / FTP / NFS / HTTP / WebDAV）里与 Android、网络 IO 无关的纯逻辑。
 *
 * 单独抽出来有两个目的：
 * 1. 路径拼接、目录条目过滤、HTTP 目录网页解析这些最容易出错的分支，可以在 JVM 单元测试里
 *    直接覆盖；各个 `*ConViewModel` / 文件列表页都改为调用本对象，测的就是线上跑的那份逻辑。
 * 2. 五个协议原先各自写了一套「取上一级 / 拼接子路径」的实现，口径并不一致
 *    （例如根目录时 FTP 返回 `""`、NFS 返回 `"/"`），统一到这里避免继续分叉。
 *
 * 约定：这里**只能**用 JDK API，不要引入 `android.*` / `androidx.*`（否则无法 JVM 单测）。
 * 各函数的边界行为基本与抽取前的原实现逐字对齐；仅有两处刻意收紧，均在函数注释里写明：
 * 目录条目过滤统一丢弃空名条目、[nfsChildPath] 把 `"/"` 也按挂载根处理。
 */
internal object FileBrowserLogic {

    // ══════════════════════════ 公共 ══════════════════════════

    /**
     * 是否是目录列表里应当丢弃的条目：`.`（当前目录）、`..`（上级目录）以及无名字的空条目。
     *
     * 原实现在 FTP 里过滤了空名、在 SMB / NFS / HTTP / WebDAV 里只过滤 `.` 和 `..`，
     * 这里统一按最严的口径处理（空名条目在列表里会渲染成一个空行）。
     */
    fun isHiddenDirEntry(name: String?): Boolean =
        name.isNullOrBlank() || name == "." || name == ".."

    // ══════════════════════════ SMB ══════════════════════════

    /**
     * SMB 的 `FILE_ATTRIBUTE_DIRECTORY` 属性位。
     *
     * 取值等同 `com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value`，
     * 写死是为了让本对象不依赖 smbj，`MediaEntryTypeTest` 里有断言锁死两者一致。
     */
    private const val SMB_ATTRIBUTE_DIRECTORY = 0x00000010L

    /** `smb://[username:password@]server/share[/path]`，账号密码段可选。 */
    private val SMB_URL_PATTERN =
        Regex("^smb://(?:([^:]+):([^@]+)@)?([^/]+)/([^/]+)(/.*)?$")

    /** 由 SMB 文件属性位判断是不是目录。 */
    fun isSmbDirectory(fileAttributes: Long): Boolean =
        (fileAttributes and SMB_ATTRIBUTE_DIRECTORY) != 0L

    /**
     * 把 UI 侧用的正斜杠路径转成 SMB 请求需要的反斜杠路径。
     *
     * `"/"` → `"\\"`；`"/影片/动作"` → `"\\影片\\动作"`；结尾的多余斜杠会被去掉。
     */
    fun normalizeSmbDirectory(path: String): String =
        if (path == "/") "\\" else path.replace("/", "\\").trimEnd('\\')

    /** 在 SMB 目录（反斜杠形式）下拼接一个子条目，`"\\"` 表示共享根目录。 */
    fun joinSmbPath(parent: String, name: String): String =
        if (parent == "\\") "\\$name" else "$parent\\$name"

    /** 反斜杠路径转回正斜杠，用于展示和拼 `smb://` 地址。 */
    fun toSmbDisplayPath(backslashPath: String): String =
        backslashPath.replace("\\", "/")

    /**
     * 解析 `smb://username:password@server/share/path/to/directory` 形式的地址。
     *
     * - 账号密码段可省略，省略时账号兜底为 `guest`、密码为空；
     * - 路径段可省略，省略或为空白时兜底为 `"/"`；
     * - 整体格式不匹配（例如不是 smb 协议、缺少 share）时返回 null，由调用方决定兜底值。
     */
    fun parseSmbUrl(url: String): SmbUrlParts? {
        val match = SMB_URL_PATTERN.find(url) ?: return null
        val (username, password, server, share, rawPath) = match.destructured
        return SmbUrlParts(
            server = server,
            share = share,
            path = rawPath.trim().ifEmpty { "/" },
            username = username.ifEmpty { "guest" },
            password = password.ifEmpty { "" }
        )
    }

    /** 反向拼回 `smb://` 地址：有账号密码才写进 userInfo 段（用于连接信息展示/保存）。 */
    fun buildSmbUrl(
        server: String,
        share: String,
        path: String,
        username: String,
        password: String
    ): String = if (username.isNotEmpty() && password.isNotEmpty()) {
        "smb://$username:$password@$server/$share$path"
    } else {
        "smb://$server/$share$path"
    }

    /**
     * 拼播放地址用的 `smb://` 地址：**始终**带上 userInfo 段（哪怕是空账号）。
     *
     * 与 [buildSmbUrl] 的差异来自递归扫描的既有行为，不要合并。
     */
    fun buildSmbUrlWithCredentials(
        server: String,
        share: String,
        path: String,
        username: String,
        password: String
    ): String = "smb://$username:$password@$server/$share$path"

    // ══════════════════════════ FTP ══════════════════════════

    /**
     * 把路径规整成 FTP 请求用的绝对目录：保证前导 `/` 和结尾 `/` 都有。
     *
     * `""` → `"/"`；`"movies"` → `"/movies/"`；`"/movies/"` → `"/movies/"`。
     */
    fun normalizeFtpDirectory(path: String): String {
        val withLeadingSlash = if (path.startsWith("/")) path else "/$path"
        return if (withLeadingSlash.endsWith("/")) withLeadingSlash else "$withLeadingSlash/"
    }

    /** 请求用目录转成界面显示用的相对路径：去掉前导和结尾的 `/`。 */
    fun ftpDisplayPath(directory: String): String =
        directory.removePrefix("/").removeSuffix("/")

    /**
     * FTP 的上一级目录（相对路径，不带前导 `/`）。
     *
     * `"movies/action"` → `"movies"`；`"movies"` → `""`（已在根目录）。
     */
    fun ftpParentPath(current: String): String {
        val cleanCurrent = current.removeSuffix("/")
        val lastSlashIndex = cleanCurrent.lastIndexOf('/')
        return if (lastSlashIndex >= 0) cleanCurrent.substring(0, lastSlashIndex) else ""
    }

    /** 拼 FTP 播放地址：`ftp://user:pass@host:port/绝对路径`。 */
    fun buildFtpUrl(
        server: String,
        port: Int,
        username: String,
        password: String,
        absolutePath: String
    ): String = "ftp://$username:$password@$server:$port$absolutePath"

    /**
     * 用「当前目录 + 资源名」拼 FTP 播放地址，目录缺少结尾 `/` 时会补上。
     */
    fun buildFtpResourceUrl(
        server: String,
        port: Int,
        username: String,
        password: String,
        currentPath: String,
        resourceName: String
    ): String {
        val cleanPath = if (currentPath.isEmpty()) {
            ""
        } else if (currentPath.endsWith("/")) {
            currentPath
        } else {
            "$currentPath/"
        }
        val cleanResourceName = resourceName.removePrefix("/")
        return buildFtpUrl(server, port, username, password, "/$cleanPath$cleanResourceName")
    }

    // ══════════════════════════ NFS ══════════════════════════

    /**
     * NFS 的上一级目录（绝对路径，根目录固定为 `"/"`）。
     *
     * 空路径和 `"/"` 都返回 `""`（表示「已经没有上一级」，调用方据此回到挂载根）；
     * `"/movies"` → `"/"`；`"/movies/action"` → `"/movies"`。
     */
    fun nfsParentPath(currentPath: String): String {
        if (currentPath.isEmpty() || currentPath == "/") return ""

        val normalizedPath = (if (currentPath.startsWith("/")) currentPath else "/$currentPath")
            .trimEnd('/')
        val lastSlashIndex = normalizedPath.lastIndexOf('/')
        return if (lastSlashIndex >= 0) {
            normalizedPath.substring(0, lastSlashIndex).ifEmpty { "/" }
        } else {
            "/"
        }
    }

    /**
     * 进入 NFS 子目录：`""` 和 `"/"` 都按挂载根处理。
     *
     * 修正说明：原 `NFSConViewModel.navigateToSubdirectory` 只判断了 `isEmpty()`，
     * 而 [NFSConViewModel] 连接后 `_currentPath` 可能停在 `"/"`，此时会拼出 `"//影片"`
     * 这样的双斜杠路径。这里统一按挂载根处理，结果恒为单斜杠开头。
     */
    fun nfsChildPath(parent: String, name: String): String =
        if (parent.isEmpty() || parent == "/") "/$name" else "$parent/$name"

    // ══════════════════════════ HTTP ══════════════════════════

    private val WHITESPACE_REGEX = Regex("\\s+")

    /**
     * 抓「`<a href="...">文字</a>` 后面跟的一小段文本」的正则。
     *
     * 第 1 组是 href，第 2 组是链接文字，第 3 组是 `</a>` 到下一个 `<` 之间的内容
     * （Nginx autoindex 把日期和大小放在这里）。
     */
    private val ANCHOR_PATTERN = Pattern.compile(
        "<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>([^<]*)",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    /**
     * 解析 HTTP 目录页（Nginx autoindex / Apache 目录列表）里的条目。
     *
     * 过滤规则：
     * - 跳过 `#锚点` 和 `javascript:` 链接；
     * - 跳过跨域、以及不在 [baseUrl] 子树下的链接；
     * - 跳过 `.`、`..` 和空名条目；
     * - 同名条目只保留第一个。
     *
     * href 会先做一次百分号解码，解码失败（例如文件名里带裸 `%`）时按原样使用。
     * `</a>` 之后那段文本只有在文件（非目录）时才解析为字节数。
     */
    fun parseHttpDirectoryListing(html: String, baseUrl: String): List<HttpDirEntry> {
        val entries = mutableListOf<HttpDirEntry>()
        val matcher = ANCHOR_PATTERN.matcher(html)

        while (matcher.find()) {
            val rawHref = matcher.group(1) ?: continue
            val href = decodeUrlComponent(rawHref)
            val afterAnchorText = matcher.group(3) ?: ""

            if (href.startsWith("#") || href.startsWith("javascript:")) continue
            // 无法解析的 href（例如 mailto: / ftp: 等未知协议）直接跳过，
            // 不能让一条坏链接把整页目录解析掉
            val resolved = runCatching { resolveHttpUrl(href, baseUrl) }.getOrNull() ?: continue
            if (!isHttpSubPath(resolved, baseUrl)) continue

            val isDirectory = href.endsWith("/")
            val cleanHref = href.trimEnd('/')
            val name = cleanHref.substringAfterLast("/", cleanHref)
            if (isHiddenDirEntry(name)) continue

            entries.add(
                HttpDirEntry(
                    name = name,
                    isDirectory = isDirectory,
                    href = href,
                    size = if (isDirectory) 0L else parseNginxSize(afterAnchorText)
                )
            )
        }
        return entries.distinctBy { it.name }
    }

    /** 百分号解码，非法转义时原样返回而不是抛异常。 */
    private fun decodeUrlComponent(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)

    /**
     * 从目录页 `</a>` 之后的文本里抠出文件大小。
     *
     * Nginx 的格式是「日期 时间 字节数」，目录则是 `-`；取不到数字时返回 0。
     */
    fun parseNginxSize(text: String): Long {
        val lastPart = text.trim().split(WHITESPACE_REGEX).lastOrNull() ?: return 0L
        if (lastPart == "-") return 0L
        return lastPart.toLongOrNull() ?: 0L
    }

    /** 以 [baseUrl] 为基准解析相对链接；链接本身非法时抛出 [java.net.MalformedURLException]。 */
    fun resolveHttpUrl(href: String, baseUrl: String): String =
        URL(URL(baseUrl), href).toString()

    /** 判断 [url] 是否与 [baseUrl] 同源（协议/主机/端口一致）且在其路径子树下。 */
    fun isHttpSubPath(url: String, baseUrl: String): Boolean = try {
        val baseUrlObj = URL(baseUrl)
        val urlObj = URL(url)
        urlObj.protocol == baseUrlObj.protocol &&
                urlObj.host == baseUrlObj.host &&
                urlObj.port == baseUrlObj.port &&
                urlObj.path.startsWith(baseUrlObj.path)
    } catch (e: Exception) {
        false
    }

    /**
     * 目录页的上一级 URL。
     *
     * 已经在站点根时原样返回（调用方据此判断「无法再往上」）。
     * 注意：重建 URL 时会丢掉 userInfo 段，账号密码由调用方另行提供。
     */
    fun httpParentUrl(currentUrl: String): String = try {
        val urlObj = URL(currentUrl)
        val path = urlObj.path
        if (path == "/" || path.count { it == '/' } <= 1) {
            currentUrl
        } else {
            val parentPath = path.trimEnd('/').substringBeforeLast("/", "")
            val origin = "${urlObj.protocol}://${urlObj.host}" +
                    (if (urlObj.port != -1) ":${urlObj.port}" else "")
            if (parentPath.isEmpty()) "$origin/" else "$origin$parentPath/"
        }
    } catch (e: Exception) {
        currentUrl
    }

    /**
     * 从完整 URL 里取出用于界面显示的逻辑路径。
     *
     * `http://host/nas/movies/action/` → `"/nas/movies/action"`；站点根 → `""`。
     */
    fun httpLogicalPath(fullUrl: String): String = try {
        val path = URL(fullUrl).path
        path.trim('/').let { if (it.isEmpty()) "" else "/$it" }
    } catch (e: Exception) {
        ""
    }

    // ══════════════════════════ WebDAV ══════════════════════════

    /**
     * 把子项名拼到父 URL 后面，两边的多余斜杠会被吃掉。
     *
     * `("http://host/dav/", "/a.mkv")` → `"http://host/dav/a.mkv"`。
     */
    fun joinUrlPath(parent: String, name: String): String =
        "${parent.trimEnd('/')}/${name.trimEnd('/').trimStart('/')}"

    /** 同上，但结果带结尾 `/`，供「进入子目录」使用。 */
    fun joinUrlDirectory(parent: String, name: String): String =
        joinUrlPath(parent, name) + "/"

    /** 保证 URL 以 `/` 结尾（WebDAV 服务端对目录 URL 敏感）。 */
    fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}

/**
 * [FileBrowserLogic.parseSmbUrl] 的解析结果。
 *
 * 与 `ui.screen.vm.SMBConfig` 字段一一对应，但不依赖 UI 层，保证本对象可以独立单测。
 */
internal data class SmbUrlParts(
    val server: String,
    val share: String,
    val path: String,
    val username: String,
    val password: String
)

/**
 * HTTP 目录页里的一个条目。
 *
 * [href] 保留服务端原始（已解码）写的相对链接，播放地址由调用方以当前目录为基准再解析一次。
 */
internal data class HttpDirEntry(
    val name: String,
    val isDirectory: Boolean,
    val href: String,
    val size: Long
)
