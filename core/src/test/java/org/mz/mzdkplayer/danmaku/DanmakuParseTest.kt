package org.mz.mzdkplayer.danmaku

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * 弹幕模块的 JVM 单元测试，覆盖两段最容易出错、又完全不依赖 Android 的核心逻辑：
 *
 * 1. [DanmakuData.fromString] —— 把 XML 里 `p` 属性那一串逗号分隔字段转成弹幕对象。
 *    弹幕站点的 `p` 字段在「旧版纯数字用户ID / 新版字符串哈希」「字段缺失 / 字段多余」
 *    上历史包袱很重，一旦某一列错位，弹幕就会颜色错、时间错、甚至崩在播放页。
 * 2. [getDanmakuXmlFromFile] —— 弹幕 XML 的全局信息与弹幕列表解析。
 *
 * 这两个函数本身不碰 `android.*`，所以能直接在 JVM 上跑，测到的就是线上那一份实现。
 */
class DanmakuParseTest {

    // ────────────────────────────── p 属性：正常字段 ──────────────────────────────

    @Test
    fun `p 属性 - 标准八字段全部解析到位`() {
        val d = DanmakuData.fromString("12.345,1,25,16777215,1600000000,0,abc123,987", "前方高能")
        assertEquals(12.345f, d.time, 0.0001f)
        assertEquals(1, d.mode)
        assertEquals(25, d.size)
        assertEquals(16777215, d.color)
        assertEquals(1600000000L, d.timestamp)
        assertEquals(0, d.pool)
        assertEquals("abc123", d.userIdHash)
        assertEquals(987L, d.rowId)
        assertEquals("前方高能", d.content)
    }

    @Test
    fun `p 属性 - 顶部与底部弹幕的模式值原样保留`() {
        assertEquals(4, DanmakuData.fromString("1,4,25,0,1,0,u,1", "底").mode)
        assertEquals(5, DanmakuData.fromString("1,5,25,0,1,0,u,1", "顶").mode)
    }

    @Test
    fun `p 属性 - 旧版纯数字用户ID与新版本字符串哈希都支持`() {
        assertEquals("12345678", DanmakuData.fromString("1,1,25,0,1,0,12345678,1", "旧").userIdHash)
        assertEquals("a1b2c3d4e5f6", DanmakuData.fromString("1,1,25,0,1,0,a1b2c3d4e5f6,1", "新").userIdHash)
    }

    @Test
    fun `p 属性 - 各字段前后空格会被裁掉`() {
        val d = DanmakuData.fromString(" 1.5 , 4 , 18 , 255 , 1600000001 , 1 , u1 , 7 ", "带空格")
        assertEquals(1.5f, d.time, 0.0001f)
        assertEquals(4, d.mode)
        assertEquals(18, d.size)
        assertEquals(255, d.color)
        assertEquals(1600000001L, d.timestamp)
        assertEquals(1, d.pool)
        assertEquals("u1", d.userIdHash)
        assertEquals(7L, d.rowId)
    }

    // ────────────────────────────── p 属性：容错与兜底 ──────────────────────────────

    @Test
    fun `p 属性 - 字段不足时缺失项用默认值兜底不抛异常`() {
        // 只给了时间与模式
        val d = DanmakuData.fromString("5.5,4", "半截字段")
        assertEquals(5.5f, d.time, 0.0001f)
        assertEquals(4, d.mode)                      // 显式给了就用给的
        assertEquals(25, d.size)                     // 字号默认 25
        assertEquals(0xFFFFFF, d.color)              // 颜色默认白
        assertEquals(0L, d.timestamp)
        assertEquals(0, d.pool)
        assertEquals("", d.userIdHash)
        assertEquals(0L, d.rowId)
        assertEquals("半截字段", d.content)
    }

    @Test
    fun `p 属性 - 空字符串走全部默认值`() {
        val d = DanmakuData.fromString("", "空属性")
        assertEquals(0f, d.time, 0.0001f)
        assertEquals(1, d.mode)                      // 默认滚动弹幕
        assertEquals(25, d.size)
        assertEquals(0xFFFFFF, d.color)
        assertEquals(0L, d.timestamp)
        assertEquals(0, d.pool)
        assertEquals("", d.userIdHash)
        assertEquals(0L, d.rowId)
    }

    @Test
    fun `p 属性 - 非数字字段回退默认值而不抛异常`() {
        val d = DanmakuData.fromString("abc,abc,abc,abc,abc,abc,abc,abc", "脏数据")
        assertEquals(0f, d.time, 0.0001f)
        assertEquals(1, d.mode)
        assertEquals(25, d.size)
        assertEquals(0xFFFFFF, d.color)
        assertEquals(0L, d.timestamp)
        assertEquals(0L, d.rowId)
    }

