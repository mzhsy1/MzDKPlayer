package org.mz.mzdkplayer.tool

// SmbUtils.kt

import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okio.IOException
import java.io.InputStream

object SmbUtils {

    /**
     * 根据完整的 SMB URI 打开文件并返回 InputStream
     * 示例 URI: smb://user:pass@host/share/path/to/file.xml
     */
    @Throws(IOException::class)
    suspend fun openSmbFileInputStream(smbUri: Uri): InputStream {
        return withContext(Dispatchers.IO) {
            val host = smbUri.host ?: throw IOException("Invalid SMB URI: no host")
            val path = smbUri.path ?: throw IOException("Invalid SMB URI: no path")

            // 提取 share 名称（第一个路径段）
            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            if (pathSegments.isEmpty()) {
                throw IOException("Invalid SMB URI: no share or path")
            }
            val shareName = pathSegments[0]
            val filePath = pathSegments.drop(1).joinToString("/") // 剩余部分为文件路径

            // 从 URI 获取用户凭证（注意：明文密码不安全，仅用于演示）
            val userInfo = smbUri.userInfo
            val username = userInfo?.split(":")?.getOrNull(0) ?: "guest"
            val password = userInfo?.split(":")?.getOrNull(1) ?: ""
            val domain = "" // 可根据需要扩展

            val client = SMBClient()
            var connection: Connection? = null
            var session: Session? = null
            var share: DiskShare? = null
            var file: com.hierynomus.smbj.share.File? = null

            try {
                connection = client.connect(host)
                val authContext = AuthenticationContext(username, password.toCharArray(), domain)
                session = connection.authenticate(authContext)
                share = session.connectShare(shareName) as DiskShare

                file = share.openFile(
                    filePath,
                    setOf(AccessMask.GENERIC_READ),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )

                return@withContext file.inputStream.also {
                    // 注意：调用者负责关闭 InputStream，它会级联关闭 file/share/session/connection
                }
            } catch (e: Exception) {
                // 清理资源
                file?.close()
                share?.close()
                session?.close()
                connection?.close()
                throw IOException("Failed to open SMB file: $smbUri", e)
            }
        }
    }

    /**
     * 根据视频的 SMB URI，构造同名弹幕 XML 文件的 SMB URI
     * 例如: smb://host/share/movie.mp4 → smb://host/share/movie.xml
     */
    fun getDanmakuSmbUri(videoSmbUri: Uri): Uri {
        val videoPath = videoSmbUri.path ?: throw IllegalArgumentException("Invalid video URI")
        val basePath = videoPath.substringBeforeLast(".", "")
        val danmakuPath = "$basePath.xml"

        return videoSmbUri.buildUpon().path(danmakuPath).build()
    }
}