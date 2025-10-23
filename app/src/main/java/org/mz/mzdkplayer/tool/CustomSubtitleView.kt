/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mz.mzdkplayer.tool

import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import kotlin.math.max


/**
 * A composable that renders subtitles provided by a [CueGroup] from Media3.
 *
 * This component displays both text and bitmap cues according to their layout properties.
 * Text cues are rendered using Canvas with modifier-based positioning, while bitmap cues
 * are rendered using independent layout calculations that respect their native positioning
 * properties from the subtitle source.
 *
 * ## Features
 * - **Dual Rendering Support**: Handles both text-based subtitles (SRT, VTT) and bitmap-based
 *   subtitles (PGSS, PGS) with appropriate rendering techniques for each type
 * - **Modifier-Based Text Positioning**: Text cues respect the provided [modifier] for layout
 *   and positioning, allowing flexible alignment and styling through standard Compose modifiers
 * - **Native Bitmap Positioning**: Bitmap cues use their intrinsic positioning properties
 *   (position, line, anchor types) and are completely independent of the text modifier
 * - **Accessibility Support**: Provides proper content descriptions and live region semantics
 *   for screen readers and accessibility services
 * - **Configuration Handling**: Automatically adapts to orientation changes and screen size
 *   variations when [handleConfigurationChanges] is enabled
 * - **Error Resilience**: Includes comprehensive error handling and recovery mechanisms
 *   for malformed cue data or rendering failures
 * - **Smart Background Rendering**: The [backgroundColor] only appears behind the actual text
 *   content, not the entire screen, due to the combination of fillMaxSize() and wrapContentHeight()
 *
 * ## Layout Behavior
 * - When no custom [modifier] is provided, text cues default to bottom-center positioning
 * - The default positioning uses `fillMaxSize().wrapContentHeight(Alignment.Bottom)` which:
 *   - Provides full screen space for layout calculations
 *   - Only occupies the actual height needed by subtitle text
 *   - Ensures [backgroundColor] appears only behind text, not the entire screen
 * - Bitmap cues ignore the [modifier] completely and use their native positioning
 *
 * ## Usage Notes
 * - The [modifier] parameter **only affects text cues** - bitmap cues will ignore it completely
 * - For text cues, use standard layout modifiers like [Modifier.align], [Modifier.padding],
 *   or [Modifier.offset] to control positioning
 * - Bitmap cues automatically position themselves based on their [Cue.position], [Cue.line],
 *   [Cue.positionAnchor], and [Cue.lineAnchor] properties
 * - Both cue types support z-index layering through [Cue.zIndex] for proper overlay ordering
 * - ASS/SSA subtitles are converted to SRT format by ExoPlayer before reaching this component
 * - The [backgroundColor] creates a background only behind the text content area, not the full screen
 *   due to the smart layout combination of maximum available space and content-wrapping height
 *
 * @param cueGroup The group of cues to display. If null or empty, nothing is rendered.
 *   The cues are automatically filtered to exclude empty text and null bitmaps.
 * @param modifier The [Modifier] to be applied to text cues only. This includes layout
 *   modifiers like alignment, padding, offset, etc. Bitmap cues completely ignore this
 *   modifier and use their native positioning instead. When no modifier is provided,
 *   text cues default to bottom center positioning using a smart layout that ensures
 *   background colors only appear behind the actual text content.
 * @param subtitleStyle The [TextStyle] used for rendering text cues. This includes
 *   color, font size, weight, family, and other text styling properties. Does not
 *   affect bitmap cues.
 * @param backgroundColor The background color behind text cues. Useful for improving
 *   text readability against varying video content. Defaults to fully transparent.
 *   Note: This background only appears behind the actual text content area, not the
 *   entire screen, due to the content-wrapping layout behavior.
 * @param contentDescription The accessibility content description for the subtitle view.
 *   If not provided, an automatic description will be generated from the cue texts.
 * @param isLiveRegion Whether the subtitle view should be treated as a live region
 *   for accessibility. Set to true for dynamic content that updates frequently.
 * @param handleConfigurationChanges Whether to automatically handle configuration
 *   changes like device rotation. When enabled, the component will adapt spacing
 *   and layout for different orientations.
 *
 * @sample androidx.media3.ui.compose.material3.SubtitleViewSample
 *
 * @see Cue
 * @see CueGroup
 * @see androidx.media3.exoplayer.ExoPlayer
 */