    @Test
    fun `p 属性 - 整数字段写成小数时回退默认值`() {
        val d = DanmakuData.fromString("1.0,1.0,25,16711680,2,0,u,3", "小数模式")
        assertEquals(1, d.mode)                      // "1.0" 不是合法 Int，回退默认滚动
        assertEquals(16711680, d.color)
    }

    @Test
    fun `p 属性 - 多余字段被忽略只取前八列`() {
        val d = DanmakuData.fromString("1,1,25,0,2,0,u,7,extra,more", "多余字段")
        assertEquals("u", d.userIdHash)
        assertEquals(7L, d.rowId)
    }

    // ────────────────────────────── p 属性：正文 ──────────────────────────────

    @Test
    fun `p 属性 - 正文原样保留不参与分割`() {
        val text = "1,2,3 看着像字段,但不是"
        assertEquals(text, DanmakuData.fromString("1,1,25,0,1,0,u,1", text).content)
    }

    @Test
    fun `p 属性 - 正文为空时仍能构造出弹幕对象`() {
        assertEquals("", DanmakuData.fromString("1,1,25,0,1,0,u,1", "").content)
    }

    // ────────────────────────────── XML 解析 ──────────────────────────────

    private fun parse(xml: String): DanmakuResponse = runBlocking {
        getDanmakuXmlFromFile(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `XML - 全局信息与弹幕列表都被解析`() {
        val resp = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <i>
              <chatserver>chat.bilibili.com</chatserver>
              <chatid>12345</chatid>
              <mission>0</mission>
              <maxlimit>8000</maxlimit>
              <state>0</state>
              <real_name>0</real_name>
              <source>k-v</source>
              <d p="1.5,1,25,16777215,1600000000,0,abc,1">第一条</d>
              <d p="2.5,5,18,16711680,1600000001,0,def,2">第二条</d>
            </i>
            """.trimIndent()
        )
        assertEquals("chat.bilibili.com", resp.chatServer)
        assertEquals(12345L, resp.chatId)
        assertEquals(8000, resp.maxLimit)
        assertEquals(0, resp.state)
        assertEquals(0, resp.realName)
        assertEquals("k-v", resp.source)
        assertEquals(2, resp.data.size)
        assertEquals("第一条", resp.data[0].content)
        assertEquals(1.5f, resp.data[0].time, 0.0001f)
        assertEquals(16777215, resp.data[0].color)
        assertEquals("第二条", resp.data[1].content)
        assertEquals(5, resp.data[1].mode)
        assertEquals(16711680, resp.data[1].color)
    }

    @Test
    fun `XML - 全局字段缺失时退回默认值`() {
        val resp = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <i>
              <d p="1,1,25,0,1,0,u,1">只有弹幕</d>
            </i>
            """.trimIndent()
        )
        assertEquals("", resp.chatServer)
        assertEquals(0L, resp.chatId)
        assertEquals(0, resp.maxLimit)
        assertEquals(0, resp.state)
        assertEquals(0, resp.realName)
        assertEquals("", resp.source)
        assertEquals(1, resp.data.size)
    }

    @Test
    fun `XML - 没有 p 属性的 d 节点被跳过`() {
        val resp = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <i>
              <d>没有属性</d>
              <d p="3,1,25,0,1,0,u,2">正常弹幕</d>
            </i>
            """.trimIndent()
        )
        assertEquals(1, resp.data.size)
        assertEquals("正常弹幕", resp.data[0].content)
    }

    @Test
    fun `XML - 没有弹幕节点时返回空列表`() {
        val resp = parse("""<?xml version="1.0" encoding="UTF-8"?><i><chatid>1</chatid></i>""")
        assertTrue(resp.data.isEmpty())
        assertEquals(1L, resp.chatId)
    }

    @Test
    fun `XML - 正文里的实体字符被还原`() {
        val resp = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <i>
              <d p="1,1,25,0,1,0,u,1">A &amp; B &lt;C&gt;</d>
            </i>
            """.trimIndent()
        )
        assertEquals("A & B <C>", resp.data[0].content)
    }

    @Test
    fun `XML - state 与 real_name 非零时原样带出`() {
        val resp = parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <i>
              <state>1</state>
              <real_name>1</real_name>
              <source>e-r</source>
            </i>
            """.trimIndent()
        )
        assertEquals(1, resp.state)
        assertEquals(1, resp.realName)
        assertEquals("e-r", resp.source)
    }

    @Test
    fun `XML - 省略 XML 声明时也能解析`() {
        val resp = parse("<i><chatid>7</chatid><d p=\"1,1,25,0,1,0,u,1\">x</d></i>")
        assertEquals(7L, resp.chatId)
        assertEquals(1, resp.data.size)
    }
}
