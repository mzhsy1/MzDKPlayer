package org.mz.mzdkplayer.ui.screen.common



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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

@Composable
fun DeleteConfirmDialog(
    title: String = "确认删除",
    message: String = "删除后将无法恢复，确定要执行此操作吗？",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
    ) {
        // 记录是否允许点击，防止TV遥控器按键连击误触
        var allowClick by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .width(320.dp)
                .background(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(8.dp)
                )
                .onPreviewKeyEvent { keyEvent ->
                    // 拦截 OK 键（DPAD_CENTER 或 ENTER）
                    if ((keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) && !allowClick) {
                        when (keyEvent.type) {
                            KeyEventType.KeyDown -> {
                                allowClick = false
                                true // 吃掉事件
                            }
                            KeyEventType.KeyUp -> {
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
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 提示信息
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 确认删除按钮 (复用你的 MyIconButton)
                MyIconButton(
                    text = "确认删除(按两下生效)",
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.delete24dp, // 使用你现有的删除图标
                    onClick = {
                        if (allowClick) {
                            onConfirm()
                            onDismiss() // 点击确认后顺便关掉弹窗
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 取消按钮
                MyIconButton(
                    text = "取消(按两下生效)",
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.close24dp, // 使用你现有的关闭图标
                    onClick = {
                        if (allowClick) {
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}