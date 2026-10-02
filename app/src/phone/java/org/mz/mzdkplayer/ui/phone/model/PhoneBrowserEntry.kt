package org.mz.mzdkplayer.ui.phone.model

import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic

/**
 * 各协议目录浏览页共用的模型与分流规则（原 `screen/PhoneBrowserComponents.kt` 的前半段）。
 *
 * 各协议在 UI 层的差异只有「条目怎么列」和「播放地址怎么拼」；列表长什么样、怎么排序、
 * 点一行去哪儿，六套页面完全一致，所以这一层独立成 model，页面只负责把协议条目映射成
 * [PhoneBrowserEntry]。
 *
 * 扩展名判定不再自己实现：一律委托给 [PhoneMediaLogic]（`kindOf` / `isLyricSidecar`），
 * 避免「UI 一套、core 一套」两个口径漂移。
 */

internal data class PhoneBrowserEntry(
    val key: String,
    val name: String,
    val isDirectory: Boolean,
    /** 文件大小；目录或取不到大小时为 null */
    val size: Long? = null,
    /**
     * 该条目的媒体地址（目录与不支持的格式为 null）。
     *
     * 刮削与播放**共用**它：`media_cache` 的主键就是这个地址，两边必须逐字一致，
     * 否则会出现「列表显示了海报、点进去播放页又变成文件名」。
     *
     * 第五阶段起视频 / 音频 / 图片**都**会填这个字段（音频与图片不参与刮削，
     * 见 `PhoneScrapeLogic.scrapeTargets` 只挑视频）。
     */
    val playbackUri: String? = null,
)

internal sealed interface PhoneBrowserState {
    data object Loading : PhoneBrowserState

    /**
     * [path] 是**真正加载出来的**目录（不一定等于路由里请求的那个）：
     * 上级目录与播放地址都基于它计算，避免拿路由参数和实际结果对不上。
     */
    data class Ready(val path: String, val entries: List<PhoneBrowserEntry>) : PhoneBrowserState

    data class Failed(val message: String) : PhoneBrowserState

    /** 缺系统权限（存储 / 局域网）：[blockedActions] 由页面提供「去授权 / 重试」按钮 */
    data class Blocked(val title: String, val message: String) : PhoneBrowserState
}

/** 目录在前，其次按名称忽略大小写排序；电视端列表页没有排序，手机端统一加一层。 */
internal fun List<PhoneBrowserEntry>.sortedForBrowser(): List<PhoneBrowserEntry> =
    sortedWith(compareByDescending<PhoneBrowserEntry> { it.isDirectory }.thenBy { it.name.lowercase() })

/** 文件名是不是手机端能播的视频（口径与电视端一致：扩展名子串匹配） */
internal fun isVideoFileName(name: String): Boolean =
    PhoneMediaLogic.kindOf(name) == PhoneMediaLogic.Kind.VIDEO

/** 能播的视频条目（扩展名在白名单里） */
internal val PhoneBrowserEntry.isVideo: Boolean
    get() = !isDirectory && isVideoFileName(name)

/** 音频条目的判定口径与电视端一致（同样是扩展名子串匹配） */
internal fun isAudioFileName(name: String): Boolean =
    PhoneMediaLogic.kindOf(name) == PhoneMediaLogic.Kind.AUDIO

internal fun isImageFileName(name: String): Boolean =
    PhoneMediaLogic.kindOf(name) == PhoneMediaLogic.Kind.IMAGE

internal val PhoneBrowserEntry.isAudio: Boolean
    get() = !isDirectory && isAudioFileName(name)

internal val PhoneBrowserEntry.isImage: Boolean
    get() = !isDirectory && isImageFileName(name)

/** 交给 `:core` 的纯逻辑做分流（视频不归它管，这里只用到音频 / 图片两种） */
internal fun PhoneBrowserEntry.toMediaItem(): PhoneMediaLogic.Item = PhoneMediaLogic.Item(
    name = name,
    playbackUri = playbackUri,
    isDirectory = isDirectory,
)

/**
 * 点击本条目的分流结果：目录、视频、无地址与不支持的格式都返回 null，由调用方自己处理
 * （目录进下一层、视频走播放路由、其余弹「不支持」）。
 */
internal fun PhoneBrowserEntry.mediaOpen(
    siblings: List<PhoneBrowserEntry>,
    dataSourceType: String,
    connectionName: String,
): PhoneMediaLogic.MediaOpen? = PhoneMediaLogic.openFor(
    item = toMediaItem(),
    siblings = siblings.map { it.toMediaItem() },
    dataSourceType = dataSourceType,
    connectionName = connectionName,
)

/**
 * 列目录时要**提前算好媒体地址**的扩展名：视频 / 音频 / 图片。
 *
 * 刮削只认视频（见 `PhoneScrapeLogic.scrapeTargets`），但地址本身是三种都要的 ——
 * 播放、音频、图片共用同一个「列目录时算好」的口径，避免点击时再拼一遍。
 */
internal fun isPlayableMediaFileName(name: String): Boolean =
    isVideoFileName(name) || isAudioFileName(name) || isImageFileName(name)

/**
 * 同目录歌词文件（`.lrc`）。
 *
 * 这种文件**不列进浏览页**：它是音频的伴生文件，点开也没有播放器，
 * 而音频页会自己按同名规则去找它（见 `PhoneMediaLogic.lyricSiblingName`）。
 */
internal val PhoneBrowserEntry.isLyricSidecar: Boolean
    get() = !isDirectory && PhoneMediaLogic.isLyricSidecar(name)

/**
 * 六个协议浏览页共用的「点击一行」分流。
 *
 * [directoryTarget] 由各协议自己算（各自的目录路径口径不同：本地是绝对路径、FTP 是不带前导 `/`
 * 的显示路径、WebDAV/HTTP 是完整目录 URL……），为 null 表示这一行（HTTP 解析失败的目录）
 * 只能提示不支持。
 */
internal fun dispatchEntryClick(
    entry: PhoneBrowserEntry,
    siblings: List<PhoneBrowserEntry>,
    dataSourceType: String,
    connectionName: String,
    directoryTarget: String?,
    onOpenDirectory: (String) -> Unit,
    onPlayVideo: (sourceUri: String, name: String, connectionName: String) -> Unit,
    onOpenMedia: (PhoneMediaLogic.MediaOpen) -> Unit,
    onUnsupported: () -> Unit,
) {
    val uri = entry.playbackUri
    when {
        entry.isDirectory -> if (directoryTarget != null) {
            onOpenDirectory(directoryTarget)
        } else {
            onUnsupported()
        }

        entry.isVideo && uri != null -> {
            // 把「同目录的视频」写进播放列表：播放页的播放列表面板与「播放下一个」都读它。
            // 与音频 / 图片同一口径 —— 列表只收同目录的兄弟文件，不会顺着别的目录跳过去。
            // 连接名一并带过去，播放页写 `media_history` 时要用。
            VideoPlaylistRepository.setPlaylist(
                siblings.mapNotNull { sibling ->
                    val siblingUri = sibling.playbackUri
                    if (sibling.isVideo && siblingUri != null) {
                        VideoItem(
                            uri = siblingUri,
                            fileName = sibling.name,
                            dataSourceType = dataSourceType,
                            connectionName = connectionName,
                        )
                    } else {
                        null
                    }
                }
            )
            onPlayVideo(uri, entry.name, connectionName)
        }

        else -> {
            val open = entry.mediaOpen(siblings, dataSourceType, connectionName)
            if (open != null) onOpenMedia(open) else onUnsupported()
        }
    }
}
