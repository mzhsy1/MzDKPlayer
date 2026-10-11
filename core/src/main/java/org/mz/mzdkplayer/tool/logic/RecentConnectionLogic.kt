package org.mz.mzdkplayer.tool.logic

import org.mz.mzdkplayer.data.model.RecentConnection

/**
 * 「最近访问的连接」纯逻辑（无 Android 依赖，单测见 `RecentConnectionLogicTest`）。
 *
 * 三件事：去重、排序、截断 —— 都在这里定义一次，存储层只管读写 JSON。
 *
 * 去重口径是 **协议类型 + 连接名**（大小写与首尾空白不敏感）：同一个连接被重复打开时只保留一条，
 * 并把最新的 [RecentConnection.route] 顶上去 —— 用户改过连接地址后，最近访问里点开的要
 * 是改完之后的新路由。反过来，不同协议下同名连接（比如都叫「我的NAS」的 SMB 与 WebDav）
 * 是两条独立记录，不能合并。
 */
object RecentConnectionLogic {

    /** 主页最多展示几条。太多会把「最近观看」挤下去，8 条足够覆盖常用存储 */
    const val MAX_ITEMS = 8

    /** 去重键：协议 + 连接名 */
    fun keyOf(type: String, name: String): String =
        "${type.trim().lowercase()}::${name.trim().lowercase()}"

    fun keyOf(connection: RecentConnection): String = keyOf(connection.type, connection.name)

    /**
     * 一条记录是否可用。
     *
     * 空白名称会让主页出现一张没字的卡片，空路由点下去必然导航失败，两者都直接丢弃；
     * 协议只认已知的五个，避免旧版本残留或手改 JSON 混进野值。
     */
    fun isValid(connection: RecentConnection): Boolean =
        connection.name.isNotBlank() &&
            connection.route.isNotBlank() &&
            connection.type in RecentConnection.KNOWN_TYPES

    /**
     * 把 [incoming] 并进 [existing]：同键旧记录被顶掉，按时间倒序，最多保留 [limit] 条。
     */
    fun merge(
        existing: List<RecentConnection>,
        incoming: RecentConnection,
        limit: Int = MAX_ITEMS
    ): List<RecentConnection> {
        if (!isValid(incoming)) return sanitize(existing, limit)
        val key = keyOf(incoming)
        return sanitize(
            listOf(incoming) + existing.filterNot { keyOf(it) == key },
            limit
        )
    }

    /** 删掉指定连接（用户删了存储配置后，最近访问里的死链一起清掉） */
    fun remove(
        existing: List<RecentConnection>,
        type: String,
        name: String
    ): List<RecentConnection> {
        val key = keyOf(type, name)
        return existing.filterNot { keyOf(it) == key }
    }

    /**
     * 读取磁盘数据后的清洗：丢脏数据 → 按去重键保留最新的那条 → 时间倒序 → 截断。
     */
    fun sanitize(
        list: List<RecentConnection>,
        limit: Int = MAX_ITEMS
    ): List<RecentConnection> {
        val deduped = LinkedHashMap<String, RecentConnection>()
        list.filter { isValid(it) }.forEach { item ->
            val key = keyOf(item)
            val old = deduped[key]
            if (old == null || item.lastVisited > old.lastVisited) {
                deduped[key] = item
            }
        }
        return deduped.values
            .sortedByDescending { it.lastVisited }
            .take(limit.coerceAtLeast(0))
    }
}
