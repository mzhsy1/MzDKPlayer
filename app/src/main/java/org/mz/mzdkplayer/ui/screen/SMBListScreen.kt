package org.mz.mzdkplayer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.logic.model.SMBConnection
import org.mz.mzdkplayer.ui.screen.vm.SMBListViewModel

/**
 * SMB列表
 */
@Composable
@Preview
fun SMBListScreen() {
    val smbListViewModel: SMBListViewModel = viewModel()
    val connections by smbListViewModel.connections.collectAsState()
    Column(modifier = Modifier.padding(20.dp)) {
        // 添加新连接按钮
        Button(
            onClick = {},
            modifier = Modifier.padding(top = 10.dp, start = 8.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            shape = ButtonDefaults.shape(shape = ShapeDefaults.ExtraSmall),
            colors = ButtonDefaults.colors(
                Color.White,
                Color.Black,
                Color.White,
                Color.Black
            )

        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "添加新连接",
                style = MaterialTheme.typography.titleSmall
            )
        }
        if (connections.isEmpty()) {
            Text("no links", color = Color.White, fontSize = 20.sp)
        } else {
            // 连接卡片列表
            LazyColumn {
                itemsIndexed(connections) { index, conn ->
                    ConnectionCard(
                        connection = conn,
                        onClick = {
                            smbListViewModel.selectConnection(conn)
                            //onNavigateToFiles(conn)
                        },
                        onDelete = { smbListViewModel.deleteConnection(conn.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    connection: SMBConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(connection.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "删除")
                }
            }
            Text("IP: ${connection.ip}")
            Text("共享目录: ${connection.shareName}")
        }
    }
}