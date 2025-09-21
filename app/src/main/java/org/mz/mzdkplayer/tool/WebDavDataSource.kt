package org.mz.mzdkplayer.tool

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.min

/**
 * 自定义的 WebDAV 数据源，用于 ExoPlayer 通过 WebDAV 协议读取文件。
 * 此实现针对大文件（如视频）进行了优化，特别是随机访问（seeking）性能。
 * 主要优化点：
 * 1. 使用 OkHttp 的 Range 请求头来直接从指定偏移量读取数据，避免下载不需要的部分。
 * 2. 移除 Okio Buffer（如果 Sardine 内部使用），改用内部 ByteArray 进行缓冲，减少网络请求次数。
 * 3. 复用 OkHttpClient 实例以优化连接。
 *
 * 注意：此实现依赖于 sardine-android 库 (com.thegrizzlylabs.sardineandroid)。
 *       它使用了内部的 OkHttpSardine 实现来获取 InputStream。
 *       需要在项目中添加 sardine-android 依赖。
 */
@UnstableApi
class WebDavDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 ---
    private var dataSpec: DataSpec? = null
    private var sardine: OkHttpSardine? = null
    private var inputStream: InputStream? = null

    // 使用 ByteArray 作为内部缓冲区
    private var readBuffer: ByteArray? = null
    private var bufferPosition: Int = 0
    private var bufferLimit: Int = 0
    private var bufferSize: Int = DEFAULT_BUFFER_SIZE_BYTES

    private var currentFileOffset: Long = 0
    private var bytesRemaining: Long = 0
    private val opened = java.util.concurrent.atomic.AtomicBoolean(false)

    // --- 配置参数 ---
    companion object {
        private const val TAG = "WebDavDataSource"
        private const val DEFAULT_BUFFER_SIZE_BYTES = 5 * 1024 * 1024 // 2MB 默认缓冲区大小
        // 用于信任所有证书的 OkHttpClient
        private val unsafeOkHttpClient: OkHttpClient by lazy {
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
                    .build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
    // --- 配置结束 ---

    /**
     * 打开数据源，准备读取指定 URI 和范围的数据。
     * @param dataSpec 包含 URI、起始位置、长度等信息的数据规范
     * @return 实际可读取的字节数
     * @throws IOException 打开过程中发生错误
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)

        if (!opened.compareAndSet(false, true)) {
            throw IOException("WebDavDataSource 已经被打开。")
        }

        this.dataSpec = dataSpec
        val uri = dataSpec.uri
        val urlString = uri.toString()

        if (urlString.isBlank()) {
            throw IOException("无效的 WebDAV URI: 空 URI")
        }

        // 从 URI 的 userInfo 部分提取凭证 (注意：这不安全，生产环境应使用更安全的方法)格式https://<username>:<password>@192.168.1.4:5006/movies/as.mkv
        val (username, password) = uri.userInfo?.split(":")?.let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("", "")
        } ?: Pair("", "")

        try {
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // 配置 OkHttpClient 忽略 SSL 验证
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val okHttpClient = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // 忽略主机名验证
                .build()
            // 初始化 Sardine 客户端
            // 使用预配置的不安全 OkHttpClient 实例
            sardine = OkHttpSardine(okHttpClient)
            if (username.isNotBlank() || password.isNotBlank()) {
                sardine?.setCredentials(username, password)
            }
            Log.d(TAG,"$username $password")
            // 方法 1: 使用 Uri.Builder (推荐，因为它处理编码等细节)
             val cleanUriString = Uri.Builder()
                .scheme(uri.scheme) // "https"
                .encodedAuthority(uri.authority?.substringAfter('@') ?: uri.authority) // 移除 userInfo 部分 ("192.168.1.4:5006")
                .encodedPath(uri.encodedPath) // "/movies/as.mkv" (保持编码)
                .encodedQuery(uri.encodedQuery) // 如果有查询参数 (?param=value)
                .encodedFragment(uri.encodedFragment) // 如果有片段 (#section)
                .build()
                .toString()
            Log.d(TAG,cleanUriString)
            // 获取文件大小（HEAD 请求）
            val davResources = sardine?.list(cleanUriString) // Depth 0, 不获取属性
            if (davResources.isNullOrEmpty()) {
                throw IOException("无法获取文件信息或文件不存在: $urlString")
            }
            val fileLength = davResources[0].contentLength

            // 验证请求的数据范围
            val startPosition = dataSpec.position
            if (startPosition < 0 || startPosition > fileLength) {
                throw IOException("无效的起始位置: $startPosition")
            }

            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            if (bytesRemaining < 0 || startPosition + bytesRemaining > fileLength) {
                throw IOException("无效的数据长度: $bytesRemaining")
            }

            // 初始化内部缓冲区状态
            currentFileOffset = startPosition
            this.readBuffer = ByteArray(bufferSize)
            this.bufferPosition = 0
            this.bufferLimit = 0

            // 注意：我们不在此处打开 InputStream，而是在第一次 read 时打开，
            // 以便利用 Range 请求进行精确读取。

            transferStarted(dataSpec)
            Log.i(TAG, "成功打开 WebDAV 数据源: $urlString, 起始位置: $startPosition, 长度: $bytesRemaining")
            return bytesRemaining

        } catch (e: Exception) {
            closeQuietly()
            throw IOException("打开 WebDAV 文件时出错: ${e.message}", e)
        }
    }

    /**
     * 从数据源读取数据到指定的缓冲区。
     * @param buffer 目标缓冲区
     * @param offset 缓冲区中的起始写入偏移量
     * @param readLength 尝试读取的最大字节数
     * @return 实际读取的字节数，或 C.RESULT_END_OF_INPUT 表示结束
     * @throws IOException 读取过程中发生错误
     */
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened.get()) {
            throw IOException("数据源未打开")
        }

        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = min(readLength.toLong(), bytesRemaining).toInt().coerceAtLeast(0)
        if (bytesToRead == 0) {
            return 0
        }

        try {
            var totalBytesRead = 0
            var currentOffset = offset

            // 循环读取，直到满足请求的字节数或遇到 EOF
            while (totalBytesRead < bytesToRead) {
                val bytesNeeded = bytesToRead - totalBytesRead

                // 检查内部缓冲区是否有数据，如果没有或不足，则从网络填充
                if (bufferPosition >= bufferLimit) {
                    refillBuffer()
                }

                // 再次检查缓冲区状态
                if (bufferPosition >= bufferLimit) {
                    if (totalBytesRead == 0) {
                        return C.RESULT_END_OF_INPUT // 如果还没读到任何数据，就是 EOF
                    } else {
                        break // 否则返回已读取的部分数据
                    }
                }

                // 现在缓冲区中应该有数据了，从中读取
                val bytesAvailableInBuffer = bufferLimit - bufferPosition
                val bytesReadFromBuffer = min(bytesNeeded, bytesAvailableInBuffer)

                // 将数据从内部缓冲区复制到输出缓冲区
                System.arraycopy(
                    readBuffer!!, // Smart cast to non-null
                    bufferPosition,
                    buffer,
                    currentOffset,
                    bytesReadFromBuffer
                )

                // 更新缓冲区内部状态
                bufferPosition += bytesReadFromBuffer
                totalBytesRead += bytesReadFromBuffer
                currentOffset += bytesReadFromBuffer
                bytesRemaining -= bytesReadFromBuffer.toLong()
                bytesTransferred(bytesReadFromBuffer)
            }

            return totalBytesRead

        } catch (e: Exception) {
            throw IOException("从 WebDAV 文件读取时出错: ${e.message}", e)
        }
    }

    /**
     * 通过从 WebDAV 服务器读取数据来填充内部缓冲区。
     * 使用 HTTP Range 请求来获取从 currentFileOffset 开始的数据块。
     *
     * @throws IOException 如果读取操作期间发生错误。
     */
    @Throws(IOException::class)
    private fun refillBuffer() {
        val internalBuffer = readBuffer ?: return
        val bytesRemainingInFile = bytesRemaining
        if (bytesRemainingInFile <= 0) {
            bufferPosition = 0
            bufferLimit = 0
            return
        }

        val chunkSize = min(bufferSize.toLong(), bytesRemainingInFile).toInt()
        val startTime = System.currentTimeMillis()

        try {
            // 关闭旧的 InputStream（如果存在）
            inputStream?.close()
            inputStream = null

            // 构造 Range 请求头: "bytes=startOffset-endOffset"
            val endOffset = currentFileOffset + chunkSize.toLong() - 1
            val rangeHeader = "bytes=$currentFileOffset-$endOffset"
            Log.v(TAG, "请求 Range: $rangeHeader")

            // 使用 Sardine 获取带有 Range 头的 InputStream
            // 注意：这依赖于 OkHttpSardine 的内部实现，它会将 headers 传递给 OkHttpClient
            inputStream = sardine?.get(dataSpec?.uri.toString(), mapOf("Range" to rangeHeader))

            if (inputStream == null) {
                throw IOException("无法从 WebDAV 服务器获取输入流")
            }

            // 从 InputStream 读取到内部缓冲区
            var totalBytesReadFromStream = 0
            while (totalBytesReadFromStream < chunkSize) {
                val bytesRead = inputStream?.read(
                    internalBuffer,
                    totalBytesReadFromStream,
                    chunkSize - totalBytesReadFromStream
                ) ?: -1

                if (bytesRead == -1) {
                    // 提前到达流末尾
                    break
                }
                totalBytesReadFromStream += bytesRead
            }

            val readTime = System.currentTimeMillis() - startTime
            if (totalBytesReadFromStream > 0) {
                val speed = if (readTime > 0) {
                    (totalBytesReadFromStream.toDouble() / readTime / 1024 / 1024) * 1000
                } else {
                    0.0
                }
                if (readTime > 100 || speed < 5.0) {
                    Log.i(
                        TAG, "读取 ${totalBytesReadFromStream / 1024 / 1024}MB 耗时 ${readTime}ms, " +
                                "速度: ${"%.2f".format(speed)} MB/s"
                    )
                }
            }


            // 更新缓冲区状态
            bufferPosition = 0
            bufferLimit = totalBytesReadFromStream // 可能小于请求的 chunkSize
            currentFileOffset += totalBytesReadFromStream.toLong()

            // 如果读取的字节少于预期且未到文件末尾，可能是服务器问题或连接中断
            if (totalBytesReadFromStream < chunkSize && bytesRemaining > totalBytesReadFromStream) {
                Log.w(TAG, "从网络读取的数据少于预期 ($totalBytesReadFromStream < $chunkSize)")
                // 可以选择抛出异常或继续（可能导致播放卡顿）
                // throw IOException("网络读取不完整")
            }


        } catch (e: Exception) {
            inputStream?.close()
            inputStream = null
            throw IOException("从 WebDAV 填充缓冲区时发生错误", e)
        }
    }


    /**
     * 获取当前打开的数据源 URI。
     * @return 当前 URI 或 null
     */
    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    /**
     * 关闭数据源，释放所有资源。
     */
    @Throws(IOException::class)
    override fun close() {
        if (opened.compareAndSet(true, false)) {
            closeQuietly()
        }
        dataSpec = null
        bytesRemaining = 0
        currentFileOffset = 0
        readBuffer = null
        bufferPosition = 0
        bufferLimit = 0
    }

    /**
     * 辅助方法：静默关闭所有资源，即使发生异常也忽略。
     */
    private fun closeQuietly() {
        try {
            inputStream?.close()
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 InputStream 时出错", ignored)
        } finally {
            inputStream = null
        }
        // OkHttp/Sardine 通常会管理连接池，不需要显式关闭客户端
        sardine = null
    }
}

/**
 * WebDavDataSource 的工厂类，用于创建 WebDavDataSource 实例。
 */
@UnstableApi
class WebDavDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return WebDavDataSource()
    }
}