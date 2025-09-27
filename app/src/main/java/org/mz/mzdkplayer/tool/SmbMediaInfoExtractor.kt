package org.mz.mzdkplayer.tool

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.util.EnumSet
import java.util.Locale

/**
 * 使用 SMBJ 库和 Android 原生 MediaMetadataRetriever 提取媒体信息的工具类
 * 支持 file, content, http(s), smb 协议，其中 smb 协议使用 smbj 库处理。
 * 注意：需要在项目中添加 SMBJ 库依赖。
 */
class SmbMediaInfoExtractor(private val context: Context) {

    companion object {
        private const val TAG = "SmbMediaInfoExtractor"
    }

    private val smbClient = SMBClient() // 可以配置连接参数

    /**
     * 提取媒体文件的元数据信息，支持 file, content, http(s), smb 协议
     * @param mediaUri 媒体文件的 Uri
     * @return 包含元数据的 Map，key 为信息类型，value 为信息内容
     */
    suspend fun extractMetadata(mediaUri: Uri): Map<String, String> = withContext(Dispatchers.IO) {
        val metadataMap = mutableMapOf<String, String>()
        val retriever = MediaMetadataRetriever()

        try {
            when (mediaUri.scheme?.lowercase()) {
                "file" -> {
                    val path = mediaUri.path
                    if (path != null) {
                        retriever.setDataSource(path)
                    } else {
                        throw IOException("File URI path is null")
                    }
                }
                "content" -> retriever.setDataSource(context, mediaUri)
                "http", "https" -> retriever.setDataSource(mediaUri.toString())
                "smb" -> {
                    // SMB 协议 - 使用 SMBJ
                    val smbFileHandle = openSmbFileInputStream(mediaUri)
                    if (smbFileHandle != null) {
                        // 将 SMB 文件内容下载到临时本地文件
                        val tempFile = downloadSmbFileToTemp(smbFileHandle, mediaUri.lastPathSegment)
                        if (tempFile != null && tempFile.exists()) {
                            // 使用临时文件路径设置数据源
                            retriever.setDataSource(tempFile.absolutePath)
                            // 可选：使用后删除临时文件，但需确保 retriever 不再需要它
                            // tempFile.delete()
                        } else {
                            throw IOException("Failed to download SMB file to temporary location")
                        }
                        // 关闭 SMB 文件句柄
                        smbFileHandle.close()
                    } else {
                        throw IOException("Failed to open SMB file stream")
                    }
                }
                else -> throw IOException("Unsupported URI scheme: ${mediaUri.scheme}")
            }

            // 成功设置数据源后，提取元数据
            extractAndPutMetadata(retriever, metadataMap)

            Log.d(TAG, "Successfully extracted metadata for URI: $mediaUri")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata for URI: $mediaUri", e)
            metadataMap["Error"] = e.message ?: "Unknown error"
        } finally {
            // 释放资源
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }

        metadataMap.toMap() // 返回不可变Map
    }

