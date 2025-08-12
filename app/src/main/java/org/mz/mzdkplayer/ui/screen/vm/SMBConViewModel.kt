package org.mz.mzdkplayer.ui.screen.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SMBConViewModel : ViewModel() {
    private val _connectionStatus = MutableStateFlow("未连接")
    val connectionStatus: StateFlow<String> = _connectionStatus
    private val _fileList = MutableStateFlow<List<String>>(emptyList())
    val fileList: StateFlow<List<String>> = _fileList

    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null

    private val mutex = Mutex()  // 协程互斥锁
    fun connectToSMB(ip: String, username: String, password: String, shareName: String) {

        viewModelScope.launch {
            disconnectSMB()  // 先清理旧连接
//            withContext(Dispatchers.Main) {
//                _connectionStatus.value = "正在尝试连接"
//            }
            mutex.withLock {
                try {

                    withContext(Dispatchers.IO) {

                        if (!isConnected()) {  // 避免重复连接
                            val client = SMBClient()
                            connection = client.connect(ip)
                            val auth = AuthenticationContext(username, password.toCharArray(), null)
                            session = connection!!.authenticate(auth)
                            share = session!!.connectShare(shareName) as DiskShare
                        }

                    }
                    _connectionStatus.value = "已连接"
                    listFiles() // 获取文件列表
                } catch (e: Exception) {
                    Log.e("SMB", "连接失败$e", e)
                    _connectionStatus.value = "连接失败: ${e.message}"
                    //disconnectSMB()
                }
            }
        }
    }

    // 列出文件
    fun listFiles() {
        viewModelScope.launch{
            try {
                withContext(Dispatchers.IO) {
                    if (isConnected()) {  // 检查连接状态
                        val files = share!!.list("")
                        _fileList.value = files.map { it.fileName }
                    } else {
                        _connectionStatus.value = "连接已断开，请重新连接"
                    }
                }
            } catch (e: TransportException) {
                _connectionStatus.value = "网络错误: ${e.message}"
                disconnectSMB()
            } catch (e: Exception) {
                _connectionStatus.value = "获取文件失败: ${e.message}"
            }
        }
    }
    fun changeSMBTest(){
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _connectionStatus.value = "正在尝试连接"
            }
        }
    }

    // 断开连接
    fun disconnectSMB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                share?.close()
            } catch (e: TransportException) {
                Log.w("SMB", "Share 已断开，无需关闭")
            } finally {
                share = null
            }

            try {
                session?.close()
            } catch (e: TransportException) {
                Log.w("SMB", "Session 已断开，无需关闭")
            } finally {
                session = null
            }

            try {
                connection?.close()
            } catch (e: TransportException) {
                Log.w("SMB", "Connection 已断开，无需关闭")
            } finally {
                connection = null
            }

            withContext(Dispatchers.Main) {
                _connectionStatus.value = "已断开"
                _fileList.value = emptyList()
            }
        }
    }

    fun isConnected(): Boolean {
        return connection?.isConnected == true && isSessionActive()
    }
    fun isSessionActive(): Boolean {
        return try {
            share?.list("")  // 尝试列出根目录（不抛出异常说明连接正常）
            true
        } catch (e: Exception) {
            false  // 抛出异常说明连接已断开
        }
    }
}