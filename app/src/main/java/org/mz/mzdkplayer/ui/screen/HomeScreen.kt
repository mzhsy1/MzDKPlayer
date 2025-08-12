package org.mz.mzdkplayer.ui.screen


import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

import org.mz.mzdkplayer.ui.theme.FilePermissionScreen

import org.mz.mzdkplayer.ui.theme.myListItemBorder
import org.mz.mzdkplayer.ui.theme.myListItemColor
import java.net.URLEncoder


@Composable
fun HomeScreen(mainNavController: NavHostController) {
    var selectPanel by remember { mutableStateOf("local") }
    val items by remember { mutableStateOf(listOf("local", "smb", "ftp", "项目4")) }
    val iconList = listOf<Int>(
        R.drawable.localfile,
        R.drawable.smb,
        R.drawable.ftp,
        R.drawable.dolby_vision_seeklogo
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(10.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    ListItem(
                        selected = selectPanel == item,
                        onClick = {
                            val primaryStoragePath = Environment.getExternalStorageDirectory().absolutePath
                            selectPanel = item;when (item) {
                            "local" -> mainNavController.navigate(
                                "FilePage/${URLEncoder.encode(primaryStoragePath,"UTF-8")}")
                            "smb" -> mainNavController.navigate("SMBListScreen")
                        };
                        },
                        modifier = Modifier.padding(top = 2.dp),
                        colors = myListItemColor(),
                        border = myListItemBorder(),
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = iconList[index]),
                                contentDescription = item
                            )
                        },
                        headlineContent = { Text(item) },
                        trailingContent = {

                        }
                    )
                }

            }
            FilePermissionScreen()

        }
    }

}






