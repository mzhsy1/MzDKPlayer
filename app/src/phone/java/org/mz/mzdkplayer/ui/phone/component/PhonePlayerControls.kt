package org.mz.mzdkplayer.ui.phone.component

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.common.findActivity
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.logic.PhoneSettingsLogic
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** 双击屏幕左右两侧时的反馈方向 */
internal enum class PhoneSeekPulse { BACK, FORWARD }

/** 控制层顶部 / 底部渐变的底色（纯黑渐隐，保证白色图标在任何画面上都看得清） */
private val ControlsScrim = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
)

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
 * 手机端播放页的**控制层**：顶部（返回 / 标题 / 网速 / 播放列表 / 设置）、
 * 中央播放暂停、底部（进度条 + 时间 + 快退 / 快进 / 上一个 / 下一个）。
 *
 * 触屏与遥控器的根本差别就在这一层：电视端进度条靠左右键累计 + 落定回调，
 * 这里直接用可拖动的 [Slider]；电视端的「上键唤面板」换成右上角的设置按钮。
 */
@Composable
internal fun PhonePlayerControls(
    title: String,
    dateText: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    /** 快进 / 快退的秒数（各取设置里的 `ffDuration` / `rwDuration`，与电视端同一份设置） */
    forwardSeconds: Int,
    backSeconds: Int,
    networkSpeed: Long?,
    /** 是否处于全屏（横屏）——只决定右下角那个按钮的图标与语义 */
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 拖拽期间进度条跟手，不受播放心跳（每 200ms 一次）影响
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val shownPosition = if (dragging) dragValue.roundToLong() else positionMs

    Column(modifier = modifier.fillMaxSize()) {
        ControlsTopBar(
            title = title,
            dateText = dateText,
            networkSpeed = networkSpeed,
            onBack = onBack,
            onOpenPlaylist = onOpenPlaylist,
            onOpenSettings = onOpenSettings,
        )

        // 中央只留白：播放暂停交给底部那一个按钮，这里不再放第二个浮在画面正中挡住内容。
        // 留白的点击落到手势层，于是「点画面任意处收起控制栏」这件事不需要额外代码。
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
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                forwardSeconds = forwardSeconds,
                backSeconds = backSeconds,
                isFullscreen = isFullscreen,
                onDrag = {
                    dragging = true
                    dragValue = it
                    onInteraction()
                },
                onDragFinished = {
                    onSeekTo(dragValue.roundToLong())
                    dragging = false
                    onInteraction()
                },
                onTogglePlayPause = {
                    onInteraction()
                    onTogglePlayPause()
                },
                onSeekBack = {
                    onInteraction()
                    onSeekBack()
                },
                onSeekForward = {
                    onInteraction()
                    onSeekForward()
                },
                onPrevious = {
                    onInteraction()
                    onPrevious()
                },
                onNext = {
                    onInteraction()
                    onNext()
                },
                onToggleFullscreen = {
                    onInteraction()
                    onToggleFullscreen()
                },
            )
        } else {
            ControlsCompactBar(
                positionMs = shownPosition,
                durationMs = durationMs,
                isPlaying = isPlaying,
                isFullscreen = isFullscreen,
                onTogglePlayPause = {
                    onInteraction()
                    onTogglePlayPause()
                },
                onToggleFullscreen = {
                    onInteraction()
                    onToggleFullscreen()
                },
            )
        }
    }
}

