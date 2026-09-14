package org.mz.mzdkplayer.ui.screen.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaDao

/**
 * 媒体元信息（刮削结果）只读查询。
 *
 * 播放界面用它把「文件名」替换成刮削后的标题，
 * 只读 media_cache 表：不联网刮削，也不写库。
 */
class MediaMetaViewModel(private val mediaDao: MediaDao) : ViewModel() {

    private val _mediaMeta = MutableStateFlow<MediaCacheEntity?>(null)
    val mediaMeta: StateFlow<MediaCacheEntity?> = _mediaMeta.asStateFlow()

    private var loadJob: Job? = null

    /**
     * 按播放地址查询刮削信息。
     * 查不到（未刮削过）时置空，调用方回退到文件名。
     */
    fun load(videoUri: String) {
        loadJob?.cancel()
        _mediaMeta.value = null
        if (videoUri.isBlank()) return

        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _mediaMeta.value = runCatching { mediaDao.getMediaByUri(videoUri) }.getOrNull()
        }
    }
}
