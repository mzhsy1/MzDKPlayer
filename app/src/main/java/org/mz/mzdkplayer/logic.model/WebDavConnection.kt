
package org.mz.mzdkplayer.logic.model

import java.util.UUID

/**
 * 代表一个 WebDAV 连接配置的数据类
 * @param id 唯一标识符 (使用 UUID)
 * @param name 连接的显示名称 (例如 "我的云盘")
 * @param baseUrl WebDAV 服务器的基础 URL
 * @param username 用户名
 * @param password 密码 (注意: 实际应用中应加密存储)
 */

data class WebDavConnection(
    val id: String? = UUID.randomUUID().toString(), // 默认生成 UUID
    val name: String?, // 显示名称
    val baseUrl: String?, // 基础 URL
    val username: String?,
    val password: String ?// 注意：实际项目中应加密存储密码
)



