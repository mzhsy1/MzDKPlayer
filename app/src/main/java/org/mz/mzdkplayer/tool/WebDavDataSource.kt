package org.mz.mzdkplayer.tool

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.min

/**
 * 优化后的 WebDAV 数据源，借鉴了 Media3 HttpDataSource 的设计模式
 */
@UnstableApi
class WebDavDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 ---
    private var dataSpec: DataSpec? = null
    private var sardine: OkHttpSardine? = null
    private var inputStream: InputStream? = null

    // 借鉴 HttpDataSource 的状态管理
    private var bytesRead: Long = 0
    private var bytesToRead: Long = 0
    private var transferStarted: Boolean = false

    // 缓冲区管理
    private var readBuffer: ByteArray? = null
    private var bufferPosition: Int = 0
    private var bufferLimit: Int = 0
    private var bufferSize: Int = DEFAULT_BUFFER_SIZE_BYTES

    // 文件位置跟踪
    private var currentFileOffset: Long = 0

    // 状态管理
    private val opened = java.util.concurrent.atomic.AtomicBoolean(false)

    // 性能监控
    private var lastLogTime = 0L
    private var totalBytesReadFromNetwork = 0L
    private var totalReadTime = 0L
    private var numReads = 0

    // --- 配置参数 ---
    companion object {
        private const val TAG = "WebDavDataSource"
        private const val DEFAULT_BUFFER_SIZE_BYTES = 8 * 1024 * 1024 // 8MB 默认缓冲区大小
        private const val MAX_REDIRECTS = 5 // 最大重定向次数

        // 用于信任所有证书的 OkHttpClient
        private val webDavClient by  lazy{
            WebDavHttpClient.restrictedTrustOkHttpClient
        }
    }

    /**
     * 打开数据源，借鉴 HttpDataSource 的错误处理和状态管理
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d(TAG, "Opening: ${dataSpec.uri}")

        // 状态检查和初始化 - 类似 HttpDataSource
        if (!opened.compareAndSet(false, true)) {
            throw IOException("WebDavDataSource 已经被打开。")
        }

        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            // 建立连接和验证
            establishConnection(dataSpec)

            // 获取文件信息并验证范围
            val fileLength = getFileLength()
            val startPosition = dataSpec.position

            // 范围验证 - 类似 HttpDataSource 的 416 处理
            if (startPosition !in 0..fileLength) {
                closeConnectionQuietly()
                throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            }

            // 计算要读取的字节数
            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            // 验证计算后的长度
            if (bytesToRead < 0 || startPosition + bytesToRead > fileLength) {
                closeConnectionQuietly()
                throw IOException("无效的数据范围: position=$startPosition, length=$bytesToRead, fileSize=$fileLength")
            }

            // 初始化读取状态
            currentFileOffset = startPosition
            this.readBuffer = ByteArray(bufferSize)
            this.bufferPosition = 0
            this.bufferLimit = 0

            Log.i(TAG, "成功打开 WebDAV 文件, 大小: ${fileLength / 1024 / 1024}MB, " +
                    "起始位置: $startPosition, 读取长度: ${bytesToRead / 1024 / 1024}MB")

            // 标记传输开始 - 类似 HttpDataSource
            transferStarted = true
            transferStarted(dataSpec)

            return bytesToRead

        } catch (e: Exception) {
            closeConnectionQuietly()
            when (e) {
                is IOException -> throw e
                else -> throw IOException("打开 WebDAV 文件时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 建立 WebDAV 连接，包含更好的错误处理
     */
    @Throws(IOException::class)
    private fun establishConnection(dataSpec: DataSpec) {
        val uri = dataSpec.uri
        val urlString = uri.toString()

        if (urlString.isBlank()) {
            throw IOException("无效的 WebDAV URI: 空 URI")
        }

        // 从 URI 的 userInfo 部分提取凭证
        val (username, password) = uri.userInfo?.split(":")?.let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("", "")
        } ?: Pair("", "")

        try {
            // 初始化 Sardine 客户端
            sardine = OkHttpSardine(webDavClient)
            if (username.isNotBlank() || password.isNotBlank()) {
                sardine?.setCredentials(username, password)
            }
            Log.d(TAG, "Credentials: $username ***")

            // 构建干净的 URI（移除用户信息）
            val cleanUriString = buildCleanUri(uri)
            Log.d(TAG, "Clean URI: $cleanUriString")

            // 验证连接和文件存在性
            validateConnection(cleanUriString)

        } catch (e: Exception) {
            throw IOException("建立 WebDAV 连接时出错: ${e.message}", e)
        }
    }

    /**
     * 构建干净的 URI（移除用户信息）
     */
    private fun buildCleanUri(uri: Uri): String {
        return Uri.Builder()
            .scheme(uri.scheme)
            .encodedAuthority(uri.authority?.substringAfter('@') ?: uri.authority)
            .encodedPath(uri.encodedPath)
            .encodedQuery(uri.encodedQuery)
            .encodedFragment(uri.encodedFragment)
            .build()
            .toString()
    }

    /**
     * 验证连接和文件存在性
     */
    @Throws(IOException::class)
    private fun validateConnection(uri: String) {
        val davResources = sardine?.list(uri)
        if (davResources.isNullOrEmpty()) {
            throw IOException("无法获取文件信息或文件不存在: $uri")
        }
    }

    /**
     * 获取文件长度，包含错误处理
     */
    @Throws(IOException::class)
    private fun getFileLength(): Long {
        return try {
            val cleanUri = buildCleanUri(dataSpec?.uri!!)
            val davResources = sardine?.list(cleanUri)
                ?: throw IOException("无法获取文件信息")

            if (davResources.isEmpty()) {
                throw IOException("文件不存在")
            }

            davResources[0].contentLength
        } catch (e: Exception) {
            throw IOException("获取文件大小时出错: ${e.message}", e)
        }
    }

    /**
     * 读取数据，借鉴 HttpDataSource 的读取模式
     */
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened.get()) {
            throw IOException("数据源未打开")
        }

        if (bytesRead == bytesToRead) {
            return C.RESULT_END_OF_INPUT
        }

        // 计算实际可读取的字节数
        val bytesToReadNow = min(
            readLength.toLong(),
            bytesToRead - bytesRead
        ).toInt().coerceAtLeast(0)

        if (bytesToReadNow == 0) {
            return 0
        }

        return readInternal(buffer, offset, bytesToReadNow)
    }

    /**
     * 内部读取实现，包含性能监控
     */
    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray, offset: Int, length: Int): Int {
        var totalBytesRead = 0
        var currentOffset = offset
        var remaining = length

        while (remaining > 0) {
            // 检查并填充缓冲区
            if (bufferPosition >= bufferLimit) {
                val bytesFilled = refillBuffer()
                if (bytesFilled == -1) {
                    break // 到达文件末尾
                }
            }

            // 从缓冲区读取数据
            val bytesAvailable = bufferLimit - bufferPosition
            if (bytesAvailable <= 0) break

            val bytesToCopy = min(remaining, bytesAvailable)
            System.arraycopy(readBuffer!!, bufferPosition, buffer, currentOffset, bytesToCopy)

            // 更新状态
            bufferPosition += bytesToCopy
            currentOffset += bytesToCopy
            totalBytesRead += bytesToCopy
            remaining -= bytesToCopy
            bytesRead += bytesToCopy.toLong()

            // 通知传输进度
            bytesTransferred(bytesToCopy)
        }

        return if (totalBytesRead == 0 && length > 0) C.RESULT_END_OF_INPUT else totalBytesRead
    }

    /**
     * 填充缓冲区，优化性能和错误处理
     */
    @Throws(IOException::class)
    private fun refillBuffer(): Int {
        val internalBuffer = readBuffer ?: throw IOException("缓冲区未初始化")

        // 检查是否已读取所有数据
        if (bytesRead >= bytesToRead) {
            bufferPosition = 0
            bufferLimit = 0
            return -1
        }

        // 计算本次要读取的字节数
        val maxBytesToRead = (bytesToRead - bytesRead).coerceAtMost(bufferSize.toLong()).toInt()
        if (maxBytesToRead <= 0) {
            return -1
        }

        val startTime = System.currentTimeMillis()

        // 从 WebDAV 服务器读取
        val bytesReadFromNetwork = try {
            readFromWebDav(maxBytesToRead)
        } catch (e: Exception) {
            throw IOException("从 WebDAV 读取数据时发生错误", e)
        }

        val readTime = System.currentTimeMillis() - startTime

        // 性能监控和日志
        if (bytesReadFromNetwork > 0) {
            monitorPerformance(bytesReadFromNetwork, readTime)
        }

        if (bytesReadFromNetwork <= 0) {
            bufferPosition = 0
            bufferLimit = 0
            return -1
        }

        // 更新缓冲区状态
        bufferPosition = 0
        bufferLimit = bytesReadFromNetwork
        currentFileOffset += bytesReadFromNetwork.toLong()

        return bytesReadFromNetwork
    }

    /**
     * 从 WebDAV 服务器读取数据
     */
    @Throws(IOException::class)
    private fun readFromWebDav(chunkSize: Int): Int {
        // 关闭旧的 InputStream
        inputStream?.close()
        inputStream = null

        // 构造 Range 请求头
        val endOffset = currentFileOffset + chunkSize - 1
        val rangeHeader = "bytes=$currentFileOffset-$endOffset"

        Log.d(TAG, "请求 Range: $rangeHeader")

        // 使用 Sardine 获取带有 Range 头的 InputStream
        inputStream = sardine?.get(dataSpec?.uri.toString(), mapOf("Range" to rangeHeader))

        if (inputStream == null) {
            throw IOException("无法从 WebDAV 服务器获取输入流")
        }

        // 从 InputStream 读取到内部缓冲区
        var totalBytesReadFromStream = 0
        while (totalBytesReadFromStream < chunkSize) {
            val bytesRead = inputStream?.read(
                readBuffer!!,
                totalBytesReadFromStream,
                chunkSize - totalBytesReadFromStream
            ) ?: -1

            if (bytesRead == -1) {
                // 提前到达流末尾
                break
            }
            totalBytesReadFromStream += bytesRead
        }

        // 检查读取完整性
        if (totalBytesReadFromStream < chunkSize && bytesToRead - bytesRead > totalBytesReadFromStream) {
            Log.w(TAG, "网络读取不完整: 请求 $chunkSize 字节, 实际读取 $totalBytesReadFromStream 字节")
        }

        return totalBytesReadFromStream
    }

    /**
     * 性能监控，借鉴 HttpDataSource 的监控思想
     */
    private fun monitorPerformance(bytesRead: Int, readTime: Long) {
        totalReadTime += readTime
        numReads++
        totalBytesReadFromNetwork += bytesRead

        val speed = if (readTime > 0) {
            (bytesRead.toDouble() / readTime / 1024 / 1024) * 1000
        } else {
            0.0
        }

        val currentTime = System.currentTimeMillis()
        // 只在性能较差或定期记录日志
        if (readTime > 200 || speed < 3.0 || currentTime - lastLogTime > 5000) {
            Log.i(TAG, "读取 ${bytesRead / 1024}KB 耗时 ${readTime}ms, " +
                    "速度: ${"%.2f".format(speed)} MB/s, 文件位置: ${currentFileOffset / 1024 / 1024}MB")
            lastLogTime = currentTime
        }
    }

    /**
     * 获取当前 URI
     */
    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    /**
     * 关闭数据源，借鉴 HttpDataSource 的资源清理模式
     */
    @Throws(IOException::class)
    override fun close() {
        Log.d(TAG, "Closing data source.")

        if (opened.compareAndSet(true, false)) {
            try {
                // 先关闭输入流
                inputStream?.close()
            } catch (e: IOException) {
                throw IOException("关闭 WebDAV 输入流时出错", e)
            } finally {
                // 清理其他资源
                closeConnectionQuietly()

                // 状态重置
                if (transferStarted) {
                    transferStarted = false
                    transferEnded()
                }

                // 清空引用
                dataSpec = null
                inputStream = null
                sardine = null
                readBuffer = null

                // 打印统计信息
                logStatistics()
            }
        }
    }

    /**
     * 静默关闭连接，类似 HttpDataSource 的 closeConnectionQuietly
     */
    private fun closeConnectionQuietly() {
        try {
            inputStream?.close()
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 InputStream 时出错", ignored)
        } finally {
            inputStream = null
        }
        // Sardine 使用连接池，不需要显式关闭
    }

    /**
     * 记录性能统计
     */
    private fun logStatistics() {
        if (numReads > 0 && totalReadTime > 0) {
            val avgSpeed = (totalBytesReadFromNetwork.toDouble() / totalReadTime / 1024 / 1024) * 1000
            val avgTimePerRead = totalReadTime.toDouble() / numReads
            Log.i(TAG, "性能统计 - 总网络读取: ${totalBytesReadFromNetwork / 1024 / 1024}MB, " +
                    "平均速度: ${"%.2f".format(avgSpeed)} MB/s, 平均读取耗时: ${"%.2f".format(avgTimePerRead)}ms")
        }
    }
}

/**
 * WebDavDataSource 的工厂类
 */
@UnstableApi
class WebDavDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return WebDavDataSource()
    }
}