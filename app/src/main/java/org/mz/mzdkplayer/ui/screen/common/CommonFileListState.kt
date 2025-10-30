// File: CommonFileListState.kt
package org.mz.mzdkplayer.ui.screen.common

//import androidx.compose.runtime.Stable
//
//@Stable
//sealed class ConnectionStatus {
//    object Disconnected : ConnectionStatus()
//    object Connecting : ConnectionStatus()
//    object Connected : ConnectionStatus()
//    data class Error(val message: String) : ConnectionStatus()
//    // 可以根据需要添加更多特定状态
//}
//
//data class FileItem(
//    val name: String,
//    val path: String,
//    val isDirectory: Boolean,
//    val extension: String = "",
//    val server: String = "",
//    val share: String = "",
//    val username: String = "",
//    val password: String = ""
//)
//
//data class FileListState(
//    val files: List<FileItem> = emptyList(),
//    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
//    val currentPath: String = "",
//    val isLoading: Boolean = false
//)