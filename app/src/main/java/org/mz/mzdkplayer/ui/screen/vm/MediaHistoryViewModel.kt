package org.mz.mzdkplayer.ui.screen.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.model.MediaHistoryRecord
import org.mz.mzdkplayer.data.repository.MediaHistoryRepository

/**
 * 优化的通用媒体播放历史记录ViewModel，支持视频和音频
 * 使用内存缓存来提升查询性能，减少本地存储访问
 */
class MediaHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaHistoryRepository(application)

    // 原始完整历史记录（从本地存储加载）
    private val _originalHistory = MutableStateFlow<List<MediaHistoryRecord>>(emptyList())

    // 过滤后的历史记录（用于UI显示）
    private val _history = MutableStateFlow<List<MediaHistoryRecord>>(emptyList())
    private val _isHistoryPanelShow = MutableStateFlow<Boolean>(false)

    val history: StateFlow<List<MediaHistoryRecord>> = _history.asStateFlow()
    val isHistoryPanelShow: StateFlow<Boolean> = _isHistoryPanelShow

    // 当前选中的历史记录
    private val _selectedHistoryRecord = MutableStateFlow<MediaHistoryRecord?>(null)
    val selectedHistoryRecord: StateFlow<MediaHistoryRecord?> = _selectedHistoryRecord

    // 查询过滤条件
    private var currentFilter: (MediaHistoryRecord) -> Boolean = { true }
    private var currentSearchTerm: String = ""
    private var currentProtocolFilter: String = ""
    private var currentConnectionFilter: String = ""
    private var currentMediaTypeFilter: String = ""
    private var currentServerAddressFilter: String = ""
    private var currentLimit: Int = -1 // -1表示不限制

    init {
        loadHistory()
    }

    /**
     * 加载原始播放历史（从本地存储一次性加载）
     */
    private fun loadHistory() {
        viewModelScope.launch {
            val original = repository.getHistory()
            _originalHistory.value = original
            // 应用当前过滤器
            applyCurrentFilter()
        }
    }

    /**
     * 应用当前的过滤条件
     */
    private fun applyCurrentFilter() {
        var filtered = _originalHistory.value.filter(currentFilter)

        // 应用限制数量
        if (currentLimit > 0) {
            filtered = filtered.take(currentLimit)
        }

        _history.value = filtered
    }

    /**
     * 获取指定mediaUri的播放位置
     * @param mediaUri 媒体URI
     * @return 播放位置（毫秒），如果找不到记录则返回0
     */
    fun getPlaybackPositionByUri(mediaUri: String): Long {
        return _originalHistory.value
            .find { it.mediaUri == mediaUri }
            ?.playbackPosition ?: 0
    }

    /**
     * 获取指定mediaUri的播放记录
     * @param mediaUri 媒体URI
     * @return MediaHistoryRecord对象，如果找不到则返回null
     */
    fun getHistoryRecordByUri(mediaUri: String): MediaHistoryRecord? {
        return _originalHistory.value.find { it.mediaUri == mediaUri }
    }

    /**
     * 保存媒体播放历史记录
     */
    fun saveHistory(
        mediaUri: String,
        fileName: String,
        playbackPosition: Long,
        mediaDuration: Long = 0,
        protocolName: String,
        connectionName: String,
        serverAddress: String,
        mediaType: String = "VIDEO"
    ) {
        viewModelScope.launch {
            repository.saveHistory(
                mediaUri = mediaUri,
                fileName = fileName,
                playbackPosition = playbackPosition,
                mediaDuration = mediaDuration,
                protocolName = protocolName,
                connectionName = connectionName,
                serverAddress = serverAddress,
                mediaType = mediaType
            )
            // 重新加载原始历史记录并应用过滤器
            val original = repository.getHistory()
            _originalHistory.value = original
            applyCurrentFilter()
        }
    }

    /**
     * 保存视频播放历史记录
     */
    fun saveVideoHistory(
        videoUri: String,
        fileName: String,
        playbackPosition: Long,
        videoDuration: Long = 0,
        protocolName: String,
        connectionName: String,
        serverAddress: String
    ) {
        viewModelScope.launch {
            repository.saveVideoHistory(
                videoUri = videoUri,
                fileName = fileName,
                playbackPosition = playbackPosition,
                videoDuration = videoDuration,
                protocolName = protocolName,
                connectionName = connectionName,
                serverAddress = serverAddress
            )
            // 重新加载原始历史记录并应用过滤器
            val original = repository.getHistory()
            _originalHistory.value = original
            applyCurrentFilter()
        }
    }

    /**
     * 保存音频播放历史记录
     */
    fun saveAudioHistory(
        audioUri: String,
        fileName: String,
        playbackPosition: Long,
        audioDuration: Long = 0,
        protocolName: String,
        connectionName: String,
        serverAddress: String
    ) {
        viewModelScope.launch {
            repository.saveAudioHistory(
                audioUri = audioUri,
                fileName = fileName,
                playbackPosition = playbackPosition,
                audioDuration = audioDuration,
                protocolName = protocolName,
                connectionName = connectionName,
                serverAddress = serverAddress
            )
            // 重新加载原始历史记录并应用过滤器
            val original = repository.getHistory()
            _originalHistory.value = original
            applyCurrentFilter()
        }
    }

    /**
     * 保存播放历史记录对象
     */
    fun saveHistory(record: MediaHistoryRecord) {
        viewModelScope.launch {
            repository.saveHistory(record)
            // 重新加载原始历史记录并应用过滤器
            val original = repository.getHistory()
            _originalHistory.value = original
            applyCurrentFilter()
        }
    }

    /**
     * 获取最新的N条播放历史记录
     */
    fun getRecentHistory(limit: Int = 20) {
        currentLimit = limit
        currentFilter = { true }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentMediaTypeFilter = ""
        currentServerAddressFilter = ""

        val filtered = _originalHistory.value.take(limit)
        _history.value = filtered
    }

    /**
     * 根据文件名搜索播放历史记录（使用内存缓存）
     */
    fun searchHistoryByFileName(fileName: String) {
        currentSearchTerm = fileName
        currentFilter = { it.fileName.contains(fileName, ignoreCase = true) }
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentMediaTypeFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 根据协议名称搜索播放历史记录（使用内存缓存）
     */
    fun searchHistoryByProtocol(protocolName: String) {
        currentProtocolFilter = protocolName
        currentFilter = { it.protocolName.equals(protocolName, ignoreCase = true) }
        currentSearchTerm = ""
        currentConnectionFilter = ""
        currentMediaTypeFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 根据连接名称搜索播放历史记录（使用内存缓存）
     */
    fun searchHistoryByConnection(connectionName: String) {
        currentConnectionFilter = connectionName
        currentFilter = { it.connectionName.equals(connectionName, ignoreCase = true) }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentMediaTypeFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 根据服务器地址搜索播放历史记录（使用内存缓存）
     */
    fun searchHistoryByServerAddress(serverAddress: String) {
        currentServerAddressFilter = serverAddress
        currentFilter = { it.serverAddress.equals(serverAddress, ignoreCase = true) }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentMediaTypeFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 根据媒体类型搜索播放历史记录（使用内存缓存）
     */
    fun searchHistoryByMediaType(mediaType: String) {
        currentMediaTypeFilter = mediaType
        currentFilter = { it.mediaType.equals(mediaType, ignoreCase = true) }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 获取视频播放历史记录（使用内存缓存）
     */
    fun getVideoHistory() {
        currentMediaTypeFilter = "VIDEO"
        currentFilter = { it.isVideo() }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 获取音频播放历史记录（使用内存缓存）
     */
    fun getAudioHistory() {
        currentMediaTypeFilter = "AUDIO"
        currentFilter = { it.isAudio() }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }

    /**
     * 按连接/协议分组获取历史记录（使用内存缓存）
     */
    fun getGroupedHistory(limitPerGroup: Int = 10) {
        currentLimit = -1
        currentFilter = { true }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentMediaTypeFilter = ""
        currentServerAddressFilter = ""

        val groupedHistory = _originalHistory.value
            .groupBy { it.connectionName.ifEmpty { it.protocolName } }
            .mapValues { entry -> entry.value.take(limitPerGroup) }

        val flattenedList = groupedHistory.values.flatten()
        _history.value = flattenedList
    }

    /**
     * 根据协议类型分组获取历史记录（使用内存缓存）
     */
    fun getHistoryByProtocolGroups(): Map<String, List<MediaHistoryRecord>> {
        return _originalHistory.value.groupBy { it.protocolName }
    }

    /**
     * 根据媒体类型分组获取历史记录（使用内存缓存）
     */
    fun getHistoryByMediaTypeGroups(): Map<String, List<MediaHistoryRecord>> {
        return _originalHistory.value.groupBy { it.mediaType }
    }

    /**
     * 获取指定URI的播放记录（使用内存缓存）
     */
    fun getHistoryByUri(mediaUri: String): MediaHistoryRecord? {
        return _originalHistory.value.find { it.mediaUri == mediaUri }
    }

    /**
     * 获取最近播放的媒体记录（使用内存缓存）
     */
    fun getMostRecentHistory(): MediaHistoryRecord? {
        return _originalHistory.value.firstOrNull()
    }

    /**
     * 获取最近播放的视频记录（使用内存缓存）
     */
    fun getMostRecentVideoHistory(): MediaHistoryRecord? {
        return _originalHistory.value.firstOrNull { it.isVideo() }
    }

    /**
     * 获取最近播放的音频记录（使用内存缓存）
     */
    fun getMostRecentAudioHistory(): MediaHistoryRecord? {
        return _originalHistory.value.firstOrNull { it.isAudio() }
    }

    /**
     * 获取指定连接的最近播放记录（使用内存缓存）
     */
    fun getMostRecentHistoryByConnection(connectionName: String): MediaHistoryRecord? {
        return _originalHistory.value.firstOrNull { it.connectionName == connectionName }
    }

    /**
     * 获取指定协议的最近播放记录（使用内存缓存）
     */
    fun getMostRecentHistoryByProtocol(protocolName: String): MediaHistoryRecord? {
        return _originalHistory.value.firstOrNull { it.protocolName == protocolName }
    }

    /**
     * 删除指定的播放历史记录
     */
    fun deleteHistoryByUri(mediaUri: String) {
        viewModelScope.launch {
            repository.deleteHistoryByUri(mediaUri)
            // 从内存缓存中也删除
            val updatedOriginal = _originalHistory.value.filter { it.mediaUri != mediaUri }
            _originalHistory.value = updatedOriginal

            // 重新应用当前过滤器
            applyCurrentFilter()
        }
    }

    /**
     * 删除指定的播放历史记录
     */
    fun deleteHistory(record: MediaHistoryRecord) {
        deleteHistoryByUri(record.mediaUri)
    }

    /**
     * 清除所有播放历史记录
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _originalHistory.value = emptyList()
            _history.value = emptyList()
        }
    }

    /**
     * 清除特定协议的历史记录
     */
    fun clearHistoryByProtocol(protocolName: String) {
        viewModelScope.launch {
            repository.clearHistoryByProtocol(protocolName)
            // 从内存缓存中也清除
            val updatedOriginal = _originalHistory.value.filter { it.protocolName != protocolName }
            _originalHistory.value = updatedOriginal

            // 重新应用当前过滤器
            applyCurrentFilter()
        }
    }

    /**
     * 清除特定连接的历史记录
     */
    fun clearHistoryByConnection(connectionName: String) {
        viewModelScope.launch {
            repository.clearHistoryByConnection(connectionName)
            // 从内存缓存中也清除
            val updatedOriginal = _originalHistory.value.filter { it.connectionName != connectionName }
            _originalHistory.value = updatedOriginal

            // 重新应用当前过滤器
            applyCurrentFilter()
        }
    }

    /**
     * 清除特定媒体类型的历史记录
     */
    fun clearHistoryByMediaType(mediaType: String) {
        viewModelScope.launch {
            repository.clearHistoryByMediaType(mediaType)
            // 从内存缓存中也清除
            val updatedOriginal = _originalHistory.value.filter { !it.mediaType.equals(mediaType, ignoreCase = true) }
            _originalHistory.value = updatedOriginal

            // 重新应用当前过滤器
            applyCurrentFilter()
        }
    }

    /**
     * 选择播放历史记录
     */
    fun selectHistoryRecord(record: MediaHistoryRecord?) {
        _selectedHistoryRecord.value = record
    }

    /**
     * 显示历史记录面板
     */
    fun showHistoryPanel() {
        _isHistoryPanelShow.value = true
    }

    /**
     * 隐藏历史记录面板
     */
    fun hideHistoryPanel() {
        _isHistoryPanelShow.value = false
    }

    /**
     * 获取播放历史记录总数
     */
    fun getHistoryCount(): Int {
        return _originalHistory.value.size
    }

    /**
     * 检查是否已存在相同的播放记录（基于媒体URI）（使用内存缓存）
     */
    fun containsHistory(mediaUri: String): Boolean {
        return _originalHistory.value.any { it.mediaUri == mediaUri }
    }

    /**
     * 重置为显示所有历史记录
     */
    fun resetToAllHistory() {
        currentFilter = { true }
        currentSearchTerm = ""
        currentProtocolFilter = ""
        currentConnectionFilter = ""
        currentMediaTypeFilter = ""
        currentServerAddressFilter = ""
        currentLimit = -1

        _history.value = _originalHistory.value
    }

    /**
     * 检查是否有播放历史记录
     */
    fun hasHistory(): Boolean {
        return getHistoryCount() > 0
    }

    /**
     * 获取原始历史记录（不经过过滤）
     */
    fun getOriginalHistory(): List<MediaHistoryRecord> {
        return _originalHistory.value
    }

    /**
     * 复合过滤：根据多个条件同时过滤
     */
    fun searchHistoryComposite(
        fileName: String = "",
        protocolName: String = "",
        connectionName: String = "",
        mediaType: String = "",
        serverAddress: String = ""
    ) {
        currentSearchTerm = fileName
        currentProtocolFilter = protocolName
        currentConnectionFilter = connectionName
        currentMediaTypeFilter = mediaType
        currentServerAddressFilter = serverAddress
        currentLimit = -1

        currentFilter = { record ->
            var match = true
            if (fileName.isNotEmpty()) {
                match = match && record.fileName.contains(fileName, ignoreCase = true)
            }
            if (protocolName.isNotEmpty()) {
                match = match && record.protocolName.equals(protocolName, ignoreCase = true)
            }
            if (connectionName.isNotEmpty()) {
                match = match && record.connectionName.equals(connectionName, ignoreCase = true)
            }
            if (mediaType.isNotEmpty()) {
                match = match && record.mediaType.equals(mediaType, ignoreCase = true)
            }
            if (serverAddress.isNotEmpty()) {
                match = match && record.serverAddress.equals(serverAddress, ignoreCase = true)
            }
            match
        }

        val filtered = _originalHistory.value.filter(currentFilter)
        _history.value = filtered
    }
}