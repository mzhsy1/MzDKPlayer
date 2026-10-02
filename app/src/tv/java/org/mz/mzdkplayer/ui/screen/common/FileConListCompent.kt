package org.mz.mzdkplayer.ui.screen.common

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor

/**
 * 各协议连接列表的强调色。
 *
 * 五个协议原本共用一套灰蓝配色（图标底 #37474F、状态点绿色），切协议时界面长得一模一样，
 * 只有标题文字不同；这里给每个协议一个代表色，用于图标底、强调条、徽标与聚焦光晕。
 */
object ConListAccent {
    val smb = Color(0xFF4FC3F7)
    val ftp = Color(0xFFFFB74D)
    val nfs = Color(0xFF81C784)
    val webDav = Color(0xFFBA68C8)
    val http = Color(0xFF64B5F6)
    val local = Color(0xFF9CCC65)
    val default = Color(0xFF90A4AE)
}

// 聚焦（未按下）时卡片底色是米白（见 ui/theme/MyCardStyle.myCardColor），卡片里的文字与图标
// 必须跟着翻成深色，否则就是白底白字。原来这些颜色写死在各个卡片里，聚焦后标签直接糊掉。
private val OnDarkTitle = Color(255, 248, 240)
private val OnDarkDetailLabel = Color(0xFF9A9A9A)
private val OnDarkDetailValue = Color(0xFFE4DFD8)
private val OnLightTitle = Color(0xFF1C1B18)
private val OnLightDetailLabel = Color(0xFF8B8177)
private val OnLightDetailValue = Color(0xFF3A3733)

/**
 * ====== 标题栏 ======
 *
 * @param protocolIconRes 协议图标；传 0 时按 [isLocalFile] 回落到本地 / 网络存储图标
 * @param accentColor 协议强调色，用于图标底与底部那条渐变分隔线
 */
@Composable
fun FCLMainTitle(
    mainNavController: NavHostController,
    titleText: String,
    addTargetRouter: String = "",
    isLocalFile: Boolean = false,
    protocolIconRes: Int = 0,
    accentColor: Color = ConListAccent.default
) {
    val icon = when {
        protocolIconRes != 0 -> protocolIconRes
        isLocalFile -> R.drawable.localfile
        else -> R.drawable.storage24dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF202027), Color(0xFF16161B))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 协议图标底：一格强调色的圆角底，替代「所有协议都用同一张灰色存储图标」
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.42f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = titleText,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (!isLocalFile) stringResource(R.string.ui_label_network_storage) else stringResource(R.string.ui_label_local_storage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9C9AA6)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 添加新连接按钮
                if (!isLocalFile) {
                    MyIconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = { mainNavController.navigate(addTargetRouter) },
                        text = stringResource(R.string.ui_label_add_connection),
                        icon = R.drawable.add24dp,
                    )
                }

                MyIconButton(
                    onClick = { /**TODO 转到设置帮助页面**/ },
                    text = stringResource(R.string.ui_label_help),
                    icon = R.drawable.help24,
                )
            }
        }

        // 底部强调线：从协议色渐变到透明，把标题栏和内容区切开
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.7f),
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * 连接列表标题：左侧一条协议色竖条 + 标题，右侧提示遥控器按键。
 *
 * 原来只有一行「已保存的连接 N 个」；菜单键开操作面板这件事没有任何地方提示过，
 * 顺手把按键说明放在标题行右侧。
 */

@Composable
fun ConnectionListTitle(
    conSize: Int = 0,
    accentColor: Color = ConListAccent.default
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp, 20.dp)
                    .background(color = accentColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.ui_label_saved_connections, conSize),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = stringResource(R.string.ui_label_con_list_remote_hint),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8C8A96)
        )
    }
}

/**
 * 空列表状态。
 *
 * @param onAddClick 传了就在空态正中补一个可聚焦的「添加连接」按钮。列表为空时 LazyColumn
 *   没有任何可聚焦项，焦点只能靠方向键去够右上角的按钮；空态自己带一个入口更省事
 */

