package com.example.mzdkplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import com.example.mzdkplayer.ui.theme.myListItemBorder
import com.example.mzdkplayer.ui.theme.myListItemColor
import kotlin.collections.mutableSetOf


@Composable
fun HomeScreen() {
    var selectPanel by remember { mutableStateOf("local") }
    val items by remember { mutableStateOf(listOf("local", "smb", "ftp", "项目4")) }
    val iconList = listOf<Int>(R.drawable.localfile,R.drawable.smb,R.drawable.ftp,R.drawable.dolby_vision_seeklogo)
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(top = 3.dp)
        ) {
            itemsIndexed(items) { index,item ->
                ListItem(
                    selected = selectPanel == item,
                    onClick = { selectPanel = item },
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


        Text(
            text = "HomeScreen",
            color = Color.White,
            fontSize = 30.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
