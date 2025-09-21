package org.mz.mzdkplayer.tool

// SmbUtils.kt

import android.annotation.SuppressLint
import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okio.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SmbUtils {

    /**
     * 根据完整的 SMB URI 打开文件并返回 InputStream
     * 示例 URI: smb://user:pass@host/share/path/to/file.xml
     */
    @Throws(IOException::class)
    suspend fun openSmbFileInputStream(smbUri: Uri): InputStream {
        return withContext(Dispatchers.IO) {
            val host = smbUri.host ?: throw IOException("Invalid SMB URI: no host")
            val path = smbUri.path ?: throw IOException("Invalid SMB URI: no path")

            // 提取 share 名称（第一个路径段）
            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            if (pathSegments.isEmpty()) {
                throw IOException("Invalid SMB URI: no share or path")
            }
            val shareName = pathSegments[0]
            val filePath = pathSegments.drop(1).joinToString("/") // 剩余部分为文件路径

            // 从 URI 获取用户凭证（注意：明文密码不安全，仅用于演示）
            val userInfo = smbUri.userInfo
            val username = userInfo?.split(":")?.getOrNull(0) ?: "guest"
            val password = userInfo?.split(":")?.getOrNull(1) ?: ""
            val domain = "" // 可根据需要扩展

            val client = SMBClient()
            var connection: Connection? = null
            var session: Session? = null
            var share: DiskShare? = null
            var file: com.hierynomus.smbj.share.File? = null

            try {
                connection = client.connect(host)
                val authContext = AuthenticationContext(username, password.toCharArray(), domain)
                session = connection.authenticate(authContext)
                share = session.connectShare(shareName) as DiskShare

                file = share.openFile(
                    filePath,
                    setOf(AccessMask.GENERIC_READ),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )

                return@withContext file.inputStream.also {
                    // 注意：调用者负责关闭 InputStream，它会级联关闭 file/share/session/connection
                }
            } catch (e: Exception) {
                // 清理资源
                file?.close()
                share?.close()
                session?.close()
                connection?.close()
                throw IOException("Failed to open SMB file: $smbUri", e)
            }
        }
    }
    /**
     * 根据完整的 WebDAV URI 打开文件并返回 InputStream
     * 示例 URI: https://user:pass@host:port/path/to/file.mkv
     *
     * 注意：此函数会创建一个新的 Sardine 实例，并配置为忽略 SSL 证书。
     * 调用者负责在使用完毕后关闭返回的 InputStream。
     */
    @Throws(IOException::class)
    suspend fun openWebDavFileInputStream(webDavUri: Uri): InputStream {
         return withContext(Dispatchers.IO) {
            // 1. 解析 URI
            val scheme = webDavUri.scheme ?: throw IOException("Invalid WebDAV URI: no scheme")
            if (scheme != "http" && scheme != "https") {
                throw IOException("Invalid WebDAV URI scheme: $scheme")
            }
            val host = webDavUri.host ?: throw IOException("Invalid WebDAV URI: no host")
            val port = webDavUri.port.takeIf { it != -1 } ?: if (scheme == "https") 443 else 80
            val path = webDavUri.path ?: throw IOException("Invalid WebDAV URI: no path")
            if (path.isEmpty() || path == "/") {
                throw IOException("Invalid WebDAV URI: path is empty or root")
            }

            // 2. 提取用户凭证
            val userInfo = webDavUri.userInfo
            val (username, password) = if (!userInfo.isNullOrEmpty()) {
                val parts = userInfo.split(":", limit = 2)
                if (parts.size != 2) {
                    throw IOException("Invalid WebDAV URI: malformed userInfo")
                }
                parts[0] to parts[1]
            } else {
                // 如果 URI 中没有用户信息，可能需要从其他地方获取或抛出异常
                // 这里我们选择抛出异常，因为函数要求 URI 是完整的
                throw IOException("Invalid WebDAV URI: no userInfo (username:password)")
            }

            // 3. 构建基础 URL (不包含文件路径)
            val baseUrl = "$scheme://$host:$port"

            // 4. 配置忽略 SSL 的 OkHttpClient (与 connectToWebDav 逻辑一致)
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, SecureRandom())
            }

            val okHttpClient = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // 忽略主机名验证
                .build()

            // 5. 创建临时 Sardine 实例
            val tempSardine: Sardine = OkHttpSardine(okHttpClient)

            // 6. 设置认证信息
            tempSardine.setCredentials(username, password)

            // 7. 构建完整的文件 URL
            // 注意：webDavUri.toString() 已经是完整的 URL，可以直接使用
            // 但为了清晰和可能的路径处理，我们也可以这样构建：
            // val fullFileUrl = "$baseUrl$path"
            val fullFileUrl = webDavUri.toString()

            try {
                // 8. 使用 Sardine.get() 获取 InputStream
                // Sardine.get() 返回的是 InputStream，通常内部会处理连接和 Range 请求
                val inputStream = tempSardine.get(fullFileUrl)

                // 注意：Sardine 实例 tempSardine 在这里没有显式关闭。
                // Sardine 接口没有 close() 方法。它的资源管理主要依赖于
                // OkHttpClient 的连接池和返回的 InputStream 的关闭。
                // 当 inputStream.close() 被调用时，底层连接应该会被正确释放回连接池或关闭。
                // 如果未来 Sardine 实现有变化，可能需要调整。

                // 返回 InputStream 给调用者，调用者负责关闭它。
                return@withContext inputStream

            } catch (e: Exception) {
                // 捕获 Sardine 相关异常（如网络错误、认证失败、文件不存在等）
                // 并将其包装为 IOException 抛出
                throw IOException("Failed to open WebDAV file input stream for URL: $fullFileUrl", e)
            }
            // 注意：tempSardine 实例在此处超出作用域，会被垃圾回收。
            // 它的 OkHttpClient 也会被垃圾回收，其连接池中的空闲连接最终也会被清理。
        }
    }



    /**
     * 根据视频的 SMB URI，构造同名弹幕 XML 文件的 SMB URI
     * 例如: smb://host/share/movie.mp4 → smb://host/share/movie.xml
     */
    fun getDanmakuSmbUri(videoSmbUri: Uri): Uri {
        val videoPath = videoSmbUri.path ?: throw IllegalArgumentException("Invalid video URI")
        val basePath = videoPath.substringBeforeLast(".", "")
        val danmakuPath = "$basePath.xml"

        return videoSmbUri.buildUpon().path(danmakuPath).build()
    }
}