package org.mz.mzdkplayer.ui.phone.component

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.common.findActivity
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.logic.PhoneSettingsLogic
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import kotlin.math.roundToInt

/** 双击屏幕左右两侧时的反馈方向 */
internal enum class PhoneSeekPulse { BACK, FORWARD }

/** 控制层顶部 / 底部渐变的底色（纯黑渐隐，保证白色图标在任何画面上都看得清） */
private val ControlsScrim = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
)

// ────────────────────────── 表达性控制层的设计常量 ──────────────────────────
//
// 播放控制压在视频画面上，不能用 `MaterialTheme.colorScheme` 的那套 surface 角色
// （浅色主题下会变成浅底，跟画面直接撕裂），所以这里固定一组「白描边 + 半透明黑容器」：
// 单个按钮是一个 tonal 容器、图标是它的内容色 —— 这正是 M3 Expressive 里
// 「容器化图标按钮」的形态，而不是裸图标浮在画面上。

/**
 * 控制按钮的容器底色。
 *
 * 用浅色半透明而不是黑色半透明：这层下面压着黑色的渐隐 scrim，
 * 黑色容器在它上面几乎看不出边界，浅色才能把「这是个按钮」画出来。
 */
private val ControlContainerColor = Color.White.copy(alpha = 0.16f)

/** 进度条：已播放白色、未播放半透明白（与电视端那套白色调一致，不跟主题走） */
private val ProgressActiveColor = Color.White
private val ProgressInactiveColor = Color.White.copy(alpha = 0.3f)

/** 按钮按下时缩到多小：Expressive 的「按下去」靠容器形变表达，而不是只换颜色 */
private const val CONTROL_PRESSED_SCALE = 0.88f

/**
 * 手机端播放页的**手势层**：单击切换控制栏、双击左右两侧快进 / 快退、
 * 长按临时倍速、左右半屏上下滑调亮度 / 音量。
 *
 * 这一层铺满整屏，压在画面之上、控制层之下。控制层里没有绑手势的空隙
 * （标题行、进度条之外的留白）不消费点击事件，会自然落到这一层上，
 * 于是「点哪儿都能收起控制栏」这件事不需要额外代码。
 *
 * [onDoubleTapLeft] / [onDoubleTapRight] 的分界是屏幕左右各 1/3：
 * 中间那 1/3 留给「单击显隐」，避免误触时跳进度；上下滑的分界则是**左右各 1/2**
 * （与主流播放器一致：左半边亮度、右半边音量）。
 *
 * 上下滑与单击/双击放在两个独立的 `pointerInput` 里：各自只在自己关心的手势上
 * 消费事件（拖动不过触摸阈值就不消费），所以单击、双击、上下滑、长按互不抢。
 *
 * 亮度 / 音量直接改系统状态（窗口 `screenBrightness` 与 `STREAM_MUSIC`），
 * 中途显示一个居中的小提示（[PhoneGestureHud]），松手 900ms 后自动消失。
 */
