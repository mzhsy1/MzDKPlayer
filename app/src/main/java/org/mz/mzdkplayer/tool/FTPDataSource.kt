package org.mz.mzdkplayer.tool

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 自定义的 FTP 数据源，用于 ExoPlayer 通过 FTP 协议读取文件。
 * 此实现遵循 ExoPlayer DataSource 的标准模式，简化了流管理。
 * 针对打开速度进行了优化：
 * 1. 增大 FTP Client 内部缓冲区 (BUFFER_SIZE)
 * 2. 设置 Socket 超时 (SOCKET_TIMEOUT_MS)
 * 优化后添加了日志功能：
 * 3. 每隔一秒计算并记录平均读取速度。
 *
 * 修改以处理服务器主动关闭控制连接的情况。
 */
@UnstableApi
class FtpDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 ---
    private var dataSpec: DataSpec? = null
    private var ftpClient: FTPClient? = null
    private var fileInputStream: InputStream? = null

    // 用于计算平均速度的变量
    private var startTimeMs: Long = 0L // 开始读取数据的时间戳 (毫秒)
    private var totalBytesRead: Long = 0L // 累计读取的字节数
    private var lastLogTimeMs: Long = 0L // 上次打印速度日志的时间戳 (毫秒)

    // 使用原子布尔值确保 opened 状态的线程安全
    private val opened = AtomicBoolean(false)

    // --- 配置参数 ---
    companion object {
        private const val TAG = "FtpDataSource"
        // 优化1: 增大缓冲区 (例如 64KB)
        private const val BUFFER_SIZE = 64 * 1024
        // 优化2: 设置 Socket 超时
        private const val SOCKET_TIMEOUT_MS = 30000 // 30秒 Socket 超时
        private const val CONNECTION_TIMEOUT_MS = 10000 // 10秒连接超时
        // 速度日志打印间隔 (毫秒)
        private const val SPEED_LOG_INTERVAL_MS = 1000L // 1秒
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
        // 通知监听器数据传输正在初始化
        transferInitializing(dataSpec)

        // 确保数据源未被打开，保证线程安全
        if (!opened.compareAndSet(false, true)) {
            throw IOException("FtpDataSource 已经被打开。")
        }

        // 初始化速度计算相关变量
        startTimeMs = 0L
        totalBytesRead = 0L
        lastLogTimeMs = 0L

        this.dataSpec = dataSpec
        val uri = dataSpec.uri

        val host = uri.host ?: throw IOException("无效的 FTP URI: 缺少主机名")
        val port = if (uri.port != -1) uri.port else 21 // 默认 FTP 端口
        val path = uri.path ?: throw IOException("无效的 FTP URI: 缺少路径")

        // --- 安全警告 ---
        // 从 URI 的 userInfo 部分提取凭证是不安全的。
        // 在生产环境中应使用更安全的方法。
        val (username, password) = uri.userInfo?.split(":")?.let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("anonymous", "exoplayer@")
        } ?: Pair("anonymous", "exoplayer@")

        Log.d(TAG, "尝试连接到 FTP 服务器: $host:$port, 用户: $username")
        Log.d(TAG, "目标文件路径: $path")

        try {
            // 初始化 FTP 客户端 - 保留您的原始逻辑
            ftpClient = FTPClient()
            ftpClient?.controlEncoding = "UTF-8"

            // 应用优化：设置缓冲区大小和超时
            ftpClient?.bufferSize = BUFFER_SIZE
            //ftpClient?.socketTimeout = SOCKET_TIMEOUT_MS
            ftpClient?.connectTimeout = CONNECTION_TIMEOUT_MS

            // 连接到服务器 - 保留您的原始逻辑
            ftpClient?.connect(host, port)

            // 登录 - 保留您的原始逻辑
            val loginSuccess = ftpClient?.login(username, password) ?: false
            if (!loginSuccess) {
                throw IOException("FTP 登录失败")
            }

            // 设置传输模式和编码 - 保留您的原始逻辑
            ftpClient?.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient?.enterLocalPassiveMode()

            // 获取文件大小 - 保留您的原始逻辑 (使用 listFiles)
            val fileLength = ftpClient?.let { client ->
                Log.w(TAG, "尝试使用 listFiles 获取文件信息。")
                try {
                    val files = client.listFiles(path)
                    if (files != null && files.isNotEmpty()) {
                        val fileSize = files[0].size
                        Log.d(TAG, "通过 listFiles 获取到文件大小: $fileSize")
                        fileSize
                    } else {
                        Log.w(TAG, "listFiles 返回空列表，无法获取文件大小。")
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "无法通过 listFiles 获取文件大小", e)
                    null
                }
            } ?: throw IOException("无法获取远程文件大小: $path")

            Log.i(TAG, "远程文件大小: $fileLength bytes")

            // 验证请求的数据范围 - 保留您的原始逻辑
            val startPosition = dataSpec.position
            if (startPosition < 0 || startPosition > fileLength) {
                throw IOException("无效的起始位置: $startPosition")
            }

            // 计算实际需要读取的字节数 - 保留您的原始逻辑
            val bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            // 再次验证计算后的长度 - 保留您的原始逻辑
            if (bytesToRead < 0 || startPosition + bytesToRead > fileLength) {
                throw IOException("无效的数据长度: $bytesToRead")
            }

            // --- 关键：设置 REST 命令和打开流 --- - 保留您的原始逻辑
            // 设置重启点 (Seek)
            if (startPosition > 0) {
                // 修正：显式检查布尔值
                val restSuccess = ftpClient?.setRestartOffset(startPosition) ?: false
                val restReplyCode = ftpClient?.replyCode ?: -1
                // 修正：显式检查布尔值
                if (restSuccess == false || !FTPReply.isPositiveIntermediate(restReplyCode)) {
                    // 注意：有些服务器对 REST 的回复码是 Intermediate (350)
                    Log.w(TAG, "设置 REST 偏移量可能不成功。偏移量: $startPosition, 回复码: $restReplyCode, 消息: ${ftpClient?.replyString ?: "无"}")
                    // 某些服务器即使返回 Intermediate 也是成功的，我们继续尝试 RETR
                } else {
                    Log.d(TAG, "成功设置 REST 偏移量: $startPosition")
                }
            }

            // 打开文件输入流 - 保留您的原始逻辑
            fileInputStream = ftpClient?.retrieveFileStream(path)
            val retrieveReplyCode = ftpClient?.replyCode ?: -1
            if (fileInputStream == null) {
                throw IOException("无法打开 FTP 文件流 (RETR)。回复码: $retrieveReplyCode, 消息: ${ftpClient?.replyString ?: "无"}")
            }
            Log.i(TAG, "成功打开 FTP 文件流 (RETR)。回复码: $retrieveReplyCode")

            // 通知监听器数据传输已开始
            transferStarted(dataSpec)
            // 记录开始读取数据的时间
            startTimeMs = System.currentTimeMillis()
            lastLogTimeMs = startTimeMs

            // 返回可读取的总字节数
            return bytesToRead

        } catch (e: Exception) {
            // 如果在打开过程中发生任何错误，确保已分配的资源被释放
            closeQuietly()
            throw IOException("打开 FTP 文件时出错: ${e.message}", e)
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
        // 检查数据源是否已打开
        if (!opened.get()) {
            throw IOException("数据源未打开")
        }

        val inputStream = fileInputStream ?: throw IOException("内部输入流未初始化")

        try {
            // 直接从 FTP 输入流读取数据
            val bytesRead = inputStream.read(buffer, offset, readLength)

            if (bytesRead == -1) {
                // EOF
                Log.d(TAG, "已到达文件末尾 (EOF)")
                // 文件读取结束时，打印最终的平均速度
                logAverageSpeed(true)
                return C.RESULT_END_OF_INPUT
            } else {
                // 更新累计读取字节数
                totalBytesRead += bytesRead
                // 通知基类已传输的字节数
                bytesTransferred(bytesRead)
                // 每隔一段时间打印一次平均速度
                logAverageSpeed(false)
                return bytesRead
            }

        } catch (e: IOException) {
            Log.e(TAG, "从 FTP 文件读取时发生 IO 错误", e)
            throw IOException("从 FTP 文件读取时出错: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "从 FTP 文件读取时发生未知错误", e)
            throw IOException("从 FTP 文件读取时发生未知错误: ${e.message}", e)
        }
    }

    /**
     * 计算并记录平均读取速度。
     * @param isFinal 是否是最后一次调用（文件结束时）
     */
    private fun logAverageSpeed(isFinal: Boolean) {
        val currentTimeMs = System.currentTimeMillis()
        val elapsedTimeMs = currentTimeMs - startTimeMs

        // 避免除以零
        if (elapsedTimeMs <= 0) return

        // 检查是否到了打印间隔，或者这是最后一次调用
        if (isFinal || (currentTimeMs - lastLogTimeMs) >= SPEED_LOG_INTERVAL_MS) {
            val averageSpeedBps = (totalBytesRead * 1000.0) / elapsedTimeMs // bytes per second
            val averageSpeedKbps = averageSpeedBps / 1024.0 // kilobytes per second
            val averageSpeedMbps = averageSpeedKbps / 1024.0 // megabytes per second

            val elapsedSeconds = elapsedTimeMs / 1000.0
            Log.i(
                TAG,
                "读取速度统计 - 已用时: ${String.format("%.2f", elapsedSeconds)}s, " +
                        "总读取: ${totalBytesRead} bytes, " +
                        "平均速度: ${String.format("%.2f", averageSpeedBps)} B/s " +
                        "(${String.format("%.2f", averageSpeedKbps)} KB/s) " +
                        "(${String.format("%.2f", averageSpeedMbps)} MB/s)"
            )

            // 更新上次打印时间
            lastLogTimeMs = currentTimeMs
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
     * 修改以更好地处理服务器主动关闭控制连接的情况。
     */
    @Throws(IOException::class)
    override fun close() {
        // 原子性地将 opened 状态从 true 设置为 false
        if (opened.compareAndSet(true, false)) {
            Log.d(TAG, "close() 被调用，开始关闭资源...")
            val client = ftpClient // Capture reference for use in finally block
            val inputStream = fileInputStream // Capture reference for use in finally block
            try {
                var inputStreamException: Exception? = null
                var completeCommandException: Exception? = null
                var logoutException: Exception? = null
                var disconnectException: Exception? = null

                // 1. 关闭 InputStream
                if (inputStream != null) {
                    try {
                        inputStream.close()
                        Log.d(TAG, "InputStream 已关闭")
                    } catch (e: IOException) {
                        inputStreamException = e
                        Log.w(TAG, "关闭 InputStream 时出错", e)
                    }
                } else {
                    Log.d(TAG, "InputStream 为空，跳过关闭")
                }
                fileInputStream = null

                // 2. 完成 FTP 命令 (关键步骤!)
                if (client != null && client.isConnected) {
                    try {
                        val completed = client.completePendingCommand()
                        val completeReplyCode = client.replyCode
                        if (completed) {
                            Log.d(TAG, "FTP completePendingCommand 成功。回复码: $completeReplyCode")
                        } else {
                            Log.w(TAG, "FTP completePendingCommand 失败。回复码: $completeReplyCode, 消息: ${client.replyString}")
                            // 注意：即使失败，我们通常也继续断开连接
                        }
                    } catch (e: FTPConnectionClosedException) {
                        // *** 关键修改点 ***
                        // 服务器在我们调用 completePendingCommand 之前关闭了连接
                        // 这通常意味着传输已完成，服务器自行清理
                        completeCommandException = e
                        Log.i(TAG, "FTP 连接已被服务器关闭，无法执行 completePendingCommand。假设传输已完成。", e)
                    } catch (e: IOException) {
                        completeCommandException = e
                        Log.e(TAG, "FTP completePendingCommand 时发生 IO 错误", e)
                    } catch (e: IllegalStateException) {
                        completeCommandException = e
                        Log.w(TAG, "FTP completePendingCommand 时发生 IllegalStateException (客户端可能已断开)", e)
                    } catch (e: Exception) {
                        completeCommandException = e
                        Log.e(TAG, "FTP completePendingCommand 时发生未知错误", e)
                    }
                } else {
                    Log.d(TAG, "FTP 客户端为空或未连接，跳过 completePendingCommand")
                }

                // 3. 断开 FTP 连接 (Logout & Disconnect)
                if (client != null) {
                    try {
                        // 尝试 logout
                        val logoutSuccess = client.logout()
                        Log.d(TAG, "FTP logout 调用返回: $logoutSuccess")
                    } catch (e: FTPConnectionClosedException) {
                        // *** 关键修改点 ***
                        // 服务器关闭了连接，logout 不可能成功
                        logoutException = e
                        Log.i(TAG, "FTP logout 时因连接已关闭而失败 (正常情况)", e)
                    } catch (e: IOException) {
                        logoutException = e
                        Log.w(TAG, "FTP logout 时发生 IO 错误", e)
                    } catch (e: Exception) {
                        logoutException = e
                        Log.w(TAG, "FTP logout 时发生未知错误", e)
                    }

                    try {
                        // 尝试 disconnect
                        client.disconnect()
                        Log.d(TAG, "FTP disconnect 成功")
                    } catch (e: IOException) {
                        disconnectException = e
                        Log.w(TAG, "FTP disconnect 时发生 IO 错误", e)
                    } catch (e: Exception) {
                        disconnectException = e
                        Log.w(TAG, "FTP disconnect 时发生未知错误", e)
                    }
                } else {
                    Log.d(TAG, "FTP 客户端为空，跳过 logout 和 disconnect")
                }
                ftpClient = null

                // --- 异常处理策略 ---
                // 如果关闭 InputStream 时出错，这通常是最关键的问题，可以考虑抛出。
                // 其他错误（completePendingCommand, logout）如果是由于服务器关闭连接导致的，
                // 我们已经记录并处理了，通常不需要中断调用者。
                if (inputStreamException != null) {
                    // 可以选择抛出或记录。这里选择记录，因为 close() 通常不抛出检查型异常
                    Log.e(TAG, "关闭 InputStream 时发生严重错误", inputStreamException)
                    // 如果必须抛出，可以考虑 RuntimeException 或 Error，但这会中断调用栈
                    // throw RuntimeException("Failed to close underlying data stream", inputStreamException)
                }

            } finally {
                // 重置内部状态
                dataSpec = null
                // 重置速度计算状态
                startTimeMs = 0L
                totalBytesRead = 0L
                lastLogTimeMs = 0L
                Log.d(TAG, "资源关闭和状态重置完成")
            }
        } else {
            Log.d(TAG, "close() 被调用，但数据源未打开或已在关闭中")
        }
    }

    /**
     * 辅助方法：静默关闭所有资源，即使发生异常也忽略。
     * 确保在 open 失败或 close 时资源得到释放。
     * 注意：主要逻辑已移至 close()，此方法用于 open 失败时的兜底。
     */
    private fun closeQuietly() {
        Log.d(TAG, "closeQuietly() 被调用")
        // 最简单的方式是直接调用 close，它会处理所有逻辑和异常。
        // 为了保持原意（open失败时的兜底），保留原始逻辑但简化异常处理。
        try {
            // 直接调用主 close 方法，它会处理 opened 状态和异常
            // 这是更安全和一致的做法
            close()
        } catch (ignored: IOException) {
            // close() 方法本身声明抛出 IOException，但 closeQuietly 通常不希望抛出
            Log.w(TAG, "closeQuietly: 忽略 close 时的 IOException", ignored)
        } catch (ignored: Exception) {
            Log.w(TAG, "closeQuietly: 忽略 close 时的未知异常", ignored)
        }
        // 重置 opened 状态以防万一 close() 没有执行（虽然不太可能）
        opened.set(false)
    }
}

/**
 * FtpDataSource 的工厂类，用于创建 FtpDataSource 实例。
 */
@UnstableApi
class FtpDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return FtpDataSource()
    }
}



