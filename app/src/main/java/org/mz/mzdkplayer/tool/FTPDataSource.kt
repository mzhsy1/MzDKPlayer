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
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * 优化后的 FTP 数据源，借鉴了 Media3 HttpDataSource 的设计模式
 */
@UnstableApi
class FtpDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 ---
    private var dataSpec: DataSpec? = null
    private var ftpClient: FTPClient? = null
    private var fileInputStream: InputStream? = null

    // 借鉴 HttpDataSource 的状态管理
    private var bytesRead: Long = 0
    private var bytesToRead: Long = 0
    private var transferStarted: Boolean = false

    // 状态管理
    private val opened = AtomicBoolean(false)

    // 性能监控
    private var startTimeMs: Long = 0L
    private var totalBytesTransferred: Long = 0L
    private var lastLogTimeMs: Long = 0L

    // --- 配置参数 ---
    companion object {
        private const val TAG = "FtpDataSource"
        private const val BUFFER_SIZE = 64 * 1024
        private const val SOCKET_TIMEOUT_MS = 30000
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val SPEED_LOG_INTERVAL_MS = 1000L
    }

    /**
     * 打开数据源，借鉴 HttpDataSource 的错误处理和状态管理
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d(TAG, "Opening: ${dataSpec.uri}")

        // 状态检查和初始化 - 类似 HttpDataSource
        if (!opened.compareAndSet(false, true)) {
            throw IOException("FtpDataSource 已经被打开。")
        }

        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        // 初始化性能监控
        startTimeMs = 0L
        totalBytesTransferred = 0L
        lastLogTimeMs = 0L

        try {
            // 建立连接
            establishConnection(dataSpec)

            // 获取文件信息并验证范围
            val fileLength = getFileLength()
            val startPosition = dataSpec.position

            // 范围验证 - 类似 HttpDataSource 的 416 处理
            if (startPosition < 0 || startPosition > fileLength) {
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

            // 设置文件位置
            setFilePosition(startPosition)

            // 打开文件流
            openFileStream(dataSpec.uri.path!!)

            Log.i(TAG, "成功打开 FTP 文件, 大小: ${fileLength / 1024 / 1024}MB, " +
                    "起始位置: $startPosition, 读取长度: ${bytesToRead / 1024 / 1024}MB")

            // 标记传输开始 - 类似 HttpDataSource
            transferStarted = true
            transferStarted(dataSpec)

            // 记录开始读取数据的时间
            startTimeMs = System.currentTimeMillis()
            lastLogTimeMs = startTimeMs

            return bytesToRead

        } catch (e: Exception) {
            closeConnectionQuietly()
            when (e) {
                is IOException -> throw e
                else -> throw IOException("打开 FTP 文件时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 建立 FTP 连接，包含更好的错误处理
     */
    @Throws(IOException::class)
    private fun establishConnection(dataSpec: DataSpec) {
        val uri = dataSpec.uri
        val host = uri.host ?: throw IOException("无效的 FTP URI: 缺少主机名")
        val port = if (uri.port != -1) uri.port else 21

        // 凭证提取
        val (username, password) = uri.userInfo?.split(":")?.let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("anonymous", "exoplayer@")
        } ?: Pair("anonymous", "exoplayer@")

        Log.d(TAG, "尝试连接到 FTP 服务器: $host:$port, 用户: $username")

        try {
            // 初始化 FTP 客户端
            ftpClient = FTPClient().apply {
                controlEncoding = "UTF-8"
                bufferSize = BUFFER_SIZE
                connectTimeout = CONNECTION_TIMEOUT_MS
            }

            // 连接到服务器
            ftpClient?.connect(host, port)

            // 验证连接响应
            val connectReplyCode = ftpClient?.replyCode ?: -1
            if (!FTPReply.isPositiveCompletion(connectReplyCode)) {
                throw IOException("FTP 连接失败: ${ftpClient?.replyString ?: "未知错误"}")
            }

            // 登录
            val loginSuccess = ftpClient?.login(username, password) ?: false
            if (!loginSuccess) {
                throw IOException("FTP 登录失败: ${ftpClient?.replyString ?: "未知错误"}")
            }

            // 配置传输设置
            ftpClient?.apply {
                setFileType(FTP.BINARY_FILE_TYPE)
                enterLocalPassiveMode()
            }

            Log.i(TAG, "FTP 连接建立成功")

        } catch (e: Exception) {
            throw IOException("建立 FTP 连接时出错: ${e.message}", e)
        }
    }

    /**
     * 获取文件长度，包含错误处理
     */
    @Throws(IOException::class)
    private fun getFileLength(): Long {
        val path = dataSpec?.uri?.path ?: throw IOException("无效的 URI: 缺少路径")

        return try {
            val files = ftpClient?.listFiles(path)
            if (files != null && files.isNotEmpty()) {
                files[0].size
            } else {
                throw IOException("无法获取远程文件大小: $path")
            }
        } catch (e: Exception) {
            throw IOException("获取文件大小时出错: ${e.message}", e)
        }
    }

    /**
     * 设置文件读取位置
     */
    @Throws(IOException::class)
    private fun setFilePosition(startPosition: Long) {
        if (startPosition > 0) {
            val restSuccess = ftpClient?.setRestartOffset(startPosition) ?: false
            val restReplyCode = ftpClient?.replyCode ?: -1

            if (restSuccess != true || FTPReply.isPositiveIntermediate(restReplyCode).not()) {
                Log.w(TAG, "设置 REST 偏移量可能不成功。偏移量: $startPosition, 回复码: $restReplyCode")
                // 继续尝试，某些服务器即使返回 Intermediate 也是成功的
            } else {
                Log.d(TAG, "成功设置 REST 偏移量: $startPosition")
            }
        }
    }

    /**
     * 打开文件流
     */
    @Throws(IOException::class)
    private fun openFileStream(path: String) {
        fileInputStream = ftpClient?.retrieveFileStream(path)
        val retrieveReplyCode = ftpClient?.replyCode ?: -1

        if (fileInputStream == null) {
            throw IOException("无法打开 FTP 文件流。回复码: $retrieveReplyCode, 消息: ${ftpClient?.replyString ?: "无"}")
        }

        Log.i(TAG, "成功打开 FTP 文件流。回复码: $retrieveReplyCode")
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
        val inputStream = fileInputStream ?: throw IOException("内部输入流未初始化")

        try {
            val bytesReadFromStream = inputStream.read(buffer, offset, length)

            if (bytesReadFromStream == -1) {
                Log.d(TAG, "已到达文件末尾 (EOF)")
                logAverageSpeed(true)
                return C.RESULT_END_OF_INPUT
            }

            // 更新状态
            bytesRead += bytesReadFromStream.toLong()
            totalBytesTransferred += bytesReadFromStream.toLong()

            // 通知传输进度
            bytesTransferred(bytesReadFromStream)

            // 性能监控
            logAverageSpeed(false)

            return bytesReadFromStream

        } catch (e: IOException) {
            throw IOException("从 FTP 文件读取时出错", e)
        } catch (e: Exception) {
            throw IOException("从 FTP 文件读取时发生未知错误", e)
        }
    }

    /**
     * 性能监控，借鉴 HttpDataSource 的监控思想
     */
    private fun logAverageSpeed(isFinal: Boolean) {
        val currentTimeMs = System.currentTimeMillis()
        val elapsedTimeMs = currentTimeMs - startTimeMs

        if (elapsedTimeMs <= 0) return

        // 只在性能较差或定期记录日志
        if (isFinal || (currentTimeMs - lastLogTimeMs) >= SPEED_LOG_INTERVAL_MS) {
            val averageSpeedBps = (totalBytesTransferred * 1000.0) / elapsedTimeMs
            val averageSpeedMbps = averageSpeedBps / 1024.0 / 1024.0

            val elapsedSeconds = elapsedTimeMs / 1000.0
            Log.i(
                TAG,
                "读取速度统计 - 已用时: ${"%.2f".format(elapsedSeconds)}s, " +
                        "总读取: ${totalBytesTransferred / 1024 / 1024}MB, " +
                        "平均速度: ${"%.2f".format(averageSpeedMbps)} MB/s"
            )

            lastLogTimeMs = currentTimeMs
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
                closeConnectionQuietly()

                // 状态重置
                if (transferStarted) {
                    transferStarted = false
                    transferEnded()
                }

                // 清空引用
                dataSpec = null
                fileInputStream = null
                ftpClient = null

            } catch (e: Exception) {
                throw IOException("关闭 FTP 数据源时出错", e)
            }
        }
    }

    /**
     * 静默关闭连接，类似 HttpDataSource 的 closeConnectionQuietly
     */
    private fun closeConnectionQuietly() {
        var inputStreamException: Exception? = null
        var completeCommandException: Exception? = null

        // 1. 关闭 InputStream
        fileInputStream?.let { inputStream ->
            try {
                inputStream.close()
                Log.d(TAG, "InputStream 已关闭")
            } catch (e: IOException) {
                inputStreamException = e
                Log.w(TAG, "关闭 InputStream 时出错", e)
            }
        }
        fileInputStream = null

        // 2. 完成 FTP 命令
        ftpClient?.let { client ->
            if (client.isConnected) {
                try {
                    val completed = client.completePendingCommand()
                    val completeReplyCode = client.replyCode
                    if (completed) {
                        Log.d(TAG, "FTP completePendingCommand 成功。回复码: $completeReplyCode")
                    } else {
                        Log.w(TAG, "FTP completePendingCommand 失败。回复码: $completeReplyCode")
                    }
                } catch (e: FTPConnectionClosedException) {
                    completeCommandException = e
                    Log.i(TAG, "FTP 连接已被服务器关闭，假设传输已完成")
                } catch (e: Exception) {
                    completeCommandException = e
                    Log.w(TAG, "FTP completePendingCommand 时发生错误", e)
                }

                // 3. 断开连接
                try {
                    client.logout()
                } catch (e: FTPConnectionClosedException) {
                    Log.i(TAG, "FTP logout 时因连接已关闭而失败")
                } catch (e: Exception) {
                    Log.w(TAG, "FTP logout 时发生错误", e)
                }

                try {
                    client.disconnect()
                    Log.d(TAG, "FTP disconnect 成功")
                } catch (e: Exception) {
                    Log.w(TAG, "FTP disconnect 时发生错误", e)
                }
            }
        }
        ftpClient = null

        // 如果有严重错误，可以考虑记录或抛出
        if (inputStreamException != null) {
            Log.e(TAG, "关闭 InputStream 时发生严重错误", inputStreamException)
        }
    }
}

/**
 * FtpDataSource 的工厂类
 */
@UnstableApi
class FtpDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return FtpDataSource()
    }
}