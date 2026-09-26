package org.mz.mzdkplayer.tool

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3
import com.emc.ecs.nfsclient.rpc.CredentialUnix
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 解析媒体文件自身的「文件时间」（即文件的最后修改时间，毫秒时间戳）。
 *
 * 只做只读查询，不修改任何文件；任何协议解析失败都返回 null，
 * 由调用方决定降级展示（例如不显示时间）。
 *
 * 支持的协议：LOCAL / SMB / FTP / NFS / HTTP / WEBDAV
 */
object FileTimeResolver {

    private const val TAG = "FileTimeResolver"

    /** 网络类协议的连接与读取超时，避免异常网络拖慢播放界面 */
    private const val TIMEOUT_MS = 8_000

    /**
     * 解析文件时间。
     *
     * @param mediaUri 播放地址（例如完整 SMB / FTP / 本地路径）
     * @param dataSourceType 数据源类型（SMB / LOCAL / FTP / NFS / HTTP / WEBDAV）
     * @return 文件最后修改时间（毫秒），拿不到时返回 null
     */
    suspend fun resolve(mediaUri: String, dataSourceType: String): Long? {
        if (mediaUri.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                when (dataSourceType.uppercase(Locale.ROOT)) {
                    "LOCAL" -> localFileTime(mediaUri)
                    "SMB" -> smbFileTime(mediaUri)
                    "FTP" -> ftpFileTime(mediaUri)
                    "NFS" -> nfsFileTime(mediaUri)
                    "WEBDAV", "HTTP", "HTTPS" -> httpFileTime(mediaUri)
                    // 类型缺失或未知时退化成按 scheme 猜测
                    else -> byScheme(mediaUri)
                }?.takeIf { it > 0L }
            } catch (e: Exception) {
                Log.w(TAG, "解析文件时间失败: $dataSourceType $mediaUri", e)
                null
            }
        }
    }

    private fun byScheme(mediaUri: String): Long? = when {
        mediaUri.startsWith("smb://", true) -> smbFileTime(mediaUri)
        mediaUri.startsWith("ftp://", true) -> ftpFileTime(mediaUri)
        mediaUri.startsWith("nfs://", true) -> nfsFileTime(mediaUri)
        mediaUri.startsWith("http://", true) || mediaUri.startsWith("https://", true) ->
            httpFileTime(mediaUri)

        else -> localFileTime(mediaUri)
    }

    // ==================== 本地文件 ====================

    private fun localFileTime(mediaUri: String): Long? {
        val path = when {
            mediaUri.startsWith("file://", true) -> Uri.parse(mediaUri).path
            mediaUri.startsWith("content://", true) -> null // content 协议无法直接取到文件时间
            else -> mediaUri
        } ?: return null
        return runCatching { File(path).lastModified() }.getOrNull()?.takeIf { it > 0L }
    }

    // ==================== SMB ====================

    private fun smbFileTime(mediaUri: String): Long? {
        val uri = Uri.parse(mediaUri)
        val host = uri.host ?: return null
        val segments = (uri.path ?: return null).split("/").filter { it.isNotEmpty() }
        if (segments.size < 2) return null

        val shareName = segments[0]
        val filePath = segments.drop(1).joinToString("/")
        val userInfo = uri.userInfo
        val username = userInfo?.substringBefore(':') ?: "guest"
        val password = userInfo?.substringAfter(':', "") ?: ""

        var client: SMBClient? = null
        var connection: Connection? = null
        var session: Session? = null
        var share: DiskShare? = null
        return try {
            val config = SmbConfig.builder()
                .withTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()
            client = SMBClient(config)
            connection = client.connect(host)
            session = connection.authenticate(
                AuthenticationContext(username, password.toCharArray(), null)
            )
            share = session.connectShare(shareName) as? DiskShare ?: return null
            share.getFileInformation(filePath)?.basicInformation?.lastWriteTime?.toEpochMillis()
        } finally {
            runCatching { share?.close() }
            runCatching { session?.close() }
            runCatching { connection?.close() }
            runCatching { client?.close() }
        }
    }

    // ==================== FTP ====================

    private fun ftpFileTime(mediaUri: String): Long? {
        val uri = Uri.parse(mediaUri)
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it != -1 } ?: 21
        val path = uri.path ?: return null
        val (username, password) = FileTimeParse.credentials(uri.userInfo, "anonymous")

        val ftpClient = FTPClient()
        ftpClient.controlEncoding = "UTF-8"
        ftpClient.connectTimeout = TIMEOUT_MS
        ftpClient.defaultTimeout = TIMEOUT_MS
        return try {
            ftpClient.connect(host, port)
            if (!ftpClient.login(username, password)) {
                Log.w(TAG, "FTP 登录失败: $host")
                return null
            }
            ftpClient.enterLocalPassiveMode()
            ftpClient.mlistFile(path)?.timestamp?.timeInMillis
        } finally {
            runCatching { ftpClient.logout() }
            runCatching { ftpClient.disconnect() }
        }
    }

    // ==================== NFS ====================

    /**
     * NFS 地址格式: nfs://<host>:<exported_path>:<path_within_export>
     * 例如: nfs://192.168.1.4:/fs/1000/nfs:/movies/movie.mkv
     */
    private fun nfsFileTime(mediaUri: String): Long? {
        val uri = Uri.parse(mediaUri)
        val server = uri.host ?: return null
        val path = uri.path ?: return null
        val (exportedPath, nfsFilePath) = FileTimeParse.splitNfsPath(path) ?: return null

        val client = Nfs3(server, exportedPath, CredentialUnix(), 3)
        val file = Nfs3File(client, nfsFilePath)
        return file.getAttributes()?.mtime?.timeInMillis
    }

    // ==================== HTTP / WebDAV ====================

    private fun httpFileTime(mediaUri: String): Long? {
        val uri = Uri.parse(mediaUri)
        if (uri.host.isNullOrEmpty()) return null

        val userInfo = uri.userInfo
        val port = uri.port.takeIf { it != -1 }?.let { ":$it" } ?: ""
        val query = uri.encodedQuery?.let { "?$it" } ?: ""
        val cleanUrl = "${uri.scheme}://${uri.host}$port${uri.encodedPath}$query"

        // 优先 HEAD，失败再退化为只取 1 字节的 GET
        lastModifiedOf(cleanUrl, userInfo, head = true)?.let { return it }
        return lastModifiedOf(cleanUrl, userInfo, head = false)
    }

    private fun lastModifiedOf(url: String, userInfo: String?, head: Boolean): Long? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as? HttpURLConnection) ?: return null
            connection.requestMethod = if (head) "HEAD" else "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (!userInfo.isNullOrEmpty()) {
                val token = Base64.encodeToString(userInfo.toByteArray(), Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $token")
            }
            if (!head) connection.setRequestProperty("Range", "bytes=0-0")

            val header = connection.getHeaderField("Last-Modified")
                ?: connection.getHeaderField("last-modified")
            if (!head) {
                // GET 只需响应头，读完 1 字节即可断开
                runCatching { connection.inputStream?.close() }
            }
            parseHttpDate(header)
        } catch (e: Exception) {
            Log.d(TAG, "读取 HTTP 文件时间失败: $url (head=$head) ${e.message}")
            null
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private fun parseHttpDate(value: String?): Long? = FileTimeParse.parseHttpDate(value)
}
