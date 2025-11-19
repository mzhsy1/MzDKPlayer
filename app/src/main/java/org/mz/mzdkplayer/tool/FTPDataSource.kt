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
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.time.toDuration

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
        private const val BUFFER_SIZE = 512 * 1024
        private const val CONNECTION_TIMEOUT_MS = 30000  // 连接超时30秒
        private const val SPEED_LOG_INTERVAL_MS = 2000L

        // 创建 Duration 对象
        //private val DATA_TIMEOUT_DURATION: Duration = Duration.ofMillis(DATA_TIMEOUT_MS.toLong())
        // private val CONNECTION_TIMEOUT_DURATION: Duration = Duration.ofMillis(CONNECTION_TIMEOUT_MS.toLong())
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
            if (startPosition !in 0..fileLength) {
                closeConnectionQuietly()
                // **注意：此处在抛出异常前也应重置状态**
                opened.set(false)
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
                // **注意：此处在抛出异常前也应重置状态**
                opened.set(false)
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

            // --- 关键修复 ---
            // 重置状态，防止 Media3 的 Loader 在捕获异常后
            // 再次调用 close() 时导致双重关闭
            opened.set(false)
            // --- 修复结束 ---

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


                //dataTimeout = DATA_TIMEOUT_DURATION
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
            // 修复：使用 mlistFile 获取单个文件的精确信息（如果服务器支持）
            // 否则 listFiles 可能会有性能问题或返回不准确
            val ftpFile: FTPFile? = ftpClient?.mlistFile(path)

            if (ftpFile != null && ftpFile.size >= 0) {
                ftpFile.size
            } else {
                // 回退到 listFiles
                Log.d(TAG, "mlistFile 失败，回退到 listFiles")
                val files = ftpClient?.listFiles(path)
                if (files != null && files.isNotEmpty()) {
                    files[0].size
                } else {
                    throw IOException("无法获取远程文件大小: $path")
                }
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
    private fun handleOpenError(e: Exception) {
        closeConnectionQuietly()
        opened.set(false) // 确保在任何异常抛出前重置状态

        when (e) {
            is IOException -> throw e
            else -> throw IOException("打开 FTP 文件时出错: ${e.message}", e)
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
     * 读取数据，修复可能的逻辑问题
     */
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened.get()) {
            throw IOException("数据源未打开")
        }

        // 修复：检查是否已经读取完所有数据
        if (bytesRead >= bytesToRead) {
            return C.RESULT_END_OF_INPUT
        }

        // 计算实际可读取的字节数
        val bytesToReadNow = min(
            readLength.toLong(),
            bytesToRead - bytesRead
        ).toInt().coerceAtLeast(0)

        if (bytesToReadNow == 0) {
            return C.RESULT_END_OF_INPUT  // 修复：返回 END_OF_INPUT 而不是 0
        }

        return readInternal(buffer, offset, bytesToReadNow)
    }

    /**
     * 内部读取实现，简化连接检查
     */
    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray, offset: Int, length: Int): Int {
        val inputStream = fileInputStream ?: throw IOException("内部输入流未初始化")

        try {
            val bytesReadFromStream = inputStream.read(buffer, offset, length)

            if (bytesReadFromStream == -1) {
                Log.d(TAG, "已到达文件末尾 (EOF)，已读取: $bytesRead/$bytesToRead 字节")
                // 修复：确保标记为完全读取
                bytesRead = bytesToRead
                return C.RESULT_END_OF_INPUT
            }

            // 更新状态
            bytesRead += bytesReadFromStream.toLong()
            totalBytesTransferred += bytesReadFromStream.toLong()

            // 通知传输进度
            bytesTransferred(bytesReadFromStream)

            // 性能监控（可选）
            if (bytesReadFromStream > 0) {
                logAverageSpeed(false)
            }

            return bytesReadFromStream

        } catch (e: IOException) {
            Log.e(TAG, "从 FTP 文件读取时发生 IO 异常", e)
            throw IOException("从 FTP 文件读取时出错: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "从 FTP 文件读取时发生未知异常", e)
            throw IOException("从 FTP 文件读取时发生未知错误", e)
        }
    }

    /**
     * 检查 FTP 连接是否仍然有效
     */
    private fun isConnectionHealthy(): Boolean {
        return try {
            ftpClient?.isConnected == true && ftpClient?.sendNoOp() == true
        } catch (e: Exception) {
            false
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
                // fileInputStream 和 ftpClient 已在 closeConnectionQuietly 中设为 null

            } catch (e: Exception) {
                // 注意：这里捕获了 Exception，但 close() 声明了 Throws IOException。
                // 最好是抛出 IOException 或在内部处理掉。
                // 鉴于 closeConnectionQuietly 已经很健壮，这里抛出异常的概率很低。
                // 保持原样，但改为抛出 IOException。
                throw IOException("关闭 FTP 数据源时出错", e)
            }
        }
    }

    /**
     * 静默关闭连接，增强对服务器主动断开连接的处理
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
                Log.d(TAG, "关闭 InputStream 时发生非关键错误", e)
            }
        }
        fileInputStream = null

        // 2. 检查 FTP 客户端状态
        ftpClient?.let { client ->
            try {
                // 检查连接是否仍然有效
                val isStillConnected = try {
                    client.isConnected && client.sendNoOp()
                } catch (e: Exception) {
                    false
                }

                if (isStillConnected) {
                    // 只有在连接仍然活跃时才尝试完成命令和登出
                    try {
                        val completed = client.completePendingCommand()
                        val completeReplyCode = client.replyCode
                        if (completed) {
                            Log.d(TAG, "FTP completePendingCommand 成功。回复码: $completeReplyCode")
                        } else {
                            Log.d(TAG, "FTP completePendingCommand 返回失败，但可能已完成传输。回复码: $completeReplyCode")
                        }
                    } catch (e: FTPConnectionClosedException) {
                        Log.d(TAG, "FTP 连接已被服务器关闭，假设传输已完成")
                    } catch (e: Exception) {
                        completeCommandException = e
                        Log.d(TAG, "FTP completePendingCommand 时发生非关键错误", e)
                    }

                    // 尝试登出
                    try {
                        client.logout()
                        Log.d(TAG, "FTP logout 成功")
                    } catch (e: FTPConnectionClosedException) {
                        Log.d(TAG, "FTP logout 时因连接已关闭而跳过")
                    } catch (e: Exception) {
                        Log.d(TAG, "FTP logout 时发生非关键错误", e)
                    }
                } else {
                    Log.d(TAG, "FTP 连接已断开，跳过命令完成和登出")
                }

                // 3. 断开连接（如果仍然连接）
                try {
                    if (client.isConnected) {
                        client.disconnect()
                        Log.d(TAG, "FTP disconnect 成功")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "FTP disconnect 时发生非关键错误", e)
                }

            } catch (e: Exception) {
                Log.d(TAG, "FTP 连接状态检查时发生异常", e)
            }
        }
        ftpClient = null // 确保 ftpClient 被设为 null

        // 记录非关键错误（降低日志级别）
        if (inputStreamException != null) {
            Log.d(TAG, "关闭 InputStream 时发生非关键错误", inputStreamException)
        }
        if (completeCommandException != null) {
            Log.d(TAG, "FTP 命令完成时发生非关键错误", completeCommandException)
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