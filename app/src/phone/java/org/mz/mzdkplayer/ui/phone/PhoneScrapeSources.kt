package org.mz.mzdkplayer.ui.phone

import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol
import org.mz.mzdkplayer.viewmodel.SettingsUiState

/**
 * 手机端「哪个来源要刮削」的口径（原写在 `PhoneApp.kt` 末尾）。
 *
 * 存储是**与电视端共用**的同一份 `SettingsRepository`，所以这里只是把
 * `SettingsUiState` 的六个开关按协议映射出来，不改任何存储键。
 */

/** 该来源在设置里是否参与刮削（电视端「刮削与媒体库」那六个开关） */
internal fun SettingsUiState.sourceEnabled(protocol: PhoneFileProtocol): Boolean = when (protocol) {
    PhoneFileProtocol.LOCAL -> local
    PhoneFileProtocol.SMB -> smb
    PhoneFileProtocol.FTP -> ftp
    PhoneFileProtocol.NFS -> nfs
    PhoneFileProtocol.WEBDAV -> webdav
    PhoneFileProtocol.HTTP -> http
}

/**
 * 进目录时是否自动刮削 = 手机端总开关（默认关）**且** 该来源开关打开。
 *
 * 两个都满足才会联网，避免「我只想看文件名，进目录却开始下载海报」。
 */
internal fun SettingsUiState.autoScrapeEnabled(protocol: PhoneFileProtocol): Boolean =
    phoneAutoScrape && sourceEnabled(protocol)

/** 与 `SettingsViewModel.toggleSource` 的入参对齐（那几个字符串是电视端的历史口径，不能改） */
internal val PhoneFileProtocol.scrapeKey: String
    get() = when (this) {
        PhoneFileProtocol.LOCAL -> "Local"
        PhoneFileProtocol.SMB -> "SMB"
        PhoneFileProtocol.FTP -> "FTP"
        PhoneFileProtocol.NFS -> "NFS"
        PhoneFileProtocol.WEBDAV -> "WebDav"
        PhoneFileProtocol.HTTP -> "HTTP"
    }
