package org.mz.mzdkplayer.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.navOptions
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults

import androidx.tv.material3.Text


import kotlinx.coroutines.launch

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
        border =ClickableSurfaceDefaults.border(
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
            pressedBorder= Border(
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
            onValueChange =onValueChange,
            modifier = Modifier
                .fillMaxWidth(0.49f)
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
                    Text(
                        text = placeholder,
                       color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(start = 20.dp),
                ) {
                    innerTextField()
                }
            },
            maxLines = 1
        )
    }
}




@Composable
fun MyIconButton(
    text: String,
    imageVector: ImageVector ,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraSmall),
        colors = ButtonDefaults.colors(
            ComposeColor.White,
            ComposeColor.Black,
            ComposeColor.White,
            ComposeColor.Black
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