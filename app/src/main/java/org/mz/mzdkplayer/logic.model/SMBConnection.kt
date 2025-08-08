package org.mz.mzdkplayer.logic.model

import java.util.UUID

/**
 * SMB单个连接配置DataClass
 **/

data class SMBConnection(
    val id: String ,// 唯一ID
    val name: String, // 连接显示名称（如"办公室NAS"）
    val ip: String,
    val username: String,
    val password: String,
    val shareName: String
)
