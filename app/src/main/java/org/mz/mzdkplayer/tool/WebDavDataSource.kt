package org.mz.mzdkplayer.tool

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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.min

/**
 * 优化后的 WebDAV 数据源，使用单次 Range 请求建立长连接流。
 */
@UnstableApi
class WebDavDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 (简化) ---
    private var dataSpec: DataSpec? = null
    private var sardine: OkHttpSardine? = null
    private var inputStream: InputStream? = null // 保持为当前连接的流

    // 借鉴 HttpDataSource 的状态管理
    private var bytesRead: Long = 0
    private var bytesToRead: Long = 0
    private var transferStarted: Boolean = false

    // 文件位置跟踪 (不再需要，因为我们使用 Range 请求)
    // private var currentFileOffset: Long = 0

    // 状态管理
    private val opened = AtomicBoolean(false)

    // 性能监控 (保留)
    private var startTimeMs: Long = 0L
    private var totalBytesTransferred: Long = 0L
    private var lastLogTimeMs: Long = 0L
    private var numReads = 0
    private var totalReadTime = 0L
    private  var lastLogTime =0L
    // --- 配置参数 ---
    companion object {
        private const val TAG = "WebDavDataSource"

        // 假设 WebDavHttpClient 提供了安全配置
        private val webDavClient by lazy {
            // 请确保 WebDavHttpClient.restrictedTrustOkHttpClient 是 OkHttpClient 实例
            // 并在应用关闭时被正确释放，以避免 OkHttp 连接池泄漏。
            WebDavHttpClient.restrictedTrustOkHttpClient
        }
    }

    /**
     * 打开数据源，建立 WebDAV 连接并获取从起始位置开始的长连接流。
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d(TAG, "Opening: ${dataSpec.uri}")

        if (!opened.compareAndSet(false, true)) {
            throw IOException("WebDavDataSource 已经被打开。")
        }

        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            // 1. 建立连接和验证 (Sardine 客户端初始化)
            establishConnection(dataSpec)

            // 2. 获取文件信息并验证范围
            val fileLength = getFileLength()
            val startPosition = dataSpec.position

            if (startPosition !in 0..fileLength) {
                handleOpenError(DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE))
            }

            // 3. 计算要读取的字节数
            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            if (bytesToRead < 0 || startPosition + bytesToRead > fileLength) {
                handleOpenError(IOException("无效的数据范围: position=$startPosition, length=$bytesToRead, fileSize=$fileLength"))
            }

            // 4. 关键：发起单个 HTTP GET 请求，并设置 Range 头
            val uriString = buildCleanUri(dataSpec.uri)
            val endOffset = startPosition + bytesToRead - 1

            // 如果 bytesToRead == C.LENGTH_UNSET，则 Range 头为 bytes=startPosition-
            val rangeValue = if (bytesToRead == C.LENGTH_UNSET.toLong()) {
                "bytes=$startPosition-"
            } else {
                "bytes=$startPosition-$endOffset"
            }

            Log.d(TAG, "请求 Range: $rangeValue")

            // ❗ 核心优化：获取从指定位置开始的长连接流 ❗
            inputStream = sardine?.get(uriString, mapOf("Range" to rangeValue))
                ?: throw IOException("无法从 WebDAV 服务器获取输入流")

            Log.i(TAG, "成功打开 WebDAV 文件流, 大小: ${fileLength / 1024 / 1024}MB, " +
                    "起始位置: $startPosition, 读取长度: ${bytesToRead / 1024 / 1024}MB")

            // 标记传输开始
            transferStarted = true
            transferStarted(dataSpec)

            // 记录性能监控的起始时间
            startTimeMs = System.currentTimeMillis()
            lastLogTimeMs = startTimeMs

            return bytesToRead

        } catch (e: Exception) {
            handleOpenError(e)
            // handleOpenError 总是会抛出异常，这里仅作保证
            throw e
        }
    }

    /**
     * 统一处理 open() 过程中的异常和状态重置。
     */
    @Throws(IOException::class)
    private fun handleOpenError(e: Exception) {
        closeConnectionQuietly()
        opened.set(false) // 确保在任何异常抛出前重置状态

        when (e) {
            is IOException -> throw e
            is DataSourceException -> throw e
            else -> throw IOException("打开 WebDAV 文件时出错: ${e.message}", e)
        }
    }

    /**
     * 建立 WebDAV 连接，只初始化 Sardine 客户端和凭证。
     */
    @Throws(IOException::class)
    private fun establishConnection(dataSpec: DataSpec) {
        val uri = dataSpec.uri

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

            // 验证连接和文件存在性 (使用 LIST/PROPFIND 验证)
            validateConnection(cleanUriString)

        } catch (e: Exception) {
            throw IOException("建立 WebDAV 连接时出错: ${e.message}", e)
        }
    }

    // ... (buildCleanUri 方法保持不变) ...
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

    // ... (validateConnection 方法保持不变) ...
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
     * 核心修改：直接从 open() 中获取的长连接流中读取数据。
     * 不再涉及缓冲区、Range 重发请求和关闭连接。
     */
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened.get()) {
            throw IOException("数据源未打开")
        }

        // 检查是否已读取完所有数据
        if (bytesRead >= bytesToRead && bytesToRead != C.LENGTH_UNSET.toLong()) {
            return C.RESULT_END_OF_INPUT
        }

        // 计算实际可读取的字节数
        val bytesToReadNow = if (bytesToRead == C.LENGTH_UNSET.toLong()) {
            // 如果长度未知，则尽力读取
            readLength
        } else {
            min(
                readLength.toLong(),
                bytesToRead - bytesRead
            ).toInt().coerceAtLeast(0)
        }

        if (bytesToReadNow == 0) {
            return C.RESULT_END_OF_INPUT
        }

        val inputStream = this.inputStream ?: throw IOException("输入流未初始化")
        val startTime = System.currentTimeMillis()

        // 核心读取操作
        val bytesReadFromStream = try {
            inputStream.read(buffer, offset, bytesToReadNow)
        } catch (e: IOException) {
            Log.e(TAG, "从 WebDAV 流读取时发生 IO 异常", e)
            throw IOException("从 WebDAV 流读取时出错: ${e.message}", e)
        }

        val readTime = System.currentTimeMillis() - startTime

        if (bytesReadFromStream == -1) {
            Log.d(TAG, "已到达流末尾 (EOF)")
            bytesRead = bytesToRead // 标记读取完成
            return C.RESULT_END_OF_INPUT
        }

        // 更新状态
        bytesRead += bytesReadFromStream.toLong()
        totalBytesTransferred += bytesReadFromStream.toLong()

        // 性能监控
        monitorPerformance(bytesReadFromStream, readTime)

        // 通知传输进度
        bytesTransferred(bytesReadFromStream)

        return bytesReadFromStream
    }

    /**
     * 性能监控 (简化为使用总传输量和总时间)
     */
    private fun monitorPerformance(bytesRead: Int, readTime: Long) {
        totalReadTime += readTime
        numReads++

        val currentTime = System.currentTimeMillis()
        if (readTime > 200 || currentTime - lastLogTime > 5000) {
            val elapsedTime = currentTime - startTimeMs
            val avgSpeed = if (elapsedTime > 0) (totalBytesTransferred * 1000.0) / elapsedTime else 0.0
            val avgSpeedMBs = avgSpeed / 1024.0 / 1024.0

            Log.i(TAG, "读取 ${bytesRead / 1024}KB 耗时 ${readTime}ms, " +
                    "总读取速度: ${"%.2f".format(avgSpeedMBs)} MB/s, 总读取: ${totalBytesTransferred / 1024 / 1024}MB")
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
     * 关闭数据源
     */
    @Throws(IOException::class)
    override fun close() {
        Log.d(TAG, "Closing data source.")

        if (opened.compareAndSet(true, false)) {
            try {
                closeConnectionQuietly()

                // 状态重置
                if (transferStarted) {
                    transferStarted = false
                    transferEnded()
                }

                // 清空引用
                dataSpec = null
                sardine = null // OkHttpSardine 实例被清空，下次 open 会新建
                // inputStream 已在 closeConnectionQuietly 中处理

                // 打印统计信息
                logStatistics()
            } catch (e: Exception) {
                throw IOException("关闭 WebDAV 数据源时出错", e)
            }
        }
    }

    /**
     * 静默关闭流，不关闭 sardine 客户端。
     */
    private fun closeConnectionQuietly() {
        try {
            inputStream?.close()
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 InputStream 时出错", ignored)
        } finally {
            inputStream = null // 确保置空
        }
    }

    /**
     * 记录性能统计
     */
    private fun logStatistics() {
        val totalTime = System.currentTimeMillis() - startTimeMs
        if (numReads > 0 && totalTime > 0) {
            val avgSpeed = (totalBytesTransferred.toDouble() / totalTime / 1024 / 1024) * 1000
            val avgTimePerRead = totalReadTime.toDouble() / numReads
            Log.i(TAG, "最终性能统计 - 总网络读取: ${totalBytesTransferred / 1024 / 1024}MB, " +
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