@Composable
internal fun PhonePlayerGestureLayer(
    onTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    enableBrightnessGesture: Boolean,
    enableVolumeGesture: Boolean,
    enableLongPress: Boolean,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var widthPx by remember { mutableStateOf(0) }
    var hud by remember { mutableStateOf<PhoneGestureHud?>(null) }

    // 拖动中不断改写同一个 hud 会重启这个计时；松手后 900ms 收起
    LaunchedEffect(hud) {
        if (hud != null) {
            delay(GESTURE_HUD_HOLD_MS)
            hud = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width }
            .pointerInput(enableBrightnessGesture, enableVolumeGesture) {
                if (!enableBrightnessGesture && !enableVolumeGesture) return@pointerInput
                var kind = PhoneGestureKind.BRIGHTNESS
                var startPercent = 0
                var totalDy = 0f

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // 左半边亮度、右半边音量
                        kind = if (offset.x < size.width / 2f) {
                            PhoneGestureKind.BRIGHTNESS
                        } else {
                            PhoneGestureKind.VOLUME
                        }
                        val enabled = when (kind) {
                            PhoneGestureKind.BRIGHTNESS -> enableBrightnessGesture
                            PhoneGestureKind.VOLUME -> enableVolumeGesture
                        }
                        if (!enabled) {
                            totalDy = Float.NaN // 标记「这次手势不处理」
                            return@detectVerticalDragGestures
                        }
                        startPercent = currentPercent(context, kind)
                        totalDy = 0f
                        hud = PhoneGestureHud(kind, startPercent)
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (totalDy.isNaN()) return@detectVerticalDragGestures
                        change.consume()
                        totalDy += dragAmount
                        val percent = PhoneSettingsLogic.applyVerticalSwing(
                            startPercent = startPercent,
                            totalDyPx = totalDy,
                            screenHeightPx = size.height,
                        )
                        applyPercent(context, kind, percent)
                        hud = PhoneGestureHud(kind, percent)
                    },
                    // 松手后提示不立刻消失，靠上面的计时器再留一会儿，避免手指抬起就看不见数值
                    onDragEnd = { },
                    onDragCancel = { hud = null },
                )
            }
            .pointerInput(widthPx, enableLongPress) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset ->
                        val third = widthPx / 3f
                        when {
                            offset.x <= third -> onDoubleTapLeft()
                            offset.x >= third * 2 -> onDoubleTapRight()
                            // 中间 1/3 不跳进度：双击落在这里通常是误触
                            else -> Unit
                        }
                    },
                    // 长按期间临时倍速；`onPress` 的收尾负责恢复（手势被取消时也会走到）
                    onLongPress = if (enableLongPress) {
                        { onLongPressStart() }
                    } else {
                        null
                    },
                    // `onPress` 不接受 null，所以这里总是挂着；`onLongPressEnd` 自己会判断
                    // 「当前是不是真的在长按」，普通点击时它什么都不做
                    onPress = {
                        tryAwaitRelease()
                        onLongPressEnd()
                    },
                )
            }
    ) {
        hud?.let {
            // 提示贴在手势那一侧：左半边调亮度、右半边调音量，位置和手指一致才知道改的是哪个
            PhoneGestureHudOverlay(
                hud = it,
                modifier = Modifier.align(
                    if (it.kind == PhoneGestureKind.BRIGHTNESS) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                ),
            )
        }
    }
}

/** 上下滑调的是什么 */
internal enum class PhoneGestureKind { BRIGHTNESS, VOLUME }

/** 手势过程中显示的小提示状态 */
internal data class PhoneGestureHud(val kind: PhoneGestureKind, val percent: Int)

/** 提示停留时长 */
private const val GESTURE_HUD_HOLD_MS = 900L

/**
 * 读当前百分比（0..100）。
 *
 * 亮度：窗口 `screenBrightness` 为 -1 时表示「跟随系统」，这时要回读系统亮度
 * （`Settings.System.SCREEN_BRIGHTNESS` 是 0..255），否则第一次上滑会从 0 开始跳。
 * 音量：直接问 `AudioManager`，静音（返回 -1）当作 0。
 */
private fun currentPercent(context: Context, kind: PhoneGestureKind): Int = when (kind) {
    PhoneGestureKind.BRIGHTNESS -> {
        val window = context.findActivity()?.window
        val current = window?.attributes?.screenBrightness ?: -1f
        if (current >= 0f) {
            (current * 100f).toInt().coerceIn(0, 100)
        } else {
            val system = runCatching {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                )
            }.getOrDefault(128)
            (system * 100f / 255f).toInt().coerceIn(0, 100)
        }
    }

    PhoneGestureKind.VOLUME -> {
        val audio = context.getSystemService(AudioManager::class.java)
        val max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
        val current = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (max <= 0 || current < 0) 0 else (current * 100f / max).toInt().coerceIn(0, 100)
    }
}

/** 把百分比写回系统 */
private fun applyPercent(context: Context, kind: PhoneGestureKind, percent: Int) {
    when (kind) {
        PhoneGestureKind.BRIGHTNESS -> {
            val activity = context.findActivity() ?: return
            val attributes = activity.window.attributes
            // 0 会让画面全黑、看着像卡死，所以留 1% 的下限
            attributes.screenBrightness = percent.coerceAtLeast(1) / 100f
            activity.window.attributes = attributes
        }

        PhoneGestureKind.VOLUME -> {
            val audio = context.getSystemService(AudioManager::class.java) ?: return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max <= 0) return
            val target = (percent * max / 100f).roundToInt().coerceIn(0, max)
            // 勿扰模式下改音量会抛 SecurityException：手势不该把播放页带崩
            runCatching {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            }
        }
    }
}

