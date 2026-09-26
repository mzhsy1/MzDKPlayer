package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 手机端文件浏览的纯逻辑：本地目录上溯与 NFS 播放地址拼接。
 *
 * 边界口径见 [PhoneFileBrowserLogic] 的注释，这里逐个锁死。
 */
class PhoneFileBrowserLogicTest {

    private val internalStorage = "/storage/emulated/0"

    @Test
    fun `本地播放地址使用 file 前缀`() {
        assertEquals(
            "file:///storage/emulated/0/Movies/a.mkv",
            PhoneFileBrowserLogic.localPlaybackUri("/storage/emulated/0/Movies/a.mkv"),
        )
    }

    @Test
    fun `本地目录能逐级上溯`() {
        assertEquals(
            "/storage/emulated/0/Movies",
            PhoneFileBrowserLogic.localParentPath("/storage/emulated/0/Movies/动作", internalStorage),
        )
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath("/storage/emulated/0/Movies", internalStorage),
        )
    }

    @Test
    fun `本地目录结尾斜杠不影响结果`() {
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath("/storage/emulated/0/Movies/", internalStorage),
        )
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath("$internalStorage/", internalStorage),
        )
    }

    @Test
    fun `本地根目录不再上溯`() {
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath(internalStorage, internalStorage),
        )
    }

    @Test
    fun `本地路径超出 root 时夹回 root`() {
        // root 之上（/storage/emulated）在手机上通常没有读取权限，不允许越界
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath("/storage/emulated", internalStorage),
        )
        // 看起来像 root 的兄弟目录也要夹回来，不能只比前缀长度
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath("/storage/emulated/0extra/Movies", internalStorage),
        )
    }

    @Test
    fun `本地路径为空时按 root 处理`() {
        assertEquals(
            internalStorage,
            PhoneFileBrowserLogic.localParentPath("", internalStorage),
        )
    }

    @Test
    fun `root 为空时按根目录处理`() {
        assertEquals("/", PhoneFileBrowserLogic.localParentPath("", ""))
        assertEquals("/", PhoneFileBrowserLogic.localParentPath("/sdcard", ""))
        // 根目录下再往上还是根目录
        assertEquals("/", PhoneFileBrowserLogic.localParentPath("/", ""))
    }

    @Test
    fun `NFS 播放地址导出路径缺少前导斜杠时补上`() {
        assertEquals(
            "nfs://192.168.1.4:/volume1/media:/Movies/a.mkv",
            PhoneFileBrowserLogic.nfsPlaybackUri("192.168.1.4", "volume1/media", "/Movies/a.mkv"),
        )
    }

    @Test
    fun `NFS 播放地址与电视端口径一致`() {
        assertEquals(
            "nfs://192.168.1.4:/volume1/media:/Movies/a.mkv",
            PhoneFileBrowserLogic.nfsPlaybackUri("192.168.1.4", "/volume1/media", "/Movies/a.mkv"),
        )
    }

    @Test
    fun `NFS 播放地址导出内路径没有前导斜杠时补上`() {
        assertEquals(
            "nfs://192.168.1.4:/media:/Movies/a.mkv",
            PhoneFileBrowserLogic.nfsPlaybackUri("192.168.1.4", "/media/", "Movies/a.mkv"),
        )
    }
}
