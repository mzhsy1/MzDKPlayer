package org.mz.mzdkplayer.ui.screen.common

import org.mz.mzdkplayer.tool.MzToastManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

@Composable

fun MyFileDialog(
    onDismiss:() -> Unit,
    fileName: String? = "未知文件名",
    onEditClick: () -> Unit,
    onCloseClick:() -> Unit
) {

    Dialog(
        onDismissRequest = { onDismiss() },
    )
    {
        // 记录是否允许点击
        var allowClick by remember { mutableStateOf(false) }
        Log.d("allowClick", allowClick.toString())
        val context = LocalContext.current
        // 捕获按键事件
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(240.dp)
                .background(
                    color = Color.DarkGray,
                    RoundedCornerShape(6.dp)
                )
                .onPreviewKeyEvent { keyEvent ->
                    // 如果是 OK 键（DPAD_CENTER 或 ENTER）
                    if ((keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) && !allowClick) {
                        when (keyEvent.type) {
                            KeyEventType.KeyDown -> {
                                // 只要 OK 还在按住，就不允许点击
                                allowClick = false
                                true    // 吃掉事件
                            }

                            KeyEventType.KeyUp -> {
                                // 松开 OK 键后才允许点击
                                allowClick = true
                                true
                            }

                            else -> true
                        }
                    } else {
                        false
                    }
                }
        ) {
            Column(modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center)) {
                if (fileName != null) {
                    Text(text = fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                MyIconButton(
                    text = stringResource(R.string.ui_label_modify_file_media_info),
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.save24dp,
                    onClick = {
                        if (allowClick) {
                            onEditClick()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyIconButton(
                    text = stringResource(R.string.ui_label_delete_file_media_info),
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.delete24dp,
                    onClick = {
                        if (allowClick) {
                            MzToastManager.show(context.getString(R.string.ui_label_click_to_delete))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyIconButton(
                    text = context.getString(R.string.ui_label_close_popup),
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.close24dp,
                    onClick = {
                        if (allowClick) {
                            onCloseClick()
                        }
                    }
                )
            }
        }
    }
}
