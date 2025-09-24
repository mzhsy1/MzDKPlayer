package org.mz.mzdkplayer.logic.model

import java.util.UUID

/**
 * 代表一个 NFS 连接配置的数据类
 * @param id 唯一标识符 (使用 UUID)
 * @param name 连接的显示名称 (例如 "我的 NFS 共享")
 * @param serverAddress NFS 服务器的 IP 地址或主机名
 * @param exportPath NFS 服务器上导出的路径 (例如 "/exported/path")
 * @param mountPoint 本地挂载点路径 (例如 "/mnt/nfs/myshare")
 * @param options 挂载选项 (例如 "rw,hard,intr,rsize=8192,wsize=8192")
 * @param username 用户名 (如果需要认证)
 * @param password 密码 (如果需要认证，注意: 实际应用中应加密存储)
 */
data class NFSConnection(
    val id: String = UUID.randomUUID().toString(), // 默认生成 UUID
    val name: String, // 显示名称
    val serverAddress: String, // NFS 服务器地址
    val shareName: String
)

