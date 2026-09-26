package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor
import androidx.compose.ui.res.stringResource

@Composable
fun ISODialog(
    onDismiss: () -> Unit,
    fileName: String? = "未知 ISO 文件",
    titles: List<String>,
    currentUri: String, // 保留 URI 以备后续扩展
    onTitleSelected: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
    ) {
        // 记录是否允许点击（TV 遥控器防误触逻辑，与 MyFileDialog.kt 完全一致）
        var allowClick by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .width(420.dp) // 比 MyFileDialog 稍宽，适合显示列表
                .height(460.dp)
                .background(
                    color = Color.DarkGray,
                    RoundedCornerShape(6.dp)
                )
                .onPreviewKeyEvent { keyEvent ->
                    if ((keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) && !allowClick) {
                        when (keyEvent.type) {
                            KeyEventType.KeyDown -> {
                                allowClick = false
                                true
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
                    .padding(20.dp)
                    .align(Alignment.Center)
            ) {
                // 标题栏
                if (fileName != null) {
                    Text(
                        text = "ISO 文件：$fileName",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ISO 内视频标题列表（共 ${titles.size} 个）",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 视频标题列表（TV 友好，使用 tv.material3.ListItem）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    items(titles) { title ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            },
                            colors = MyFileListItemColor(),
                            onClick = {
                                if (allowClick) {
                                    onTitleSelected(title)
                                }
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            selected = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 关闭按钮（与 MyFileDialog 风格一致）
                MyIconButton(
                    text = stringResource(R.string.ui_label_close_popup),
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