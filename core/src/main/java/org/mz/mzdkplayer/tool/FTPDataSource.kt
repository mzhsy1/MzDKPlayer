package org.mz.mzdkplayer.tool

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * 优化后的 FTP 数据源
 * 核心改进：
 * 1. 使用 BufferedInputStream 包装原始流，大幅提升流式读取效率。
 * 2. 实现了 FTPClient 的静态复用 (Keep-Alive)。
 * 3. close() 时只关闭数据流并完成 Pending Command，不断开控制连接。
 */
@UnstableApi
class FtpDataSource : BaseDataSource(/* isNetwork= */ true) {

    companion object {
        private const val TAG = "FtpDataSource"

        // 应用层缓冲区：建议 1MB，减少频繁的网络 IO 请求
        private const val BUFFER_SIZE = 1024 * 1024
        private const val CONNECTION_TIMEOUT_MS = 30000

        // --- 全局静态缓存 ---
        private var cachedFtpClient: FTPClient? = null
        private var currentHost: String? = null
        private var currentUser: String? = null

        private val lock = Any()

        /**
         * 静态释放方法：供外部调用，彻底断开 FTP 连接
         */
        fun releaseGlobalResources() = synchronized(lock) {
            Log.i(TAG, "Releasing GLOBAL FTP resources...")
            try {
                if (cachedFtpClient?.isConnected == true) {
                    try {
                        cachedFtpClient?.logout()
                    } catch (ignored: Exception) {}
                    try {
                        cachedFtpClient?.disconnect()
                    } catch (ignored: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing global FTP resources", e)
            } finally {
                cachedFtpClient = null
                currentHost = null
                currentUser = null
            }
        }
    }

    // --- 实例变量 ---
    private var dataSpec: DataSpec? = null
    private var inputStream: InputStream? = null // 经过 Buffered 包装的流

    private var bytesRead: Long = 0
    private var bytesToRead: Long = 0
    private var opened = false

    private var startTimeMs: Long = 0L
    private var totalBytesTransferred: Long = 0L

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d(TAG, "Opening: ${dataSpec.uri}")
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            // 1. 获取/复用全局连接
            ensureGlobalConnection(dataSpec)

            // 2. 获取文件大小
            val fileLength = getFileLength(dataSpec.uri.path!!)
            val startPosition = dataSpec.position

            if (startPosition > fileLength) {
                throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            }

            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            // 3. 设置断点续传位置
            if (startPosition > 0) {
                cachedFtpClient?.restartOffset = startPosition
            }

            // 4. 打开数据流并进行缓冲包装 (核心优化)
            val rawInputStream = cachedFtpClient?.retrieveFileStream(dataSpec.uri.path)
            val replyCode = cachedFtpClient?.replyCode ?: 0

            if (rawInputStream == null) {
                throw IOException("无法打开 FTP 文件流。回复码: $replyCode")
            }

            // 使用 BufferedInputStream 减少小数据块读取对性能的影响
            inputStream = BufferedInputStream(rawInputStream, BUFFER_SIZE)

            opened = true
            transferStarted(dataSpec)

            startTimeMs = System.currentTimeMillis()
            return bytesToRead

        } catch (e: Exception) {
            // 发生错误，清理全局连接以便下次重试
            closeConnectionQuietly(forceReleaseGlobal = true)
            if (e is IOException) throw e
            throw IOException("Open error: ${e.message}", e)
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        }

        val bytesToReadNow = if (bytesToRead == C.LENGTH_UNSET.toLong()) {
            readLength
        } else {
            min(readLength.toLong(), bytesToRead - bytesRead).toInt()
        }
        if (bytesToReadNow <= 0) return C.RESULT_END_OF_INPUT

        val stream = inputStream ?: throw IOException("Stream is null")

        try {
            // 现在的 read 将直接从 BufferedInputStream 的内存缓冲中读取数据
            val bytesReadNow = stream.read(buffer, offset, bytesToReadNow)
            if (bytesReadNow == -1) {
                if (bytesToRead != C.LENGTH_UNSET.toLong()) throw EOFException()
                return C.RESULT_END_OF_INPUT
            }

            bytesRead += bytesReadNow
            totalBytesTransferred += bytesReadNow
            bytesTransferred(bytesReadNow)

            // 速度监控
            if (bytesReadNow > 0 && totalBytesTransferred % (2 * 1024 * 1024) == 0L) {
                val time = System.currentTimeMillis() - startTimeMs
                if (time > 0) {
                    val speed = (totalBytesTransferred * 1000.0 / time) / 1024 / 1024
                    Log.d(TAG, "FTP Speed: %.2f MB/s".format(speed))
                }
            }

            return bytesReadNow
        } catch (e: IOException) {
            throw IOException("Read error", e)
        }
    }

