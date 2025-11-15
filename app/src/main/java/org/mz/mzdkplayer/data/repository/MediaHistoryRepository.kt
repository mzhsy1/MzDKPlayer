package org.mz.mzdkplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.mz.mzdkplayer.data.model.MediaHistoryRecord
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通用媒体播放历史记录管理类
 * 存储播放记录：完整URI、文件名、播放时间、协议、连接信息等
 * 支持多种协议（SMB、NFS、本地文件、网络流等），适用于视频和音频
 * @param context Application 或 Activity Context
 */
class MediaHistoryRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("media_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val historyListType = object : TypeToken<List<MediaHistoryRecord>>() {}.type

    companion object {
        private const val KEY_HISTORY = "media_history"
        private const val MAX_HISTORY_COUNT = 500 // 最多存储500条记录
        private const val TAG = "MediaHistoryRepository"
    }



    /**
     * 保存媒体播放历史记录
     * @param record 播放记录对象
     */
    fun saveHistory(record: MediaHistoryRecord) {
        try {
            val currentHistory = getHistory()
            val updatedHistory = mutableListOf<MediaHistoryRecord>()

            // 检查是否已存在相同的URI记录，如果存在则更新播放位置而不是添加新记录
            val existingIndex = currentHistory.indexOfFirst { it.mediaUri == record.mediaUri }
            if (existingIndex >= 0) {
                // 更新现有记录的播放位置、媒体时长和时间戳
                val updatedRecord = currentHistory[existingIndex].copy(
                    playbackPosition = record.playbackPosition,
                    mediaDuration = record.mediaDuration,
                    timestamp = record.timestamp
                )
                val remainingHistory = currentHistory.filterIndexed { index, _ -> index != existingIndex }
                updatedHistory.add(updatedRecord)
                updatedHistory.addAll(remainingHistory.take(MAX_HISTORY_COUNT - 1))
            } else {
                // 添加新记录到列表开头
                updatedHistory.add(record)
                // 添加之前的记录，但限制总数不超过最大值
                updatedHistory.addAll(currentHistory.take(MAX_HISTORY_COUNT - 1))
            }

            // 保存到SharedPreferences
            prefs.edit {
                putString(KEY_HISTORY, gson.toJson(updatedHistory))
            }
            Log.d(TAG, "媒体播放记录保存成功: ${record.fileName} (${record.mediaType})")
        } catch (e: Exception) {
            Log.e(TAG, "保存媒体播放记录失败", e)
        }
    }

    /**
     * 保存媒体播放历史记录的便捷方法
     * @param mediaUri 媒体URI
     * @param fileName 文件名
     * @param playbackPosition 播放位置（毫秒）
     * @param mediaDuration 媒体总时长（毫秒）
     * @param protocolName 协议名称
     * @param connectionName 连接名称
     * @param serverAddress 服务器地址
     * @param mediaType 媒体类型（VIDEO、AUDIO等）
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
        val record = MediaHistoryRecord(
            mediaUri = mediaUri,
            fileName = fileName,
            playbackPosition = playbackPosition,
            mediaDuration = mediaDuration,
            protocolName = protocolName,
            connectionName = connectionName,
            serverAddress = serverAddress,
            mediaType = mediaType
        )
        saveHistory(record)
    }

    /**
     * 保存视频播放历史记录的便捷方法
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
        saveHistory(
            mediaUri = videoUri,
            fileName = fileName,
            playbackPosition = playbackPosition,
            mediaDuration = videoDuration,
            protocolName = protocolName,
            connectionName = connectionName,
            serverAddress = serverAddress,
            mediaType = "VIDEO"
        )
    }

    /**
     * 保存音频播放历史记录的便捷方法
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
        saveHistory(
            mediaUri = audioUri,
            fileName = fileName,
            playbackPosition = playbackPosition,
            mediaDuration = audioDuration,
            protocolName = protocolName,
            connectionName = connectionName,
            serverAddress = serverAddress,
            mediaType = "AUDIO"
        )
    }

    /**
     * 获取所有播放历史记录（按时间倒序排列，最新的在前）
     * @return 播放历史记录列表
     */
    fun getHistory(): List<MediaHistoryRecord> {
        val json = prefs.getString(KEY_HISTORY, null)
        return if (!json.isNullOrEmpty()) {
            try {
                val loadedHistory = gson.fromJson<List<MediaHistoryRecord>>(json, historyListType)

                // 确保返回的数据不为空且按时间倒序排列，同时过滤掉可能损坏的记录
                loadedHistory?.filterNotNull()?.filter { isValidRecord(it) }?.sortedByDescending { it.timestamp }
                    ?: emptyList()
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "JSON解析失败，可能是数据损坏，将清除历史记录", e)
                // 如果JSON解析失败，清除损坏的数据并返回空列表
                clearAllHistory()
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "获取播放历史失败", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 验证记录是否有效（防止因字段缺失导致崩溃）
     */
    private fun isValidRecord(record: MediaHistoryRecord): Boolean {
        return try {
            record.mediaUri.isNotBlank() &&
                    record.fileName.isNotBlank() &&
                    record.protocolName.isNotBlank() &&
                    record.playbackPosition >= 0
        } catch (e: Exception) {
            Log.w(TAG, "发现无效的播放记录", e)
            false
        }
    }

    /**
     * 获取最新的N条播放历史记录（适合TV界面显示）
     * @param limit 返回记录数量限制
     * @return 最新的播放历史记录列表
     */
    fun getRecentHistory(limit: Int = 20): List<MediaHistoryRecord> {
        return getHistory().take(limit)
    }

    /**
     * 获取最近的播放历史记录，按连接/协议分组（适合TV界面按连接分类显示）
     * @param limitPerGroup 每个分组的最大记录数
     * @return 按连接名称分组的播放历史记录映射
     */
    fun getGroupedHistory(limitPerGroup: Int = 10): Map<String, List<MediaHistoryRecord>> {
        val history = getHistory()
        return history
            .groupBy { it.connectionName.ifEmpty { it.protocolName } }
            .mapValues { entry -> entry.value.take(limitPerGroup) }
    }

    /**
     * 根据文件名查找播放历史记录
     * @param fileName 文件名
     * @return 匹配的记录列表
     */
    fun getHistoryByFileName(fileName: String): List<MediaHistoryRecord> {
        return getHistory().filter { it.fileName.contains(fileName, ignoreCase = true) }
    }

    /**
     * 根据协议名称查找播放历史记录
     * @param protocolName 协议名称（如SMB、NFS、FILE、HTTP等）
     * @return 匹配的记录列表
     */
    fun getHistoryByProtocol(protocolName: String): List<MediaHistoryRecord> {
        return getHistory().filter { it.protocolName.equals(protocolName, ignoreCase = true) }
    }

    /**
     * 根据连接名称查找播放历史记录
     * @param connectionName 连接名称
     * @return 匹配的记录列表
     */
    fun getHistoryByConnection(connectionName: String): List<MediaHistoryRecord> {
        return getHistory().filter { it.connectionName.equals(connectionName, ignoreCase = true) }
    }

    /**
     * 根据服务器地址查找播放历史记录
     * @param serverAddress 服务器地址
     * @return 匹配的记录列表
     */
    fun getHistoryByServerAddress(serverAddress: String): List<MediaHistoryRecord> {
        return getHistory().filter { it.serverAddress.equals(serverAddress, ignoreCase = true) }
    }

    /**
     * 根据媒体类型查找播放历史记录
     * @param mediaType 媒体类型（VIDEO、AUDIO等）
     * @return 匹配的记录列表
     */
    fun getHistoryByMediaType(mediaType: String): List<MediaHistoryRecord> {
        return getHistory().filter { it.mediaType.equals(mediaType, ignoreCase = true) }
    }

    /**
     * 获取视频播放历史记录
     * @return 视频播放记录列表
     */
    fun getVideoHistory(): List<MediaHistoryRecord> {
        return getHistoryByMediaType("VIDEO")
    }

    /**
     * 获取音频播放历史记录
     * @return 音频播放记录列表
     */
    fun getAudioHistory(): List<MediaHistoryRecord> {
        return getHistoryByMediaType("AUDIO")
    }

    /**
     * 根据协议类型分组获取历史记录
     * @return 按协议类型分组的播放历史记录映射
     */
    fun getHistoryByProtocolGroups(): Map<String, List<MediaHistoryRecord>> {
        return getHistory().groupBy { it.protocolName }
    }

    /**
     * 根据媒体类型分组获取历史记录
     * @return 按媒体类型分组的播放历史记录映射
     */
    fun getHistoryByMediaTypeGroups(): Map<String, List<MediaHistoryRecord>> {
        return getHistory().groupBy { it.mediaType }
    }

    /**
     * 清除所有播放历史记录
     */
    fun clearAllHistory() {
        prefs.edit {
            remove(KEY_HISTORY)
        }
        Log.d(TAG, "所有媒体播放历史记录已清除")
    }

    /**
     * 清除特定协议的历史记录
     * @param protocolName 要清除的协议名称
     */
    fun clearHistoryByProtocol(protocolName: String) {
        try {
            val currentHistory = getHistory()
            val updatedHistory = currentHistory.filter { it.protocolName != protocolName }

            prefs.edit {
                putString(KEY_HISTORY, gson.toJson(updatedHistory))
            }
            Log.d(TAG, "协议 $protocolName 的播放历史记录已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除协议历史记录失败", e)
        }
    }

    /**
     * 清除特定连接的历史记录
     * @param connectionName 要清除的连接名称
     */
    fun clearHistoryByConnection(connectionName: String) {
        try {
            val currentHistory = getHistory()
            val updatedHistory = currentHistory.filter { it.connectionName != connectionName }

            prefs.edit {
                putString(KEY_HISTORY, gson.toJson(updatedHistory))
            }
            Log.d(TAG, "连接 $connectionName 的播放历史记录已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除连接历史记录失败", e)
        }
    }

    /**
     * 清除特定媒体类型的历史记录
     * @param mediaType 要清除的媒体类型
     */
    fun clearHistoryByMediaType(mediaType: String) {
        try {
            val currentHistory = getHistory()
            val updatedHistory = currentHistory.filter { !it.mediaType.equals(mediaType, ignoreCase = true) }

            prefs.edit {
                putString(KEY_HISTORY, gson.toJson(updatedHistory))
            }
            Log.d(TAG, "媒体类型 $mediaType 的播放历史记录已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除媒体类型历史记录失败", e)
        }
    }

    /**
     * 删除指定的播放历史记录
     * @param mediaUri 要删除的媒体URI
     */
    fun deleteHistoryByUri(mediaUri: String) {
        try {
            val currentHistory = getHistory()
            val updatedHistory = currentHistory.filter { it.mediaUri != mediaUri }

            prefs.edit {
                putString(KEY_HISTORY, gson.toJson(updatedHistory))
            }
            Log.d(TAG, "播放历史记录已删除: $mediaUri")
        } catch (e: Exception) {
            Log.e(TAG, "删除播放历史记录失败", e)
        }
    }

    /**
     * 删除指定的播放历史记录
     * @param record 要删除的记录
     */
    fun deleteHistory(record: MediaHistoryRecord) {
        deleteHistoryByUri(record.mediaUri)
    }

    /**
     * 获取播放历史记录总数
     * @return 记录总数
     */
    fun getHistoryCount(): Int {
        return getHistory().size
    }

    /**
     * 检查是否已存在相同的播放记录（基于媒体URI）
     * @param mediaUri 媒体URI
     * @return 如果存在返回true，否则返回false
     */
    fun containsHistory(mediaUri: String): Boolean {
        return getHistory().any { it.mediaUri == mediaUri }
    }

    /**
     * 获取指定URI的播放记录（用于继续播放功能）
     * @param mediaUri 媒体URI
     * @return 播放记录，如果不存在返回null
     */
    fun getHistoryByUri(mediaUri: String): MediaHistoryRecord? {
        return getHistory().find { it.mediaUri == mediaUri }
    }

    /**
     * 获取最近播放的媒体记录
     * @return 最近播放的媒体记录，如果不存在返回null
     */
    fun getMostRecentHistory(): MediaHistoryRecord? {
        return getHistory().firstOrNull()
    }

    /**
     * 获取最近播放的视频记录
     * @return 最近播放的视频记录，如果不存在返回null
     */
    fun getMostRecentVideoHistory(): MediaHistoryRecord? {
        return getVideoHistory().firstOrNull()
    }

    /**
     * 获取最近播放的音频记录
     * @return 最近播放的音频记录，如果不存在返回null
     */
    fun getMostRecentAudioHistory(): MediaHistoryRecord? {
        return getAudioHistory().firstOrNull()
    }

    /**
     * 获取指定连接的最近播放记录
     * @param connectionName 连接名称
     * @return 该连接的最近播放记录，如果不存在返回null
     */
    fun getMostRecentHistoryByConnection(connectionName: String): MediaHistoryRecord? {
        return getHistory().firstOrNull { it.connectionName == connectionName }
    }

    /**
     * 获取指定协议的最近播放记录
     * @param protocolName 协议名称
     * @return 该协议的最近播放记录，如果不存在返回null
     */
    fun getMostRecentHistoryByProtocol(protocolName: String): MediaHistoryRecord? {
        return getHistory().firstOrNull { it.protocolName == protocolName }
    }

    /**
     * 获取格式化的时间字符串
     * @param timestamp 时间戳
     * @param pattern 格式化模式，默认为 "yyyy-MM-dd HH:mm:ss"
     * @return 格式化后的时间字符串
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 获取播放位置百分比（兼容旧方法名）
     * @param record 播放记录
     * @return 播放百分比（0-100）
     */
    @Deprecated("使用 record.getPlaybackPercentage() 替代", ReplaceWith("record.getPlaybackPercentage()"))
    fun getPlaybackPercentage(record: MediaHistoryRecord): Int {
        return record.getPlaybackPercentage()
    }
}



