import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * 共享业务层：数据 / 播放内核 / 工具 / ViewModel。
 *
 * 这里**没有**任何设计系统依赖（既没有 androidx.tv.material3，也没有 androidx.compose.material3），
 * 所以电视端与手机端各自用什么版本的 Material 都不会互相污染。
 * 也**没有** android 资源目录：需要文案的地方一律把「资源 id → 字符串」的映射留在 UI 层。
 */
android {
    namespace = "org.mz.mzdkplayer.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26

        val localProperties = rootProject.file("local.properties")
        val properties = Properties().apply {
            if (localProperties.exists()) {
                load(localProperties.inputStream())
            }
        }
        val tmdbApiKey = properties.getProperty("TMDB_API_KEY", "")
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")

        // 版本号的唯一来源仍然是 app/build.gradle.kts 的 versionName（release.yml 也用同一个口径读它），
        // 这里只是为了 TMDB 请求的 User-Agent 拿到应用版本，不参与任何打包决策。
        val appVersionName = Regex("""versionName\s*=\s*"([^"]+)"""")
            .find(rootProject.file("app/build.gradle.kts").readText())
            ?.groupValues?.get(1)
            ?: "0.0.0"
        buildConfigField("String", "APP_VERSION_NAME", "\"$appVersionName\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    compilerOptions {
        jvmToolchain(21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose 只用到 ui / foundation / runtime（Modifier、Color、Canvas、remember…），
    // 刻意不引 material3：材质版本由 app 的两个 flavor 各自决定。
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)

    // 播放内核（Exo + VLC 双内核，与 main 一致）
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.libvlc.all)
    implementation(files(rootProject.file("app/libs/lib-decoder-ffmpeg-release.aar")))
    // akdanmaku 的公开类型会出现在业务层签名里（DanmakuType / VideoPlayerViewModel.danmakuConfig），
    // 而且 app 的弹幕渲染组件直接用它的 API，所以这里必须是 api —— 顺带避免同一个 aar 被两个模块各声明一次。
    api(files(rootProject.file("app/libs/akdanmaku.aar")))

    // 弹幕渲染（akdanmaku 依赖 gdx / ashley）
    implementation(libs.ashley)
    implementation(libs.gdx)
    implementation(libs.gdx.backend.android)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    // 网络协议：SMB / NFS / FTP / WebDAV / HTTP
    implementation(libs.smbj)
    // NFS / FTP 的类型（Nfs3File、FTPFile）会出现在业务层交给 UI 的回调里，
    // 电视端列表页直接遍历它们，所以这两个必须是 api。
    api(libs.nfs.client)
    api(libs.commons.net)
    implementation(libs.thegrizzlylabs.sardine.android) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "stax", module = "stax-api")
        exclude(group = "stax", module = "stax")
        exclude(group = "xmlpull", module = "xmlpull")
    }

    // 刮削 / 解析 / 工具
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.jaudiotagger)
    // 电视端「手机扫码遥控」要直接 start/stop 这个服务，而它是 NanoHTTPD 的子类
    api(libs.nanohttpd)
    implementation(libs.core)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.logback.android)

    testImplementation(libs.junit)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}