/** 手势提示：一个圆角块 + 「亮度 60%」 */
@Composable
private fun PhoneGestureHudOverlay(hud: PhoneGestureHud, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 24.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.72f),
        contentColor = Color.White,
    ) {
        Text(
            text = stringResource(
                when (hud.kind) {
                    PhoneGestureKind.BRIGHTNESS -> R.string.phone_gesture_brightness
                    PhoneGestureKind.VOLUME -> R.string.phone_gesture_volume
                }
            ) + " ${hud.percent}%",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

/**
 * 手机端播放页的**控制层** —— 布局对齐 B 站，横竖两套各按各的排法：
 *
 * ```
 * 横屏  ← 片名 / 日期                    列表 设置 退出全屏
 *        00:04/50:39
 *        ─────────●────────────
 *        ⏸ ⏭  [ 弹幕设置 ]      播放列表 速度 1080P
 *
 * 竖屏  ←                                列表 设置
 *        ─────────●────────────
 *        ⏸ 弹          00:57/41:21 ⛶
 * ```
 *
 * 竖屏不摆片名：片名 / 日期在画面下面的信息区里已经有了（B 站竖屏也是这么分的）。
 *
 * **没有快退 / 快进按钮**：这两个动作交给双击画面左右 1/3 的手势
 * （见 `PhonePlayerGestureLayer`），长按还能临时倍速。
 *
 * M3 Expressive 的落点保留在交互上：按钮按下有弹簧回弹、进度条手柄抓住时变大、
 * 控制层整体淡入淡出。配色是压在画面上的一套固定白 + 半透明黑，不跟主题色走
 * （否则浅色主题下整层会变成浅底，压在画面里看不清）。
 */
@Composable
internal fun PhonePlayerControls(
    title: String,
    dateText: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasNext: Boolean,
    networkSpeed: Long?,
    /** 当前视频轨的清晰度文案（横屏右下角那颗「1080P」），取不到时为 null */
    qualityText: String?,
    /** 中间那颗胶囊的文案（弹幕开关状态） */
    danmakuText: String,
    /** 全屏（横屏）时画面区铺满整屏，顶部栏要自己避一次状态栏；竖屏外侧已经避过 */
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenVideoTracks: () -> Unit,
    onOpenDanmaku: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 拖拽期间进度条跟手，不受播放心跳（每 200ms 一次）影响
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableLongStateOf(0L) }
    val shownPosition = if (dragging) dragValue else positionMs

    val handleDrag: (Long) -> Unit = { target ->
        dragging = true
        dragValue = target
        onInteraction()
    }
    val handleDragFinished: () -> Unit = {
        onSeekTo(dragValue)
        dragging = false
        onInteraction()
    }

    Column(modifier = modifier.fillMaxSize()) {
        ControlsTopBar(
            title = title,
            dateText = dateText,
            networkSpeed = networkSpeed,
            // 竖屏不摆标题：片名、日期这些信息，画面下面的信息区已经有了，
            // 再压在画面上只会白占一条（B 站竖屏也是这么处理的）
            showTitle = isFullscreen,
            applyStatusBarInset = isFullscreen,
            isFullscreen = isFullscreen,
            onBack = onBack,
            onOpenPlaylist = onOpenPlaylist,
            onOpenSettings = onOpenSettings,
            onToggleFullscreen = onToggleFullscreen,
        )

        // 中间留白：点击落到手势层，于是「点画面任意处收起控制栏」不需要额外代码
        Spacer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        if (isFullscreen) {
            ControlsBottomBar(
                shownPosition = shownPosition,
                durationMs = durationMs,
                isPlaying = isPlaying,
                dragging = dragging,
                hasNext = hasNext,
                qualityText = qualityText,
                danmakuText = danmakuText,
                onDrag = handleDrag,
                onDragFinished = handleDragFinished,
                onTogglePlayPause = {
                    onInteraction()
                    onTogglePlayPause()
                },
                onNext = {
                    onInteraction()
                    onNext()
                },
                onOpenPlaylist = {
                    onInteraction()
                    onOpenPlaylist()
                },
                onOpenSpeed = {
                    onInteraction()
                    onOpenSpeed()
                },
                onOpenVideoTracks = {
                    onInteraction()
                    onOpenVideoTracks()
                },
                onOpenDanmaku = {
                    onInteraction()
                    onOpenDanmaku()
                },
            )
        } else {
            ControlsCompactBar(
                shownPosition = shownPosition,
                durationMs = durationMs,
                isPlaying = isPlaying,
                dragging = dragging,
                onDrag = handleDrag,
                onDragFinished = handleDragFinished,
                onTogglePlayPause = {
                    onInteraction()
                    onTogglePlayPause()
                },
                onOpenDanmaku = {
                    onInteraction()
                    onOpenDanmaku()
                },
                onToggleFullscreen = {
                    onInteraction()
                    onToggleFullscreen()
                },
            )
        }
    }
}

/**
 * 顶部控制行 —— 布局对齐 B 站：左侧一个裸返回箭头，紧跟着片名（下面一行小字），
 * 右侧一排裸图标。
 *
 * 竖屏不摆片名：片名、日期在画面下面的信息区里已经有了（B 站竖屏也是这么分的），
 * 这里只留两侧的图标，中间整条让给画面。
 */
@Composable
private fun ControlsTopBar(
    title: String,
    dateText: String?,
    networkSpeed: Long?,
    /** 是否摆片名 / 日期：横屏摆（画面下方没有信息区），竖屏留给画面下方的信息区 */
    showTitle: Boolean,
    /** 全屏时画面顶到屏幕边缘，这里要自己避一次状态栏 / 挖孔 */
    applyStatusBarInset: Boolean,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ControlsScrim)
            .then(if (applyStatusBarInset) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.phone_action_back),
            onClick = onBack,
            container = false,
            iconSize = 26.dp,
        )

        if (showTitle) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp, end = 8.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!dateText.isNullOrBlank()) {
                    Text(
                        text = dateText,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            // 竖屏：返回靠左，图标靠右，中间整条留给画面
            Spacer(modifier = Modifier.weight(1f))
        }

        if (networkSpeed != null) {
            NetworkSpeedPill(text = formatNetworkSpeed(networkSpeed))
        }

        PlayerIconButton(
            icon = PhoneIcons.Playlist,
            contentDescription = stringResource(R.string.ui_label_playlist),
            onClick = onOpenPlaylist,
            container = false,
            iconSize = 24.dp,
        )
        PlayerIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.ui_label_settings),
            onClick = onOpenSettings,
            modifier = Modifier.padding(start = 2.dp),
            container = false,
            iconSize = 24.dp,
        )
        if (isFullscreen) {
            // 横屏没有别的地方能退回竖屏（B 站靠转屏），这颗按钮保留在图标行里
            PlayerIconButton(
                icon = PhoneIcons.FullscreenExit,
                contentDescription = stringResource(R.string.phone_player_exit_fullscreen),
                onClick = onToggleFullscreen,
                modifier = Modifier.padding(start = 2.dp),
                container = false,
                iconSize = 24.dp,
            )
        }
    }
}

