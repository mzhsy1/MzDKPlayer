package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.HTTPLinkConnection
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.data.model.SMBConnection
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.tool.logic.FileBrowserLogic
import org.mz.mzdkplayer.tool.logic.PhoneFileBrowserLogic.httpInitialDirectory
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.ui.phone.SMB_ROOT_PATH
import org.mz.mzdkplayer.viewmodel.FTPListViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkListViewModel
import org.mz.mzdkplayer.viewmodel.NFSListViewModel
import org.mz.mzdkplayer.viewmodel.SMBListViewModel
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel
import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol

/** 各协议连接在列表里共用的展示形态；[initialPath] 是点进去时第一个要加载的目录/地址 */
private data class PhoneConnectionItem(
    val id: String,
    val title: String,
    val detail: String,
    val initialPath: String,
)

/** FTP 连接没填端口时的兜底值，与新增对话框的默认值一致 */
private const val FTP_FALLBACK_PORT = 21

/** NFS 的挂载根：浏览页里 "" 表示挂载根 */
private const val NFS_MOUNT_ROOT = "/"

/**
 * 文件管理页（第三阶段：本地 / SMB / FTP / NFS / WebDAV / HTTP 全覆盖）。
 *
 * 顶部一排协议标签，下面是该协议的连接列表（本机没有连接概念，直接给「内部存储」入口）。
 * 连接的增删复用电视端各协议的 `XxxListViewModel`（底层都是 SharedPreferences + Gson）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneFilesScreen(
    smbListViewModel: SMBListViewModel,
    ftpListViewModel: FTPListViewModel,
    nfsListViewModel: NFSListViewModel,
    webDavListViewModel: WebDavListViewModel,
    httpLinkListViewModel: HTTPLinkListViewModel,
    onOpen: (protocol: PhoneFileProtocol, connectionId: String, path: String) -> Unit,
) {
    val smbConnections by smbListViewModel.connections.collectAsState()
    val ftpConnections by ftpListViewModel.connections.collectAsState()
    val nfsConnections by nfsListViewModel.connections.collectAsState()
    val webDavConnections by webDavListViewModel.connections.collectAsState()
    val httpConnections by httpLinkListViewModel.connections.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var protocol by rememberSaveable { mutableStateOf(PhoneFileProtocol.SMB) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<PhoneFileProtocol, PhoneConnectionItem>?>(null) }
    val duplicateMessage = stringResource(R.string.phone_files_duplicate)

    val notifyDuplicate: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(duplicateMessage) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.ui_label_file_browsing)) },
                subtitle = { Text(protocol.displayLabel()) },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (protocol.supportsConnections) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.ui_label_add_connection),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = protocol.ordinal,
                edgePadding = 16.dp,
            ) {
                PhoneFileProtocol.entries.forEach { item ->
                    Tab(
                        selected = item == protocol,
                        onClick = { protocol = item },
                        text = { Text(item.displayLabel()) },
                    )
                }
            }

            val emptyTitle = stringResource(R.string.phone_files_empty_title, protocol.displayLabel())
            val emptySub = stringResource(R.string.phone_files_empty_sub)

            when (protocol) {
                PhoneFileProtocol.LOCAL -> LocalStorageEntry(
                    onClick = {
                        onOpen(PhoneFileProtocol.LOCAL, "", localBrowserRoot())
                    }
                )

                PhoneFileProtocol.SMB -> ConnectionSection(
                    connections = smbConnections.map { it.toItem() },
                    emptyTitle = emptyTitle,
                    emptySub = emptySub,
                    onOpen = { onOpen(PhoneFileProtocol.SMB, it.id, it.initialPath) },
                    onDelete = { pendingDelete = PhoneFileProtocol.SMB to it },
                )

                PhoneFileProtocol.FTP -> ConnectionSection(
                    connections = ftpConnections.map { it.toItem() },
                    emptyTitle = emptyTitle,
                    emptySub = emptySub,
                    onOpen = { onOpen(PhoneFileProtocol.FTP, it.id, it.initialPath) },
                    onDelete = { pendingDelete = PhoneFileProtocol.FTP to it },
                )

                PhoneFileProtocol.NFS -> ConnectionSection(
                    connections = nfsConnections.map { it.toItem() },
                    emptyTitle = emptyTitle,
                    emptySub = emptySub,
                    onOpen = { onOpen(PhoneFileProtocol.NFS, it.id, it.initialPath) },
                    onDelete = { pendingDelete = PhoneFileProtocol.NFS to it },
                )

                PhoneFileProtocol.WEBDAV -> ConnectionSection(
                    connections = webDavConnections.map { it.toItem() },
                    emptyTitle = emptyTitle,
                    emptySub = emptySub,
                    onOpen = { onOpen(PhoneFileProtocol.WEBDAV, it.id, it.initialPath) },
                    onDelete = { pendingDelete = PhoneFileProtocol.WEBDAV to it },
                )

                PhoneFileProtocol.HTTP -> ConnectionSection(
                    connections = httpConnections.map { it.toItem() },
                    emptyTitle = emptyTitle,
                    emptySub = emptySub,
                    onOpen = { onOpen(PhoneFileProtocol.HTTP, it.id, it.initialPath) },
                    onDelete = { pendingDelete = PhoneFileProtocol.HTTP to it },
                )
            }
        }
    }

    if (showAddDialog && protocol.supportsConnections) {
        val dismiss = { showAddDialog = false }
        when (protocol) {
            PhoneFileProtocol.LOCAL -> Unit

            PhoneFileProtocol.SMB -> AddSmbConnectionDialog(dismiss) { connection ->
                dismiss()
                if (!smbListViewModel.addConnection(connection)) notifyDuplicate()
            }

            PhoneFileProtocol.FTP -> AddFtpConnectionDialog(dismiss) { connection ->
                dismiss()
                if (!ftpListViewModel.addConnection(connection)) notifyDuplicate()
            }

            PhoneFileProtocol.NFS -> AddNfsConnectionDialog(dismiss) { connection ->
                dismiss()
                if (!nfsListViewModel.addConnection(connection)) notifyDuplicate()
            }

            PhoneFileProtocol.WEBDAV -> AddWebDavConnectionDialog(dismiss) { connection ->
                dismiss()
                if (!webDavListViewModel.addConnection(connection)) notifyDuplicate()
            }

            PhoneFileProtocol.HTTP -> AddHttpConnectionDialog(dismiss) { connection ->
                dismiss()
                if (!httpLinkListViewModel.addConnection(connection)) notifyDuplicate()
            }
        }
    }

    pendingDelete?.let { (targetProtocol, item) ->
        DeleteConnectionDialog(
            connectionName = item.title,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                when (targetProtocol) {
                    PhoneFileProtocol.LOCAL -> Unit
                    PhoneFileProtocol.SMB -> smbListViewModel.deleteConnection(item.id)
                    PhoneFileProtocol.FTP -> ftpListViewModel.deleteConnection(item.id)
                    PhoneFileProtocol.NFS -> nfsListViewModel.deleteConnection(item.id)
                    PhoneFileProtocol.WEBDAV -> webDavListViewModel.deleteConnection(item.id)
                    PhoneFileProtocol.HTTP -> httpLinkListViewModel.deleteConnection(item.id)
                }
            },
        )
    }
}

/**
 * 协议标签：只有「本机」需要翻译，其余是协议名（与电视端 `FileHomeScreen` 口径一致）。
 *
 * internal 而不 private：手机端设置页的「刮削来源」开关也用同一套标签。
 */
