package org.mz.mzdkplayer.tool

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.fileinformation.FileStandardInformation
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
import java.io.EOFException
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * SMB连接池管理器
 */
class SmbConnectionPool {
    companion object {
        @Volatile
        private var INSTANCE: SmbConnectionPool? = null

        fun getInstance(): SmbConnectionPool {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmbConnectionPool().also { INSTANCE = it }
            }
        }
    }

    // 连接池：以主机+凭证为key，存储连接信息
    private val connectionPool = ConcurrentHashMap<String, ConnectionInfo>()
    private val poolLock = ReentrantReadWriteLock()

    data class ConnectionInfo(
        val client: SMBClient,
        val connection: Connection,
        val session: Session,
        val share: DiskShare,
        val lastUsedTime: Long = System.currentTimeMillis()
    )

    /**
     * 获取或创建连接
     */
    fun getConnection(uri: Uri): ConnectionInfo {
        val key = generateConnectionKey(uri)

        // 尝试从池中获取现有连接
        poolLock.read {
            connectionPool[key]?.let { connInfo ->
                if (connInfo.connection.isConnected && isSessionActive(connInfo)) {
                    // 更新使用时间
                    val updatedInfo = connInfo.copy(lastUsedTime = System.currentTimeMillis())
                    connectionPool[key] = updatedInfo
                    return updatedInfo
                } else {
                    // 连接已断开，从池中移除
                    connectionPool.remove(key)
                }
            }
        }

        // 创建新连接
        poolLock.write {
            // 双重检查，防止并发创建
            connectionPool[key]?.let { connInfo ->
                if (connInfo.connection.isConnected && isSessionActive(connInfo)) {
                    val updatedInfo = connInfo.copy(lastUsedTime = System.currentTimeMillis())
                    connectionPool[key] = updatedInfo
                    return updatedInfo
                }
            }

            // 创建新连接
            val newConnInfo = createNewConnection(uri)
            connectionPool[key] = newConnInfo
            return newConnInfo
        }
    }

    /**
     * 创建新的SMB连接
     */
    private fun createNewConnection(uri: Uri): ConnectionInfo {
        val host = uri.host ?: throw IOException("无效的 SMB URI: 缺少主机名")
        val path = uri.path ?: throw IOException("无效的 SMB URI: 缺少路径")

        val pathSegments = path.split("/").filter { it.isNotEmpty() }
        if (pathSegments.size < 2) {
            throw IOException("无效的 SMB URI: 无法提取共享名和文件路径")
        }

        val shareName = pathSegments[0]

        // 凭证提取
        val (username, password) = uri.userInfo?.split(":")?.let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("guest", "")
        } ?: Pair("guest", "")

        val domain = ""

        // 配置
        val clientConfig = SmbConfig.builder()
            .withDialects(EnumSet.of(
                SMB2Dialect.SMB_3_1_1,
                SMB2Dialect.SMB_3_0,
                SMB2Dialect.SMB_3_0_2,
                SMB2Dialect.SMB_2XX,
                SMB2Dialect.SMB_2_1,
            ))
            .withMultiProtocolNegotiate(true)
            .withBufferSize(8 * 1024 * 1024)
            .withSoTimeout(60000)
            .withReadBufferSize(8 * 1024 * 1024)
            .withTransactBufferSize(1 * 1024 * 1024)
            .build()

        val smbClient = SMBClient(clientConfig)
        val connection = smbClient.connect(host) ?: throw IOException("无法创建 SMB 连接")

        // 认证
        val authContext = AuthenticationContext(username, password.toCharArray(), domain)
        val session = connection.authenticate(authContext) ?: throw IOException("会话认证失败")

        // 连接共享
        val diskShare = session.connectShare(shareName) as? DiskShare
            ?: throw IOException("连接共享失败或共享不是磁盘共享")

        return ConnectionInfo(smbClient, connection, session, diskShare)
    }

    /**
     * 检查会话是否活跃
     */
    @OptIn(UnstableApi::class)
    private fun isSessionActive(connInfo: ConnectionInfo): Boolean {
        return try {
            connInfo.share.list("")  // 尝试列出根目录
            true
        } catch (e: Exception) {
            Log.w("SmbConnectionPool", "会话检查失败: ${e.message}")
            false
        }
    }

    /**
     * 生成连接键
     */
    private fun generateConnectionKey(uri: Uri): String {
        val userInfo = uri.userInfo ?: "guest:"
        val host = uri.host ?: ""
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "$userInfo@$host$port"
    }

    /**
     * 清理空闲连接
     */
    @OptIn(UnstableApi::class)
    fun cleanupIdleConnections(idleTimeoutMs: Long = 300000) { // 5分钟
        val currentTime = System.currentTimeMillis()
        poolLock.write {
            val keysToRemove = connectionPool.filter { (_, connInfo) ->
                currentTime - connInfo.lastUsedTime > idleTimeoutMs
            }.keys

            keysToRemove.forEach { key ->
                val connInfo = connectionPool[key]
                try {
                    connInfo?.client?.close()
                } catch (e: Exception) {
                    Log.w("SmbConnectionPool", "关闭空闲连接时出错: ${e.message}")
                }
                connectionPool.remove(key)
            }
        }
    }

    /**
     * 关闭所有连接
     */
    @OptIn(UnstableApi::class)
    fun closeAllConnections() {
        poolLock.write {
            connectionPool.forEach { (_, connInfo) ->
                try {
                    connInfo.client.close()
                } catch (e: Exception) {
                    Log.w("SmbConnectionPool", "关闭连接时出错: ${e.message}")
                }
            }
            connectionPool.clear()
        }
    }
}

