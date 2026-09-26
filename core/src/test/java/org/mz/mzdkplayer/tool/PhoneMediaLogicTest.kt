package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 手机端「点击条目该开哪个页面」的纯逻辑：类型判定、音频播放列表、图片序列。
 *
 * 边界口径见 [PhoneMediaLogic] 的注释，这里逐个锁死。
 */
class PhoneMediaLogicTest {

    private fun item(name: String, uri: String? = "smb://host/$name", dir: Boolean = false) =
        PhoneMediaLogic.Item(name = name, playbackUri = uri, isDirectory = dir)

    @Test
    fun `按扩展名分别识别视频音频图片`() {
        assertEquals(PhoneMediaLogic.Kind.VIDEO, PhoneMediaLogic.kindOf("影片.mkv"))
        assertEquals(PhoneMediaLogic.Kind.AUDIO, PhoneMediaLogic.kindOf("歌曲.flac"))
        assertEquals(PhoneMediaLogic.Kind.IMAGE, PhoneMediaLogic.kindOf("剧照.JPG"))
        assertEquals(PhoneMediaLogic.Kind.OTHER, PhoneMediaLogic.kindOf("字幕.srt"))
        assertEquals(PhoneMediaLogic.Kind.OTHER, PhoneMediaLogic.kindOf("没有扩展名"))
    }

    @Test
    fun `目录与没有地址的条目都不可打开`() {
        assertEquals(PhoneMediaLogic.Kind.OTHER, PhoneMediaLogic.kindOf(item("歌曲", dir = true)))
        assertEquals(PhoneMediaLogic.Kind.OTHER, PhoneMediaLogic.kindOf(item("歌曲.flac", uri = null)))
        assertEquals(PhoneMediaLogic.Kind.OTHER, PhoneMediaLogic.kindOf(item("歌曲.flac", uri = "  ")))
    }

    @Test
    fun `视频不由本对象接管`() {
        val target = item("影片.mp4")

        assertNull(PhoneMediaLogic.openFor(target, listOf(target), "SMB", "家"))
    }

    @Test
    fun `点击音频只收本目录的音频且下标对得上`() {
        val siblings = listOf(
            item("第一首.mp3"),
            item("封面.jpg"),
            item("第二首.flac"),
            item("字幕.srt"),
            item("第三首.wav"),
        )

        val open = PhoneMediaLogic.openFor(siblings[2], siblings, "SMB", "音乐库")

        assertEquals(PhoneMediaLogic.Kind.AUDIO, open?.kind)
        assertEquals("第二首.flac", open?.title)
        assertEquals(1, open?.currentIndex)
        assertEquals(
            listOf("第一首.mp3", "第二首.flac", "第三首.wav"),
            open?.audioItems?.map { it.fileName },
        )
        assertEquals("SMB", open?.audioItems?.first()?.dataSourceType)
        assertEquals("SMB", open?.dataSourceType)
        assertEquals("音乐库", open?.connectionName)
    }

    @Test
    fun `音频播放地址就是条目里算好的那一个`() {
        val siblings = listOf(
            item("第一首.mp3", uri = "ftp://user:pwd@host:21/music/a.mp3"),
            item("第二首.mp3", uri = "ftp://user:pwd@host:21/music/b.mp3"),
        )

        val open = PhoneMediaLogic.openFor(siblings[1], siblings, "FTP", "")

        assertEquals(
            listOf(
                "ftp://user:pwd@host:21/music/a.mp3",
                "ftp://user:pwd@host:21/music/b.mp3",
            ),
            open?.audioItems?.map { it.uri },
        )
    }

    @Test
    fun `点击图片只收本目录的图片`() {
        val siblings = listOf(
            item("第一张.jpg"),
            item("歌曲.mp3"),
            item("第二张.png"),
            item("第三张.webp"),
            item("子目录", dir = true),
        )

        val open = PhoneMediaLogic.openFor(siblings[3], siblings, "WEBDAV", "")

        assertEquals(PhoneMediaLogic.Kind.IMAGE, open?.kind)
        assertEquals("第三张.webp", open?.title)
        assertEquals(2, open?.currentIndex)
        assertEquals(
            listOf(
                "smb://host/第一张.jpg",
                "smb://host/第二张.png",
                "smb://host/第三张.webp",
            ),
            open?.imageUris,
        )
        assertTrue(open?.audioItems?.isEmpty() == true)
    }

    @Test
    fun `地址不在兄弟列表里时不接管`() {
        val target = item("孤儿.mp3", uri = "smb://host/别的目录/孤儿.mp3")
        val siblings = listOf(item("第一首.mp3"), item("第二首.mp3"), target)

        // 自己也算兄弟之一，所以正常情况一定能命中；这里模拟「算地址用的口径变了」
        assertNull(
            PhoneMediaLogic.openFor(
                item("孤儿.mp3", uri = "smb://host/另一个.mp3"),
                siblings,
                "SMB",
                "",
            )
        )
    }

