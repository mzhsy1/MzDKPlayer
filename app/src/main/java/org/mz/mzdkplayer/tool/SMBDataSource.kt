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
import kotlin.math.min

/**
 * 优化后的 SMB 数据源
 * 核心改进：
 * 1. 使用随机访问 (file.read at offset) 替代流式 skip，支持秒开 Seek。
 * 2. 实现手动缓冲区，平衡网络请求频率与内存占用。
 * 3. 线程安全的全局连接复用。
 */
@UnstableApi
class SmbDataSource(
    private val config: SmbDataSourceConfig = SmbDataSourceConfig()
) : BaseDataSource(/* isNetwork= */ true) {

    companion object {
        private const val TAG = "SmbDataSource"

        // --- 全局静态缓存 ---
        private var sharedSmbClient: SMBClient? = null
        private var cachedConnection: Connection? = null
        private var cachedSession: Session? = null
        private var cachedShare: DiskShare? = null

        private var currentHost: String? = null
        private val lock = Any()

        /**
         * 静态释放方法
         */
        fun releaseGlobalResources() = synchronized(lock) {
            Log.i(TAG, "Releasing GLOBAL SMB resources...")
            try {
                cachedShare?.close()
                cachedSession?.close()
                cachedConnection?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing global resources", e)
            } finally {
                cachedShare = null
                cachedSession = null
                cachedConnection = null
                currentHost = null
                sharedSmbClient = null
            }
            Log.i(TAG, "Releasing GLOBAL SMB END...")
        }
    }

    // --- 实例变量 ---
    private var dataSpec: DataSpec? = null
    private var file: File? = null

    private var bytesToRead: Long = 0
    private var bytesRead: Long = 0
    private var opened = false

    // 缓冲区管理
    private var readBuffer: ByteArray? = null
    private var bufferPosition: Int = 0
    private var bufferLimit: Int = 0
    private var currentFileOffset: Long = 0

    // SMB 协议配置
    private val PREFERRED_SMB_DIALECTS = EnumSet.of(
        SMB2Dialect.SMB_2XX,
        SMB2Dialect.SMB_2_1,
        SMB2Dialect.SMB_2_0_2
    )

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.bytesRead = 0
        this.bytesToRead = 0
        transferInitializing(dataSpec)

        try {
            val uri = dataSpec.uri
            ensureGlobalConnection(uri)

            val path = uri.path ?: throw IOException("无效路径")
            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            if (pathSegments.size < 2) throw IOException("路径必须包含共享名和文件路径")
            val filePath = pathSegments.drop(1).joinToString("/")

            file = cachedShare?.openFile(
                filePath,
                setOf(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            ) ?: throw IOException("无法打开文件: $filePath")

            val fileInfo = file!!.fileInformation.standardInformation
            val fileLength = fileInfo.endOfFile
            val startPosition = dataSpec.position

            if (startPosition > fileLength) {
                throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            }

            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - startPosition
            }

            // 初始化读取状态
            currentFileOffset = startPosition
            readBuffer = ByteArray(config.bufferSizeBytes)
            bufferPosition = 0
            bufferLimit = 0

            opened = true
            transferStarted(dataSpec)

            return bytesToRead
        } catch (e: Exception) {
            closeQuietly()
            if (e is IOException) throw e
            throw IOException("Open error: ${e.message}", e)
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        if (bytesRead == bytesToRead) return C.RESULT_END_OF_INPUT

        var totalBytesRead = 0
        var currentOffset = offset
        var remaining = min(readLength.toLong(), bytesToRead - bytesRead).toInt()

        while (remaining > 0) {
            // 1. 如果缓冲区为空，填充它
            if (bufferPosition >= bufferLimit) {
                if (!refillBuffer()) {
                    break
                }
            }

            // 2. 从缓冲区拷贝数据
            val bytesAvailable = bufferLimit - bufferPosition
            val bytesToCopy = min(remaining, bytesAvailable)
            System.arraycopy(readBuffer!!, bufferPosition, buffer, currentOffset, bytesToCopy)

            bufferPosition += bytesToCopy
            currentOffset += bytesToCopy
            totalBytesRead += bytesToCopy
            remaining -= bytesToCopy
            bytesRead += bytesToCopy
            
            bytesTransferred(bytesToCopy)
        }

        return if (totalBytesRead == 0 && readLength > 0) C.RESULT_END_OF_INPUT else totalBytesRead
    }

    private fun refillBuffer(): Boolean {
        val f = file ?: return false
        val bytesRemaining = bytesToRead - bytesRead
        if (bytesRemaining <= 0) return false

        val bytesToReadFromNetwork = min(bytesRemaining, config.bufferSizeBytes.toLong()).toInt()
        
        // 使用随机访问读取，直接指定文件偏移量
        // SMBJ File.read(buffer, fileOffset, bufferOffset, length)
        val bytesReadNow = f.read(readBuffer!!, currentFileOffset, 0, bytesToReadFromNetwork)
        
        if (bytesReadNow <= 0) return false

        bufferPosition = 0
        bufferLimit = bytesReadNow
        currentFileOffset += bytesReadNow
        return true
    }

    private fun ensureGlobalConnection(uri: Uri) = synchronized(lock) {
        val host = uri.host ?: throw IOException("Host missing")
        val (user, pass) = parseUserInfo(uri)

        if (cachedConnection == null || !cachedConnection!!.isConnected || currentHost != host) {
            releaseGlobalResources()

            if (sharedSmbClient == null) {
                val clientConfig = SmbConfig.builder()
                    .withDialects(PREFERRED_SMB_DIALECTS)
                    .withMultiProtocolNegotiate(true)
                    .withBufferSize(config.socketBufferSizeBytes)
                    .withSoTimeout(0)
                    .withTimeout(60_000, TimeUnit.MILLISECONDS)
                    .build()
                sharedSmbClient = SMBClient(clientConfig)
            }

            cachedConnection = sharedSmbClient!!.connect(host)
            currentHost = host
            val authContext = AuthenticationContext(user, pass.toCharArray(), "")
            cachedSession = cachedConnection!!.authenticate(authContext)
        }

        val shareName = uri.path?.split("/")?.filter { it.isNotEmpty() }?.get(0)
            ?: throw IOException("No share name")

        if (cachedShare == null || cachedShare?.smbPath?.shareName != shareName) {
            cachedShare?.close()
            cachedShare = cachedSession?.connectShare(shareName) as? DiskShare
                ?: throw IOException("Connect share failed: $shareName")
        }
    }

    private fun parseUserInfo(uri: Uri): Pair<String, String> {
        val userInfo = uri.userInfo
        if (userInfo.isNullOrEmpty()) return Pair("guest", "")
        val parts = userInfo.split(":")
        return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(parts[0], "")
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        closeQuietly()
        dataSpec = null
    }

    private fun closeQuietly() {
        try {
            file?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing file", e)
        } finally {
            file = null
            readBuffer = null
        }
    }
}

@UnstableApi
class SmbDataSourceFactory(
    private val config: SmbDataSourceConfig = SmbDataSourceConfig()
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return SmbDataSource(config)
    }
}

data class SmbDataSourceConfig(
    val bufferSizeBytes: Int = 1 * 1024 * 1024,
    val socketBufferSizeBytes: Int = 4 * 1024 * 1024,
    val soTimeoutMs: Int = 60_000,
)