/** 网速胶囊：控制层里唯一会持续变化的数字，给它一个容器才不会跟着标题飘 */
@Composable
private fun NetworkSpeedPill(text: String) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.3f),
        contentColor = Color.White,
        modifier = Modifier.padding(end = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * 控制层的图标按钮：按下时弹簧缩小（Expressive 的「按压有形变」），
 * 触摸区固定 [containerSize]，图标本身 [iconSize]。
 *
 * [container] 决定要不要画那层圆形半透明容器：B 站那套布局（顶部图标行、底部控制行）
 * 一律是裸图标，容器留白会让整行显得臃肿；只有少数需要强调的按钮才套容器。
 */
@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    container: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) CONTROL_PRESSED_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playerIconButtonScale",
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(
                if (container) {
                    Modifier.background(
                        if (enabled) {
                            ControlContainerColor
                        } else {
                            // 不可用时连容器一起淡下去，只剩一个位置感
                            Color.White.copy(alpha = 0.06f)
                        }
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint(enabled),
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * 播放页的进度条：一条**细线 + 小圆点手柄**。
 *
 * 刻意不用 M3 的 `Slider`：那颗 Slider 是「粗轨道 + 高竖条手柄」的表达性形态，
 * 压在竖屏画面区里太重（轨道 16dp、手柄 44dp，几乎和一行按钮一样高）。
 * 这里跟随主流播放器的形态 —— 一条 3dp 的细线贴着控制行，视觉上不抢画面，
 * 触摸区仍有 28dp 高；手柄平时是 5dp 小圆点、按住拖时放大到 1.8 倍，
 * 用尺寸变化表达「抓住了」（这正是 Expressive 的「按压有形变」）。
 *
 * 绘制与手势自己做，是因为 `Slider` 的轨道厚度 / 手柄尺寸都是内置常量，
 * 用 `Modifier.height` 只能把它裁掉一半。
 */
@Composable
private fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    onDrag: (Long) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier = Modifier,
    /** 正在拖动：手柄放大一圈 */
    dragging: Boolean = false,
    trackHeight: Dp = 3.dp,
    touchHeight: Dp = 28.dp,
    thumbRadius: Dp = 5.dp,
) {
    val enabled = durationMs > 0L
    val fraction = if (enabled) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    var barWidthPx by remember { mutableStateOf(0) }
    // 手势 lambda 只装一次，读到的必须是最新的时长（切集后 duration 会变）
    val latestDuration by rememberUpdatedState(durationMs)
    val latestEnabled by rememberUpdatedState(enabled)

    fun positionAt(x: Float): Long {
        if (barWidthPx <= 0 || !latestEnabled) return 0L
        val ratio = (x / barWidthPx).coerceIn(0f, 1f)
        return (ratio * latestDuration).toLong()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(touchHeight)
            .onSizeChanged { barWidthPx = it.width }
            .pointerInput(latestEnabled) {
                if (!latestEnabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onDrag(positionAt(offset.x)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onDrag(positionAt(change.position.x))
                    },
                    onDragEnd = { onDragFinished() },
                    onDragCancel = { onDragFinished() },
                )
            }
            .pointerInput(latestEnabled) {
                if (!latestEnabled) return@pointerInput
                // 点哪儿跳哪儿：点完立刻落定，与拖动结束走同一条路径
                detectTapGestures { offset ->
                    onDrag(positionAt(offset.x))
                    onDragFinished()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val centerY = size.height / 2f
            val lineHeight = trackHeight.toPx()
            val thumbPx = thumbRadius.toPx() * if (dragging) 1.8f else 1f

            drawLine(
                color = ProgressInactiveColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = lineHeight,
                cap = StrokeCap.Round,
            )
            if (fraction > 0f) {
                drawLine(
                    color = ProgressActiveColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width * fraction, centerY),
                    strokeWidth = lineHeight,
                    cap = StrokeCap.Round,
                )
            }
            // 手柄贴到两端时也要完整落在线内，否则会被裁掉一半
            val thumbX = (size.width * fraction).coerceIn(thumbPx, size.width - thumbPx)
            drawCircle(
                color = ProgressActiveColor,
                radius = thumbPx,
                center = Offset(thumbX, centerY),
            )
        }
    }
}

/**
 * 横屏（全屏）时的底部控制区 —— 布局对齐 B 站：
 *
 * ```
 * 00:04/50:39                      ← 时间在进度条上方、靠左
 * ─────────────●───────────────    ← 细进度条，几乎贴左右边
 * ⏸  ⏭   [ 弹幕设置 ]     列表 速度 1080P
 * ```
 *
 * **左侧只有播放暂停 + 下一个**：B 站横屏没有上一集按钮，翻集走右端的「列表」面板
 * （也就是我们的播放列表）；快退 / 快进仍然只有双击画面左右 1/3 的手势。
 * **中间那颗胶囊**在 B 站是弹幕输入框，本地播放没有发弹幕这回事，
 * 于是换成弹幕设置的入口 —— 位置、形状照旧。
 * **右端三颗文字按钮**对应 B 站的「选集 / 倍速 / 1080P」：播放列表、倍速、当前视频轨清晰度。
 */
@Composable
private fun ControlsBottomBar(
    shownPosition: Long,
    durationMs: Long,
    isPlaying: Boolean,
    dragging: Boolean,
    hasNext: Boolean,
    /** 当前视频轨的清晰度文案（右下角那颗），取不到时为 null */
    qualityText: String?,
    /** 中间胶囊的文案（弹幕开关状态） */
    danmakuText: String,
    onDrag: (Long) -> Unit,
    onDragFinished: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenVideoTracks: () -> Unit,
    onOpenDanmaku: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        // 时间在进度条**上方**、靠左；拖动时显示目标位置（手指不挡视线）
        Text(
            text = "${Tools.formatTime(shownPosition)}/${Tools.formatTime(durationMs)}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
            modifier = Modifier.padding(bottom = 2.dp),
        )

        PlayerProgressBar(
            positionMs = shownPosition,
            durationMs = durationMs,
            onDrag = onDrag,
            onDragFinished = onDragFinished,
            dragging = dragging,
            trackHeight = 3.dp,
            touchHeight = 26.dp,
            thumbRadius = 5.dp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 播放 / 暂停：B 站这一颗是裸图标，尺寸比旁边的图标大一档
            PlayerIconButton(
                icon = if (isPlaying) PhoneIcons.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.phone_player_play_pause),
                onClick = onTogglePlayPause,
                container = false,
                containerSize = 44.dp,
                iconSize = 28.dp,
            )

            // 下一个（B 站横屏只有「下一集」，没有上一集）
            PlayerIconButton(
                icon = PhoneIcons.SkipNext,
                contentDescription = stringResource(R.string.phone_player_next_video),
                onClick = onNext,
                enabled = hasNext,
                container = false,
                containerSize = 44.dp,
                iconSize = 24.dp,
            )

            // 中间：B 站那里是弹幕输入框，这里换成弹幕设置的入口（位置与形状照旧）
            DanmakuCapsule(
                text = danmakuText,
                onClick = onOpenDanmaku,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            )

            // 右端三颗文字按钮：选集 / 倍速 / 画质
            PlayerTextButton(
                text = stringResource(R.string.ui_label_playlist),
                onClick = onOpenPlaylist,
            )
            PlayerTextButton(
                text = stringResource(R.string.ui_label_speed),
                onClick = onOpenSpeed,
            )
            PlayerTextButton(
                text = qualityText ?: stringResource(R.string.ui_label_video_track),
                onClick = onOpenVideoTracks,
            )
        }
    }
}

