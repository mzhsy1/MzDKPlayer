package org.mz.mzdkplayer.data.model

data class VideoItem(
    val uri: String,
    val fileName: String,
    val dataSourceType: String,
    val connectionName: String = ""
)
