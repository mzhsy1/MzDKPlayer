package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.logic.FileSortLogic
import org.mz.mzdkplayer.tool.logic.FileSortOption
import org.mz.mzdkplayer.ui.common.fileSortLabel
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

/**
 * 文件列表页的排序入口（电视端六个文件浏览页共用）。
 *
 * 形态是「排序图标 + 当前排序」一整行，而不是一个圆形图标按钮：
 * - 遥控器上图标按钮不好按到（搜索框会吃掉左右键，光标移动是输入框自己的事），
 *   一排横贯右栏的条目用上下键就能到；
 * - 当前口径直接写在脸上（「文件夹优先 · 名称 A→Z」），不用点开才知道。
 *
 * 为什么不用「点一下换一档」：排序档位有 7 个，遥控器上一路按过去太折磨，
 * 弹层一次列全、上下选一下更快；但设置页仍保留「点一下换一档」的循环写法。
 *
 * 弹层里的「文件夹优先」是开关，切换后**不关闭**弹层（还能继续选字段/方向）；
 * 点具体档位则立即应用并关闭。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileSortEntry(
    current: FileSortOption,
    onSortChange: (FileSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        selected = false,
        onClick = { showDialog = true },
        colors = MyFileListItemColor(),
        modifier = modifier.height(48.dp),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.baseline_sort_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFE9C46A),
            )
        },
        headlineContent = {
            Text(
                text = fileSortLabel(current),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )

    if (showDialog) {
        FileSortDialog(
            current = current,
            onToggleFoldersFirst = { onSortChange(current.copy(foldersFirst = it)) },
            onSelect = {
                onSortChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

/**
 * 排序选择弹层。
 *
 * 焦点细节：初始焦点直接落在「当前生效的那一档」上，一进来按上下就能换。
 *
 * 防误触用「开门闸」（打开 250ms 内不响应点击）而不是拦按键：MyFileDialog / ISODialog 那套
 * `onPreviewKeyEvent` 拦 KeyUp 的做法会把 **下一次** 确定键的 KeyUp 也一起吃掉
 * （keyUp 被 preview 消费，clickable 收不到，等于要按两次才生效）。
 * 这里改成打开后 250ms 内忽略点击：打开弹层的那次按键自然落在窗口之外，
 * 之后的按键一次就能选中。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileSortDialog(
    current: FileSortOption,
    onToggleFoldersFirst: (Boolean) -> Unit,
    onSelect: (FileSortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        var ready by remember { mutableStateOf(false) }
        val selectedItemRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            // 等一帧让弹层里的节点挂上 FocusRequester；拿不到就交给系统默认焦点
            delay(120)
            runCatching { selectedItemRequester.requestFocus() }
            delay(150)
            ready = true
        }

        Box(
            modifier = Modifier
                .width(380.dp)
                .background(Color(0xFF1B1B21), RoundedCornerShape(18.dp))
                .padding(vertical = 12.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_sort_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFE9C46A),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.ui_label_file_sort),
                        color = Color(0xFFF4F1EA),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(6.dp))

                SortOptionRow(
                    label = stringResource(R.string.ui_label_file_sort_folders_first),
                    selected = current.foldersFirst,
                    onClick = { if (ready) onToggleFoldersFirst(!current.foldersFirst) },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                FileSortLogic.PRESETS.forEach { preset ->
                    val option = preset.copy(foldersFirst = current.foldersFirst)
                    val isSelected = preset.field == current.field && preset.descending == current.descending
                    SortOptionRow(
                        label = fileSortLabel(option),
                        selected = isSelected,
                        // 当前档位挂 FocusRequester，弹层打开后方向键立刻可用
                        modifier = if (isSelected) Modifier.focusRequester(selectedItemRequester) else Modifier,
                        // 防止「打开弹层的那一次确定键」直接触发某一项
                        onClick = { if (ready) onSelect(option) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SortOptionRow(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        colors = MyFileListItemColor(),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 10.dp),
        headlineContent = {
            Text(text = label, fontSize = 15.sp, maxLines = 1)
        },
        trailingContent = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.check24dp),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFB8860B),
                )
            }
        },
    )
}