    private fun ensureGlobalConnection(dataSpec: DataSpec) = synchronized(lock) {
        val uri = dataSpec.uri
        val host = uri.host ?: throw IOException("Missing host")
        val port = if (uri.port != -1) uri.port else 21
        val (username, password) = parseUserInfo(uri)

        // 检查连接复用条件
        if (cachedFtpClient != null &&
            cachedFtpClient!!.isConnected &&
            currentHost == host &&
            currentUser == username) {

            try {
                if (cachedFtpClient!!.sendNoOp()) {
                    cachedFtpClient!!.setFileType(FTP.BINARY_FILE_TYPE)
                    cachedFtpClient!!.enterLocalPassiveMode()
                    return@synchronized
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cached connection stale, reconnecting...")
            }
        }

        releaseGlobalResources()

        Log.d(TAG, "Creating NEW FTP connection to $host")
        val client = FTPClient()
        client.controlEncoding = "UTF-8"
        client.bufferSize = BUFFER_SIZE // 设置底层 Socket 缓冲区
        client.connectTimeout = CONNECTION_TIMEOUT_MS

        client.connect(host, port)
        if (!FTPReply.isPositiveCompletion(client.replyCode)) {
            throw IOException("Connect failed: ${client.replyString}")
        }

        if (!client.login(username, password)) {
            throw IOException("Login failed: ${client.replyString}")
        }

        client.setFileType(FTP.BINARY_FILE_TYPE)
        client.enterLocalPassiveMode()

        cachedFtpClient = client
        currentHost = host
        currentUser = username
    }

    private fun getFileLength(path: String): Long {
        val client = cachedFtpClient ?: throw IOException("Client not connected")
        try {
            val mFile = client.mlistFile(path)
            if (mFile != null) return mFile.size

            val files = client.listFiles(path)
            if (files != null && files.isNotEmpty()) return files[0].size
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get file length via mlist/list", e)
        }
        throw IOException("Cannot determine file size for $path")
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        closeConnectionQuietly(forceReleaseGlobal = false)
        dataSpec = null
    }

    private fun closeConnectionQuietly(forceReleaseGlobal: Boolean) {
        try {
            // 关闭 BufferedInputStream 会同时关闭底层的原始数据流
            inputStream?.close()
        } catch (ignored: Exception) {}
        inputStream = null

        if (forceReleaseGlobal) {
            releaseGlobalResources()
        } else {
            // 尝试完成 FTP 事务以复用控制连接
            try {
                if (cachedFtpClient?.isConnected == true) {
                    if (!cachedFtpClient!!.completePendingCommand()) {
                        Log.w(TAG, "Failed to complete pending command, clearing connection.")
                        releaseGlobalResources()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error completing FTP command", e)
                releaseGlobalResources()
            }
        }
    }

    private fun parseUserInfo(uri: Uri): Pair<String, String> {
        val userInfo = uri.userInfo
        if (userInfo.isNullOrEmpty()) return Pair("anonymous", "")
        val parts = userInfo.split(":")
        return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(parts[0], "")
    }
}

@UnstableApi
class FtpDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return FtpDataSource()
    }
}