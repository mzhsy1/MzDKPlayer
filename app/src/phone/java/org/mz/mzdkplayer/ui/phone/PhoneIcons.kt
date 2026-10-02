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

    // ---- 第七阶段（视频播放页）：core 里没有字幕 / 同步 / 倍速 / 比例 / 弹幕 / 完成动作，自绘 ----

    /** 字幕（一个带两条字幕线的圆角框） */
    val Subtitles: ImageVector by lazy {
        materialIcon(
            "Subtitles",
            "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2z" +
                    "M4 12h4v2H4v-2zm10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z"
        )
    }

    /** 同步（字幕时间轴偏移与「重置」共用） */
    val Sync: ImageVector by lazy {
        materialIcon(
            "Sync",
            "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12" +
                    "c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12" +
                    "c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"
        )
    }

    /** 倍速（仪表盘） */
    val Speed: ImageVector by lazy {
        materialIcon(
            "Speed",
            "M20.38 8.57l-1.23 1.85a8 8 0 0 1-.22 7.58H5.07A8 8 0 0 1 15.58 6.85l1.85-1.23" +
                    "A10 10 0 0 0 3.35 19a2 2 0 0 0 1.72 1h13.85a2 2 0 0 0 1.74-1 10 10 0 0 0-.27-10.44z" +
                    "m-9.79 6.84a2 2 0 0 0 2.83 0l5.66-8.49-8.49 5.66a2 2 0 0 0 0 2.83z"
        )
    }

    /** 画面比例（外框 + 两个直角标记） */
    val AspectRatio: ImageVector by lazy {
        materialIcon(
            "AspectRatio",
            "M19 12h-2v3h-3v2h5v-5zM7 9h3V7H5v5h2V9zm14-6H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18" +
                    "c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16.01H3V4.99h18v14.02z"
        )
    }

    /** 弹幕（带三行文字的对话框） */
    val Danmaku: ImageVector by lazy {
        materialIcon(
            "Danmaku",
            "M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z" +
                    "M18 14H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"
        )
    }

    /** 播放完成动作（小旗） */
    val FinishAction: ImageVector by lazy {
        materialIcon("FinishAction", "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z")
    }

    /** 快退 / 快进（双击屏幕与底部按钮共用） */
    val Rewind: ImageVector by lazy {
        materialIcon("FastRewind", "M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z")
    }

    val Forward: ImageVector by lazy {
        materialIcon("FastForward", "M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z")
    }

    /**
     * 全屏 / 退出全屏（播放页的横竖屏切换）。
     *
     * `Fullscreen` 与 `FullscreenExit` 都在 `material-icons-extended` 里，
     * core 只带到 `Settings` / `PlayArrow` 这一批，所以按官方 24dp 路径自绘。
     */
    val Fullscreen: ImageVector by lazy {
        materialIcon(
            "Fullscreen",
            "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"
        )
    }

    val FullscreenExit: ImageVector by lazy {
        materialIcon(
            "FullscreenExit",
            "M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"
        )
    }

    // ---- 第八阶段（设置页）：core 里有 Build / Info，但**没有**减号，自绘 ----

    /**
     * 减号（数值调节的「−」按钮）。
     *
     * `Icons.Filled.Add` 在 core 里，`Remove` 不在；这里补一个同尺寸的横杠，
     * 免得为了一个减号把 material-icons-extended 拖进来。
     */
    val Minus: ImageVector by lazy {
        materialIcon("Minus", "M19 13H5v-2h14v2z")
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
