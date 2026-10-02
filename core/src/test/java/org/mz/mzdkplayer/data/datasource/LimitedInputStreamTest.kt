package org.mz.mzdkplayer.data.datasource

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * [LimitedInputStream] 的 JVM 单元测试。
 *
 * 它是「探测型读取」的闸门：弹幕 XML / NFO / 歌词都从这里过，
 * 上限算错的表现同样是静默的（解析出一半就 EOF）。这里把上限的三种位置
 * （够读、刚好读满、读不满）以及 `InputStream` 契约相关的行为逐条钉住。
 */
class LimitedInputStreamTest {

    /** 生成 `0, 1, 2, ...` 的字节序列，便于断言「读到第几个字节为止」。 */
    private fun bytesOf(size: Int): ByteArray = ByteArray(size) { it.toByte() }

    private fun limited(size: Int, maxBytes: Long) =
        LimitedInputStream(ByteArrayInputStream(bytesOf(size)), maxBytes)

    private fun InputStream.drain(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /** 记录 `close()` 次数、其余行为透传给委托流的探针。 */
    private class CountingInputStream(private val delegate: InputStream) : InputStream() {
        var closeCount = 0
            private set

        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun available(): Int = delegate.available()
        override fun close() {
            closeCount++
            delegate.close()
        }
    }

    // ────────────────────────────── 上限内的读取 ──────────────────────────────

    @Test
    fun `未超上限 - 原样读出全部字节`() {
        assertArrayEquals(bytesOf(10), limited(size = 10, maxBytes = 100).drain())
    }

    @Test
    fun `未超上限 - 底层流提前耗尽时正常结束`() {
        assertArrayEquals(bytesOf(3), limited(size = 3, maxBytes = 100).drain())
    }

    @Test
    fun `恰好等于上限 - 全部读出后返回 -1`() {
        val stream = limited(size = 10, maxBytes = 10)
        assertEquals(10, stream.drain().size)
        assertEquals(-1, stream.read())
    }

    // ────────────────────────────── 触到上限 ──────────────────────────────

    @Test
    fun `超过上限 - 只放出前 maxBytes 个字节`() {
        assertArrayEquals(bytesOf(4), limited(size = 10, maxBytes = 4).drain())
    }

    @Test
    fun `超过上限 - 单字节读取在额度用尽后返回 -1`() {
        val stream = limited(size = 10, maxBytes = 2)
        assertEquals(0, stream.read())
        assertEquals(1, stream.read())
        assertEquals(-1, stream.read())
        assertEquals(-1, stream.read())
    }

    @Test
    fun `超过上限 - 批量读取被截断到剩余额度`() {
        val stream = limited(size = 10, maxBytes = 6)
        val buffer = ByteArray(16)
        // 请求 16 字节，但池子里只剩 6
        assertEquals(6, stream.read(buffer, 0, buffer.size))
        assertArrayEquals(bytesOf(6), buffer.copyOf(6))
        assertEquals(-1, stream.read(buffer, 0, buffer.size))
    }

    @Test
    fun `超过上限 - 分批读取合计不超过上限`() {
        val stream = limited(size = 100, maxBytes = 7)
        val buffer = ByteArray(3)
        var total = 0
        while (true) {
            val read = stream.read(buffer, 0, buffer.size)
            if (read == -1) break
            total += read
        }
        assertEquals(7, total)
    }

    @Test
    fun `上限为 0 - 立刻 EOF`() {
        val stream = limited(size = 10, maxBytes = 0)
        assertEquals(-1, stream.read())
        assertEquals(-1, stream.read(ByteArray(4), 0, 4))
    }

    // ────────────────────────────── available ──────────────────────────────

    @Test
    fun `available - 取原流余量与剩余额度的较小值`() {
        val stream = limited(size = 10, maxBytes = 4)
        assertEquals(4, stream.available())
    }

    @Test
    fun `available - 额度比原流余量大时以原流为准`() {
        val stream = limited(size = 3, maxBytes = 100)
        assertEquals(3, stream.available())
    }

    @Test
    fun `available - 读数推进后跟着减少`() {
        val stream = limited(size = 10, maxBytes = 6)
        stream.read(ByteArray(2), 0, 2)
        assertEquals(4, stream.available())
        stream.read(ByteArray(4), 0, 4)
        assertEquals(0, stream.available())
    }

    // ────────────────────────────── 契约与资源 ──────────────────────────────

    @Test
    fun `契约 - 不支持 mark 与 reset`() {
        val stream = limited(size = 10, maxBytes = 10)
        assertFalse(stream.markSupported())
        stream.mark(4) // 不抛异常即可
        val error = assertThrows(IOException::class.java) { stream.reset() }
        assertEquals("Reset not supported", error.message)
    }

    @Test
    fun `契约 - len 为 0 时返回 -1 而不是 0（既有偏差）`() {
        // InputStream 的约定是 len == 0 时返回 0；这里沿用了历史实现（先判额度再判长度）。
        // 断在这里是为了「改口径时必须被看见」，不是认可这个行为。
        val stream = limited(size = 10, maxBytes = 10)
        assertEquals(-1, stream.read(ByteArray(4), 0, 0))
    }

    @Test
    fun `契约 - 越界的 off 与 len 由底层流抛异常`() {
        val stream = limited(size = 10, maxBytes = 10)
        assertThrows(IndexOutOfBoundsException::class.java) { stream.read(ByteArray(2), 1, 5) }
    }

    @Test
    fun `资源 - close 幂等且只透传一次`() {
        val counting = CountingInputStream(ByteArrayInputStream(bytesOf(10)))
        val stream = LimitedInputStream(counting, 10)
        stream.close()
        stream.close()
        assertEquals(1, counting.closeCount)
    }
}