@Composable
internal fun PhoneFileProtocol.displayLabel(): String =
    if (this == PhoneFileProtocol.LOCAL) {
        stringResource(R.string.ui_label_local_files)
    } else {
        routeValue
    }

/** 「内部存储」入口：本机没有连接的概念，直接进浏览器 */
@Composable
private fun LocalStorageEntry(onClick: () -> Unit) {
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        FilesEntryCard(
            title = stringResource(R.string.phone_files_local_entry_title),
            detail = stringResource(R.string.phone_files_local_entry_sub),
            // 主入口用 primary 色块，是本页唯一的高饱和分区
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            detailColor = MaterialTheme.colorScheme.onPrimaryContainer,
            iconContainer = MaterialTheme.colorScheme.primary,
            iconContent = MaterialTheme.colorScheme.onPrimary,
            onClick = onClick,
        )
    }
}

/**
 * 文件页入口卡片：Expressive 风格的大圆角色块。
 *
 * 层级靠 surface 容器色而非阴影（原来的 `ElevatedCard` 用阴影堆层级，与 Expressive 口径不一致），
 * 卡片用 `extraLarge` 圆角、内部图标块用 `large`，形成「大容器大圆角 / 小元素小圆角」的形状对比。
 */
@Composable
private fun FilesEntryCard(
    title: String,
    detail: String,
    containerColor: Color,
    contentColor: Color,
    detailColor: Color,
    iconContainer: Color,
    iconContent: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PhoneIcons.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = iconContent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = detailColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
        }
    }
}

