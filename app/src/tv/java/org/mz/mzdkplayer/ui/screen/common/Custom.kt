package org.mz.mzdkplayer.ui.screen.common

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults

import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults

import androidx.tv.material3.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.common.MzToastManager
import org.mz.mzdkplayer.common.MzToastState
import org.mz.mzdkplayer.ui.theme.myIconButtonColor

/**
 * 电视端输入框。
 *
 * androidx.tv.material3 至今没有 TextField，这里用它的 Surface / ShapeDefaults / typography 拼一个：
 * - 焦点由 BasicTextField 自己持有，外层 Surface 不再可聚焦 —— 旧实现外层是个可聚焦的
 *   ClickableSurface，于是「聚焦外框 → 按确定键 → 才进输入」要按两次，而且外框的聚焦态
 *   与输入框的聚焦态是两套状态，边框高亮经常对不上
 * - 聚焦时描边换成主题色并轻微放大，未聚焦是低调的深灰底
 * - 支持常驻字段名 label（输入后不消失）、placeholder、前置图标、密码掩码与错误态
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: Int? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    enabled: Boolean = true,
    // 紧凑尺寸只给「带字段名的表单字段」用（连接表单页为了矮屏一屏放下）；
    // 搜索框这类没有 label 的输入框保持宽松高度，别被表单页的紧凑口径带跑
    compact: Boolean = label != null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
    textStyle: TextStyle = TextStyle.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isTfFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val tfFocusRequester = remember { FocusRequester() } // 独立焦点请求器

    val shape = ShapeDefaults.Medium
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFF1A1A1A)
            isTfFocused -> Color(0xFF2F2F2F)
            else -> Color(0xFF232323)
        },
        label = "tvTextFieldContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFE53935)
            !enabled -> Color(0xFF333333)
            isTfFocused -> Color.White
            else -> Color(0xFF3D3D3D)
        },
        label = "tvTextFieldBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isTfFocused) 2.dp else 1.dp,
        label = "tvTextFieldBorderWidth"
    )
    val scale by animateFloatAsState(
        targetValue = if (isTfFocused) 1.02f else 1f,
        label = "tvTextFieldScale"
    )
    val contentColor = when {
        !enabled -> Color(0xFF7A7A7A)
        isError -> Color(0xFFFF8A80)
        else -> Color.White
    }
    // 调用方传进来的字号 / 字重优先，没写的部分用 tv-material3 的 bodyLarge 兜底
    val fieldTextStyle = MaterialTheme.typography.bodyLarge.merge(textStyle).copy(color = contentColor)
    val verticalPadding = if (compact) 7.dp else 16.dp
    val labelSpacing = if (compact) 2.dp else 4.dp

    Surface(
        modifier = modifier
            .scale(scale)
            .border(width = borderWidth, color = borderColor, shape = shape),
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = if (isTfFocused) Color.White else Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isError -> Color(0xFFFF8A80)
                            isTfFocused -> Color.White
                            else -> Color(0xFF9E9E9E)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(labelSpacing))
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = fieldTextStyle.copy(color = Color(0xFF6E6E6E)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = fieldTextStyle,
                        cursorBrush = SolidColor(contentColor),
                        interactionSource = interactionSource,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = KeyboardActions(onAny = { }),
                        visualTransformation = if (isPassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(tfFocusRequester)
                            .onKeyEvent { keyEvent ->
                                // 上下键自己接管（交给焦点系统），其余按键一律放行：
                                // 旧实现在所有按键上都返回 true，连外接键盘的字符键都会被吞掉
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    when (keyEvent.key) {
                                        Key.DirectionUp -> {
                                            focusManager.moveFocus(FocusDirection.Up)
                                            true
                                        }

                                        Key.DirectionDown -> {
                                            focusManager.moveFocus(FocusDirection.Down)
                                            true
                                        }

                                        Key.Back -> {
                                            focusManager.moveFocus(FocusDirection.Exit)
                                            true
                                        }

                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            },
                    )
                }
            }
        }
    }
}

// MyIconButton 代码保持不变
@Composable
fun MyIconButton(
    text: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isFileBut: Boolean = false
) {
    val buttonWithIconContentPadding =
        PaddingValues(
            start = 6.dp,
            top = 4.dp,
            end = 10.dp,
            bottom = 4.dp
        )
    Button( // 明确指定是 Tv Material3 的 Button
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = if (isFileBut) buttonWithIconContentPadding else ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraSmall),
        scale = ButtonDefaults.scale(focusedScale = 1.03f),
        colors = myIconButtonColor()

    ) {
        Icon(
            painter = painterResource(icon),
            modifier = if (isFileBut) Modifier.size(16.dp) else Modifier,
            contentDescription = null
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = text,
            style = if (isFileBut) MaterialTheme.typography.titleSmall.copy(
                fontSize = 10.sp
            ) else MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }


}

fun showToast(context: Context,  message: String, duration: Int = 0) {
    MzToastManager.show(message)
}

@Composable
fun rememberMzToastState() = remember { MzToastState() }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MzToast(state: MzToastState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = state.isVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}




