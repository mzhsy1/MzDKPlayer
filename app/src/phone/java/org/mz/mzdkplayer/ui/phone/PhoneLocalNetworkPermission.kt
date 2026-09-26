package org.mz.mzdkplayer.ui.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Android 17（API 37）的「本地网络保护」（Local Network Protections）。
 *
 * 判定口径按官方文档：应用 **targetSdk ≥ 37** 时，访问局域网地址必须持有
 * `ACCESS_LOCAL_NETWORK`（属于 `NEARBY_DEVICES` 权限组），否则网络栈在底层直接丢弃这些连接。
 * TCP 的表现是**超时**（`java.net.SocketTimeoutException`）而不是连接被拒绝 ——
 * 所以 SMB 侧看到的是「failed to connect to /192.168.x.x (port 445) ... after 5000ms」，
 * 而同一台设备用其他方式访问同一个 445 端口却是通的。
 *
 * Android 16 上这一保护还只是 opt-in（要靠 `am compat enable RESTRICT_LOCAL_NETWORK` 才会生效），
 * 所以这里只在 Android 17 及以上才要求授权，免得把本来能用的功能拦掉。
 */
internal object PhoneLocalNetworkPermission {

    /** 权限名（Android 17 新增；这里直接引用常量，编译期内联，低版本系统只是不认识这个字符串） */
    const val PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK

    /** 只有 Android 17（API 37）起才需要申请：更早的系统靠 `INTERNET` 就够 */
    val isRequired: Boolean = Build.VERSION.SDK_INT >= 37

    /** 是否已授权；不需要申请的系统一律视为已授权 */
    fun isGranted(context: Context): Boolean = !isRequired ||
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
}
