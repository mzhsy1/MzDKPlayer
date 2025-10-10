package org.mz.mzdkplayer.ui.screen.vm

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.media3.common.Tracks


class VideoPlayerViewModel:ViewModel() {
    private val getUpVideoDataLiveData = MutableLiveData<Map<String,String>>()



    fun getUpVideoDetailsData(map: Map<String, String>){
        getUpVideoDataLiveData.value = map
    }
    val mutableSetOfAudioTrackGroups = mutableListOf<Tracks.Group>()
    val mutableSetOfVideoTrackGroups = mutableListOf<Tracks.Group>()

    val mutableSetOfTextTrackGroups = mutableListOf<Tracks.Group>()
    var selectedAtIndex by  mutableIntStateOf(0)
    var selectedVtIndex by mutableIntStateOf(0)

    var selectedStIndex by mutableIntStateOf(0)
    var onTracksChangedState by  mutableIntStateOf(0)

    var isSubtitleViewVis by mutableIntStateOf(View.VISIBLE)

    var isCusSubtitleViewVis by mutableStateOf(false)

    var isSubtitlePanelVis by mutableStateOf("S")

    var selectedAorVorS by mutableStateOf("A")

    var atpVisibility by mutableStateOf(false)
    var atpFocus by mutableStateOf(false)

    var danmakuVisibility by mutableStateOf(true)
    fun updateSubtitleVisibility(visible: Int) {
        isSubtitleViewVis = visible
    }
    fun updateCusSubtitleVisibility(visible: Boolean) {
        isCusSubtitleViewVis = visible
    }
    var textSize = 1.0f
}