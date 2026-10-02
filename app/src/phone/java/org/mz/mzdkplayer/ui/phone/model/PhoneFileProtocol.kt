package org.mz.mzdkplayer.ui.phone.model

/**
 * 文件页支持的协议（手机端五种协议 + SMB）。
 *
 * 原来是写在 `screen/PhoneFilesScreen.kt` 里的：导航层、设置页、各协议页都要用它，
 * 被一个页面文件「关」着不合适，所以独立成 model。
 *
 * [routeValue] 同时就是播放页的 `dataSourceType`（口径与电视端 `selectedDataSourceFactory` 一致），
 * 也是「协议浏览页」路由里的协议段；[supportsConnections] 决定文件页要不要给「新增连接」按钮。
 */
enum class PhoneFileProtocol(val routeValue: String, val supportsConnections: Boolean) {
    LOCAL("LOCAL", false),
    SMB("SMB", true),
    FTP("FTP", true),
    NFS("NFS", true),
    WEBDAV("WEBDAV", true),
    HTTP("HTTP", true),
}