@Composable
fun ConnectionListEmpty(
    poolText: String,
    protocolIconRes: Int = R.drawable.storage24dp,
    accentColor: Color = ConListAccent.default,
    onAddClick: (() -> Unit)? = null
) {
    val addFocusRequester = remember { FocusRequester() }
    if (onAddClick != null) {
        LaunchedEffect(Unit) {
            addFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    )
    {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(accentColor.copy(alpha = 0.10f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = protocolIconRes),
                    contentDescription = "Empty",
                    tint = accentColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ui_label_no_pool_connections, poolText),
                color = Color(0xFFEDEAF2),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.ui_label_add_first_pool_connection, poolText),
                color = Color(0xFF8C8A96),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            if (onAddClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                MyIconButton(
                    text = stringResource(R.string.ui_label_add_connection),
                    icon = R.drawable.add24dp,
                    modifier = Modifier.focusRequester(addFocusRequester),
                    onClick = onAddClick
                )
            }
        }
    }
}

/**
 * 文件列表通用卡片
 *
 * @param accentColor 协议强调色：图标底、聚焦描边与光晕
 * @param protocolIconRes 协议图标
 * @param protocolLabel 协议徽标文字（如 "SMB"），为空则不显示徽标
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectionCard(
    modifier: Modifier,
    index: Int,
    connectionCardInfo: ConnectionCardInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLogClick: () -> Unit,
    isSelected: Boolean,
    isOPanelShow: Boolean,
    selectedIndex: Int,
    accentColor: Color = ConListAccent.default,
    protocolIconRes: Int = R.drawable.storage24dp,
    protocolLabel: String? = null,
) {
    val focusRequester = remember { FocusRequester() }
    // 当操作面板显示/隐藏状态改变时，如果当前卡片是选中的，则请求焦点
    LaunchedEffect(!isOPanelShow) {
        if (selectedIndex == index && !isOPanelShow) {
            Log.d("Card", "Requesting focus for selected card at index: $index")
            focusRequester.freeFocus()
            focusRequester.requestFocus()
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    // 聚焦底色米白、按下底色深灰，所以只在「聚焦且未按下」时用深色文字
    val lightSurface = isFocused && !isPressed
    val titleColor = if (lightSurface) OnLightTitle else OnDarkTitle
    val labelColor = if (lightSurface) OnLightDetailLabel else OnDarkDetailLabel
    val valueColor = if (lightSurface) OnLightDetailValue else OnDarkDetailValue
    val separatorColor = if (lightSurface) Color(0x22000000) else Color(0x33FFFFFF)
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .focusRequester(focusRequester),
        onClick = onClick,
        onLongClick = onLogClick,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(shape = shape),
        colors = myCardColor(),
        scale = CardDefaults.scale(
            scale = 1f,
            focusedScale = 1.02f, // 聚焦时轻微放大
            pressedScale = 0.99f // 按下时轻微缩小
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(width = 1.dp, color = Color.White.copy(alpha = 0.05f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = accentColor),
                shape = shape
            )
        ),
        glow = CardDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow(
                elevation = 16.dp,
                elevationColor = accentColor.copy(alpha = 0.30f)
            ),
            pressedGlow = Glow.None
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标区域
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = if (isSelected) 0.26f else 0.18f))
                    .border(
                        width = 1.dp,
                        // 刚操作过的那一项（面板关闭后）留一圈更亮的描边，一眼看得出刚才动的是哪张卡
                        color = accentColor.copy(alpha = if (isSelected) 0.8f else 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = protocolIconRes),
                    contentDescription = protocolLabel,
                    tint = if (lightSurface) accentColor else accentColor.copy(alpha = 0.95f),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // 内容区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 连接名称 + 协议徽标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = connectionCardInfo.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!protocolLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        ProtocolBadge(text = protocolLabel, accentColor = accentColor)
                    }
                }

                // 连接详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConnectionInfoItem(
                        label = stringResource(R.string.ui_label_server),
                        value = connectionCardInfo.address,
                        labelColor = labelColor,
                        valueColor = valueColor
                    )

                    InfoSeparator(color = separatorColor)

                    ConnectionInfoItem(
                        label = stringResource(R.string.ui_label_shared_directory),
                        value = connectionCardInfo.shareName,
                        labelColor = labelColor,
                        valueColor = valueColor
                    )

                    if (connectionCardInfo.username.isNotEmpty()) {
                        InfoSeparator(color = separatorColor)
                        ConnectionInfoItem(
                            label = stringResource(R.string.ui_label_username),
                            value = connectionCardInfo.username,
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 状态指示器
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = accentColor, shape = CircleShape)
            )
        }
    }
}

/** 协议徽标：卡片标题右侧的小胶囊 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtocolBadge(text: String, accentColor: Color) {
    Surface(
        shape = ShapeDefaults.Small,
        colors = SurfaceDefaults.colors(
            containerColor = accentColor.copy(alpha = 0.18f),
            contentColor = accentColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/** 详情之间的竖分隔线 */
