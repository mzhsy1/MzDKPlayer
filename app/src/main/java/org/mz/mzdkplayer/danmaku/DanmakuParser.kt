package org.mz.mzdkplayer.danmaku

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

// 👇 这是你要实现的核心函数 —— 从文件（本地或SMB）读取并解析弹幕XML
suspend fun getDanmakuXmlFromFile(
    danmakuFileInputStream: InputStream? // 从SMB或本地打开的XML文件输入流
): DanmakuResponse {
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()

    val doc = withContext(Dispatchers.IO) {
        dBuilder.parse(danmakuFileInputStream)
    }
    doc.documentElement.normalize()

    // 提取全局信息
    val chatServer = doc.getElementsByTagName("chatserver").item(0)?.textContent ?: ""
    val chatId = doc.getElementsByTagName("chatid").item(0)?.textContent?.toLong() ?: 0L
    val maxLimit = doc.getElementsByTagName("maxlimit").item(0)?.textContent?.toInt() ?: 0
    val state = doc.getElementsByTagName("state").item(0)?.textContent?.toInt() ?: 0
    val realName = doc.getElementsByTagName("real_name").item(0)?.textContent?.toInt() ?: 0
    val source = runCatching {
        doc.getElementsByTagName("source").item(0)?.textContent ?: ""
    }.getOrDefault("")

    // 提取弹幕列表
    val data = mutableListOf<DanmakuData>()
    val danmakuNodes = doc.getElementsByTagName("d")

    for (i in 0 until danmakuNodes.length) {
        val danmakuNode = danmakuNodes.item(i)
        val pAttr = danmakuNode.attributes.getNamedItem("p")?.textContent
        val text = danmakuNode.textContent ?: ""

        if (pAttr != null) {
            data.add(DanmakuData.fromString(pAttr, text))
        }
    }

    return DanmakuResponse(chatServer, chatId, maxLimit, state, realName, source, data)
}