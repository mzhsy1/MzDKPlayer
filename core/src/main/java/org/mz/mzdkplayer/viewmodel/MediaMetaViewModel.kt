package org.mz.mzdkplayer.viewmodel

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
 *
 * 手机端目录列表用 [loadMany] 一次取回整目录的刮削记录（[mediaMetaMap]），
 * 避免逐条目查询；[load] 仍是播放页那套单条口径，两者互不影响。
 */
class MediaMetaViewModel(private val mediaDao: MediaDao) : ViewModel() {

    private val _mediaMeta = MutableStateFlow<MediaCacheEntity?>(null)
    val mediaMeta: StateFlow<MediaCacheEntity?> = _mediaMeta.asStateFlow()

    private var loadJob: Job? = null

    /** 播放地址 → 刮削记录；只包含有记录的地址，查不到的条目由调用方回退到文件名 */
    private val _mediaMetaMap = MutableStateFlow<Map<String, MediaCacheEntity>>(emptyMap())
    val mediaMetaMap: StateFlow<Map<String, MediaCacheEntity>> = _mediaMetaMap.asStateFlow()

    private var loadManyJob: Job? = null

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

    /**
     * 批量查询刮削信息（手机端目录列表）。
     *
     * [videoUris] 为空时直接清空：SQLite 不认 `IN ()`，空集合不能下到 DAO。
     * 上一次未完成的查询会被取消，避免快速翻目录时旧结果覆盖新结果。
     */
    fun loadMany(videoUris: List<String>) {
        val targets = videoUris.filter { it.isNotBlank() }.distinct()
        loadManyJob?.cancel()
        if (targets.isEmpty()) {
            _mediaMetaMap.value = emptyMap()
            return
        }

        loadManyJob = viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                mediaDao.getMediaByUris(targets).associateBy { it.videoUri }
            }.getOrDefault(emptyMap())
            _mediaMetaMap.value = result
        }
    }
}
