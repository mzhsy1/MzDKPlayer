package org.mz.mzdkplayer.tool

// SmbUtils.kt

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3
import com.emc.ecs.nfsclient.rpc.CredentialUnix
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
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.core.net.toUri
import okhttp3.Request
import okhttp3.Response

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
     * 根据完整的 FTP URI 打开文件并返回 InputStream
     * 示例 URI: ftp://user:pass@host:port/path/to/file.xml
     */
    @Throws(IOException::class)
    suspend fun openFtpFileInputStream(ftpUri: Uri): InputStream {
        return withContext(Dispatchers.IO) {
            val host = ftpUri.host ?: throw IOException("Invalid FTP URI: no host")
            val port = ftpUri.port.takeIf { it != -1 } ?: 21 // 默认 FTP 端口
            val path = ftpUri.path ?: throw IOException("Invalid FTP URI: no path")

            // 从 URI 获取用户凭证
            val userInfo = ftpUri.userInfo
            val username = userInfo?.split(":")?.getOrNull(0) ?: "anonymous"
            val password = userInfo?.split(":")?.getOrNull(1) ?: ""

            val ftpClient = FTPClient()
            // 应用优化：设置编码、缓冲区大小和超时
            ftpClient.controlEncoding = "UTF-8"
            ftpClient.bufferSize = 8192 // BUFFER_SIZE
            ftpClient.connectTimeout = 10000 // CONNECTION_TIMEOUT_MS
            // ftpClient.soTimeout = 10000 // SOCKET_TIMEOUT_MS (可选)

            var success = false // 标记是否成功登录
            var inputStream: InputStream? = null
            try {
                // 连接到服务器
                ftpClient.connect(host, port)

                // 登录
                val loginSuccess = ftpClient.login(username, password)
                if (!loginSuccess) {
                    throw IOException("FTP 登录失败 for user: $username")
                }
                success = true // 标记登录成功

                // 设置传输模式和被动模式
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
                ftpClient.enterLocalPassiveMode()

                // 打开文件输入流
                inputStream = ftpClient.retrieveFileStream(path)
                if (inputStream == null) {
                    throw IOException("无法打开 FTP 文件输入流: $path. 服务器回复: ${ftpClient.replyString}")
                }

                // 返回一个包装的 InputStream，在关闭时也处理 FTP 连接
                object : InputStream() {
                    private var isClosed = false // 防止重复关闭

                    override fun read(): Int {
                        check(!isClosed) { "Stream is closed" }
                        return inputStream.read()
                    }

                    override fun read(b: ByteArray): Int {
                        check(!isClosed) { "Stream is closed" }
                        return inputStream.read(b)
                    }

                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        check(!isClosed) { "Stream is closed" }
                        return inputStream.read(b, off, len)
                    }

                    override fun close() {
                        if (isClosed) return
                        isClosed = true

                        var completeOk = false
                        var inputStreamException: Throwable? = null
                        var completeCommandException: Throwable? = null
                        var logoutException: Throwable? = null
                        var disconnectException: Throwable? = null

                        try {
                            // 1. 首先关闭输入流
                            try {
                                inputStream.close()
                            } catch (e: Exception) {
                                inputStreamException = e
                            }

                            // 2. 然后完成 FTP 命令序列
                            try {
                                completeOk = ftpClient.completePendingCommand()
                                if (!completeOk) {
                                    // 可以记录警告日志，但不一定抛异常
                                    // Log.w("FTP", "completePendingCommand failed: ${ftpClient.replyString}")
                                }
                            } catch (e: Exception) {
                                completeCommandException = e
                                // Log.e("FTP", "Exception in completePendingCommand", e)
                            }

                            // 3. 最后注销和断开连接
                            try {
                                if (success) { // 只有成功登录才尝试注销
                                    ftpClient.logout()
                                }
                            } catch (e: Exception) {
                                logoutException = e
                                // Log.w("FTP", "Exception during logout", e)
                            }

                            try {
                                if (ftpClient.isConnected) {
                                    ftpClient.disconnect()
                                }
                            } catch (e: Exception) {
                                disconnectException = e
                                // Log.w("FTP", "Exception during disconnect", e)
                            }

                            // 如果关闭输入流或完成命令时发生严重错误，则抛出
                            if (inputStreamException != null) {
                                throw IOException("Error closing FTP input stream", inputStreamException)
                            }
                            if (completeCommandException != null) {
                                // 如果 completePendingCommand 失败，可能连接已损坏，但仍需确保连接关闭
                                // 这里选择将它作为警告处理，除非它导致了关键问题
                                // 或者，如果这是关键错误，可以抛出：
                                // throw IOException("Error completing FTP command", completeCommandException)
                                // 当前选择记录但不中断调用者（假设数据已读取完毕）
                                // Log.w("FTP", "FTP command completion error (ignoring if data read OK)", completeCommandException)
                            }


                        } finally {
                            // 确保所有资源尝试都被释放，即使前面有异常
                            // 注意：FTPClient 的 logout 和 disconnect 通常内部有 isConnected 检查，
                            // 但我们显式检查以避免不必要的调用或潜在问题。
                            // 上面的 try-catch 已经处理了这些，这里的 finally 主要是为了极端情况下的兜底。
                            // 在当前结构下，这里的 finally 可能是多余的，因为上面已经处理了。
                            // 保留注释说明意图。
                        }
                    }
                }

            } catch (e: Exception) {
                // 在初始化或打开流阶段发生异常，需要清理资源
                try {
                    inputStream?.close() // 尝试关闭可能已打开的流
                } catch (closeEx: Exception) {
                    // 忽略关闭异常
                }

                if (success) { // 只有成功登录才尝试注销
                    try {
                        ftpClient.logout()
                    } catch (logoutEx: Exception) {
                        // 忽略注销异常
                    }
                }
                try {
                    if (ftpClient.isConnected) {
                        ftpClient.disconnect()
                    }
                } catch (disconnectEx: Exception) {
                    // 忽略断开异常
                }
                throw IOException("Failed to open FTP file: $ftpUri", e)
            }
        }
    }

    /**
     * 根据完整的 NFS URI 打开文件并返回 InputStream
     * 示例 URI: nfs://host:/exported_path:path_within_export
     * 例如: nfs://192.168.1.4:/fs/1000/nfs:/xml/danmaku.xml
     * 注意: URI 格式解析依赖于 NFSDataSource 中的逻辑
     */
    @Throws(IOException::class)
    suspend fun openNfsFileInputStream(nfsUri: Uri): InputStream {
        return withContext(Dispatchers.IO) {
            // --- 解析 NFS URI ---
            if (nfsUri.scheme?.lowercase() != "nfs") {
                throw IOException("Invalid NFS URI scheme: ${nfsUri.scheme}")
            }

            val serverAddress = nfsUri.host ?: throw IOException("Invalid NFS URI: no host")
            val path = nfsUri.path ?: throw IOException("Invalid NFS URI: no path")

            // --- 修正 URI 解析逻辑 ---
            // 从 path 中解析 exported_path 和 path_within_export
            // path 格式应为: /<exported_path>:<path_within_export>
            if (!path.startsWith("/")) {
                throw IOException("Invalid NFS URI path: '$path'. Must start with '/'.")
            }
            val colonIndexInPath = path.indexOf(':', 1) // 从索引1开始查找，确保不是开头的斜杠
            if (colonIndexInPath == -1) {
                throw IOException("Invalid NFS URI path: '$path'. Missing colon ':' separating exported_path and path_within_export.")
            }

            // exported_path 是从第一个 '/' 到第一个冒号 ':' 之间的部分
            val exportedPath = path.substring(1, colonIndexInPath) // substring(1, ...) 排除开头的 '/'
            if (exportedPath.isEmpty()) {
                throw IOException("Invalid NFS URI path: '$path'. exported_path is empty.")
            }

            // path_within_export 是冒号 ':' 之后的部分
            val pathWithinExport = path.substring(colonIndexInPath + 1)
            if (pathWithinExport.isEmpty()) {
                Log.w("openNfsFileInputStream", "Warning: NFS URI path: '$path'. path_within_export is empty. Using root path '/'.")
                // 可以选择抛出异常或使用根路径，这里选择使用根路径
            }
            // --- URI 解析逻辑修正结束 ---

            Log.d("openNfsFileInputStream", "Connecting to NFS server: $serverAddress, export: $exportedPath")
            Log.d("openNfsFileInputStream", "Opening file path: $pathWithinExport")

            // 准备认证信息 (使用默认 UID/GID 0)
            val credential = CredentialUnix()

            var nfsClient: Nfs3? = null
            var nfsFile: Nfs3File? = null

            try {
                // 创建 NFS 客户端并连接/挂载
                val client = Nfs3(serverAddress, exportedPath, credential, 3)
                nfsClient = client

                // 构造 NFS 文件路径 (相对于挂载点)
                val nfsFilePath = if (pathWithinExport.startsWith("/")) {
                    pathWithinExport
                } else {
                    "/$pathWithinExport"
                }

                // 打开 NFS 文件
                val file = Nfs3File(client, nfsFilePath)
                //file.read()
                if (!file.exists()) {
                    throw IOException("NFS file does not exist: $nfsFilePath")
                }
                if (!file.isFile) {
                    throw IOException("NFS path is not a file: $nfsFilePath")
                }
                nfsFile = file

                // 直接返回文件的 InputStream
                // 注意: Nfs3File 可能没有直接的 inputStream 属性。
                // 需要根据 NFS 客户端库的实际 API 来实现。
                // 如果库不直接提供，可能需要创建一个包装类。
                // 但根据你的要求，假设库提供了类似的方法或我们可以创建一个。
                // 例如，如果库提供了一个方法来获取一个 InputStream 包装器。
                // 这里我们直接使用 file.inputStream (假设存在或通过其他方式创建)
                // 如果库没有直接提供，可能需要像之前那样创建一个基于 Nfs3File 的 InputStream 包装器。
                // 但根据你的简化要求和对 SMB 的类比，我们尝试直接返回。
                // 由于 Nfs3File 本身通常不直接是 InputStream，我们需要创建一个。
                // 这里提供一个基于 Nfs3File.read 方法的简单包装。
                // 这个包装器需要实现 InputStream 的 read 方法，内部调用 file.read(offset, length, buffer, offset_in_buffer)
                // 为了简化，我们创建一个匿名内部类 InputStream，持有 file 引用。

                // 创建一个基于 Nfs3File 的简单 InputStream 包装器
                val inputStream = object : InputStream() {
                    private var filePointer: Long = 0
                    private val fileLength = file.length()
                    private var closed = false // 添加关闭标志

                    override fun read(): Int {
                        if (closed) throw IOException("Stream is closed")
                        if (filePointer >= fileLength) return -1 // EOF

                        val buffer = ByteArray(1)
                        val response = file.read(filePointer, 1, buffer, 0)
                        val bytesRead = response.bytesRead
                        if (bytesRead <= 0) {
                            return -1 // EOF or error
                        }
                        filePointer++
                        return buffer[0].toInt() and 0xFF // Convert byte to int, masking sign
                    }

                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (closed) throw IOException("Stream is closed")
                        if (off < 0 || len < 0 || off + len > b.size) {
                            throw IndexOutOfBoundsException()
                        }
                        if (len == 0) return 0
                        if (filePointer >= fileLength) return -1 // EOF

                        val bytesToRead = minOf(len.toLong(), fileLength - filePointer).toInt()
                        if (bytesToRead <= 0) return -1

                        val response = file.read(filePointer, bytesToRead, b, off)
                        val bytesRead = response.bytesRead
                        if (bytesRead <= 0) {
                            return -1 // EOF or error
                        }
                        filePointer += bytesRead
                        return bytesRead
                    }

                    override fun skip(n: Long): Long {
                        if (closed) throw IOException("Stream is closed")
                        if (n <= 0) return 0
                        val bytesSkippable = minOf(n, fileLength - filePointer)
                        filePointer += bytesSkippable
                        return bytesSkippable
                    }

                    override fun available(): Int {
                        if (closed) throw IOException("Stream is closed")
                        return minOf(fileLength - filePointer, Int.MAX_VALUE.toLong()).toInt()
                    }

                    override fun close() {
                        if (!closed) {
                            closed = true
                            // 在流关闭时，标记资源需要被清理
                            // 实际清理发生在外部函数的 finally 块中
                            // 或者在这里直接尝试清理（如果 Nfs3File 有 close 方法）
                            // nfsFile?.close() // 如果 Nfs3File 有此方法
                            // nfsClient?.close() // 如果 Nfs3 有此方法
                            // 但通常依赖外部清理或 GC
                        }
                    }
                }

                return@withContext inputStream

            } catch (e: Exception) {
                // 清理资源
                try {
                    // 如果 Nfs3File 有 close 方法，尝试关闭它
                    // nfsFile?.close() // Uncomment if Nfs3File has a close method
                } catch (closeException: Exception) {
                    Log.w("openNfsFileInputStream", "Error closing NFS file during error handling", closeException)
                }
                try {
                    // Nfs3 客户端通常没有显式关闭方法，置为 null 即可
                    nfsClient = null
                } catch (closeException: Exception) {
                    Log.w("openNfsFileInputStream", "Error closing NFS client during error handling", closeException)
                }
                throw IOException("Failed to open NFS file: $nfsUri", e)
            }
        }
    }



    /**
     * 根据完整的 HTTP Link URL 打开 XML 弹幕文件并返回 InputStream
     * 示例 URL: http://host:port/path/to/danmaku.xml
     * @param xmlUrl HTTP Link 上 XML 文件的完整 URL
     * @param okHttpClient 用于发起请求的 OkHttpClient 实例
     * @return 用于读取 XML 文件的 InputStream
     * @throws IOException 如果连接失败或无法打开流
     */
    @Throws(IOException::class)
    suspend fun openHTTPLinkXmlInputStream(xmlUrl: String): InputStream {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val okHttpClient = OkHttpClient()
            val request = Request.Builder().url(xmlUrl).build()
            val call = okHttpClient.newCall(request)
            val response: okhttp3.Response = call.execute() // 同步执行，因为我们需要 Response 对象

            if (!response.isSuccessful) {
                throw IOException("HTTP error code: ${response.code}, message: ${response.message}")
            }

            val responseBody = response.body
            if (responseBody == null) {
                throw IOException("Response body is null for URL: $xmlUrl")
            }

            // 获取输入流
            val inputStream = responseBody.byteStream()

            // 返回一个包装的 InputStream，在关闭时也处理 HTTP 响应
            object : InputStream() {
                private var isClosed = false // 防止重复关闭

                override fun read(): Int {
                    check(!isClosed) { "Stream is closed" }
                    return inputStream.read()
                }

                override fun read(b: ByteArray): Int {
                    check(!isClosed) { "Stream is closed" }
                    return inputStream.read(b)
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    check(!isClosed) { "Stream is closed" }
                    return inputStream.read(b, off, len)
                }

                override fun close() {
                    if (isClosed) return
                    isClosed = true
                    // 关闭输入流，OkHttp 会自动管理连接的释放
                    inputStream.close()
                    // Response 对象在 inputStream 关闭后通常也应被关闭
                    // 虽然 byteStream() 后 body 可能已被消耗，但显式关闭是好习惯
                    response.close()
                }
            }
        }
    }


    /**
     * 根据视频的 video URI，构造同名弹幕 XML 文件的 SMB URI
     * 例如: smb://host/share/movie.mp4 → smb://host/share/movie.xml
     */
    fun getDanmakuSmbUri(videoSmbUri: Uri): Uri {
        val videoPath = videoSmbUri.path ?: throw IllegalArgumentException("Invalid video URI")
        val basePath = videoPath.substringBeforeLast(".", "")
        val danmakuPath = "$basePath.xml"

        return videoSmbUri.buildUpon().path(danmakuPath).build()
    }





    /**
     * 根据视频的 NFS video URI，构造同名弹幕 XML 文件的 NFS URI
     * NFS URI 格式: nfs://host:/exported_path:path_within_export
     * 例如: nfs://192.168.1.4:/fs/1000/nfs:/movies/movie.mp4
     *      -> nfs://192.168.1.4:/fs/1000/nfs:/movies/movie.xml
     * 注意: 修正了 Uri.buildUpon().path() 会编码冒号 ':' 的问题。
     */
    fun getDanmakuNfsUri(videoNfsUri: Uri): Uri {
        // 验证 scheme
        if (videoNfsUri.scheme?.lowercase() != "nfs") {
            throw IllegalArgumentException("Invalid NFS video URI scheme: ${videoNfsUri.scheme}")
        }

        val originalPath = videoNfsUri.path ?: throw IllegalArgumentException("Invalid NFS video URI: no path")

        // --- 修正 URI 解析逻辑 (与 openNfsFileInputStream 和 NFSDataSource 保持一致) ---
        if (!originalPath.startsWith("/")) {
            throw IllegalArgumentException("Invalid NFS video URI path: '$originalPath'. Must start with '/'.")
        }
        val colonIndexInPath = originalPath.indexOf(':', 1) // 从索引1开始查找
        if (colonIndexInPath == -1) {
            throw IllegalArgumentException("Invalid NFS video URI path: '$originalPath'. Missing colon ':' separating exported_path and path_within_export.")
        }

        // exported_path 是从第一个 '/' 到第一个冒号 ':' 之间的部分
        val exportedPath = originalPath.substring(1, colonIndexInPath)
        if (exportedPath.isEmpty()) {
            throw IllegalArgumentException("Invalid NFS video URI path: '$originalPath'. exported_path is empty.")
        }

        // path_within_export 是冒号 ':' 之后的部分
        val pathWithinExport = originalPath.substring(colonIndexInPath + 1)
        if (pathWithinExport.isEmpty()) {
            throw IllegalArgumentException("Invalid NFS video URI path: '$originalPath'. path_within_export is empty.")
        }
        // --- 解析逻辑结束 ---

        // 从 path_within_export 中提取基础路径和文件名
        val lastSlashIndex = pathWithinExport.lastIndexOf('/')
        val directoryPath = if (lastSlashIndex != -1) {
            pathWithinExport.substring(0, lastSlashIndex + 1) // 包含最后的 '/'
        } else {
            "" // 文件在导出根目录下
        }
        val fileName = if (lastSlashIndex != -1) {
            pathWithinExport.substring(lastSlashIndex + 1)
        } else {
            pathWithinExport // 整个 pathWithinExport 就是文件名
        }

        // 替换文件扩展名
        val baseName = fileName.substringBeforeLast(".", "")
        if (baseName.isEmpty()) {
            // 如果文件名没有扩展名前的部分（例如 ".xml" 或 "file"），则认为 baseName 为空是不合理的
            // 或者可以考虑直接在原文件名后加 .xml
            // 这里选择抛出异常，因为通常视频文件都有名称部分
            throw IllegalArgumentException("Invalid file name in NFS video URI path: '$fileName'. No name part before extension.")
        }
        val danmakuFileName = "$baseName.xml"

        // 组合新的 path_within_export
        val danmakuPathWithinExport = "$directoryPath$danmakuFileName"

        // 组合完整的 NFS 弹幕 URI 路径: /<exported_path>:<danmaku_path_within_export>
        val danmakuNfsPath = "/$exportedPath:$danmakuPathWithinExport"

        // 获取 host
        val host = videoNfsUri.host ?: throw IllegalArgumentException("Invalid NFS video URI: no host")

        // 手动构建最终的 URI 字符串，避免 Uri.Builder 对 ':' 进行编码
        val danmakuUriString = "nfs://$host:$danmakuNfsPath"

        // 使用 Uri.parse 解析构建好的字符串
        return danmakuUriString.toUri()
    }








}