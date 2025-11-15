# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ============ mbassy (EL resolution) ============
-dontwarn javax.el.**
-dontwarn net.engio.mbassy.dispatch.el.**

# ============ smbj (GSS/Kerberos) ============
-dontwarn org.ietf.jgss.**
-dontwarn com.hierynomus.smbj.auth.SpnegoAuthenticator
-dontwarn com.hierynomus.smbj.auth.GSSAuthenticationContext

-keep class org.mz.mzdkplayer.data.model.** { *; }

# ====================== Media3 专属 R8 规则（解决视频播放闪退） ======================
# 保留 Media3 全部核心类（必须！避免 R8 移除内部类 nb.a/kb.a）
-keep class androidx.media3.** { *; }

# 保留 Media3 源数据和播放器核心功能
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }

 #保留 @Keep 注解（Media3 使用了此注解）
-keep class androidx.annotation.Keep { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ====================== 其他必要库的保留规则（避免兼容性问题） ======================
## 保留 SMB 相关类（你的 SmbDataSource 依赖）
#-keep class com.hierynomus.smbj.** { *; }
#-keep class com.hierynomus.smbj.protocol.** { *; }
#
## 保留 Retrofit 和 Gson（避免网络请求崩溃）
#-keep class retrofit.** { *; }
#-keep interface retrofit.** { *; }
#-keep class com.google.gson.** { *; }
#-keep class com.google.gson.stream.** { *; }
#
## 保留 Coil 图片加载（避免图片相关崩溃）
#-keep class coil.** { *; }
#-keep class coil.image.** { *; }
#
## 保留 Compose 和 AndroidX 基础类（避免 UI 问题）
#-keep class androidx.compose.** { *; }
#-keep class androidx.core.** { *; }
#-keep class androidx.lifecycle.** { *; }

# ====================== 通用安全规则（防止意外移除） ======================
# 保留所有反射调用（ExoPlayer 依赖反射）
#-keepclassmembers class * {
#    *;
#}

# 保留所有注解（避免混淆导致的注解失效）
#-keep class androidx.annotation.** { *; }
#-keep class java.lang.annotation.** { *; }