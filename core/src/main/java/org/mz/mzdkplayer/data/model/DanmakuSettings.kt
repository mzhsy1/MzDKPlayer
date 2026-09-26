package org.mz.mzdkplayer.data.model

data class DanmakuSettings(
    val isSwitchEnabled: Boolean,
    val selectedRatio: String,
    val fontSize: Int,
    val transparency: Int,
    val selectedTypes: Set<String>
)