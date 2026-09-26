package org.mz.mzdkplayer.ui.common

import android.webkit.MimeTypeMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult // 注意这里导入了具体的实现类
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.buffer
import okio.source
import org.mz.mzdkplayer.tool.SmbUtils
import java.io.InputStream

/**
 * 让 Coil 能直接读 `smb:// / ftp:// / nfs:// / webdav(http):// / file://` 的图片。
 *
 * 电视端的图片查看与文件列表预览、手机端的图片查看页共用这一份实现
 * （放在 `main` 源集，两个 flavor 都能用；**不能**放进 `:core` —— 那会把 coil3 依赖带进业务层）。
 *
 * ⚠️ 注意：这些自定义 scheme 无法被 Coil 内置的 Fetcher 处理，必须使用带
 * [RemoteMediaFetcher] 的 ImageLoader，并且 model 必须传 [RemoteMedia]（不能只传 String 的 URI）。
 */
data class RemoteMedia(
    val uri: String,
    val type: String // "SMB", "FTP", "WEBDAV", "NFS", "LOCAL", "HTTP"
)

/** 创建/复用支持远程协议的 ImageLoader（调用方负责 remember，不要每次重组新建） */
@Composable
fun rememberRemoteMediaImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(RemoteMediaFetcher.Factory())
            }
            .build()
    }
}

/** 根据文件名后缀猜测 MIME 类型，兜底 image/jpeg */
private fun guessMimeType(uri: String): String {
    val extension = uri.substringAfterLast('/').substringAfterLast('.', "")
        .substringBefore('?')
        .lowercase()
    if (extension.isEmpty()) return "image/jpeg"
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension)
        ?: "image/jpeg"
}

class RemoteMediaFetcher(
    private val data: RemoteMedia,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val uri = data.uri.toUri()
        val inputStream: InputStream
        // 根据 type 调用不同的工具方法获取 InputStream
        withContext(Dispatchers.IO) {
            when (data.type) {
                "SMB" -> inputStream = SmbUtils.openSmbFileInputStream(uri, "pics")
                "FTP" -> inputStream = SmbUtils.openFtpFileInputStream(uri, "pics")
                "NFS" -> inputStream = SmbUtils.openNfsFileInputStream(uri, "pics")
                "WEBDAV" -> inputStream = SmbUtils.openWebDavFileInputStream(uri, "pics")
                "LOCAL" -> inputStream = SmbUtils.openLocalFileInputStream(uri)

                // 你的视频数据源处理逻辑中，WEBDAV 也使用了 http/https scheme，但 type 为 WEBDAV
                // 这里的 "HTTP" type 专门用于处理普通的 HTTP/HTTPS 链接
                "HTTP" -> inputStream = SmbUtils.openHTTPLinkXmlInputStream(uri.toString(), "pics")

                else -> throw IllegalArgumentException("Unsupported DataSource type: ${data.type}")
            }
        }
        // 将 InputStream 转换为 BufferedSource
        val bufferedSource = inputStream.source().buffer()

        // 关键点：
        // (1) 使用 ImageSource 工厂函数，必须传入 FileSystem.SYSTEM
        // (2) 返回 SourceFetchResult 实例
        return SourceFetchResult(
            source = ImageSource(
                source = bufferedSource,
                fileSystem = FileSystem.SYSTEM // 必传参数，用于创建临时文件
            ),
            // 根据后缀推断，避免 png/webp 被强制当成 jpeg 而选错解码器
            mimeType = guessMimeType(data.uri),
            dataSource = DataSource.NETWORK
        )
    }

    class Factory : Fetcher.Factory<RemoteMedia> {
        override fun create(
            data: RemoteMedia,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return RemoteMediaFetcher(data, options)
        }
    }
}
