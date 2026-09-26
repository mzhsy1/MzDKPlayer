package org.mz.mzdkplayer.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.model.AudioItem
import java.io.File

object AudioPlaylistRepository {
    private val _playlist = MutableStateFlow<List<AudioItem>>(emptyList())
    val playlist: StateFlow<List<AudioItem>> = _playlist.asStateFlow()

    private var storageFile: File? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        if (storageFile == null) {
            storageFile = File(context.filesDir, "current_audio_playlist.json")
            try {
                if (storageFile?.exists() == true) {
                    val json = storageFile?.readText()
                    val type = object : TypeToken<List<AudioItem>>() {}.type
                    val list: List<AudioItem> = gson.fromJson(json, type) ?: emptyList()
                    _playlist.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPlaylist(list: List<AudioItem>) {
        _playlist.value = list
        saveToDisk(list)
    }

    private fun saveToDisk(list: List<AudioItem>) {
        scope.launch {
            try {
                storageFile?.writeText(gson.toJson(list))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getPlaylist(): List<AudioItem> = _playlist.value
}
