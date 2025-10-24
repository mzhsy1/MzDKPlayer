package org.mz.mzdkplayer.logic.model

data class DanmakuSettings(
    val isSwitchEnabled: Boolean,
    val selectedRatio: String,
    val fontSize: Int,
    val transparency: Int,
    val selectedTypes: Set<String>
)