package org.mz.mzdkplayer.ui.screen.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AudioPlayerViewModel:ViewModel() {
    var atpFocus by mutableStateOf(false)
    var atpVisibility by mutableStateOf(false)

    var selectedAorVorS by mutableStateOf("L")

    var selectedAtIndex by mutableStateOf(0)
}