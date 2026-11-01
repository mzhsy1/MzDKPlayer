package org.mz.mzdkplayer.tool.logic

/**
 * 播放页里「遥控器上键 / 下键」能绑定的功能（纯逻辑，单测见 `DpadActionPolicyTest`）。
 *
 * 这些 code 原样存进 `SettingsRepository.dpadUpAction / dpadDownAction`，播放页按键时按 code 分流，
 * 电视端设置页按 [ORDER] 循环切换。之前「可选功能清单」私藏在设置页、播放页只靠字符串字面量比对，
 * 加一项就得两边对暗号；现在收敛到一处，加功能只改这里 + 设置页的文案映射。
 */
object DpadActionPolicy {

    const val AUDIO = "A"
    const val SUBTITLE = "S"
    const val DANMAKU = "D"
    const val VIDEO = "V"
    const val SPEED = "SPEED"
    const val ASPECT_RATIO = "R"

    /**
     * 直接唤出底部控制栏（进度条），不开右侧抽屉面板。
     *
     * 存在的意义：关掉「方向键快进唤起底部进度条」之后，单击左右键不再弹控制栏，
     * 而确认键只会「暂停 + 弹控制栏」，想单纯看一眼进度条没别的入口 —— 让上下键能绑它。
     */
    const val CONTROLS = "CONTROLS"

    /**
     * 直接打开播放列表（右侧抽屉，选中当前正在播的这一集）。
     *
     * 追剧时换集比调音轨频繁得多，原来要「菜单 → 设置面板 → 播放列表」三步，
     * 绑到上/下键后一键可达。
     */
    const val PLAYLIST = "PLAYLIST"

    /**
     * 设置页循环切换的顺序。
     *
     * 新功能一律**追加在末尾**：老用户存的是 `"D"` / `"A"` 这类短码，插在当前项前面会让
     * 「再按一下下一个」变成另一项，等于悄悄改了别人的配置。
     */
    val ORDER: List<String> = listOf(
        AUDIO,
        SUBTITLE,
        DANMAKU,
        VIDEO,
        SPEED,
        ASPECT_RATIO,
        CONTROLS,
        PLAYLIST
    )

    /** 轮转到下一个功能；存了未知 code（降级、手改 XML）时回到第一个。 */
    fun next(current: String): String {
        val index = ORDER.indexOf(current)
        val nextIndex = if (index < 0) 0 else (index + 1) % ORDER.size
        return ORDER[nextIndex]
    }

    /** 绑的是不是「底部进度条」——播放页据此决定是弹控制栏还是开面板。 */
    fun isControls(code: String): Boolean = code == CONTROLS

    /** 绑的是不是「播放列表」——播放页据此直接打开播放列表，而不是停在设置面板首页。 */
    fun isPlaylist(code: String): Boolean = code == PLAYLIST
}
