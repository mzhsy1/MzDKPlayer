package org.mz.mzdkplayer.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.local.AudioCacheEntity
import org.mz.mzdkplayer.data.local.AudioDao
import org.mz.mzdkplayer.data.model.AudioInfo
import org.mz.mzdkplayer.tool.AudioNameParser

class AudioViewModel(
    private val audioDao: AudioDao
) : ViewModel() {

    private val _allAudio = MutableStateFlow<List<AudioCacheEntity>>(emptyList())
    val allAudio: StateFlow<List<AudioCacheEntity>> = _allAudio.asStateFlow()

    // 扫描状态
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    init {
        // 监听数据库变化，实时更新 UI
        viewModelScope.launch {
            audioDao.getAllAudio().collect {
                _allAudio.value = it
            }
        }
    }

    /**
     * 纯文件名解析入库 (极速模式)
     * 不需要 InputStream，不需要网络请求
     */
    fun batchScrapeAudioInfo(
        audioList: List<Pair<String, String>>, // fileName, audioUri
        dataSourceType: String,
        connectionName: String
    ) {
        if (_isScanning.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            Log.d("AudioViewModel", "开始快速扫描音频，数量: ${audioList.size}")

            try {
                // 批量处理
                val newEntities = audioList.map { (fileName, audioUri) ->
                    // 1. 查重 (如果数据量极大，建议一次性查出所有存在的 URI 做内存比对，这里简单起见逐个查)
                    // 为了性能，如果确定是覆盖更新，也可以跳过查重直接 replace
//                    if (audioDao.getAudioByUri(audioUri) != null) {
//                        return@mapNotNull null
//                    }

                    // 2. 解析文件名
                    val metadata = AudioNameParser.parse(fileName)

                    // 3. 构建实体
                    AudioCacheEntity(
                        audioUri = audioUri,
                        dataSourceType = dataSourceType,
                        fileName = fileName,
                        connectionName = connectionName,
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        duration = 0 // 暂无时长
                    )
                }

                // 直接调用批量插入，IGNORE 策略会自动跳过已存在的记录
                audioDao.insertAll(newEntities)

                Log.d("AudioViewModel", "扫描完成，新增入库: ${newEntities.size} 首")
                _isScanning.value = false
            } catch (e: Exception) {
                Log.e("AudioViewModel", "扫描出错", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * 确保这个播放地址在 `audio_cache` 里有一条记录，没有就按文件名解析后补一条。
     *
     * 手机端点击音频文件时先调它：`updateAudioInfo` 走的是 `UPDATE ... WHERE audioUri = :uri`，
     * 库里没有这一行的话元数据与封面会**静默丢掉**。是 `suspend` 而不是丢进 `viewModelScope`，
     * 就是为了让「先补行、再回写」的顺序有保证（`insertAll` 用的是 IGNORE，重复调用不会覆盖已有元数据）。
     */
    suspend fun ensureAudioCache(
        fileName: String,
        audioUri: String,
        dataSourceType: String,
        connectionName: String,
    ) {
        if (audioUri.isBlank() || audioDao.getAudioByUri(audioUri) != null) return
        val metadata = AudioNameParser.parse(fileName)
        audioDao.insertAudio(
            AudioCacheEntity(
                audioUri = audioUri,
                dataSourceType = dataSourceType,
                fileName = fileName,
                connectionName = connectionName,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                duration = 0 // 暂无时长
            )
        )
    }

    /**
     * 回写音频元数据到数据库
     */
    fun updateAudioInfo(
        uri: String,
        info: AudioInfo,
        localCoverPath: String?,
        duration: Long,
        isDetailsLoaded: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 防止无效更新
            if (uri.isBlank()) return@launch

            audioDao.updateAudioMetadata(
                uri = uri,
                title = info.title ?: "未知标题", // 没解析出来保持原样或默认值，根据需求调整
                artist = info.artist ?: "未知艺术家",
                album = info.album ?: "未知专辑",
                duration = duration,
                lyrics = info.lyrics,
                localCoverPath = localCoverPath,
                isDetailsLoaded,
                bit = info.bit,
                sampleRate = info.sampleRate,
                bitsPerSample = info.bitsPerSample,
            )
            Log.d("AudioViewModel", "数据库已更新元数据: $uri")
        }
    }

    suspend fun getAudioCacheByUri(uri: String): AudioCacheEntity? {
        return audioDao.getAudioByUri(uri)
    }

    fun clearLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            audioDao.clearAllAudio()
        }
    }
}