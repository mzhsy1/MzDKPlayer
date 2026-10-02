package org.mz.mzdkplayer.tool.logic

/**
 * 「探测型读取要不要限制字节数」的判定 —— `sampleMimeType` → 读取策略。
 *
 * 这段判断原本在 `SmbUtils` 的五个 `openXxxFileInputStream` 里各抄了一份
 * （SMB / WebDAV / FTP / NFS / HTTP Link，五个方法共 20 行 if-else），
 * 「哪些类型放开、放开到多大」这类规则一旦要调，得改五处，故收敛到这里。
 *
 * 本对象只做字符串判断，不依赖 Android / 网络，可直接在 JVM 单测里跑。
 */
internal object StreamSizePolicy {

    /** 元数据读取的字节上限：弹幕 XML / NFO / 歌词都按这个口径。 */
    const val MAX_BYTES: Long = 5L * 1024 * 1024

    /**
     * 判定口径与历史实现逐字一致 —— **子串匹配、区分大小写**：
     * `VIDEO/mp4` 不会被认成视频；`pics` 是历史遗留拼写（标准 MIME 以 image 开头，
     * 但 ExoPlayer 与本地探测在图片上给的就是 `pics`），两者都保持原样。
     *
     * 音频只需出现 `audio` 就放开，不要求 `audio/raw`：`看月亮爬上来 - 张杰.wav`
     * 的 `id3 ` chunk 挂在 33MB 的 `data` 之后，被 5MB 上限砍掉后标题 / 封面 / 歌词
     * 全都读不出来（实测 2026-09-26）。
     */
    fun of(sampleMimeType: String): Policy = when {
        sampleMimeType.contains("audio") -> Policy.UNLIMITED
        sampleMimeType.contains("video") -> Policy.UNLIMITED
        sampleMimeType.contains("pics") -> Policy.UNLIMITED_RAW
        else -> Policy.LIMITED
    }

    enum class Policy {
        /** 音频 / 视频：整个文件都要读出来。 */
        UNLIMITED,

        /**
         * 图片：同样不限长度，之所以与 [UNLIMITED] 分列，是因为 FTP / HTTP 两处的
         * 历史实现是**提前 return 原始流**（不套「关闭时收尾连接」的那层包装流）。
         * 拆成两个常量是为了如实记录这个差异，测试也据此钉住；真要统一只需删掉本项。
         */
        UNLIMITED_RAW,

        /** 其余（弹幕 XML / NFO / 歌词）：最多读 [MAX_BYTES]。 */
        LIMITED,
    }
}