    /**
     * 获取媒体文件的缩略图/封面，支持多种协议
     * @param mediaUri 媒体文件的 Uri
     * @param option 缩略图选项 (默认为 OPTION_CLOSEST_SYNC)
     *               可选值: MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
     *                      MediaMetadataRetriever.OPTION_NEXT_SYNC,
     *                      MediaMetadataRetriever.OPTION_CLOSEST_SYNC (默认),
     *                      MediaMetadataRetriever.OPTION_CLOSEST
     * @return 缩略图 Bitmap，如果无法获取则返回 null
     */
    suspend fun extractThumbnail(mediaUri: Uri, option: Int = MediaMetadataRetriever.OPTION_CLOSEST_SYNC): Bitmap? = withContext(Dispatchers.IO) {
        var thumbnail: Bitmap? = null
        val retriever = MediaMetadataRetriever()

        try {
            when (mediaUri.scheme?.lowercase()) {
                "file" -> {
                    val path = mediaUri.path
                    if (path != null) {
                        retriever.setDataSource(path)
                    } else {
                        throw IOException("File URI path is null")
                    }
                }
                "content" -> retriever.setDataSource(context, mediaUri)
                "http", "https" -> retriever.setDataSource(mediaUri.toString())
                "smb" -> {
                    val smbFileHandle = openSmbFileInputStream(mediaUri)
                    if (smbFileHandle != null) {
                        val tempFile = downloadSmbFileToTemp(smbFileHandle, mediaUri.lastPathSegment)
                        if (tempFile != null && tempFile.exists()) {
                            retriever.setDataSource(tempFile.absolutePath)
                        } else {
                            throw IOException("Failed to download SMB file for thumbnail extraction")
                        }
                        smbFileHandle.close()
                    } else {
                        throw IOException("Failed to open SMB file stream for thumbnail")
                    }
                }
                else -> throw IOException("Unsupported URI scheme: ${mediaUri.scheme}")
            }

            thumbnail = retriever.getFrameAtTime(0, option)
            Log.d(TAG, "Successfully extracted thumbnail for URI: ${mediaUri.toString()}")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting thumbnail for URI: ${mediaUri.toString()}, Option: $option", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever for thumbnail", e)
            }
        }

        thumbnail
    }

    /**
     * 从 MediaMetadataRetriever 实例中提取常用元数据并放入 Map
     */
    private fun extractAndPutMetadata(retriever: MediaMetadataRetriever, map: MutableMap<String, String>) {
        putIfNotNull(map, "Title", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
        putIfNotNull(map, "Artist", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
        putIfNotNull(map, "Album", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
        putIfNotNull(map, "Author", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR))
        putIfNotNull(map, "Composer", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER))
        putIfNotNull(map, "Date", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE))
        putIfNotNull(map, "Year", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR))
        putIfNotNull(map, "MimeType", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
        putIfNotNull(map, "VideoWidth", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        putIfNotNull(map, "VideoHeight", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        putIfNotNull(map, "VideoFrameCount", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT))
        putIfNotNull(map, "VideoRotation", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
        putIfNotNull(map, "Bitrate", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE))
        putIfNotNull(map, "SampleRate", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE))
        putIfNotNull(map, "TrackNumber", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER))
        putIfNotNull(map, "AlbumArtist", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
        putIfNotNull(map, "Genre", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
        putIfNotNull(map, "Location", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (durationStr != null) {
            val durationMs = durationStr.toLongOrNull()
            if (durationMs != null) {
                map["DurationMs"] = durationMs.toString()
                map["DurationFormatted"] = formatDuration(durationMs)
            }
        }
    }

    private fun putIfNotNull(map: MutableMap<String, String>, key: String, value: String?) {
        if (value != null) {
            map[key] = value
        }
    }

    /**
     * 将毫秒数格式化为 HH:MM:SS 或 MM:SS 的字符串，使用默认 Locale
     * @param durationMs 持续时间（毫秒）
     * @return 格式化的时间字符串
     */
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 根据完整的 SMB URI 打开文件并返回 SMBJ File 句柄
     * 示例 URI: smb://user:pass@host/share/path/to/file.mp4
     * @param smbUri SMB 文件的 Uri
     * @return SMBJ File 句柄，如果打开失败则返回 null
     * @throws IOException 如果 URI 格式错误或连接失败
     */
    @Throws(IOException::class)
    private suspend fun openSmbFileInputStream(smbUri: Uri): File? = withContext(Dispatchers.IO) {
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

        var connection: Connection? = null
        var session: Session? = null
        var share: DiskShare? = null
        var file: File? = null

        try {
            connection = smbClient.connect(host)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            session = connection.authenticate(authContext)
            share = session.connectShare(shareName) as DiskShare

            file = share.openFile(
                filePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )

            Log.d(TAG, "Successfully opened SMB file: $smbUri")
            return@withContext file // 返回句柄，调用者负责关闭
        } catch (e: Exception) {
            Log.e(TAG, "Error opening SMB file: $smbUri", e)
            // 清理资源
            try { file?.close() } catch (ex: Exception) { Log.e(TAG, "Error closing file", ex) }
            try { share?.close() } catch (ex: Exception) { Log.e(TAG, "Error closing share", ex) }
            try { session?.close() } catch (ex: Exception) { Log.e(TAG, "Error closing session", ex) }
            try { connection?.close() } catch (ex: Exception) { Log.e(TAG, "Error closing connection", ex) }
            throw IOException("Failed to open SMB file: $smbUri", e)
        }
    }

    /**
     * 将 SMBJ File 句柄的内容下载到临时本地文件
     * @param smbFileHandle 已打开的 SMBJ File 句柄
     * @param fileNameHint 用于生成临时文件名的提示
     * @return 临时文件对象，如果下载失败则返回 null
     */
    private suspend fun downloadSmbFileToTemp(smbFileHandle: File, fileNameHint: String?): java.io.File? = withContext(Dispatchers.IO) {
        var outputStream: FileOutputStream? = null
        var tempFile: java.io.File? = null

        try {
            // 创建临时文件
            val suffix = if (fileNameHint != null) {
                val ext = fileNameHint.substringAfterLast('.', "")
                if (ext.isNotEmpty()) ".$ext" else ".tmp"
            } else ".tmp"
            tempFile = java.io.File.createTempFile("smb_temp_", suffix, context.cacheDir)
            Log.d(TAG, "Created temporary file: ${tempFile.absolutePath}")

            outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(8192)
            var totalRead = 0L
            while (true) {
                // 使用正确的 read 方法签名: read(buffer, fileOffset)
                val bytesRead = smbFileHandle.read(buffer, totalRead)
                if (bytesRead <= 0) break
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
            }
            outputStream.flush()

            Log.d(TAG, "Successfully downloaded SMB file to temporary location: ${tempFile.absolutePath}, Size: $totalRead bytes")
            return@withContext tempFile

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading SMB file to temp: ${e.message}", e)
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete() // 清理失败的临时文件
            }
        } finally {
            try {
                outputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing output stream during SMB download", e)
            }
        }

        null
    }
}



