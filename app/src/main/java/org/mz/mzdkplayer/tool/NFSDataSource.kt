package org.mz.mzdkplayer.tool

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.emc.ecs.nfsclient.nfs.NfsReadResponse
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3
import com.emc.ecs.nfsclient.rpc.CredentialUnix
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * 自定义的 NFS 数据源，用于 ExoPlayer 通过 NFS 协议读取文件。
 * 此实现基于 com.emc.ecs:nfs-client 库。
 * 主要优化点：
 * 1. 使用 Nfs3File.read() 直接从指定偏移量读取，避免低效的跳转。
 * 2. 使用内部 ByteArray 进行缓冲，减少 NFS 网络请求次数。
 */
@UnstableApi
class NFSDataSource : BaseDataSource(/* isNetwork= */ true) {

    // --- 成员变量 ---
    private var dataSpec: DataSpec? = null
    private var nfsClient: Nfs3? = null // NFS 客户端实例
    private var nfsFile: Nfs3File? = null // 打开的 NFS 文件

    // 使用 ByteArray 作为内部缓冲区
    private var readBuffer: ByteArray? = null // 内部缓冲区
    private var bufferPosition: Int = 0       // 缓冲区中当前读取位置
    private var bufferLimit: Int = 0          // 缓冲区中实际填充的数据大小
    private var bufferSize: Int = DEFAULT_BUFFER_SIZE_BYTES // 缓冲区大小

    // 跟踪当前在 NFS 文件中的逻辑读取位置（相对于文件开头）
    private var currentFileOffset: Long = 0
    // 剩余待读取的字节数
    private var bytesRemaining: Long = 0
    // 使用原子布尔值确保 opened 状态的线程安全
    private val opened = AtomicBoolean(false)

    // --- 配置参数 ---
    companion object {
        private const val TAG = "NFSDataSource"
        // 内部缓冲区大小
        private const val DEFAULT_BUFFER_SIZE_BYTES = 8 * 1024 * 1024 // 2MB 默认缓冲区大小
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
            throw IOException("NFSDataSource 已经被打开。")
        }

        this.dataSpec = dataSpec
        val uri = dataSpec.uri
        Log.d(TAG, "dataSpec.uri: ${uri.toString()}") // 添加调试日志

        // --- 解析 NFS URI ---
        // 根据日志分析，ExoPlayer 传递的 URI 结构可能如下：
        // nfs://<host>:<some_port_or_empty>/
        // path: /<exported_path>:<path_within_export>
        //
        // 示例: nfs://192.168.1.4:/fs/1000/nfs:/movies/...
        // authority: 192.168.1.4:
        // path: /fs/1000/nfs:/movies/...
        if (uri.scheme?.lowercase() != "nfs") {
            throw IOException("无效的 NFS URI scheme: ${uri.scheme}")
        }

        val serverAddress = uri.host ?: throw IOException("无效的 NFS URI: 缺少服务器地址 (host)")
        Log.d(TAG, "解析后的 serverAddress: $serverAddress") // 添加调试日志

        val path = uri.path ?: throw IOException("无效的 NFS URI: 缺少 path")
        Log.d(TAG, "原始 path: $path") // 添加调试日志

        // --- 修正 URI 解析逻辑 v2 ---
        // 从 path 中解析 exported_path 和 path_within_export
        // path 格式应为: /<exported_path>:<path_within_export>
        if (!path.startsWith("/")) {
            throw IOException("无效的 NFS URI path: '$path'. 必须以 '/' 开头。")
        }
        val colonIndexInPath = path.indexOf(':', 1) // 从索引1开始查找，确保不是开头的斜杠
        if (colonIndexInPath == -1) {
            throw IOException("无效的 NFS URI path: '$path'. 缺少分隔 exported_path 和 path_within_export 的冒号 ':'。")
        }

        // exported_path 是从第一个 '/' 到第一个冒号 ':' 之间的部分
        val exportedPath = path.substring(1, colonIndexInPath) // substring(1, ...) 排除开头的 '/'
        if (exportedPath.isEmpty()) {
            throw IOException("无效的 NFS URI path: '$path'. exported_path 为空。")
        }

        // path_within_export 是冒号 ':' 之后的部分
        val pathWithinExport = path.substring(colonIndexInPath + 1)
        if (pathWithinExport.isEmpty()) {
            Log.w(TAG, "警告: NFS URI path: '$path'. path_within_export 为空。将使用根路径 '/'。")
            // pathWithinExport = "/" // 或者可以设置为空字符串，取决于 Nfs3File 的期望
        }
        // --- URI 解析逻辑修正结束 ---

        Log.d(TAG, "解析后的 exportedPath: $exportedPath")   // 添加调试日志
        Log.d(TAG, "解析后的 pathWithinExport: $pathWithinExport") // 添加调试日志

