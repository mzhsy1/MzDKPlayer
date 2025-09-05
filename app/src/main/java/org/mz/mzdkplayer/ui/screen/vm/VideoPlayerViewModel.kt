package org.mz.mzdkplayer.ui.screen.vm

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
    var atpVisibility = false


    fun getUpVideoDetailsData(map: Map<String, String>){
        getUpVideoDataLiveData.value = map
    }
    val mutableSetOfAudioTrackGroups = mutableListOf<Tracks.Group>()
    val mutableSetOfVideoTrackGroups = mutableListOf<Tracks.Group>()
    var selectedAtIndex by  mutableIntStateOf(0)
    var selectedVtIndex by mutableIntStateOf(0)
    var onTracksChangedState by  mutableIntStateOf(0)

    var textSize = 1.0f
}