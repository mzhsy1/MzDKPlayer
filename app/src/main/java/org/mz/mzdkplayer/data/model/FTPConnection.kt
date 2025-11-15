package org.mz.mzdkplayer.data.model

import java.util.UUID

data class FTPConnection(
    val id: String ?= UUID.randomUUID().toString(), // 默认生成 UUID
    val name: String?, // 连接显示名称（如"办公室NAS"）
    val ip: String?,
    val port:Int?,
    val username: String?,
    val password: String?,
    val shareName: String ?// 链接文件夹名称
)