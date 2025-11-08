package org.mz.mzdkplayer.logic.model

/**
 * 各个协议连接统一状态
 */
sealed class FileConnectionStatus {
    object Disconnected : FileConnectionStatus()
    object Connecting : FileConnectionStatus()
    object Connected : FileConnectionStatus()
    object LoadingFile : FileConnectionStatus()
    object FilesLoaded : FileConnectionStatus()
    data class Error(val message: String) : FileConnectionStatus()

    // 添加一个用于 UI 显示的描述方法
    override fun toString(): String {
        return when (this) {
            Disconnected -> "已断开"
            Connecting -> "连接中..."
            Connected -> "已连接"
            LoadingFile -> "正在加载文件"
            FilesLoaded -> "加载文件完成"
            is Error -> "错误: $message"
        }
    }
}

sealed class LocalFileLoadStatus {
    object LoadingFile : LocalFileLoadStatus()
    object FilesLoaded : LocalFileLoadStatus()
    data class Error(val message: String) : LocalFileLoadStatus()
}