    @Test
    fun `播放列表里找不到地址时退化成第一首`() {
        val items = listOf(
            org.mz.mzdkplayer.data.model.AudioItem("smb://a.mp3", "a.mp3", "SMB"),
            org.mz.mzdkplayer.data.model.AudioItem("smb://b.mp3", "b.mp3", "SMB"),
        )

        assertEquals(1, PhoneMediaLogic.indexOfUri(items, "smb://b.mp3"))
        assertEquals(0, PhoneMediaLogic.indexOfUri(items, "smb://丢失.mp3"))
        assertEquals(0, PhoneMediaLogic.indexOfUri(emptyList(), "smb://a.mp3"))
    }

    @Test
    fun `图片序列的路由串拼拆可逆`() {
        val encoded = listOf("QUJD", "REVG", "R0hJ")

        assertEquals("QUJD,REVG,R0hJ", PhoneMediaLogic.joinArgs(encoded))
        assertEquals(encoded, PhoneMediaLogic.splitArgs("QUJD,REVG,R0hJ"))
        assertEquals(emptyList<String>(), PhoneMediaLogic.splitArgs(null))
        assertEquals(emptyList<String>(), PhoneMediaLogic.splitArgs(""))
    }

    @Test
    fun `同名歌词只把后缀换成 lrc`() {
        assertEquals("歌曲.lrc", PhoneMediaLogic.lyricSiblingName("歌曲.flac"))
        assertEquals("看月亮爬上来 - 张杰.lrc", PhoneMediaLogic.lyricSiblingName("看月亮爬上来 - 张杰.wav"))
        // 大写后缀、多点名字都要保住基名
        assertEquals("A.B.lrc", PhoneMediaLogic.lyricSiblingName("A.B.MP3"))
        // 没有后缀时直接加
        assertEquals("没有后缀.lrc", PhoneMediaLogic.lyricSiblingName("没有后缀"))
    }

    @Test
    fun `同目录兄弟地址各协议都按换尾段算`() {
        assertEquals(
            "smb://wang1:138138@192.168.5.35/mv/我觉得.lrc",
            PhoneMediaLogic.siblingUri("smb://wang1:138138@192.168.5.35/mv/我觉得.mp3", "我觉得.lrc"),
        )
        assertEquals(
            "nfs://192.168.5.35:/export/music/歌.lrc",
            PhoneMediaLogic.siblingUri("nfs://192.168.5.35:/export/music/歌.flac", "歌.lrc"),
        )
        assertEquals(
            "file:///storage/emulated/0/Music/歌.lrc",
            PhoneMediaLogic.siblingUri("file:///storage/emulated/0/Music/歌.mp3", "歌.lrc"),
        )
        // 空格不编码，原样拼上
        assertEquals(
            "ftp://host/mv/看月亮爬上来 - 张杰.lrc",
            PhoneMediaLogic.siblingUri("ftp://host/mv/看月亮爬上来 - 张杰.wav", "看月亮爬上来 - 张杰.lrc"),
        )
    }

    @Test
    fun `HTTP 直链的 query 不会带进歌词地址`() {
        assertEquals(
            "https://host/media/歌.lrc",
            PhoneMediaLogic.siblingUri("https://host/media/歌.mp3?token=abc&t=1", "歌.lrc"),
        )
    }

    @Test
    fun `没法换尾段时返回 null`() {
        assertNull(PhoneMediaLogic.siblingUri("", "歌.lrc"))
        assertNull(PhoneMediaLogic.siblingUri("smb://host/mv/歌.mp3", ""))
        assertNull(PhoneMediaLogic.siblingUri("没有斜杠的名字.mp3", "歌.lrc"))
        assertNull(PhoneMediaLogic.siblingUri("smb://host/mv/", "歌.lrc"))
    }

    @Test
    fun `歌词文本按 BOM 优先再 UTF-8 再 GBK 解`() {
        val text = "[00:01.00]第一句"

        // UTF-8 BOM
        assertEquals(
            text,
            PhoneMediaLogic.decodeLyricText(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray()),
        )
        // 无 BOM 的 UTF-8
        assertEquals(text, PhoneMediaLogic.decodeLyricText(text.toByteArray()))
        // UTF-16LE 带 BOM
        assertEquals(
            text,
            PhoneMediaLogic.decodeLyricText(("\uFEFF" + text).toByteArray(Charsets.UTF_16LE)),
        )
        // GBK：用 GBK 编码的字节必须还能解回中文
        val gbk = "[00:01.00]纯音乐，请欣赏".toByteArray(java.nio.charset.Charset.forName("GBK"))
        assertEquals("[00:01.00]纯音乐，请欣赏", PhoneMediaLogic.decodeLyricText(gbk))
        // 空内容
        assertNull(PhoneMediaLogic.decodeLyricText(ByteArray(0)))
        assertNull(PhoneMediaLogic.decodeLyricText("   \r\n".toByteArray()))
    }
}