@Composable
private fun ConnectionSection(
    connections: List<PhoneConnectionItem>,
    emptyTitle: String,
    emptySub: String,
    onOpen: (PhoneConnectionItem) -> Unit,
    onDelete: (PhoneConnectionItem) -> Unit,
) {
    if (connections.isEmpty()) {
        EmptyConnections(title = emptyTitle, subtitle = emptySub)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 下标参与 key：历史连接数据里 id 可能为空串，单用 id 会撞 key 直接崩
        itemsIndexed(connections, key = { index, item -> "$index-${item.id}" }) { _, connection ->
            ConnectionCard(
                connection = connection,
                onClick = { onOpen(connection) },
                onDelete = { onDelete(connection) },
            )
        }
    }
}

@Composable
private fun EmptyConnections(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 装饰圆：空状态用一个大色块承载图标，比裸图标更有分量
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PhoneIcons.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: PhoneConnectionItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    FilesEntryCard(
        title = connection.title,
        detail = connection.detail,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        detailColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContainer = MaterialTheme.colorScheme.secondaryContainer,
        iconContent = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = onClick,
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.ui_label_delete_connection),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

// ---- 各协议连接 → 列表展示形态（detail 用「·」拼可读字段，与第一阶段 SMB 卡片一致）----

private fun SMBConnection.toItem(): PhoneConnectionItem = PhoneConnectionItem(
    id = id.orEmpty(),
    title = name.orEmpty().ifBlank { ip.orEmpty() },
    detail = listOfNotNull(
        ip?.takeIf { it.isNotBlank() },
        shareName?.takeIf { it.isNotBlank() },
    ).joinToString(" · "),
    initialPath = SMB_ROOT_PATH,
)

private fun FTPConnection.toItem(): PhoneConnectionItem = PhoneConnectionItem(
    id = id.orEmpty(),
    title = name.orEmpty().ifBlank { ip.orEmpty() },
    detail = listOfNotNull(
        ip?.takeIf { it.isNotBlank() }?.let { host -> host + ":${port ?: FTP_FALLBACK_PORT}" },
        shareName?.takeIf { it.isNotBlank() },
    ).joinToString(" · "),
    // 起始目录取连接里配置的那一个（空串表示共享根目录，浏览页按显示路径处理）
    initialPath = FileBrowserLogic.ftpDisplayPath(
        FileBrowserLogic.normalizeFtpDirectory(shareName.orEmpty())
    ),
)

private fun NFSConnection.toItem(): PhoneConnectionItem = PhoneConnectionItem(
    id = id.orEmpty(),
    title = name.orEmpty().ifBlank { serverAddress.orEmpty() },
    detail = listOfNotNull(
        serverAddress?.takeIf { it.isNotBlank() },
        shareName?.takeIf { it.isNotBlank() },
    ).joinToString(" · "),
    initialPath = NFS_MOUNT_ROOT,
)

private fun WebDavConnection.toItem(): PhoneConnectionItem = PhoneConnectionItem(
    id = id.orEmpty(),
    title = name.orEmpty().ifBlank { baseUrl.orEmpty() },
    detail = baseUrl.orEmpty(),
    initialPath = FileBrowserLogic.ensureTrailingSlash(baseUrl.orEmpty()),
)

private fun HTTPLinkConnection.toItem(): PhoneConnectionItem = PhoneConnectionItem(
    id = id.orEmpty(),
    title = name.orEmpty().ifBlank { serverAddress.orEmpty() },
    detail = httpInitialDirectory(serverAddress, shareName),
    initialPath = httpInitialDirectory(serverAddress, shareName),
)
