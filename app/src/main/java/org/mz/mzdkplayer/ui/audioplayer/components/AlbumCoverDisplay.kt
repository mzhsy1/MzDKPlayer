package org.mz.mzdkplayer.ui.audioplayer.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R

@Composable
fun AlbumCoverDisplay(coverArt: Bitmap?) {
    if (coverArt != null) {
        Image(
            bitmap = coverArt.asImageBitmap(),
            contentDescription = "专辑封面",
            modifier = Modifier
                .heightIn(240.dp,240.dp).widthIn(240.dp,240.dp), // 保持封面大小
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.album),
            contentDescription = "默认专辑封面",
            modifier = Modifier
                .heightIn(240.dp,240.dp).widthIn(240.dp,240.dp), // 保持封面大小
            contentScale = ContentScale.Crop
        )
    }
}