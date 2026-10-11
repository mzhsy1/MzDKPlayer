package org.mz.mzdkplayer.data.model

/**
 * 主页「最近访问的连接」条目（SMB / WebDav / FTP / NFS / HTTP）。
 *
 * 各协议的连接配置本身存在各自的 SharedPreferences 里（见 `XxxConnectionRepository`），
 * 这里**只记「谁被打开过、怎么打开」**，不复制任何连接字段：
 * - [route] 存的就是点击卡片后要导航的那条路由（各协议文件列表页的路由），
 *   好处是「最近访问」不需要知道每个协议的路由参数怎么拼，直接 navigate 即可；
 * - [name] 只用于展示与去重，连接的地址 / 账号改了不影响这条记录能不能点开。
 */
data class RecentConnection(
    /** 协议类型，取值见 [Companion] 里的常量 */
    val type: String = TYPE_SMB,
    /** 连接显示名称 */
    val name: String = "",
    /** 打开该连接时导航的路由（含 base64 参数） */
    val route: String = "",
    /** 最后一次打开的时间戳（毫秒） */
    val lastVisited: Long = 0L
) {
    companion object {
        const val TYPE_SMB = "SMB"
        const val TYPE_WEBDAV = "WebDav"
        const val TYPE_FTP = "FTP"
        const val TYPE_NFS = "NFS"
        const val TYPE_HTTP = "HTTP"

        /** 已知协议类型，用于过滤脏数据 */
        val KNOWN_TYPES = setOf(TYPE_SMB, TYPE_WEBDAV, TYPE_FTP, TYPE_NFS, TYPE_HTTP)
    }
}
