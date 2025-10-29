package org.mz.mzdkplayer.ui.theme

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface

import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    placeholder: String = "",
    textStyle: TextStyle = TextStyle.Default,
) {
    // 1. 为每个输入框创建独立的状态
    val interactionSource = remember { MutableInteractionSource() }
    val isTfFocused by interactionSource.collectIsFocusedAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val tfFocusRequester = remember { FocusRequester() } // 独立焦点请求器
    var lastValue by remember { mutableStateOf(value) }
    Surface(
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = colors,
        interactionSource = interactionSource,
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = if (isTfFocused) 2.dp else 1.dp,
                    color = animateColorAsState(
                        targetValue = if (isTfFocused) Color.White
                        else MaterialTheme.colorScheme.border,
                        label = ""
                    ).value
                ),
            ),
            pressedBorder = Border(
                border = BorderStroke(
                    width = if (isTfFocused) 2.dp else 1.dp,
                    color = animateColorAsState(
                        targetValue = if (isTfFocused) Color.White
                        else MaterialTheme.colorScheme.border,
                        label = ""
                    ).value
                ),
            )
        ),
        // tonalElevation = 2.dp,
        modifier = modifier,
        onClick = { tfFocusRequester.requestFocus() }
    ) {
        BasicTextField(
            value = value,
            textStyle = textStyle.copy(color = Color.White),
            onValueChange = onValueChange,
            // --- 修改部分：添加 cursorBrush 参数 ---
            cursorBrush = SolidColor(Color.White), // 设置光标颜色为白色
            // --- 修改部分结束 ---
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(
                    vertical = 4.dp,
                    horizontal = 8.dp
                )
                .focusRequester(tfFocusRequester)
                .onKeyEvent {
                    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        when (it.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                focusManager.moveFocus(FocusDirection.Down)
                            }

                            KeyEvent.KEYCODE_DPAD_UP -> {
                                focusManager.moveFocus(FocusDirection.Up)
                            }

                            KeyEvent.KEYCODE_BACK -> {
                                focusManager.moveFocus(FocusDirection.Exit)
                            }
                        }
                    }
                    true
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            keyboardActions = KeyboardActions(
                onAny = { }
            ),
            visualTransformation = VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .align(Alignment.CenterStart),
                    ) {
                        Text(
                            text = placeholder,
                            color = Color.Gray
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(start = 15.dp),
                ) {
                    innerTextField()
                }
            },
            maxLines = 1
        )
    }
}

// MyIconButton 代码保持不变
@Composable
fun MyIconButton(
    text: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button( // 明确指定是 Tv Material3 的 Button
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraSmall),
        scale = ButtonDefaults.scale( focusedScale = 1.03f),
        colors = ButtonDefaults.colors(
            Color.DarkGray, // background
            Color.White, // content
            Color.White, // disabled background
            Color.Black  // disabled content
        )

    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}




