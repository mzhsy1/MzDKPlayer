package org.mz.mzdkplayer.ui.phone

import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic
import org.mz.mzdkplayer.tool.Tools.fromBase64
import org.mz.mzdkplayer.tool.Tools.toBase64

/**
 * 手机端路由表。
 *
 * 参数一律走 **Base64（URL_SAFE / NO_WRAP）**，与电视端 `MzDKPlayerAPP` 的口径一致：
 * SMB 路径里带 `/`、中文、`#` 之类字符时，直接塞进路由会截断或解析失败。
 */
object PhoneRoutes {

    // ---- 底部导航的三个主页面 ----
    const val HOME = "phone/home"
    const val FILES = "phone/files"
    const val SETTINGS = "phone/settings"

    /**
     * 设置页的二级分类（第八阶段）。
     *
     * 与 [SETTINGS] 是两段不同的路由：`phone/settings` 是设置首页（底部标签栏的三个主页之一），
     * `phone/settings/playback` 之类的分类页是二级页面，进入后底部标签栏会收起。
     * 导航按路径段数匹配，两者不会互相抢。
     */
    const val SETTINGS_SECTION = "phone/settings/{category}"

    // ---- 二级页面 ----
    const val SMB_BROWSER = "phone/smb/{connectionId}/{path}"

    /**
     * 通用协议浏览页（第三阶段）：[protocol] 取 `LOCAL / SMB 之外的 FTP / NFS / WEBDAV / HTTP`，
     * 同时就是播放页的 `dataSourceType`。
     */
    const val BROWSER = "phone/browser/{protocol}/{connectionId}/{path}"

    /**
     * 视频播放页（第七阶段补齐）。
     *
     * [title] 已经在第七阶段改成**文件名**（此前有的入口塞的是刮削标题）：播放页要用它
     * 写 `media_history`、也要用它去 `media_cache` 查刮削结果，展示标题由播放页自己拼。
     *
     * [connectionName] 是第七阶段新加的：写播放历史要用（本机文件为空串）。
     */
    const val PLAYER = "phone/player/{sourceUri}/{dataSourceType}/{title}/{connectionName}"

    /**
     * 手动匹配页（第四阶段）：[videoUri] 同时是 `media_cache` 的主键与播放地址。
     * [dataSourceType] / [connectionName] 原样写回缓存，所以要和列表页用的那一份完全一致。
     */
    const val MATCH = "phone/match/{videoUri}/{dataSourceType}/{fileName}/{connectionName}"

    /**
     * 影片详情页（第四阶段）：沉浸式展示一个视频文件的刮削结果。
     *
     * 参数与 [MATCH] **完全一致** —— 详情页里的「重新匹配」要原样带过去，
     * 所以两者的编码规则写在同一处（[mediaArgs]）。
     */
    const val DETAIL = "phone/detail/{videoUri}/{dataSourceType}/{fileName}/{connectionName}"

    /**
     * 音频播放页（第五阶段）。
     *
     * 只带 [currentIndex]：播放列表由列表页算好后写进 `AudioPlaylistRepository`（会落盘），
     * 播放页从仓库读回来 —— 一路把整张列表塞进路由会把地址撑得很长，而 `AudioItem`
     * 里本来就有一个字段是播放地址，重复传递没有意义。
     *
     * [dataSourceType] / [connectionName] 仍要带：播放进度写入 `media_history` 时要用它们。
     */
    const val AUDIO = "phone/audio/{currentIndex}/{dataSourceType}/{connectionName}"

    /**
     * 图片查看页（第五阶段）。
     *
     * [uris] 是**本目录**所有图片地址各自的 Base64 再用 `,` 拼起来的一整段
     * （Base64 的 URL_SAFE 字符集里没有逗号，不需要转义），[index] 是点开的那一张。
     * 带上整张列表是为了支持左右滑动切换上一张 / 下一张。
     */
    const val IMAGE = "phone/image/{dataSourceType}/{uris}/{index}"

