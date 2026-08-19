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
import java.util.regex.Pattern
import androidx.core.net.toUri

/**
 * 同名字幕扫描器：根据视频 URI 扫描其所在目录，找出与视频同名的字幕文件。
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

    // 常见字幕文件扩展名（统一小写）
//    private val SUBTITLE_EXTENSIONS = setOf(
//        "srt", "ass", "ssa", "vtt", "sub", "idx", "smi", "sami",
//        "scc", "ttml", "dfxp", "stl", "lrc", "sup", "pgs", "mks"
//    )
     private val SUBTITLE_EXTENSIONS = setOf(
        "srt", "ass", "ssa", "vtt", "sub",
        "sup", "pgs",
    )
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
            f.isFile && isSameNameSubtitle(f.name, videoFile.name)
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
                    buildSubtitlePairs(videoUri, fileName, names)
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
            buildSubtitlePairs(videoUri, fileName, names)
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
            buildSubtitlePairs(videoUri, fileName, names)
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
            buildSubtitlePairs(videoUri, fileName, names)
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
            buildSubtitlePairs(videoUri, fileName, names)
        } catch (e: Exception) {
            Log.w(TAG, "HTTP scan failed: ${e.message}")
            emptyList()
        }
    }

    private fun listHttpDirNames(dirUrl: String): List<String> {
        val request = Request.Builder().url(dirUrl).get().build()
        val response = HTTP_CLIENT.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val html = response.body?.string() ?: return emptyList()

        val pattern = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        val names = mutableListOf<String>()
        while (matcher.find()) {
            var href = matcher.group(1) ?: continue
            href = java.net.URLDecoder.decode(href, "UTF-8")
            if (href.startsWith("#") || href.startsWith("javascript:")) continue
            val name = href.trimEnd('/').substringAfterLast('/')
            if (name.isNotBlank() && name != "." && name != "..") names.add(name)
        }
        return names.distinct()
    }

    // ---------- 公共工具 ----------

    /**
     * 判断文件名是否为「视频名」的同名字幕。
     * 规则：以「视频名.」开头（覆盖 .srt / .chs.srt / .国语.ass 等变体），且扩展名属于字幕集合，且不等于视频自身。
     */
    private fun isSameNameSubtitle(candidateName: String, videoName: String): Boolean {
        val baseName = videoName.substringBeforeLast('.', videoName)
        if (baseName.isBlank()) return false
        if (candidateName == videoName) return false
        if (!candidateName.startsWith("$baseName.")) return false
        val ext = candidateName.substringAfterLast('.', "").lowercase()
        return ext in SUBTITLE_EXTENSIONS
    }

    /**
     * 从目录文件名列表里筛出同名字幕，并拼成 (字幕URI, 文件名) 列表。
     */
    private fun buildSubtitlePairs(
        videoUri: String,
        videoName: String,
        dirNames: List<String>
    ): List<Pair<String, String>> {
        val matched = dirNames
            .filter { isSameNameSubtitle(it, videoName) }
            .distinct()
            .sorted()
        val prefix = dirPrefixOf(videoUri)
        return matched.map { prefix + it to it }
    }

    /**
     * 提取视频 URI 的目录前缀（保留协议/凭证/host/目录，以 / 结尾）。
     */
    private fun dirPrefixOf(videoUri: String): String {
        val idx = videoUri.lastIndexOf('/')
        return if (idx >= 0) videoUri.substring(0, idx + 1) else ""
    }

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
            .authority(authority)
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
