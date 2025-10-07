package org.mz.mzdkplayer.tool

import java.io.IOException
import java.io.InputStream

/**
 * 限制读取字节数的输入流包装器
 */
class LimitedInputStream(
    private val originalInputStream: InputStream,
    private val maxBytes: Long
) : InputStream() {
    private var bytesRead: Long = 0
    private var closed = false

    override fun read(): Int {
        if (bytesRead >= maxBytes) return -1
        val result = originalInputStream.read()
        if (result != -1) {
            bytesRead++
        }
        return result
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (bytesRead >= maxBytes) return -1

        val remaining = maxBytes - bytesRead
        val bytesToRead = minOf(len, remaining.toInt())

        if (bytesToRead <= 0) return -1

        val result = originalInputStream.read(b, off, bytesToRead)
        if (result > 0) {
            bytesRead += result
        }
        return result
    }

    override fun available(): Int {
        val originalAvailable = originalInputStream.available()
        val remaining = maxBytes - bytesRead
        return minOf(originalAvailable, remaining.toInt())
    }

    override fun close() {
        if (!closed) {
            closed = true
            originalInputStream.close()
        }
    }

    override fun markSupported(): Boolean = false

    override fun mark(readlimit: Int) {
        // mark 不被支持
    }

    override fun reset() {
        throw IOException("Reset not supported")
    }
}