        try {
            Log.i(TAG, "尝试连接到 NFS 服务器: $serverAddress, 共享: $exportedPath")

            // 准备认证信息 (使用默认 UID/GID 0)
            val credential = CredentialUnix()

            // 创建 NFS 客户端并连接/挂载
            // 这会自动处理 RPC MOUNT 协议交互
            val client = Nfs3(
                serverAddress,
                exportedPath,
                credential,
                3 // 最大重试次数
            )
            nfsClient = client

            // 构造 NFS 文件路径 (相对于挂载点)
            // Nfs3File 期望的路径通常是相对于导出目录的路径，且通常以 '/' 开头
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

            val fileLength = file.length()

            // 验证请求的数据范围是否有效
            val startPosition = dataSpec.position
            if (startPosition < 0 || startPosition > fileLength) {
                throw IOException("无效的起始位置: $startPosition (文件大小: $fileLength)")
            }

            // 计算实际需要读取的字节数
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            // 再次验证计算后的长度
            if (bytesRemaining < 0 || startPosition + bytesRemaining > fileLength) {
                throw IOException("无效的数据长度: $bytesRemaining (起始: $startPosition, 文件大小: $fileLength)")
            }

            // 初始化内部缓冲区状态，为指定的起始位置做准备
            currentFileOffset = startPosition
            // 初始化缓冲区
            this.readBuffer = ByteArray(bufferSize)
            this.bufferPosition = 0
            this.bufferLimit = 0

            Log.i(TAG, "成功打开 NFS 文件: $nfsFilePath (大小: $fileLength, 起始: $startPosition, 长度: $bytesRemaining)")

            // 通知监听器数据传输已开始
            transferStarted(dataSpec)

            return bytesRemaining // 返回可读取的总字节数

        } catch (e: Exception) {
            // 如果在打开过程中发生任何错误，确保已分配的资源被释放
            Log.e(TAG, "打开 NFS 文件时出错", e)
            closeQuietly()
            throw IOException("打开 NFS 文件时出错: ${e.message}", e)
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

        // 如果没有剩余数据可读，则返回结束标志
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        // 计算本次实际尝试读取的字节数（不超过剩余量和请求量）
        val bytesToRead = min(readLength.toLong(), bytesRemaining).toInt().coerceAtLeast(0)

        if (bytesToRead == 0) {
            return 0 // 请求读取 0 字节，直接返回
        }

        try {
            var totalBytesRead = 0
            var currentOffset = offset

            // 循环读取，直到满足请求的字节数或遇到 EOF
            while (totalBytesRead < bytesToRead) {
                val bytesNeeded = bytesToRead - totalBytesRead

                // 检查内部缓冲区是否有数据，如果没有或不足，则从 NFS 填充
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
                // 通知基类已传输的字节数
                bytesTransferred(bytesReadFromBuffer)
            }

            return totalBytesRead


        } catch (e: Exception) {
            throw IOException("从 NFS 文件读取时出错: ${e.message}", e)
        }
    }

    /**
     * 通过从 NFS 文件读取数据来填充内部缓冲区。
     *
     * @throws IOException 如果读取操作期间发生错误。
     */
    @Throws(IOException::class)
    private fun refillBuffer() {
        val internalBuffer = readBuffer ?: return // 再次检查缓冲区是否已初始化
        val file = nfsFile ?: return // 检查文件是否已打开

        // 计算文件中剩余的字节数
        val bytesRemainingInFile = bytesRemaining
        if (bytesRemainingInFile <= 0) {
            // 文件结束，重置缓冲区状态
            bufferPosition = 0
            bufferLimit = 0
            return
        }

        // 确定这次要读取的块大小（不超过缓冲区大小和文件剩余大小）
        val chunkSize = min(bufferSize.toLong(), bytesRemainingInFile).toInt()

        // --- 修正部分 ---
        // 执行从 NFS 文件的实际读取操作
        // file.read 返回 NfsReadResponse 对象
        val readResponse: NfsReadResponse = try {
            // 调用 Nfs3File.read 方法
            file.read(currentFileOffset, chunkSize, internalBuffer, 0)
        } catch (e: Exception) {
            // 捕获可能的 NFS 读取异常并包装为 IOException
            Log.e(TAG, "从 NFS 读取数据时发生错误", e)
            throw IOException("从 NFS 读取数据时发生错误", e)
        }

        // 从 NfsReadResponse 对象中获取实际读取的字节数
        val bytesReadFromFile = readResponse.bytesRead

        // 检查读取结果
        if (bytesReadFromFile <= 0) {
            // 读取失败或到达文件末尾，重置缓冲区状态
            Log.w(TAG, "从 NFS 读取到 0 字节或负数，可能已到达文件末尾。")
            bufferPosition = 0
            bufferLimit = 0
            return // 不更新 currentFileOffset
        }

        // 成功读取后，更新缓冲区状态
        bufferPosition = 0           // 重置缓冲区读取位置到开头
        bufferLimit = bytesReadFromFile // 设置缓冲区有效数据长度
        currentFileOffset += bytesReadFromFile.toLong() // 更新文件读取位置
        // --- 修正结束 ---
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
        // 原子性地将 opened 状态从 true 设置为 false
        if (opened.compareAndSet(true, false)) {
            closeQuietly() // 执行实际的资源关闭操作
        }
        // 重置内部状态
        dataSpec = null
        bytesRemaining = 0
        currentFileOffset = 0
        readBuffer = null // 清空缓冲区引用
        bufferPosition = 0
        bufferLimit = 0
    }

    /**
     * 辅助方法：静默关闭所有资源，即使发生异常也忽略。
     * 确保在 open 失败或 close 时资源得到释放。
     */
    private fun closeQuietly() {
        try {
           // nfsFile?.close() // 检查 Nfs3File 是否有 close 方法，如果没有则忽略
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 NFS 文件时出错", ignored)
        }
        try {
            // Nfs3 客户端本身没有显式的 "close" 或 "unmount" 方法
            // 通常，当 Nfs3 实例不再被引用时，其内部资源会被垃圾回收
            // 但为了明确状态，我们可以将其置为 null
            nfsClient = null
        } catch (ignored: Exception) {
            Log.w(TAG, "关闭 NFS 客户端时出错", ignored)
        }

        // 清空引用
        nfsFile = null
        nfsClient = null
    }
}

/**
 * NFSDataSource 的工厂类，用于创建 NFSDataSource 实例。
 */
@UnstableApi
class NFSDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return NFSDataSource()
    }
}



