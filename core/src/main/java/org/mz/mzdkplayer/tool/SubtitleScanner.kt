package org.mz.mzdkplayer.tool

import android.net.Uri
import android.util.Log
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3
import com.emc.ecs.nfsclient.rpc.CredentialUnix
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

/**
 * 同名字幕扫描器：根据视频 URI 扫描其所在目录，找出与视频同名的字幕文件。
 *
 * 「哪些文件算同名字幕」以及字幕 URI 的拼装口径统一收在 [SubtitleMatchLogic]
 * （纯 JDK，可被 `SubtitleMatchTest` 直接覆盖），本文件只负责各协议的连接与列目录。
 *
 * 支持协议：
 * - 本地文件（file:// 或绝对路径）
 * - SMB（smb://user:pass@host/share/...）
 * - NFS（nfs://host/export:path...）
 * - FTP（ftp://user:pass@host:port/...）
 * - WebDAV（http/https，dataSourceType == "WEBDAV"）
 * - HTTP（http/https，其他，解析 Nginx 风格目录页）
 */
object SubtitleScanner {

    private const val TAG = "SubtitleScanner"

    // 同名匹配 / URI 拼装口径统一收在 SubtitleMatchLogic（可被单测覆盖）
    /**
     * 扫描入口：在 IO 线程执行，返回 (字幕URI, 文件名) 列表。
     */
    suspend fun scan(videoUri: String, dataSourceType: String): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val uri = videoUri.toUri()
            when (uri.scheme?.lowercase()) {
                "file", null -> scanLocal(videoUri)
                "smb" -> scanSmb(videoUri, uri)
                "nfs" -> scanNfs(videoUri, uri)
                "ftp" -> scanFtp(videoUri, uri)
                "http", "https" ->
                    if (dataSourceType == "WEBDAV") scanWebDav(videoUri, uri)
                    else scanHttp(videoUri, uri)
                else -> emptyList()
            }
        }

    // ---------- 本地 ----------

    private fun scanLocal(videoUri: String): List<Pair<String, String>> {
        val uri = videoUri.toUri()
        val path: String? = when (uri.scheme?.lowercase()) {
            null -> videoUri
            "file" -> uri.path
            else -> return emptyList()
        }
        if (path.isNullOrBlank()) return emptyList()

        val videoFile = java.io.File(path)
        val parent = videoFile.parentFile ?: return emptyList()

        val matched = parent.listFiles { f ->
            f.isFile && SubtitleMatchLogic.isSameNameSubtitle(f.name, videoFile.name)
        }?.map { f -> f.toURI().toString() to f.name } ?: emptyList()

        return matched.sortedBy { it.second }
    }

    // ---------- SMB ----------

    private fun scanSmb(videoUri: String, uri: Uri): List<Pair<String, String>> {
        val host = uri.host ?: return emptyList()
        val (user, pass) = parseUserInfo(uri, "guest", "")
        val segments = uri.path?.split("/")?.filter { it.isNotEmpty() } ?: return emptyList()
        if (segments.size < 2) return emptyList() // [share, fileName]

        val shareName = segments.first()
        val fileName = segments.last()
        val dirParts = segments.drop(1).dropLast(1)
        val smbDir = if (dirParts.isEmpty()) "\\" else "\\" + dirParts.joinToString("\\")

        val client = SMBClient(
            SmbConfig.builder()
                .withTimeout(10, TimeUnit.SECONDS)
                .build()
        )
        return try {
            val connection = client.connect(host)
            try {
                val session = connection.authenticate(AuthenticationContext(user, pass.toCharArray(), ""))
                val share = session.connectShare(shareName) as? DiskShare ?: return emptyList()
                try {
                    val names = share.list(smbDir).map { it.fileName }
                    SubtitleMatchLogic.buildSubtitlePairs(videoUri, fileName, names)
                } finally {
                    runCatching { share.close() }
                }
            } finally {
                runCatching { connection.close() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SMB scan failed: ${e.message}")
            emptyList()
        } finally {
            runCatching { client.close() }
        }
    }

    // ---------- NFS ----------

    private fun scanNfs(videoUri: String, uri: Uri): List<Pair<String, String>> {
        val host = uri.host ?: return emptyList()
        val path = uri.path ?: return emptyList()
        val colonIndex = path.indexOf(':', 1)
        if (colonIndex <= 0) return emptyList()

        val exportedPath = path.substring(1, colonIndex)
        val pathWithinExport = path.substring(colonIndex + 1)
        val fileName = pathWithinExport.substringAfterLast('/')
        val dirPath = pathWithinExport.substringBeforeLast('/').ifEmpty { "/" }

        return try {
            val client = Nfs3(host, exportedPath, CredentialUnix(), 3)
            val dir = Nfs3File(client, dirPath)
            if (!dir.exists() || !dir.isDirectory) return emptyList()
            val names = dir.listFiles()?.filterNotNull()?.map { it.name } ?: emptyList()
            SubtitleMatchLogic.buildSubtitlePairs(videoUri, fileName, names)
        } catch (e: Exception) {
            Log.w(TAG, "NFS scan failed: ${e.message}")
            emptyList()
        }
    }

    // ---------- FTP ----------

    private fun scanFtp(videoUri: String, uri: Uri): List<Pair<String, String>> {
        val host = uri.host ?: return emptyList()
        val port = if (uri.port != -1) uri.port else 21
        val (user, pass) = parseUserInfo(uri, "anonymous", "")
        val path = uri.path ?: return emptyList()
        val fileName = path.substringAfterLast('/')
        val dirPath = path.substringBeforeLast('/').ifEmpty { "/" }

        val client = FTPClient()
        client.controlEncoding = "UTF-8"
        client.connectTimeout = 15_000
        return try {
            client.connect(host, port)
            if (!FTPReply.isPositiveCompletion(client.replyCode)) return emptyList()
            if (!client.login(user, pass)) return emptyList()
            client.enterLocalPassiveMode()
            val names = client.listFiles(dirPath)?.map { it.name } ?: emptyList()
            SubtitleMatchLogic.buildSubtitlePairs(videoUri, fileName, names)
        } catch (e: Exception) {
            Log.w(TAG, "FTP scan failed: ${e.message}")
            emptyList()
        } finally {
            runCatching { client.logout() }
            runCatching { client.disconnect() }
        }
    }

    // ---------- WebDAV ----------

    private fun scanWebDav(videoUri: String, uri: Uri): List<Pair<String, String>> {
        val (user, pass) = parseUserInfo(uri, "", "")
        val path = uri.path ?: return emptyList()
        val fileName = path.substringAfterLast('/')
        val dirPath = path.substringBeforeLast('/').ifEmpty { "/" }
        val dirUrl = buildCleanDirUrl(uri, dirPath) ?: return emptyList()

        return try {
            val sardine = OkHttpSardine()
            if (user.isNotBlank() || pass.isNotBlank()) {
                sardine.setCredentials(user, pass)
            }
            val names = sardine.list(dirUrl)
                .map { it.name }
                .filter { it != "." && it != ".." }
            SubtitleMatchLogic.buildSubtitlePairs(videoUri, fileName, names)
        } catch (e: Exception) {
            Log.w(TAG, "WebDAV scan failed: ${e.message}")
            emptyList()
        }
    }

    // ---------- HTTP（Nginx 风格目录页） ----------

    private fun scanHttp(videoUri: String, uri: Uri): List<Pair<String, String>> {
        val path = uri.path ?: return emptyList()
        val fileName = path.substringAfterLast('/')
        val dirPath = path.substringBeforeLast('/').ifEmpty { "/" }
        val dirUrl = buildCleanDirUrl(uri, dirPath) ?: return emptyList()

        return try {
            val names = listHttpDirNames(dirUrl)
            SubtitleMatchLogic.buildSubtitlePairs(videoUri, fileName, names)
        } catch (e: Exception) {
            Log.w(TAG, "HTTP scan failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * 抓取 HTTP 目录页并列出其中的条目名。
     *
     * HTML 解析复用 [FileBrowserLogic.parseHttpDirectoryListing]（Nginx autoindex / Apache 目录列表），
     * 不再自己写一套正则：好处是同名条目去重、`../` 与锚点过滤、百分号解码容错、
     * 以及「只认当前目录子树下的链接」这几条口径与文件浏览页完全一致。
     */
    private fun listHttpDirNames(dirUrl: String): List<String> {
        val request = Request.Builder().url(dirUrl).get().build()
        val response = HTTP_CLIENT.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val html = response.body?.string() ?: return emptyList()

        // baseUrl 必须带结尾 '/'：否则相对链接会被当作「替换最后一段」来解析
        // （/movies + 影片.srt → /影片.srt），同目录条目会被「子树校验」全部丢掉。
        val baseUrl = if (dirUrl.endsWith('/')) dirUrl else "$dirUrl/"
        return FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name }
    }

    // ---------- 公共工具 ----------

    /**
     * 解析 URI 中的 user:pass。
     */
    private fun parseUserInfo(uri: Uri, defaultUser: String, defaultPass: String): Pair<String, String> {
        val userInfo = uri.userInfo
        if (userInfo.isNullOrEmpty()) return defaultUser to defaultPass
        val parts = userInfo.split(":", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
    }

    /**
     * 构造用于列目录的干净 URL（不含 userInfo，凭证走各自客户端）。
     */
    private fun buildCleanDirUrl(uri: Uri, dirPath: String): String? {
        val host = uri.host ?: return null
        val authority = host + (if (uri.port != -1) ":${uri.port}" else "")
        return Uri.Builder()
            .scheme(uri.scheme)
            .encodedAuthority(authority) // 使用 encodedAuthority 避免对端口冒号进行二次编码
            .path(dirPath)
            .build()
            .toString()
    }

    private val HTTP_CLIENT: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
