package org.mz.mzdkplayer

import android.provider.MediaStore
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import com.example.mzdkplayer.R
import org.mz.mzdkplayer.ui.theme.myListItemBorder
import org.mz.mzdkplayer.ui.theme.myListItemColor
import java.io.File

@Composable
fun FileScreen(path: String?) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<File>() }

    LaunchedEffect(Unit) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )
        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri(path),
            projection,
            null,
            null,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                files.add(File(path))
            }
        }
    }

    LazyColumn {
        items(files) { file ->
            ListItem(
                selected = false,
                onClick = {  },

                modifier = Modifier.padding(top = 2.dp),
                colors = myListItemColor(),
                border = myListItemBorder(),
                leadingContent = {
                    if (file.isDirectory){
                        Icon(
                            painter = painterResource(id = R.drawable.localfile),
                            contentDescription = "wenj"
                        )}else{
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                            contentDescription = "wenj"
                        )
                    }
                },
                headlineContent = { Text(file.name) },
                trailingContent = {

                }
            )
        }
    }
}