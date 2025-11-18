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
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min
import kotlin.concurrent.withLock

/**
 * 优化后的 SMB 数据源，修复连接关闭问题
 */
@UnstableApi
class SmbDataSource(
    private val config: SmbDataSourceConfig = SmbDataSourceConfig()
) : BaseDataSource(/* isNetwork= */ true) {
    // --- 成员变量 ---

    private var dataSpec: DataSpec? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: File? = null
    private var smbClient: SMBClient? = null

    // 借鉴 HttpDataSource 的状态管理
    private var bytesRead: Long = 0
    private var bytesToRead: Long = 0
    private var transferStarted: Boolean = false

    // 缓冲区管理
    private var readBuffer: ByteArray? = null
    private var bufferPosition: Int = 0
    private var bufferLimit: Int = 0
    private var bufferSize: Int = config.bufferSizeBytes

    // 文件位置跟踪
    private var currentFileOffset: Long = 0

    // 连接和协议配置
    private val PREFERRED_SMB_DIALECTS = EnumSet.of(
        SMB2Dialect.SMB_3_1_1,
        SMB2Dialect.SMB_3_0,
        SMB2Dialect.SMB_3_0_2,
        SMB2Dialect.SMB_2XX,
        SMB2Dialect.SMB_2_1,
    )

    // 状态管理
    private val opened = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    // 性能监控
    private var lastLogTime = 0L
    private var totalBytesRead = 0L
    private var totalReadTime = 0L
    private var numReads = 0

    // 线程安全锁
    private val lock = ReentrantLock()

    /**
     * 打开数据源，修复状态管理问题
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d("SmbDataSource", "Opening: ${dataSpec.uri}")

        // 状态检查和初始化
        if (!opened.compareAndSet(false, true)) {
            throw IOException("SmbDataSource 已经被打开。")
        }
        if (closed.get()) {
            throw IOException("SmbDataSource 已经被关闭，无法重新打开。")
        }

        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            // 建立连接
            establishConnection(dataSpec)

            // 获取文件信息并验证范围
            val fileLength = getFileLength()
            val startPosition = dataSpec.position

            // 范围验证
            if (startPosition !in 0..fileLength) {
                close()
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
                close()
                throw IOException("无效的数据范围: position=$startPosition, length=$bytesToRead, fileSize=$fileLength")
            }

            // 初始化读取状态
            currentFileOffset = startPosition
            this.readBuffer = ByteArray(bufferSize)
            this.bufferPosition = 0
            this.bufferLimit = 0

            Log.i("SmbDataSource", "成功打开文件, 大小: ${fileLength / 1024 / 1024}MB, " +
                    "起始位置: $startPosition, 读取长度: ${bytesToRead / 1024 / 1024}MB")

            // 标记传输开始
            transferStarted = true
            transferStarted(dataSpec)

            return bytesToRead

        } catch (e: Exception) {
            // 确保在异常时清理资源
            close()
            when (e) {
                is IOException -> throw e
                else -> throw IOException("打开 SMB 文件时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 建立 SMB 连接，改进连接状态检查
     */
    @Throws(IOException::class)
    private fun establishConnection(dataSpec: DataSpec) {
        lock.withLock {
            val uri = dataSpec.uri
            val host = uri.host ?: throw IOException("无效的 SMB URI: 缺少主机名")
            val path = uri.path ?: throw IOException("无效的 SMB URI: 缺少路径")

            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            if (pathSegments.size < 2) {
                throw IOException("无效的 SMB URI: 无法提取共享名和文件路径")
            }

            val shareName = pathSegments[0]
            val filePath = pathSegments.drop(1).joinToString("/")

            // 凭证提取
            val (username, password) = uri.userInfo?.split(":")?.let {
                if (it.size == 2) Pair(it[0], it[1]) else Pair("guest", "")
            } ?: Pair("guest", "")

            val domain = ""

            // 检查连接是否仍然有效
            if (!isConnectionValid()) {
                Log.d("SmbDataSource", "连接无效，重新建立连接")
                closeResources()
            }

            // 配置和连接
            if (!isConnectionValid()) {
                val clientConfig = SmbConfig.builder()
                    .withDialects(PREFERRED_SMB_DIALECTS)
                    .withMultiProtocolNegotiate(true)
                    .withBufferSize(config.smbBufferSizeBytes)
                    .withSoTimeout(config.soTimeoutMs)
                    .withTimeout(120000, TimeUnit.MILLISECONDS)
                    .withReadBufferSize(config.readBufferSizeBytes)
                    .withTransactBufferSize(1 * 1024 * 1024)
                    .build()

                val connectionStartTime = System.currentTimeMillis()

                smbClient = SMBClient(clientConfig)
                connection = smbClient?.connect(host) ?: throw IOException("无法创建 SMB 连接")

                // 认证
                val authContext = AuthenticationContext(username, password.toCharArray(), domain)
                session = connection?.authenticate(authContext) ?: throw IOException("会话认证失败")

                // 连接共享
                share = session?.connectShare(shareName) as? DiskShare
                    ?: throw IOException("连接共享失败或共享不是磁盘共享")

                val totalTime = System.currentTimeMillis() - connectionStartTime
                Log.d("SmbDataSource", "连接建立总耗时: ${totalTime}ms")
            }

            // 打开文件
            file = share?.openFile(
                filePath,
                setOf(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            ) ?: throw IOException("打开文件失败")
        }
    }

    /**
     * 改进的连接有效性检查
     */
    private fun isConnectionValid(): Boolean {
        return try {
            connection?.isConnected == true &&
                    session != null &&
                    share != null &&
                    share?.isConnected == true
        } catch (e: Exception) {
            Log.w("SmbDataSource", "检查连接有效性时出错: ${e.message}")
            false
        }
    }

    /**
     * 获取文件长度，改进错误处理
     */
    @Throws(IOException::class)
    private fun getFileLength(): Long {
        return lock.withLock {
            try {
                val fileInfo = file?.fileInformation?.standardInformation
                    ?: throw IOException("获取文件信息失败")
                fileInfo.endOfFile
            } catch (e: Exception) {
                throw IOException("获取文件大小时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 读取数据，添加连接状态检查
     */
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened.get() || closed.get()) {
            throw IOException("数据源未打开或已关闭")
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
     * 内部读取实现，添加连接验证
     */
    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray, offset: Int, length: Int): Int {
        var totalBytesRead = 0
        var currentOffset = offset
        var remaining = length

        while (remaining > 0) {
            // 检查连接状态
            if (!isConnectionValid()) {
                throw IOException("SMB 连接已断开")
            }

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
     * 填充缓冲区，改进错误恢复
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

        // 从 SMB 文件读取，添加重试机制
        val bytesReadFromFile = try {
            lock.withLock {
                file?.read(internalBuffer, currentFileOffset, 0, maxBytesToRead) ?: -1
            }
        } catch (e: Exception) {
            // 检查是否是连接问题
            if (!isConnectionValid()) {
                throw IOException("SMB 连接已断开，无法读取数据", e)
            }
            throw IOException("从 SMB 读取数据时发生错误", e)
        }

        val readTime = System.currentTimeMillis() - startTime

        // 性能监控和日志
        if (bytesReadFromFile > 0) {
            monitorPerformance(bytesReadFromFile, readTime)
        }

        if (bytesReadFromFile <= 0) {
            bufferPosition = 0
            bufferLimit = 0
            return -1
        }

        // 更新缓冲区状态
        bufferPosition = 0
        bufferLimit = bytesReadFromFile
        currentFileOffset += bytesReadFromFile.toLong()

        return bytesReadFromFile
    }

    /**
     * 性能监控
     */
    private fun monitorPerformance(bytesRead: Int, readTime: Long) {
        totalReadTime += readTime
        numReads++
        totalBytesRead += bytesRead

        val speed = if (readTime > 0) {
            (bytesRead.toDouble() / readTime / 1024 / 1024) * 1000
        } else {
            0.0
        }

        val currentTime = System.currentTimeMillis()
        if (readTime > 100 || speed < config.minLogSpeedMBs || currentTime - lastLogTime > config.logIntervalMs) {
            Log.i("SmbDataSource", "读取 ${bytesRead / 1024}KB 耗时 ${readTime}ms, " +
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
     * 关闭数据源，彻底修复关闭问题
     */
    @Throws(IOException::class)
    override fun close() {
        Log.d("SmbDataSource", "Closing data source.")

        if (closed.compareAndSet(false, true)) {
            try {
                // 标记为已关闭
                opened.set(false)

                if (transferStarted) {
                    transferStarted = false
                    transferEnded()
                }

                // 安全关闭资源
                closeResources()

                // 清空引用
                dataSpec = null
                readBuffer = null

                // 打印统计信息
                logStatistics()

            } catch (e: Exception) {
                Log.w("SmbDataSource", "关闭数据源时发生错误: ${e.message}")
                // 不重新抛出异常，避免掩盖原始错误
            }
        }
    }

    /**
     * 安全关闭所有资源，修复关闭顺序问题
     */
    private fun closeResources() {
        lock.withLock {
            // 按照正确的顺序关闭资源：文件 -> 共享 -> 会话 -> 连接 -> 客户端
            val exceptions = mutableListOf<Exception>()

            try {
                file?.close()
            } catch (e: Exception) {
                exceptions.add(e)
                Log.w("SmbDataSource", "关闭文件时出错: ${e.message}")
            } finally {
                file = null
            }

            try {
                share?.close()
            } catch (e: Exception) {
                exceptions.add(e)
                Log.w("SmbDataSource", "关闭共享时出错: ${e.message}")
            } finally {
                share = null
            }

            try {
                session?.close()
            } catch (e: Exception) {
                exceptions.add(e)
                Log.w("SmbDataSource", "关闭会话时出错: ${e.message}")
            } finally {
                session = null
            }

            try {
                connection?.close()
            } catch (e: Exception) {
                exceptions.add(e)
                Log.w("SmbDataSource", "关闭连接时出错: ${e.message}")
            } finally {
                connection = null
            }

            try {
                smbClient?.close()
            } catch (e: Exception) {
                exceptions.add(e)
                Log.w("SmbDataSource", "关闭 SMB 客户端时出错: ${e.message}")
            } finally {
                smbClient = null
            }

            // 如果有多个异常，只记录不抛出，避免掩盖原始问题
            if (exceptions.isNotEmpty()) {
                Log.w("SmbDataSource", "关闭资源时遇到 ${exceptions.size} 个错误")
            }
        }
    }

    /**
     * 记录性能统计
     */
    private fun logStatistics() {
        if (numReads > 0 && totalReadTime > 0) {
            val avgSpeed = (totalBytesRead.toDouble() / totalReadTime / 1024 / 1024) * 1000
            val avgTimePerRead = totalReadTime.toDouble() / numReads
            Log.i("SmbDataSource", "性能统计 - 总读取: ${totalBytesRead / 1024 / 1024}MB, " +
                    "平均速度: ${"%.2f".format(avgSpeed)} MB/s, 平均读取耗时: ${"%.2f".format(avgTimePerRead)}ms")
        }
    }
}

/**
 * SmbDataSource 的工厂类
 */
@UnstableApi
class SmbDataSourceFactory(private val config: SmbDataSourceConfig = SmbDataSourceConfig()) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return SmbDataSource(config)
    }
}

/**
 * SmbDataSource 的配置类
 */
data class SmbDataSourceConfig(
    val bufferSizeBytes: Int = 8 * 1024 * 1024, // 8MB 内部缓冲区大小
    val smbBufferSizeBytes: Int = 8 * 1024 * 1024, // SMB 协议缓冲区大小
    val readBufferSizeBytes: Int = 8 * 1024 * 1024, // SMB 读取缓冲区大小
    val soTimeoutMs: Int = 120000, // Socket 超时时间 120s
    val logIntervalMs: Long = 5000, // 日志打印间隔
    val minLogSpeedMBs: Double = 5.0 // 触发日志的最低速度阈值 (MB/s)
)

// 为了编译需要添加的常量
const val PlaybackException_ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 2004