@Composable
private fun ControlsTopBar(
    title: String,
    dateText: String?,
    networkSpeed: Long?,
    onBack: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ControlsScrim)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.phone_action_back),
                tint = Color.White,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
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
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (networkSpeed != null) {
            Text(
                text = formatNetworkSpeed(networkSpeed),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        IconButton(onClick = onOpenPlaylist) {
            Icon(
                imageVector = PhoneIcons.Playlist,
                contentDescription = stringResource(R.string.ui_label_playlist),
                tint = Color.White,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.ui_label_settings),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ControlsBottomBar(
    shownPosition: Long,
    durationMs: Long,
    isPlaying: Boolean,
    dragging: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    forwardSeconds: Int,
    backSeconds: Int,
    isFullscreen: Boolean,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val enabled = durationMs > 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 这里用「收 value 的旧重载」而不是新的 `SliderState`：新 API 把 value 当 remember 的
        // key，进度每 200ms 心跳一次就会重建状态、把正在拖拽的手势打断（音频页同款说明）。
        Slider(
            value = if (enabled) shownPosition.toFloat().coerceIn(0f, durationMs.toFloat()) else 0f,
            onValueChange = onDrag,
            onValueChangeFinished = onDragFinished,
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
            enabled = enabled,
            // 不额外收紧高度：Slider 自带的 48dp 触摸区是「手指能按住」的下限
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = Tools.formatTime(shownPosition),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(64.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (dragging) {
                    // 拖拽时把「将要跳到的位置」放在总时长那一侧，手指不挡视线
                    Tools.formatTime(shownPosition)
                } else {
                    Tools.formatTime(durationMs)
                },
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(
                    imageVector = PhoneIcons.SkipPrevious,
                    contentDescription = stringResource(R.string.phone_player_previous_video),
                    tint = iconTint(hasPrevious),
                    modifier = Modifier.size(30.dp),
                )
            }

            IconButton(
                onClick = onSeekBack,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = PhoneIcons.Rewind,
                    contentDescription = stringResource(
                        R.string.phone_player_rewind, backSeconds
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            FilledIconButton(
                onClick = onTogglePlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.92f),
                    contentColor = Color.Black,
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(52.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) PhoneIcons.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.phone_player_play_pause),
                    modifier = Modifier.size(28.dp),
                )
            }

            IconButton(
                onClick = onSeekForward,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = PhoneIcons.Forward,
                    contentDescription = stringResource(
                        R.string.phone_player_forward, forwardSeconds
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(
                    imageVector = PhoneIcons.SkipNext,
                    contentDescription = stringResource(R.string.phone_player_next_video),
                    tint = iconTint(hasNext),
                    modifier = Modifier.size(30.dp),
                )
            }

            // 全屏：转横屏让画面占满整块屏幕，再点回到竖屏。
            // 播放页本身已经是沉浸全屏（无状态栏），但竖屏下 16:9 画面只占中间一条，
            // 只有横屏才谈得上「全屏」，所以这个按钮做的是转屏而不是隐藏系统栏。
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Icon(
                    imageVector = if (isFullscreen) {
                        PhoneIcons.FullscreenExit
                    } else {
                        PhoneIcons.Fullscreen
                    },
                    contentDescription = stringResource(
                        if (isFullscreen) {
                            R.string.phone_player_exit_fullscreen
                        } else {
                            R.string.phone_player_fullscreen
                        }
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

/**
 * 竖屏（未全屏）时的**精简控制行**：只有「播放暂停 · 时间 · 全屏」这一行。
 *
 * 竖屏的画面区只有 16:9 那么高（这台机器上约 245dp），把「进度条 + 时间 + 五个按钮」
 * 整块铺进去会盖掉大半个画面 —— B 站竖屏也只留这一行，拖进度条要横屏。
 * 快进 / 快退不受影响：双击画面左右 1/3 的手势照旧（那是 `PhonePlayerGestureLayer` 的事）。
 */
@Composable
private fun ControlsCompactBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                )
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onTogglePlayPause) {
            Icon(
                imageVector = if (isPlaying) PhoneIcons.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.phone_player_play_pause),
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "${Tools.formatTime(positionMs)} / ${Tools.formatTime(durationMs)}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )

        IconButton(onClick = onToggleFullscreen) {
            Icon(
                imageVector = if (isFullscreen) {
                    PhoneIcons.FullscreenExit
                } else {
                    PhoneIcons.Fullscreen
                },
                contentDescription = stringResource(
                    if (isFullscreen) {
                        R.string.phone_player_exit_fullscreen
                    } else {
                        R.string.phone_player_fullscreen
                    }
                ),
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

private fun iconTint(enabled: Boolean): Color =
    if (enabled) Color.White else Color.White.copy(alpha = 0.35f)

/**
 * 双击快进 / 快退时的中央脉冲提示：一个大箭头 + `±15s`，700ms 后自己消失。
 */
@Composable
internal fun PhoneSeekPulseOverlay(
    pulse: PhoneSeekPulse?,
    seekStepSeconds: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = pulse != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
        ) {
            Column(
                modifier = Modifier.align(
                    when (pulse) {
                        PhoneSeekPulse.BACK -> Alignment.CenterStart
                        else -> Alignment.CenterEnd
                    }
                ).padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = if (pulse == PhoneSeekPulse.BACK) {
                        PhoneIcons.Rewind
                    } else {
                        PhoneIcons.Forward
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = if (pulse == PhoneSeekPulse.BACK) {
                        "-${seekStepSeconds}s"
                    } else {
                        "+${seekStepSeconds}s"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
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
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.72f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
                        style = MaterialTheme.typography.labelLarge,
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
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Text(
            text = message.orEmpty(),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = CircleShape,
                )
                .then(
                    if (onClick != null) {
                        Modifier.pointerInput(onClick) {
                            detectTapGestures { onClick() }
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
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
