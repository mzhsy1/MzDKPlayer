package org.mz.mzdkplayer.tool

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

object NfoReader {
    private const val TAG = "NfoReader"

    /**
     * 获取 NFO 文件的输入流，逻辑参考 VideoPlayerScreen 中的弹幕加载
     */
    @SuppressLint("Recycle")
    suspend fun getNfoStream(
        context: Context,
        videoUri: String,
        dataSourceType: String
    ): InputStream? = withContext(Dispatchers.IO) {
        val uri = videoUri.toUri()
        val scheme = uri.scheme?.lowercase() ?: return@withContext null

        // 构造 NFO URI (将后缀改为 .nfo)
        val nfoUri = constructNfoUri(uri, dataSourceType) ?: return@withContext null

        try {
            when (scheme) {
                "smb" -> {
                    // 使用 SMB 工具打开输入流
                    SmbUtils.openSmbFileInputStream(nfoUri, "video")
                }

                "http", "https" -> {
                    // 打开 HTTP/WebDAV 输入流
                    when (dataSourceType) {
                        "WEBDAV" -> {
                            SmbUtils.openWebDavFileInputStream(nfoUri, "video")
                        }

                        "HTTP" -> {
                            SmbUtils.openHTTPLinkXmlInputStream(nfoUri.toString(), "video")
                        }

                        else -> {
                            URL(nfoUri.toString()).openStream()
                        }
                    }
                }

                "file" -> {
                    // 打开本地文件输入流
                    context.contentResolver.openInputStream(nfoUri)
                }

                "ftp" -> {
                    // 使用 FTP 工具打开输入流
                    SmbUtils.openFtpFileInputStream(nfoUri, "video")
                }

                "nfs" -> {
                    // 使用 NFS 工具打开输入流
                    SmbUtils.openNfsFileInputStream(nfoUri, "video")
                }

                else -> {
                    Log.w(TAG, "Unsupported scheme for NFO URI: $nfoUri")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load NFO from $nfoUri (DataSource: $dataSourceType)", e)
            null
        }
    }

    /**
     * 根据视频 URI 构造对应的 NFO URI
     */
    private fun constructNfoUri(videoUri: Uri, dataSourceType: String): Uri? {
        return try {
            if (dataSourceType == "NFS") {
                // NFS 协议对路径解析有特殊要求（包含导出路径和内部路径，中间有冒号）
                getNfoNfsUri(videoUri)
            } else {
                val path = videoUri.path ?: return null
                // 没有扩展名时按统一口径在末尾追加 .nfo（此前是直接放弃查询）
                val nfoPath = SidecarPathLogic.withExtension(path, ".nfo")
                videoUri.buildUpon().path(nfoPath).build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error constructing NFO URI for $videoUri", e)
            null
        }
    }

    /**
     * 特殊处理 NFS 协议的 NFO URI 构造
     * 参考 SmbUtils.getDanmakuNfsUri
     */
    private fun getNfoNfsUri(videoNfsUri: Uri): Uri {
        val originalPath = videoNfsUri.path ?: throw IllegalArgumentException("Invalid NFS path")

        // 分离导出路径和内部路径 (e.g., /exported_path:path/within)
        val nfsParts = SidecarPathLogic.splitNfsRaw(originalPath)
            ?: throw IllegalArgumentException("Malformed NFS path")

        val exportedPath = nfsParts.first
        val pathWithinExport = nfsParts.second

        val nfoPathWithinExport = SidecarPathLogic.withExtension(pathWithinExport, ".nfo")
        val nfoNfsPath = SidecarPathLogic.joinNfsPath(exportedPath, nfoPathWithinExport)
        val host = videoNfsUri.host ?: throw IllegalArgumentException("Missing host")

        return "nfs://$host:$nfoNfsPath".toUri()
    }
}
