package org.mz.mzdkplayer.logic.model

import java.util.UUID

data class HTTPLinkConnection (
    val id: String? = UUID.randomUUID().toString(), // 默认生成 UUID
    val name: String?, // 显示名称
    val serverAddress: String?, // HTTP 服务器地址
    val shareName: String?//挂载目录
)