/**
 * 底部那颗弹幕胶囊（B 站是弹幕输入框）。
 *
 * 本地播放没有「发弹幕」这回事，于是做成弹幕设置的入口：形状、颜色、位置与 B 站一致，
 * 文案换成开关状态。左对齐的文案（不是居中）也是为了贴住 B 站那颗输入框的样子。
 */
@Composable
private fun DanmakuCapsule(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White,
        modifier = modifier.height(36.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 底部右端的文字按钮（B 站的「选集 / 倍速 / 1080P」）：无容器，只有一圈涟漪 */
@Composable
private fun PlayerTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

/**
 * 竖屏（未全屏）时的底部控制区 —— 布局对齐 B 站竖屏：
 *
 * ```
 * ─────────────●───────────────   ← 2dp 细进度条
 * ⏸  弹      00:57/41:21  ⛶       ← 左：播放暂停 + 弹幕开关；右：时间 + 全屏
 * ```
 *
 * 竖屏的画面区只有 16:9 那么高（这台机器上约 245dp），所以这一块比横屏再矮一档：
 * 进度条 2dp、按钮都不套容器。快退 / 快进只有手势（双击左右 1/3），按钮行不放它们。
 */
@Composable
private fun ControlsCompactBar(
    shownPosition: Long,
    durationMs: Long,
    isPlaying: Boolean,
    dragging: Boolean,
    onDrag: (Long) -> Unit,
    onDragFinished: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenDanmaku: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                )
            )
            .padding(horizontal = 8.dp),
    ) {
        // 竖屏画面区只有 16:9 那么高：进度条压成 2dp 细线、触摸区 24dp、手柄 4dp 小圆点
        PlayerProgressBar(
            positionMs = shownPosition,
            durationMs = durationMs,
            onDrag = onDrag,
            onDragFinished = onDragFinished,
            dragging = dragging,
            trackHeight = 2.dp,
            touchHeight = 24.dp,
            thumbRadius = 4.dp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：播放暂停 + 弹幕开关（B 站竖屏也是这两颗，都是裸图标）
            PlayerIconButton(
                icon = if (isPlaying) PhoneIcons.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.phone_player_play_pause),
                onClick = onTogglePlayPause,
                container = false,
                iconSize = 26.dp,
            )
            PlayerIconButton(
                icon = PhoneIcons.Danmaku,
                contentDescription = stringResource(R.string.ui_label_danmaku_settings),
                onClick = onOpenDanmaku,
                modifier = Modifier.padding(start = 6.dp),
                container = false,
                iconSize = 22.dp,
            )

            Spacer(modifier = Modifier.weight(1f))

            // 右侧：时间紧贴全屏按钮，与 B 站竖屏那一行的排法一致
            Text(
                text = "${Tools.formatTime(shownPosition)}/${Tools.formatTime(durationMs)}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
                modifier = Modifier.padding(end = 6.dp),
            )

            PlayerIconButton(
                icon = PhoneIcons.Fullscreen,
                contentDescription = stringResource(R.string.phone_player_fullscreen),
                onClick = onToggleFullscreen,
                container = false,
                iconSize = 22.dp,
            )
        }
    }
}