    /**
     * 空参占位符。
     *
     * 导航路由的路径段**不能为空**（`Base64("")` 就是空串，`phone/browser/FTP//xxx` 匹配不上），
     * 所以空值统一写成 `~` —— Base64 的字符集里没有 `~`，不会和真实编码值撞车。
     */
    private const val BLANK_ARG = "~"

    /** 设置页的二级分类页：[category] 取 `PhoneSettingCategory.routeValue` */
    fun settingsSection(category: String): String = "phone/settings/$category"

    /** SMB 目录浏览页：[connectionId] 是 `SMBConnection.id`，[path] 是显示用正斜杠路径（`/` 表示共享根目录）。 */
    fun smbBrowser(connectionId: String, path: String): String =
        "phone/smb/${connectionId.toBase64()}/${path.toBase64()}"

    /**
     * 协议浏览页：[connectionId] 是各协议 `XxxConnection.id`（本机协议传空串），
     * [path] 是该协议自己的目录口径（本地=绝对路径、FTP=不带前导 `/` 的显示路径、
     * NFS=导出内绝对路径、WebDAV/HTTP=完整目录 URL）。
     */
    fun browser(protocol: String, connectionId: String, path: String): String =
        "phone/browser/$protocol/" +
                "${connectionId.ifBlank { BLANK_ARG }.toBase64()}/" +
                "${path.ifBlank { BLANK_ARG }.toBase64()}"

    /**
     * 视频播放页：[sourceUri] 是完整播放地址（已含账号密码），[dataSourceType] 是 SMB/LOCAL/FTP…，
     * [fileName] 是原始文件名，[connectionName] 是本机文件时为空的连接名。
     */
    fun player(
        sourceUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String = "",
    ): String =
        "phone/player/${sourceUri.toBase64()}/$dataSourceType/" +
                "${fileName.toBase64()}/${connectionName.ifBlank { BLANK_ARG }.toBase64()}"

    /** 手动匹配页：[connectionName] 本机文件为空（用 `~` 占位） */
    fun match(videoUri: String, dataSourceType: String, fileName: String, connectionName: String): String =
        "phone/match/${mediaArgs(videoUri, dataSourceType, fileName, connectionName)}"

    /** 影片详情页（参数口径与 [match] 一致） */
    fun detail(videoUri: String, dataSourceType: String, fileName: String, connectionName: String): String =
        "phone/detail/${mediaArgs(videoUri, dataSourceType, fileName, connectionName)}"

    /** 音频播放页：[currentIndex] 是在 `AudioPlaylistRepository` 那份列表里的下标 */
    fun audio(currentIndex: Int, dataSourceType: String, connectionName: String): String =
        "phone/audio/$currentIndex/$dataSourceType/" +
                "${connectionName.ifBlank { BLANK_ARG }.toBase64()}"

    /**
     * 图片查看页：[uris] 已按 `PhoneMediaLogic.openFor` 的口径收成「本目录的图片」，
     * [index] 是点开的那一张的下标。
     */
    fun image(dataSourceType: String, uris: List<String>, index: Int): String =
        "phone/image/$dataSourceType/${PhoneMediaLogic.joinArgs(uris.map { it.toBase64() })}/$index"

    /** 详情页与匹配页共用的参数段 */
    private fun mediaArgs(
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String,
    ): String =
        "${videoUri.toBase64()}/$dataSourceType/" +
                "${fileName.toBase64()}/${connectionName.ifBlank { BLANK_ARG }.toBase64()}"

    /**
     * 还原 [BLANK_ARG] 占位的路由参数（顺带把 Base64 解回去）。
     *
     * 空值在路由里写的是 `BLANK_ARG` 的 Base64（`fg==`），所以**必须先解码再比**
     * ——直接拿原串比 `~` 是比不中的（第七阶段修掉的老问题：本机文件的连接名
     * 一直带着一个 `~` 落进 `media_cache` 与播放历史）。
     */
    fun decodeArg(raw: String?): String {
        if (raw.isNullOrEmpty() || raw == BLANK_ARG) return ""
        val decoded = raw.fromBase64()
        return if (decoded == BLANK_ARG) "" else decoded
    }
}
