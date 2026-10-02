package org.mz.mzdkplayer.danmaku

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.mz.mzdkplayer.data.model.DanmakuScreenRatio
import org.mz.mzdkplayer.data.model.DanmakuSettings
import org.mz.mzdkplayer.viewmodel.VideoPlayerViewModel

/**
 * 「弹幕设置 → 渲染器能吃的配置」这一段共用实现（第七阶段从电视端播放页下沉）。
 *
 * 两端要拼的东西完全一样：保留策略钉死 BILIBILI、可见性、占用比例、字号 / 透明度的百分比换算、
 * 以及按类型过滤。此前这套 `copy(...)` 只写在电视端播放页里，手机端要复刻就只能再抄一遍，
 * 抄错的表现是「手机上设置弹幕字号没反应」这类很难查的偏色问题。
 */

/**
 * 把一份弹幕设置 + 当前 ViewModel 状态拼成 [DanmakuConfig]。
 *
 * [visibility] 默认取 ViewModel 里的当前值（电视端原来就是读它）：面板里开关一动就会
 * 同步写回 `danmakuVisibility`，所以它与设置里的开关始终一致。
 */
fun VideoPlayerViewModel.danmakuConfigFor(
    settings: DanmakuSettings,
    visibility: Boolean = danmakuVisibility,
): DanmakuConfig = danmakuConfig.copy(
    retainerPolicy = RETAINER_BILIBILI,
    visibility = visibility,
    screenPart = DanmakuScreenRatio.fromDisplayName(settings.selectedRatio).ratioValue,
    textSizeScale = settings.fontSize.toFloat() / 100,
    alpha = settings.transparency.toFloat() / 100,
    // 过滤器是「按类型隐藏」，空集合表示不隐藏任何类型
    dataFilter = listOf(createDanmakuTypeFilter(settings.selectedTypes)),
)

/**
 * 把 [config] 推给渲染器，让改动立刻生效。
 *
 * [previousScreenPart] 是上一次推下去的占用比例：比例变了必须额外刷新排版与保留策略，
 * 否则弹幕仍然按旧的行数排布（电视端踩过这个坑）。返回这次生效的比例，供下次比较。
 */
fun DanmakuPlayer.pushDanmakuConfig(
    config: DanmakuConfig,
    previousScreenPart: Float? = null,
): Float {
    // 顺序与电视端播放页原来的写法一致：先通知过滤器重算，再换配置，最后刷可见性
    config.updateFilter()
    updateConfig(config)
    config.updateVisibility()

    val screenPart = config.screenPart
    if (previousScreenPart == null || previousScreenPart != screenPart) {
        config.updateLayout()
        config.updateRetainer()
    }
    return screenPart
}

/**
 * 弹幕 XML 的解析结果 → 渲染数据。
 *
 * 电视端原本把这段映射内联在播放页的 `LaunchedEffect` 里，手机端同样需要，故下沉到 :core。
 *
 * 两处口径刻意保持原样：`rowId` 为 0 的弹幕要给一个随机 id（渲染器拿它做去重，
 * 全 0 会被当成同一条）；模式 4 / 5 分别映射到顶部 / 底部——**与解析出的注释相反**，
 * 但这是电视端一直在用的映射，改它会让原本显示正常的弹幕跳位置。
 */
fun List<DanmakuData>.toDanmakuItems(): List<DanmakuItemData> = map { data ->
    DanmakuItemData(
        danmakuId = if (data.rowId != 0L) data.rowId else (Math.random() * 100_000_000).toLong(),
        position = (data.time * 1000).toLong(),
        content = data.content,
        mode = when (data.mode) {
            4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
            5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
            else -> DanmakuItemData.DANMAKU_MODE_ROLLING
        },
        textSize = data.size,
        textColor = data.color,
    )
}
