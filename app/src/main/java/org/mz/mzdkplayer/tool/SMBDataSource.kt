package org.mz.mzdkplayer.tool

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import okio.IOException
import java.io.EOFException
import java.io.InputStream

@UnstableApi
class SmbDataSource : DataSource {
    private var dataSpec: DataSpec? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: File? = null
    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0
    private var opened = false
    override fun addTransferListener(transferListener: TransferListener) {

    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        val uri = dataSpec.uri
        val host = uri.host ?: throw IOException("Invalid SMB URI: no host")
        val path = uri.path ?: throw IOException("Invalid SMB URI: no path")
        val shareName = path.split("/").getOrNull(1) ?: throw IOException("Invalid SMB URI: could not extract share name")
        val filePath = path.substringAfter(shareName).trimStart('/')

        // 从 URI 或其他地方获取凭证信息（注意安全！）
        // 示例：假设用户信息通过 URI 的 userInfo 部分传递（不安全，仅作示例）
        val username = uri.userInfo?.split(":")?.getOrNull(0) ?: "guest"
        val password = uri.userInfo?.split(":")?.getOrNull(1) ?: ""
        val domain = "" // 如果需要域名

        val client = SMBClient()
        connection = client.connect(host)
        val authContext = AuthenticationContext(username, password.toCharArray(), domain)
        session = connection!!.authenticate(authContext)
        share = session!!.connectShare(shareName) as DiskShare

        // 打开文件
        file = share!!.openFile(
            filePath,
            setOf(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null
        )

        inputStream = file!!.inputStream

        // 处理随机访问 (seeking)
        val position = dataSpec.position
        if (position > 0) {
            // SMBJ 的 InputStream 可能不支持 skip，或者效率不高。可以使用 readAndDiscard。
            var skipped: Long = 0
            while (skipped < position) {
                val skipBytes = inputStream!!.skip(position - skipped)
                if (skipBytes <= 0) {
                    throw EOFException("Unexpected end of stream while skipping")
                }
                skipped += skipBytes
            }
        }

        val length = file!!.fileInformation.standardInformation.endOfFile
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            length - dataSpec.position
        }
        if (bytesRemaining < 0) {
            throw IOException("Length is negative: $bytesRemaining")
        }

        opened = true
        // 通知监听器打开完成（如果有）
        // transferStarted(dataSpec, ...) 如果需要可以调用

        return bytesRemaining
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }
        val bytesToRead = minOf(readLength.coerceAtLeast(0), bytesRemaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        if (bytesToRead == 0) {
            return 0
        }
        val bytesRead = inputStream!!.read(buffer, offset, bytesToRead)
        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw EOFException("Unexpected end of stream")
            }
            return C.RESULT_END_OF_INPUT
        }
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        // 通知监听器数据传输（如果有）
        // bytesTransferred(bytesRead) 如果需要可以调用
        return bytesRead
    }

    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    override fun close() {
        opened = false
        inputStream?.close()
        file?.close()
        share?.close()
        session?.close()
        connection?.close()
        inputStream = null
        file = null
        share = null
        session = null
        connection = null
        dataSpec = null
        bytesRemaining = 0
    }




}

// 还需要一个 Factory 来创建 DataSource
@UnstableApi
class SmbDataSourceFactory : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return SmbDataSource()
    }
}
