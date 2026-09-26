package org.mz.mzdkplayer.tool

import fi.iki.elonen.NanoHTTPD
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

// 加上这个注解，即使代码混淆了，JSON 解析依然能匹配到正确的字段名
data class RemoteConfig(
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("port") val port: String? = null,
    @SerializedName("shareName") val shareName: String? = null,
    @SerializedName("aliasName") val aliasName: String? = null
)

class RemoteInputServer(
    port: Int,
    private val onReceive: (RemoteConfig) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.POST) {
            return try {
                val files = HashMap<String, String>()
                // 这一步是必须的，否则无法读取 POST 的内容
                session.parseBody(files)

                // 手机端直接 fetch 发送的是 JSON 字符串，通常在 "postData" 键里
                val json = files["postData"]

                if (json.isNullOrBlank()) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Empty Data")
                }

                // 解析
                val config = Gson().fromJson(json, RemoteConfig::class.java)

                CoroutineScope(Dispatchers.Main).launch {
                    onReceive(config)
                }

                // 返回给手机端的响应头，允许跨域（如果是调试环境）
                val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "success")
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                e.printStackTrace()
                // 将详细错误发回手机，方便调试
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Server Error: ${e.message}")
            }
        }
        // 返回给手机的 HTML 页面
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, getHtmlPage())
    }

    private fun getHtmlPage(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>TV 输入助手</title>
                <style>
                    body { font-family: sans-serif; padding: 20px; background: #f5f5f5; color: #333; }
                    .card { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); max-width: 500px; margin: 0 auto; }
                    h3 { text-align: center; color: #2196F3; margin-top: 0; }
                    
                    /* 描述标签样式 */
                    label { display: block; font-size: 14px; font-weight: bold; margin-top: 15px; margin-bottom: 5px; color: #666; }
                    
                    input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; box-sizing: border-box; font-size: 16px; }
                    button { width: 100%; padding: 15px; background: #2196F3; color: white; border: none; border-radius: 8px; font-size: 16px; margin-top: 25px; cursor: pointer; }
                    #status { margin-top: 15px; text-align: center; font-weight: 500; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h3>配置挂载信息</h3>
                    
                    <label>服务器地址</label>
                    <input id="ip" placeholder="IP地址 / 服务器地址">
                    
                    <label>用户名</label>
                    <input id="username" placeholder="用户名">
                    
                    <label>密码</label>
                    <input id="password" type="password" placeholder="密码">
                    
                    <label>路径名称</label>
                    <input id="shareName" placeholder="共享路径/名称/NFS导出路径">
                    
                    <label>别名</label>
                    <input id="aliasName" placeholder="别名 (可选)">
                    
                    <button onclick="send()">发送到电视</button>
                    <p id="status"></p>
                </div>
                <script>
                    function send() {
                        const statusEl = document.getElementById('status');
                        const data = {
                            ip: document.getElementById('ip').value,
                            username: document.getElementById('username').value,
                            password: document.getElementById('password').value,
                            shareName: document.getElementById('shareName').value,
                            aliasName: document.getElementById('aliasName').value
                        };
                        statusEl.innerText = '正在发送...';
                        statusEl.style.color = 'gray';
                        
                        fetch('/', {
                            method: 'POST',
                            body: JSON.stringify(data)
                        }).then(res => {
                            if(res.ok) { 
                                statusEl.innerText = '✅ 发送成功！请看电视';
                                statusEl.style.color = 'green';
                            } else {
                                statusEl.innerText = '❌ 发送失败，请在电视端断开连接重试';
                                statusEl.style.color = 'red';
                            }
                        }).catch(e => {
                            statusEl.innerText = '错误: ' + e;
                            statusEl.style.color = 'red';
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}