private fun iconTint(enabled: Boolean): Color =
    if (enabled) Color.White else Color.White.copy(alpha = 0.35f)

/**
 * 双击快进 / 快退时的中央提示：一个圆形容器里放箭头 + `±15s`，700ms 后自己消失。
 *
 * 不再铺一层整屏遮罩：Expressive 的反馈是「就地长出来的一个小东西」，
 * 整屏变暗会跟着双击节奏闪一下。入场用弹簧缩放，跟双击的节奏对得上；
 * 提示贴在手势那一侧（左 / 右），和画面里手指的位置一致。
 */
@Composable
internal fun PhoneSeekPulseOverlay(
    pulse: PhoneSeekPulse?,
    seekStepSeconds: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = pulse != null,
        enter = fadeIn(tween(durationMillis = 120)) + scaleIn(
            initialScale = 0.72f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
        exit = fadeOut(tween(durationMillis = 160)) +
                scaleOut(targetScale = 0.9f, animationSpec = tween(durationMillis = 160)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = when (pulse) {
                PhoneSeekPulse.BACK -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            },
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .size(96.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (pulse == PhoneSeekPulse.BACK) {
                            PhoneIcons.Rewind
                        } else {
                            PhoneIcons.Forward
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        text = if (pulse == PhoneSeekPulse.BACK) {
                            "-${seekStepSeconds}s"
                        } else {
                            "+${seekStepSeconds}s"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * 长按画面时的倍速提示：中央一个「2.0x 快进中」。
 *
 * 长按只是**临时**改倍速，画面变快本身不够明确（2x 尤其看不出来），
 * 所以手指按下去就把档位显示出来，松手（`visible` 转 false）立刻收起。
 */
@Composable
internal fun PhoneLongPressSpeedOverlay(
    visible: Boolean,
    speedText: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 120)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
        exit = fadeOut(tween(durationMillis = 140)) +
                scaleOut(targetScale = 0.9f, animationSpec = tween(durationMillis = 140)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PhoneIcons.Forward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.phone_player_long_press_speed, speedText),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

/**
 * 播放页自己的一条轻提示（自动加载字幕、已是最后一个视频、继续播放……）。
 *
 * 播放页是全屏沉浸的，没有 `Scaffold` 可以挂 Snackbar，所以自己画一条。
 * [onClick] 非空时整条可点 —— 「继续播放」那条的文案本身就是「点击此处从头播放」，
 * 与其再放一个小按钮，不如整条吃点击（手指不用瞄准）。
 */
@Composable
internal fun PhonePlayerBanner(
    message: String?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(durationMillis = 160)) + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
        exit = fadeOut(tween(durationMillis = 140)),
        modifier = modifier,
    ) {
        // 用 Surface 的 onClick 重载：涟漪会被裁进 shape 里，不用自己接 pointerInput；
        // 「继续播放」那条不需要按钮，整条吃点击就是它的操作方式
        Surface(
            onClick = onClick ?: {},
            enabled = onClick != null,
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/** 网速文案：B/s → KB/s → MB/s，与电视端右上角那一行是同一口径 */
@Composable
private fun formatNetworkSpeed(bytesPerSecond: Long): String {
    val bps = stringResource(R.string.unit_bps)
    val kbps = stringResource(R.string.unit_kbps)
    val mbps = stringResource(R.string.unit_mbps)
    val locale = LocalLocale.current.platformLocale

    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond $bps"
        bytesPerSecond < 1024 * 1024 ->
            "${String.format(locale, "%.1f", bytesPerSecond / 1024.0)} $kbps"

        else -> "${String.format(locale, "%.1f", bytesPerSecond / 1024.0 / 1024.0)} $mbps"
    }
}