/**
 * 优化后的 SMB 数据源，借鉴了 Media3 HttpDataSource 的设计模式
 */
@UnstableApi
class SmbDataSource(
    private val config: SmbDataSourceConfig = SmbDataSourceConfig()
) : BaseDataSource(/* isNetwork= */ true) {
    // --- 成员变量 ---

    private var dataSpec: DataSpec? = null
    private var connectionInfo: SmbConnectionPool.ConnectionInfo? = null
    private var file: File? = null

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

    // 状态管理
    private val opened = AtomicBoolean(false)

    // 添加锁机制防止并发访问
    private val accessLock = ReentrantLock()

    // Seek操作的重试机制
    private var lastSeekPosition: Long = -1
    private var seekRetryCount: Int = 0
    private val maxSeekRetries: Int = 3

    // 连接健康检查
    private var lastActivityTime: Long = 0

    // 性能监控
    private var lastLogTime = 0L
    private var totalBytesRead = 0L
    private var totalReadTime = 0L
    private var numReads = 0

    /**
     * 打开数据源，借鉴 HttpDataSource 的错误处理和状态管理
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d("SmbDataSource", "Opening: ${dataSpec.uri}")

        // 状态检查和初始化 - 类似 HttpDataSource
        if (!opened.compareAndSet(false, true)) {
            throw IOException("SmbDataSource 已经被打开。")
        }

        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            // 从连接池获取连接
            connectionInfo = SmbConnectionPool.getInstance().getConnection(dataSpec.uri)

            // 获取文件信息并验证范围
            val fileLength = getFileLength()
            val startPosition = dataSpec.position

            // 范围验证 - 类似 HttpDataSource 的 416 处理
            if (startPosition < 0 || startPosition > fileLength) {
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
                throw IOException("无效的数据范围: position=$startPosition, length=$bytesToRead, fileSize=$fileLength")
            }

            // 初始化读取状态
            currentFileOffset = startPosition
            this.readBuffer = ByteArray(bufferSize)
            this.bufferPosition = 0
            this.bufferLimit = 0

            // 更新最后活动时间
            lastActivityTime = System.currentTimeMillis()

            Log.i("SmbDataSource", "成功打开文件, 大小: ${fileLength / 1024 / 1024}MB, " +
                    "起始位置: $startPosition, 读取长度: ${bytesToRead / 1024 / 1024}MB")

            // 标记传输开始 - 类似 HttpDataSource
            transferStarted = true
            transferStarted(dataSpec)

            return bytesToRead

        } catch (e: Exception) {
            when (e) {
                is IOException -> throw e
                else -> throw IOException("打开 SMB 文件时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 获取文件长度，包含错误处理
     */
    @Throws(IOException::class)
    private fun getFileLength(): Long {
        return try {
            val uri = dataSpec?.uri ?: throw IOException("dataSpec 为空")
            val path = uri.path ?: throw IOException("无效的 SMB URI: 缺少路径")

            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            if (pathSegments.size < 2) {
                throw IOException("无效的 SMB URI: 无法提取共享名和文件路径")
            }

            val filePath = pathSegments.drop(1).joinToString("/")

            // 打开文件以获取信息（临时）
            val tempFile = connectionInfo?.share?.openFile(
                filePath,
                setOf(AccessMask.FILE_READ_ATTRIBUTES),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )

            val fileInfo = tempFile?.fileInformation?.standardInformation
            val fileLength = fileInfo?.endOfFile ?: throw IOException("获取文件信息失败")

            // 关闭临时文件句柄
            tempFile.close()

            fileLength
        } catch (e: Exception) {
            throw IOException("获取文件大小时出错: ${e.message}", e)
        }
    }

    /**
     * 读取数据，借鉴 HttpDataSource 的读取模式，增加并发控制和错误恢复
     */
    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return accessLock.withLock {
            if (!opened.get()) {
                throw IOException("数据源未打开")
            }

            if (bytesRead == bytesToRead) {
                return@withLock C.RESULT_END_OF_INPUT
            }

            // 计算实际可读取的字节数
            val bytesToReadNow = min(
                readLength.toLong(),
                bytesToRead - bytesRead
            ).toInt().coerceAtLeast(0)

            if (bytesToReadNow == 0) {
                return@withLock 0
            }

            return@withLock readInternal(buffer, offset, bytesToReadNow)
        }
    }

    /**
     * 内部读取实现，包含性能监控和错误恢复
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
     * 填充缓冲区，优化性能和错误处理，增加连接健康检查
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

        // 检查连接健康状态
        if (!isConnected()) {
            Log.w("SmbDataSource", "连接已断开，尝试重新建立...")
            try {
                reconnect()
            } catch (e: Exception) {
                throw IOException("重新建立连接失败: ${e.message}", e)
            }
        }

        // 获取文件路径并打开文件（如果尚未打开）
        if (file == null) {
            val uri = dataSpec?.uri ?: throw IOException("dataSpec 为空")
            val path = uri.path ?: throw IOException("无效的 SMB URI: 缺少路径")

            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            if (pathSegments.size < 2) {
                throw IOException("无效的 SMB URI: 无法提取共享名和文件路径")
            }

            val filePath = pathSegments.drop(1).joinToString("/")

            file = connectionInfo?.share?.openFile(
                filePath,
                setOf(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            ) ?: throw IOException("打开文件失败")
        }

        // 从 SMB 文件读取
        val bytesReadFromFile = try {
            file?.read(internalBuffer, currentFileOffset, 0, maxBytesToRead) ?: -1
        } catch (e: Exception) {
            Log.e("SmbDataSource", "从 SMB 读取数据时发生错误: ${e.message}")

            // 尝试重新连接并重试
            if (isConnected()) {
                try {
                    Log.d("SmbDataSource", "尝试重新建立连接...")
                    reconnect()
                    // 重新打开文件
                    reopenFile()
                    // 重新读取
                    file?.read(internalBuffer, currentFileOffset, 0, maxBytesToRead) ?: -1
                } catch (retryException: Exception) {
                    Log.e("SmbDataSource", "重试读取也失败: ${retryException.message}")
                    throw IOException("读取失败且重试失败", retryException)
                }
            } else {
                throw IOException("从 SMB 读取数据时发生错误", e)
            }
        }

        val readTime = System.currentTimeMillis() - startTime

        // 性能监控和日志
        if (bytesReadFromFile > 0) {
            monitorPerformance(bytesReadFromFile, readTime)
            // 更新最后活动时间
            lastActivityTime = System.currentTimeMillis()
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
     * 重新连接
     */
    private fun reconnect() {
        val currentDataSpec = dataSpec ?: throw IOException("dataSpec 为空")
        connectionInfo = SmbConnectionPool.getInstance().getConnection(currentDataSpec.uri)
    }

    /**
     * 重新打开文件
     */
    private fun reopenFile() {
        val uri = dataSpec?.uri ?: throw IOException("dataSpec 为空")
        val path = uri.path ?: throw IOException("无效的 SMB URI: 缺少路径")

        val pathSegments = path.split("/").filter { it.isNotEmpty() }
        if (pathSegments.size < 2) {
            throw IOException("无效的 SMB URI: 无法提取共享名和文件路径")
        }

        val filePath = pathSegments.drop(1).joinToString("/")

        // 关闭旧文件句柄
        file?.close()

        // 重新打开文件
        file = connectionInfo?.share?.openFile(
            filePath,
            setOf(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null
        ) ?: throw IOException("重新打开文件失败")
    }

    /**
     * 性能监控，借鉴 HttpDataSource 的监控思想
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
     * 关闭数据源，借鉴 HttpDataSource 的资源清理模式
     */
    @Throws(IOException::class)
    override fun close() {
        Log.d("SmbDataSource", "Closing data source.")

        if (opened.compareAndSet(true, false)) {
            try {
                // 先关闭文件流
                file?.close()
                file = null
            } catch (e: IOException) {
                Log.w("SmbDataSource", "关闭 SMB 文件时出错: ${e.message}")
            } finally {
                // 状态重置
                if (transferStarted) {
                    transferStarted = false
                    transferEnded()
                }

                // 清空引用
                dataSpec = null
                readBuffer = null

                // 重置状态变量
                lastSeekPosition = -1
                seekRetryCount = 0

                // 打印统计信息
                logStatistics()
            }
        }
    }

    /**
     * 检查连接是否健康
     */
    fun isConnected(): Boolean {
        val connInfo = connectionInfo ?: return false
        val currentTime = System.currentTimeMillis()

        // 检查连接是否超时
        if (currentTime - lastActivityTime > 30000) { // 30秒无活动则认为连接可能断开
            return false
        }

        return connInfo.connection.isConnected && isSessionActive(connInfo)
    }

    /**
     * 检查会话是否活跃
     */
    private fun isSessionActive(connInfo: SmbConnectionPool.ConnectionInfo): Boolean {
        return try {
            connInfo.share.list("")  // 尝试列出根目录
            true
        } catch (e: Exception) {
            Log.w("SmbDataSource", "会话活动检查失败: ${e.message}")
            false
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

    /**
     * 设置Seek位置，用于外部调用
     */
    fun setCurrentPosition(position: Long) {
        accessLock.withLock {
            if (currentFileOffset != position) {
                lastSeekPosition = position
                currentFileOffset = position
                // 重置缓冲区状态
                bufferPosition = 0
                bufferLimit = 0
                // 重置Seek重试计数
                seekRetryCount = 0
            }
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
    val soTimeoutMs: Int = 60000, // Socket 超时时间
    val logIntervalMs: Long = 5000, // 日志打印间隔
    val minLogSpeedMBs: Double = 5.0 // 触发日志的最低速度阈值 (MB/s)
)

// 为了编译需要添加的常量
const val PlaybackException_ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 2004



