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
import com.emc.ecs.nfsclient.nfs.NfsReadResponse
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3
import com.emc.ecs.nfsclient.rpc.CredentialUnix
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * 优化后的 NFS 数据源，借鉴了 Media3 HttpDataSource 的设计模式
 */
@UnstableApi
class NFSDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 ---
    private var dataSpec: DataSpec? = null
    private var nfsClient: Nfs3? = null
    private var nfsFile: Nfs3File? = null

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
    private val opened = AtomicBoolean(false)

    // 性能监控
    private var lastLogTime = 0L
    private var totalBytesReadFromNfs = 0L
    private var totalReadTime = 0L
    private var numReads = 0

    // --- 配置参数 ---
    companion object {
        private const val TAG = "NFSDataSource"
        private const val DEFAULT_BUFFER_SIZE_BYTES = 8 * 1024 * 1024 // 8MB 默认缓冲区大小
        private const val MAX_RETRY_COUNT = 3 // NFS 客户端最大重试次数
    }

    /**
     * 打开数据源，借鉴 HttpDataSource 的错误处理和状态管理
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        Log.d(TAG, "Opening: ${dataSpec.uri}")

        // 状态检查和初始化 - 类似 HttpDataSource
        if (!opened.compareAndSet(false, true)) {
            throw IOException("NFSDataSource 已经被打开。")
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

            // 范围验证 - 类似 HttpDataSource 的 416 处理
            if (startPosition !in 0..fileLength) {
                closeConnectionQuietly()
                opened.set(false) // <-- 修复 1: 范围越界时重置状态
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
                opened.set(false) // <-- 修复 2: 无效长度时重置状态
                throw IOException("无效的数据范围: position=$startPosition, length=$bytesToRead, fileSize=$fileLength")
            }

            // 初始化读取状态
            currentFileOffset = startPosition
            this.readBuffer = ByteArray(bufferSize)
            this.bufferPosition = 0
            this.bufferLimit = 0

            Log.i(TAG, "成功打开 NFS 文件, 大小: ${fileLength / 1024 / 1024}MB, " +
                    "起始位置: $startPosition, 读取长度: ${bytesToRead / 1024 / 1024}MB")

            // 标记传输开始 - 类似 HttpDataSource
            transferStarted = true
            transferStarted(dataSpec)

            return bytesToRead

        } catch (e: Exception) {
            closeConnectionQuietly()
            opened.set(false) // <-- 修复 3: 捕获到一般异常时重置状态
            when (e) {
                is IOException -> throw e
                else -> throw IOException("打开 NFS 文件时出错: ${e.message}", e)
            }
        }
    }

    /**
     * 建立 NFS 连接，包含更好的错误处理
     */
    @Throws(IOException::class)
    private fun establishConnection(dataSpec: DataSpec) {
        val uri = dataSpec.uri
        Log.d(TAG, "dataSpec.uri: ${uri.toString()}")

        if (uri.scheme?.lowercase() != "nfs") {
            throw IOException("无效的 NFS URI scheme: ${uri.scheme}")
        }

        val serverAddress = uri.host ?: throw IOException("无效的 NFS URI: 缺少服务器地址 (host)")
        Log.d(TAG, "解析后的 serverAddress: $serverAddress")

        val path = uri.path ?: throw IOException("无效的 NFS URI: 缺少 path")
        Log.d(TAG, "原始 path: $path")

        // 解析 exported_path 和 path_within_export
        if (!path.startsWith("/")) {
            throw IOException("无效的 NFS URI path: '$path'. 必须以 '/' 开头。")
        }
        val colonIndexInPath = path.indexOf(':', 1)
        if (colonIndexInPath == -1) {
            throw IOException("无效的 NFS URI path: '$path'. 缺少分隔 exported_path 和 path_within_export 的冒号 ':'。")
        }

        val exportedPath = path.substring(1, colonIndexInPath)
        if (exportedPath.isEmpty()) {
            throw IOException("无效的 NFS URI path: '$path'. exported_path 为空。")
        }

        val pathWithinExport = path.substring(colonIndexInPath + 1)
        if (pathWithinExport.isEmpty()) {
            Log.w(TAG, "警告: NFS URI path: '$path'. path_within_export 为空。将使用根路径 '/'。")
        }

        Log.d(TAG, "解析后的 exportedPath: $exportedPath")
        Log.d(TAG, "解析后的 pathWithinExport: $pathWithinExport")

        try {
            Log.i(TAG, "尝试连接到 NFS 服务器: $serverAddress, 共享: $exportedPath")

            // 准备认证信息
            val credential = CredentialUnix()

            // 创建 NFS 客户端
            val client = Nfs3(
                serverAddress,
                exportedPath,
                credential,
                MAX_RETRY_COUNT
            )
            nfsClient = client

            // 构造 NFS 文件路径
            val nfsFilePath = if (pathWithinExport.startsWith("/")) {
                pathWithinExport
            } else {
                "/$pathWithinExport"
            }

            // 打开 NFS 文件
            val file = Nfs3File(client, nfsFilePath)
            if (!file.exists()) {
                throw IOException("NFS 文件不存在: $nfsFilePath")
            }
            if (!file.isFile) {
                throw IOException("NFS 路径不是文件: $nfsFilePath")
            }
            nfsFile = file

            Log.i(TAG, "NFS 连接建立成功")

        } catch (e: Exception) {
            throw IOException("建立 NFS 连接时出错: ${e.message}", e)
        }
    }

    /**
     * 获取文件长度，包含错误处理
     */
    @Throws(IOException::class)
    private fun getFileLength(): Long {
        return try {
            val file = nfsFile ?: throw IOException("NFS 文件未打开")
            file.length()
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
            // 如果已经没有更多数据需要读取（已达到 bytesToRead 限制）
            return C.RESULT_END_OF_INPUT
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
        //val internalBuffer = readBuffer ?: throw IOException("缓冲区未初始化")
        val file = nfsFile ?: throw IOException("NFS 文件未打开")

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

        // 从 NFS 文件读取
        val bytesReadFromNfs = try {
            readFromNfs(file, maxBytesToRead)
        } catch (e: Exception) {
            throw IOException("从 NFS 读取数据时发生错误", e)
        }

        val readTime = System.currentTimeMillis() - startTime

        // 性能监控和日志
        if (bytesReadFromNfs > 0) {
            monitorPerformance(bytesReadFromNfs, readTime)
        }

        if (bytesReadFromNfs <= 0) {
            bufferPosition = 0
            bufferLimit = 0
            return -1
        }

        // 更新缓冲区状态
        bufferPosition = 0
        bufferLimit = bytesReadFromNfs
        currentFileOffset += bytesReadFromNfs.toLong()

        return bytesReadFromNfs
    }

    /**
     * 从 NFS 文件读取数据
     */
    @Throws(IOException::class)
    private fun readFromNfs(file: Nfs3File, chunkSize: Int): Int {
        val internalBuffer = readBuffer ?: throw IOException("缓冲区未初始化")

        val readResponse: NfsReadResponse = try {
            file.read(currentFileOffset, chunkSize, internalBuffer, 0)
        } catch (e: Exception) {
            Log.e(TAG, "从 NFS 读取数据时发生错误", e)
            throw IOException("NFS 读取操作失败", e)
        }

        val bytesReadFromFile = readResponse.bytesRead

        // 检查读取结果
        if (bytesReadFromFile < 0) {
            Log.w(TAG, "从 NFS 读取到负数字节数: $bytesReadFromFile")
            return -1
        }

        if (bytesReadFromFile == 0) {
            Log.d(TAG, "从 NFS 读取到 0 字节，可能已到达文件末尾")
            return -1
        }

        // 检查读取完整性
        if (bytesReadFromFile < chunkSize && bytesToRead - bytesRead > bytesReadFromFile) {
            //Log.w(TAG, "NFS 读取不完整: 请求 $chunkSize 字节, 实际读取 $bytesReadFromFile 字节")
        }

        return bytesReadFromFile
    }

    /**
     * 性能监控，借鉴 HttpDataSource 的监控思想
     */
    private fun monitorPerformance(bytesRead: Int, readTime: Long) {
        totalReadTime += readTime
        numReads++
        totalBytesReadFromNfs += bytesRead

        val speed = if (readTime > 0) {
            (bytesRead.toDouble() / readTime / 1024 / 1024) * 1000
        } else {
            0.0
        }

        val currentTime = System.currentTimeMillis()
        // 只在性能较差或定期记录日志
        if (readTime > 100 || speed < 10.0 || currentTime - lastLogTime > 5000) {
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
                closeConnectionQuietly()

                // 状态重置
                if (transferStarted) {
                    transferStarted = false
                    transferEnded()
                }

                // 清空引用
                dataSpec = null
                nfsFile = null
                nfsClient = null
                readBuffer = null

                // 打印统计信息
                logStatistics()
            } catch (e: Exception) {
                throw IOException("关闭 NFS 数据源时出错", e)
            }
        }
    }

    /**
     * 静默关闭连接，类似 HttpDataSource 的 closeConnectionQuietly
     */
    private fun closeConnectionQuietly() {
        try {
            // Nfs3File 通常没有显式的 close 方法
            // 但我们可以将其置为 null 以帮助垃圾回收
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 NFS 文件时出错", ignored)
        }

        try {
            // Nfs3 客户端本身没有显式的 "close" 方法
            // 置为 null 以帮助垃圾回收
            nfsClient = null
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 NFS 客户端时出错", ignored)
        }

        // 清空引用
        nfsFile = null
        nfsClient = null
    }

    /**
     * 记录性能统计
     */
    private fun logStatistics() {
        if (numReads > 0 && totalReadTime > 0) {
            val avgSpeed = (totalBytesReadFromNfs.toDouble() / totalReadTime / 1024 / 1024) * 1000
            val avgTimePerRead = totalReadTime.toDouble() / numReads
            Log.i(TAG, "性能统计 - 总 NFS 读取: ${totalBytesReadFromNfs / 1024 / 1024}MB, " +
                    "平均速度: ${"%.2f".format(avgSpeed)} MB/s, 平均读取耗时: ${"%.2f".format(avgTimePerRead)}ms")
        }
    }
}

/**
 * NFSDataSource 的工厂类
 */
@UnstableApi
class NFSDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return NFSDataSource()
    }
}