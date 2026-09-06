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
import okhttp3.Credentials
import okhttp3.Request
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * 优化后的 WebDAV 数据源
 * * 优化点：
 * 1. 使用全局单例 OkHttpSardine，配合底层 OkHttp 连接池复用 TCP 连接。
 * 2. 提供 globalReleaseResources 用于彻底清理。
 */
@UnstableApi
class WebDavDataSource : BaseDataSource(/* isNetwork= */ true) {

    companion object {
        private const val TAG = "WebDavDataSource"

        // 全局复用的 Sardine 实例
        private var sharedSardine: OkHttpSardine? = null

        // 假设 WebDavHttpClient 提供了配置好的 OkHttpClient (单例)
        // 这是一个外部依赖，确保它配置了合适的连接池
        private val sharedOkHttpClient by lazy {
            WebDavHttpClient.restrictedTrustOkHttpClient
        }

        private val lock = Any()

        /**
         * 静态释放方法：供外部（如 VideoPlayerScreen）调用
         * 彻底关闭连接池，释放资源。
         */
        fun releaseGlobalResources() = synchronized(lock) {
            Log.i(TAG, "Releasing GLOBAL WebDAV resources...")
            try {
                sharedSardine = null
                // 注意：如果 WebDavHttpClient 是全局单例且其他地方也在用，
                // 这里可能不需要关闭 sharedOkHttpClient，或者调用 dispatcher().cancelAll()
                // sharedOkHttpClient.connectionPool.evictAll()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing global resources", e)
            }
        }
    }

    // --- 实例变量 ---
    private var dataSpec: DataSpec? = null
    private var inputStream: InputStream? = null

    private var bytesRead: Long = 0
    private var bytesToRead: Long = 0
    private var transferStarted: Boolean = false
    private var opened = false

    private var myUsername = ""
    private var myPassword = ""

    // 性能监控
    private var startTimeMs: Long = 0L
    private var totalBytesTransferred: Long = 0L
    private var lastLogTime = 0L
    private var numReads = 0
    private var totalReadTime = 0L


    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d(TAG, "Opening: ${dataSpec.uri}")
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            // 1. 准备/复用 Client
            prepareGlobalClient(dataSpec)

            // 2. 获取文件长度 (使用复用的 Client)
            val fileLength = getFileLength(myUsername, myPassword)
            val startPosition = dataSpec.position

            if (startPosition > fileLength) {
                throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            }

            // 3. 计算读取长度
            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            // 4. 发起 Range 请求获取流
            val uriString = buildCleanUri(dataSpec.uri)
            val endOffset = startPosition + bytesToRead - 1
            val rangeValue = if (bytesToRead == C.LENGTH_UNSET.toLong()) {
                "bytes=$startPosition-"
            } else {
                "bytes=$startPosition-$endOffset"
            }

            Log.d(TAG, "请求 Range: $rangeValue")

            // 使用全局实例发起请求
            inputStream = sharedSardine?.get(uriString, mapOf("Range" to rangeValue))
                ?: throw IOException("无法从 WebDAV 服务器获取输入流")

            opened = true
            transferStarted = true
            transferStarted(dataSpec)

            startTimeMs = System.currentTimeMillis()

            return bytesToRead

        } catch (e: Exception) {
            closeConnectionQuietly()
            // 如果是 IOException 直接抛出，否则包装
            if (e is IOException) throw e
            throw IOException("Open error: ${e.message}", e)
        }
    }

    /**
     * 准备全局 Client，设置凭证
     */
    private fun prepareGlobalClient(dataSpec: DataSpec) = synchronized(lock) {
        val uri = dataSpec.uri
        val (username, password) = uri.userInfo?.split(":")?.let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("", "")
        } ?: Pair("", "")

        myUsername = username
        myPassword = password

        // 如果实例不存在，或者需要重置（OkHttpSardine 通常支持动态 setCredentials，所以复用实例即可）
        if (sharedSardine == null) {
            sharedSardine = OkHttpSardine()
        }

        // 每次 open 都更新一下凭证，防止同一个 Sardine 实例用于不同账号
        if (username.isNotBlank() || password.isNotBlank()) {
            sharedSardine?.setCredentials(username, password)
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

        if (bytesToReadNow == 0) return C.RESULT_END_OF_INPUT

        val stream = inputStream ?: throw IOException("Stream is null")
        val startTime = System.currentTimeMillis()

        try {
            val bytesReadNow = stream.read(buffer, offset, bytesToReadNow)

            if (bytesReadNow == -1) {
                if (bytesToRead != C.LENGTH_UNSET.toLong()) throw EOFException()
                return C.RESULT_END_OF_INPUT
            }

            bytesRead += bytesReadNow
            totalBytesTransferred += bytesReadNow
            val readTime = System.currentTimeMillis() - startTime

            monitorPerformance(bytesReadNow, readTime)
            bytesTransferred(bytesReadNow)

            return bytesReadNow
        } catch (e: IOException) {
            throw IOException("Read error: ${e.message}", e)
        }
    }

    private fun getFileLength(u: String, p: String): Long {
        try {
            val cleanUri = buildCleanUri(dataSpec?.uri!!)
            val credential = Credentials.basic(u, p)
            val request = Request.Builder()
                .url(cleanUri)
                .header("Authorization", credential)
                .header("Range", "bytes=0-1")
                .get()
                .build()

            sharedOkHttpClient.newCall(request).execute().use { response ->
                if (response.code == 206) {
                    val contentRange = response.header("Content-Range")
                    contentRange?.substringAfterLast("/")?.toLongOrNull()?.let { return it }
                }
                if (response.code == 200) {
                    val contentType = response.header("Content-Type")
                    if (contentType?.contains("text/html") == true) throw IOException("WebDAV returned HTML")
                    response.header("Content-Length")?.toLongOrNull()?.let { return it }
                }
                // 如果没获取到，可以尝试 list (PROPFIND) 作为 fallback，或者抛错
                throw IOException("无法获取长度 code=${response.code}")
            }
        } catch (e: Exception) {
            throw IOException("Get length error: ${e.message}", e)
        }
    }

    // ... monitorPerformance, buildCleanUri 等保持不变 ...

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

    override fun getUri(): Uri? = dataSpec?.uri

    /**
     * Close: 对于 HTTP，我们必须关闭 InputStream 才能将连接还给连接池。
     * 这里的复用是由底层 OkHttpClient 自动管理的，只要我们不销毁 sharedSardine/sharedOkHttpClient 即可。
     */
    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        closeConnectionQuietly()
        dataSpec = null
    }

    private fun closeConnectionQuietly() {
        try {
            inputStream?.close()
        } catch (ignored: Exception) {}
        inputStream = null
    }
}

@UnstableApi
class WebDavDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return WebDavDataSource()
    }
}