@Composable
private fun InfoSeparator(color: Color) {
    Box(
        modifier = Modifier
            .size(1.dp, 22.dp)
            .background(color = color)
    )
}

@Composable
fun OperationListItem(
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    iconRes: Int? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    // 聚焦时 ListItem 底色是米白，原来写死的暖白文字会变成白底白字（「编辑信息」那项一直看不清）
    val resolvedColor = if (isFocused && !isPressed) OnLightTitle else textColor
    val leading: (@Composable BoxScope.() -> Unit)? = if (iconRes != null) {
        {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = resolvedColor,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        null
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp)),
        selected = false,
        onClick = {
            if (isPressed) {
                onClick()
            }
        },
        interactionSource = interactionSource,
        colors = myListItemCoverColor(),
        leadingContent = leading,
        headlineContent = {
            Text(
                text = text,
                color = resolvedColor,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium
            )
        }
    )
}

@Composable
fun ConnectionInfoItem(
    label: String,
    value: String?,
    labelColor: Color = OnDarkDetailLabel,
    valueColor: Color = OnDarkDetailValue
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * // 操作面板（右侧弹出）
 *
 * @param accentColor 协议强调色，用于标题左侧的竖条与协议名
 * @param protocolLabel 协议名，显示在标题下方
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConOpPanel(
    modifier: Modifier,
    isOPanelShow: Boolean = false,
    panelFocusRequester: FocusRequester,
    onClickForDel: () -> Unit,
    onClickForEdit: () -> Unit,
    onClickForCancel: () -> Unit,
    accentColor: Color = ConListAccent.default,
    protocolLabel: String? = null
) {
    AnimatedVisibility(
        visible = isOPanelShow,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF232329))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(18.dp)
                )
                .focusRequester(panelFocusRequester)
        ) {
            // 面板标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .background(color = accentColor, shape = RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ui_label_connection_operation),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!protocolLabel.isNullOrBlank()) {
                        Text(
                            text = protocolLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )

            // 使用 Column 替代 LazyColumn 确保内容居中
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OperationListItem(
                    text = stringResource(R.string.ui_label_delete_connection),
                    textColor = Color(0xFFF44336),
                    iconRes = R.drawable.delete24dp,
                    onClick = onClickForDel
                )

                OperationListItem(
                    text = stringResource(R.string.ui_label_edit_information),
                    textColor = OnDarkTitle,
                    iconRes = R.drawable.info24dp,
                    onClick = onClickForEdit
                )

                OperationListItem(
                    text = stringResource(R.string.ui_label_cancel),
                    textColor = OnDarkDetailLabel,
                    iconRes = R.drawable.close24dp,
                    onClick = onClickForCancel
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )

            Text(
                text = stringResource(R.string.ui_label_press_back_to_close),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8C8A96),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }
    }
}


data class ConnectionCardInfo(
    val name: String,
    val address: String,
    val shareName: String,
    val username: String = "无"
)
