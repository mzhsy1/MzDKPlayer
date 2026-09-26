package org.mz.mzdkplayer.ui.phone

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 手机端只补几个 `material-icons-core` 里没有、但界面上必须用的图标。
 *
 * core 只带 49 个图标（Icons.Filled.Home / Settings / PlayArrow / Add / Search / Star … 都够用），
 * 缺的是「暂停 / 文件夹 / 影片 / 刮削」。为了这几个图标引入体积巨大的
 * `material-icons-extended` 不划算，这里用 Material 官方的 24dp 路径自建。
 */
internal object PhoneIcons {

    /** 暂停。core 里只有 PlayArrow，没有 Pause */
    val Pause: ImageVector by lazy {
        materialIcon("Pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z")
    }

    /** 文件夹，文件浏览列表里的目录项 */
    val Folder: ImageVector by lazy {
        materialIcon(
            "Folder",
            "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"
        )
    }

    /** 影片，文件浏览列表里的视频项 */
    val Movie: ImageVector by lazy {
        materialIcon(
            "Movie",
            "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"
        )
    }

    /**
     * 刮削（两颗四角星）。
     *
     * `auto_awesome` 在 core 里没有；这里用两个对称的四角星自绘，比照原样搬运
     * material-icons-extended 的多段路径更不容易看错。
     */
    val Scrape: ImageVector by lazy {
        materialIcon(
            "Scrape",
            "M11 2l1.9 6.1L19 10l-6.1 1.9L11 18l-1.9-6.1L3 10l6.1-1.9L11 2z" +
                    "M18.5 14l1.05 3.45L23 18.5l-3.45 1.05L18.5 23l-1.05-3.45L14 18.5l3.45-1.05L18.5 14z"
        )
    }

    // ---- 第五阶段（音乐播放）：core 里没有音乐 / 上一首 / 下一首 / 播放列表，自绘 ----

    /** 音符，文件浏览列表里的音频项与音频播放页的封面占位 */
    val Music: ImageVector by lazy {
        materialIcon(
            "Music",
            "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"
        )
    }

    val SkipNext: ImageVector by lazy {
        materialIcon("SkipNext", "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z")
    }

    val SkipPrevious: ImageVector by lazy {
        materialIcon("SkipPrevious", "M6 6h2v12H6zm3.5 6l8.5 6V6z")
    }

    /** 播放列表（左侧三条曲目 + 右侧播放三角） */
    val Playlist: ImageVector by lazy {
        materialIcon("Playlist", "M3 10h11v2H3zm0-4h11v2H3zm0 8h7v2H3zm13-1v8l6-4z")
    }

    // ---- 第五阶段（图片查看）：core 里没有图片 / 缩放 / 旋转，自绘 ----

    /** 图片，文件浏览列表里的图片项 */
    val Image: ImageVector by lazy {
        materialIcon(
            "Image",
            "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2z" +
                    "M8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
        )
    }

    val ZoomIn: ImageVector by lazy {
        materialIcon(
            "ZoomIn",
            "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5" +
                    "5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5z" +
                    "m-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" +
                    "M12 10h-2v2H9v-2H7V9h2V7h1v2h2v1z"
        )
    }

    val ZoomOut: ImageVector by lazy {
        materialIcon(
            "ZoomOut",
            "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5" +
                    "5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5z" +
                    "m-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" +
                    "M7 9h5v1H7z"
        )
    }

    val RotateLeft: ImageVector by lazy {
        materialIcon(
            "RotateLeft",
            "M7.11 8.53L5.7 7.11C4.8 8.27 4.24 9.61 4.07 11h2.02c.14-.87.49-1.72 1.02-2.47z" +
                    "M6.09 13H4.07c.17 1.39.72 2.73 1.62 3.89l1.41-1.42c-.52-.75-.87-1.59-1.01-2.47z" +
                    "m1.01 5.32c1.16.9 2.51 1.44 3.9 1.61V17.9c-.87-.15-1.71-.49-2.46-1.03L7.1 18.32z" +
                    "M13 4.07V1L8.45 5.55 13 10V6.09c2.84.48 5 2.94 5 5.91s-2.16 5.43-5 5.91v2.02" +
                    "c3.95-.49 7-3.85 7-7.93s-3.05-7.44-7-7.93z"
        )
    }

    val RotateRight: ImageVector by lazy {
        materialIcon(
            "RotateRight",
            "M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02" +
                    "c-2.84-.48-5-2.94-5-5.91s2.16-5.43 5-5.91V10l4.55-4.45z" +
                    "M19.93 11c-.17-1.39-.72-2.73-1.62-3.89l-1.42 1.42c.54.75.88 1.6 1.02 2.47h2.02z" +
                    "M13 17.9v2.02c1.39-.17 2.74-.71 3.9-1.61l-1.44-1.44c-.75.54-1.59.89-2.46 1.03z" +
                    "m3.89-2.42l1.42 1.41c.9-1.16 1.45-2.5 1.62-3.89h-2.02c-.14.87-.48 1.72-1.02 2.48z"
        )
    }
}

private fun materialIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black),
    ).build()