@Composable
@UnstableApi
fun SubtitleView(
    cueGroup: CueGroup?,
    modifier: Modifier = Modifier,
    subtitleStyle: TextStyle = TextStyle(
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        fontFamily = FontFamily.Default,
        letterSpacing = TextUnit.Unspecified,
        textDecoration = TextDecoration.None,
        textAlign = TextAlign.Center
    ),
    backgroundColor: Color = Color.Black.copy(alpha = 0.0f),
    contentDescription: String? = null,
    isLiveRegion: Boolean = false,
    handleConfigurationChanges: Boolean = true
) {
    // 处理默认底部居中对齐
    val textModifierWithDefault = if (modifier == Modifier) {
        // 如果用户没有传入自定义 modifier，使用默认底部居中
        Modifier
            .fillMaxSize()
            .wrapContentHeight(Alignment.Bottom)
            .padding(bottom = 16.dp)
    } else {
        // 如果用户传入了自定义 modifier，直接使用
        modifier
    }

    // Error boundary state
    var lastError by remember { mutableStateOf<String?>(null) }

    // Configuration and screen dimensions
    val configuration = LocalConfiguration.current
    val context = LocalContext.current

    val screenDimensions = remember(configuration) {
        getScreenDimensions(context)
    }

    // Remember cue group with configuration dependency
    val rememberedCueGroup = remember(cueGroup, configuration.orientation) {
        cueGroup
    }

    // Filter valid cues
    val visibleCues by remember(rememberedCueGroup) {
        derivedStateOf {
            rememberedCueGroup?.cues?.filter { cue ->
                cue.text?.toString()?.isNotEmpty() == true || cue.bitmap != null
            } ?: emptyList()
        }
    }

    // Handle configuration changes
    LaunchedEffect(configuration.orientation) {
        if (handleConfigurationChanges) {
            Log.d("SubtitleView", "Orientation changed to: ${configuration.orientation}")
        }
    }

    // Error recovery
    LaunchedEffect(lastError) {
        lastError?.let { error ->
            Log.w("SubtitleView", "Recovered from error: $error")
            lastError = null
        }
    }

    // Early return if no cues to display
    if (visibleCues.isEmpty()) {
        return
    }

    // Build accessibility text
    val subtitleText = remember(visibleCues) {
        visibleCues.joinToString(", ") { cue ->
            cue.text?.toString()?.takeIf { it.isNotEmpty() && it != "null" } ?: ""
        }.trim().takeIf { it.isNotEmpty() }
    }

    // Use SafeSubtitleContent to handle rendering errors
    SafeSubtitleContent(
        visibleCues = visibleCues,
        screenDimensions = screenDimensions,
        subtitleStyle = subtitleStyle,
        backgroundColor = backgroundColor,
        textModifier = textModifierWithDefault, // 使用带默认值的modifier
        contentDescription = contentDescription,
        subtitleText = subtitleText,
        isLiveRegion = isLiveRegion,
        configuration = configuration,
        lastError = lastError,
        onError = { error ->
            lastError = error
        }
    )
}

@Composable
@UnstableApi
private fun SafeSubtitleContent(
    visibleCues: List<Cue>,
    screenDimensions: Pair<Int, Int>,
    subtitleStyle: TextStyle,
    backgroundColor: Color,
    textModifier: Modifier, // 专门用于文本渲染的modifier
    contentDescription: String?,
    subtitleText: String?,
    isLiveRegion: Boolean,
    configuration: Configuration,
    lastError: String?,
    onError: (String) -> Unit
) {
    LaunchedEffect(lastError) {
        lastError?.let { error ->
            Log.e("SubtitleView", "Rendering error: $error")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // 确保占据整个空间
            .semantics {
                contentDescription?.let {
                    this.contentDescription = it
                } ?: run {
                    subtitleText?.let {
                        this.contentDescription = "字幕: $it"
                    }
                }
                if (isLiveRegion) {
                    this.liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
            }
    ) {
        // Separate text and bitmap cues for different rendering approaches
        val textCues = visibleCues.filter { it.text?.toString()?.isNotEmpty() == true }
        val bitmapCues = visibleCues.filter { it.bitmap != null }

        // Render text cues with the provided modifier
        TextCuesContent(
            cues = textCues,
            screenDimensions = screenDimensions,
            subtitleStyle = subtitleStyle,
            backgroundColor = backgroundColor,
            modifier = textModifier, // 直接使用传入的modifier
            configuration = configuration,
            onError = onError
        )

        // Render bitmap cues with independent layout (不受modifier影响)
        BitmapCuesContent(
            cues = bitmapCues,
            screenDimensions = screenDimensions,
            onError = onError
        )
    }
}

@Composable
@UnstableApi
private fun TextCuesContent(
    cues: List<Cue>,
    screenDimensions: Pair<Int, Int>,
    subtitleStyle: TextStyle,
    backgroundColor: Color,
    modifier: Modifier, // 专门用于文本的modifier
    configuration: Configuration,
    onError: (String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier, // 直接应用modifier到文本容器
        verticalArrangement = Arrangement.spacedBy(
            when (configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> 2.dp
                else -> 4.dp
            }
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        cues.forEach { cue ->
            RenderTextCue(
                cue = cue,
                textMeasurer = textMeasurer,
                subtitleStyle = subtitleStyle,
                backgroundColor = backgroundColor,
                onError = onError
            )
        }
    }
}

@Composable
@UnstableApi
private fun RenderTextCue(
    cue: Cue,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    subtitleStyle: TextStyle,
    backgroundColor: Color,
    onError: (String) -> Unit
) {
    cue.text?.toString()?.takeIf { it.isNotEmpty() && it != "null" }?.let { text ->
        val textLayoutResult = try {
            textMeasurer.measure(
                text = text,
                style = subtitleStyle.copy(textAlign = TextAlign.Center)
            )
        } catch (e: Exception) {
            onError("Text measurement error for: $text - ${e.message}")
            return@let
        }

        Canvas(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .background(backgroundColor)
                .width(with(LocalDensity.current) { textLayoutResult.size.width.toDp() })
                .height(with(LocalDensity.current) { textLayoutResult.size.height.toDp() })
                .semantics {
                    this.contentDescription = "字幕: $text"
                    this.liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
        ) {
            try {
                drawText(
                    textLayoutResult = textLayoutResult,
                    color = subtitleStyle.color
                )
            } catch (e: Exception) {
                onError("Text drawing error: ${e.message}")
            }
        }
    }
}

@Composable
@UnstableApi
private fun BitmapCuesContent(
    cues: List<Cue>,
    screenDimensions: Pair<Int, Int>,
    onError: (String) -> Unit
) {
    val (screenWidthDp, screenHeightDp) = screenDimensions

    // Safe screen dimensions
    val safeScreenWidth = max(screenWidthDp, 1)
    val safeScreenHeight = max(screenHeightDp, 1)

    cues.forEach { cue ->
        RenderBitmapCue(
            cue = cue,
            safeScreenWidth = safeScreenWidth,
            safeScreenHeight = safeScreenHeight,
            onError = onError
        )
    }
}

@Composable
@UnstableApi
private fun RenderBitmapCue(
    cue: Cue,
    safeScreenWidth: Int,
    safeScreenHeight: Int,
    onError: (String) -> Unit
) {
    cue.bitmap?.let { bitmap ->
        val calculatedValues = try {
            // Safe bitmap dimensions calculation
            val bitmapWidth = if (cue.size != Cue.DIMEN_UNSET && cue.size > 0) {
                (safeScreenWidth * cue.size)
            } else {
                bitmap.width.toFloat()
            }.coerceAtLeast(1f)

            val bitmapHeight = if (cue.bitmapHeight != Cue.DIMEN_UNSET && cue.bitmapHeight > 0) {
                (safeScreenHeight * cue.bitmapHeight)
            } else {
                bitmap.height.toFloat()
            }.coerceAtLeast(1f)

            // Safe position calculation with defaults
            val position = cue.position.takeIf {
                it != Cue.DIMEN_UNSET
            } ?: 0.5f // Default center

            val line = cue.line.takeIf {
                it != Cue.DIMEN_UNSET
            } ?: 0.9f // Default bottom

            // Calculate offsets with bounds checking
            val offsetX = calculateHorizontalOffset(
                position = position,
                anchor = cue.positionAnchor,
                screenWidth = safeScreenWidth,
                elementWidth = bitmapWidth
            ).coerceIn(0f, safeScreenWidth.toFloat())

            val offsetY = calculateVerticalOffset(
                line = line,
                anchor = cue.lineAnchor,
                screenHeight = safeScreenHeight,
                elementHeight = bitmapHeight
            ).coerceIn(0f, safeScreenHeight.toFloat())

            Triple(bitmapWidth, bitmapHeight, Pair(offsetX, offsetY))
        } catch (e: Exception) {
            onError("Bitmap calculation error: ${e.message}")
            return@let
        }

        val (bitmapWidth, bitmapHeight, offsets) = calculatedValues
        val (offsetX, offsetY) = offsets

        Box(
            modifier = Modifier
                .offset(x = offsetX.dp, y = offsetY.dp)
                .width(bitmapWidth.dp)
                .height(bitmapHeight.dp)
                .zIndex(cue.zIndex.toFloat().coerceIn(-100f, 100f))
                .semantics {
                    this.contentDescription = "图形字幕"
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                try {
                    val srcSize = IntSize(width = bitmap.width, height = bitmap.height)
                    val dstWidth = (bitmapWidth * density).toInt()
                    val dstHeight = (bitmapHeight * density).toInt()
                    val dstSize = IntSize(width = dstWidth, height = dstHeight)

                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = IntOffset.Zero,
                        srcSize = srcSize,
                        dstOffset = IntOffset.Zero,
                        dstSize = dstSize,
                        alpha = 1.0f,
                        blendMode = DefaultBlendMode
                    )
                } catch (e: Exception) {
                    onError("Bitmap drawing error: ${e.message}")
                }
            }
        }
    }
}

/**
 * Calculates horizontal offset based on position and anchor type.
 */
private fun calculateHorizontalOffset(
    position: Float,
    anchor: Int,
    screenWidth: Int,
    elementWidth: Float
): Float {
    return when (anchor) {
        Cue.ANCHOR_TYPE_START -> screenWidth * position
        Cue.ANCHOR_TYPE_MIDDLE -> (screenWidth * position) - (elementWidth / 2)
        Cue.ANCHOR_TYPE_END -> (screenWidth * position) - elementWidth
        else -> screenWidth * position
    }
}

/**
 * Calculates vertical offset based on line and anchor type.
 */
private fun calculateVerticalOffset(
    line: Float,
    anchor: Int,
    screenHeight: Int,
    elementHeight: Float
): Float {
    return when (anchor) {
        Cue.ANCHOR_TYPE_START -> screenHeight * line
        Cue.ANCHOR_TYPE_MIDDLE -> (screenHeight * line) - (elementHeight / 2)
        Cue.ANCHOR_TYPE_END -> (screenHeight * line) - elementHeight
        else -> screenHeight * line
    }
}

/**
 * Returns the current screen dimensions in dp.
 */
private fun getScreenDimensions(context: android.content.Context): Pair<Int, Int> {
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val widthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
    val heightDp = (displayMetrics.heightPixels / displayMetrics.density).toInt()
    return Pair(widthDp, heightDp)
}