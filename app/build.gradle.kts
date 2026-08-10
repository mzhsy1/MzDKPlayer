
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.mz.mzdkplayer"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.mz.mzdkplayer"
        minSdk = 23
        targetSdk = 37
        versionCode = 103
        versionName = "1.16.9"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a","x86")
        }
        val localProperties = rootProject.file("local.properties")
        val properties = Properties().apply {
            if (localProperties.exists()) {
                load(localProperties.inputStream())
            }
        }

        val tmdbApiKey = properties.getProperty("TMDB_API_KEY", "")
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }
    splits {
        // 配置 ABI 拆分
        abi {
            // 启用 ABI 拆分
            isEnable = true

            // 清空默认的所有 ABI 列表，然后指定你需要拆分的架构
            reset()
            include("armeabi-v7a", "arm64-v8a")

            // 是否创建一个包含所有架构的“通用包”？
            // 如果设为 true，会多生成一个全架构的 APK
            isUniversalApk = true
        }
    }
    packaging {
        jniLibs {
            // 压缩 .so 文件到 APK 中（不解压安装）
            // 设为 true 则 APK 变小，但安装后占用空间变大
            // 设为 false 则 APK 略大，但在现代 Android 上运行更高效
            useLegacyPackaging = true

            // 如果遇到重复的 so 文件报错，可以用 pickFirst
            //pickFirsts.add("lib/**/libc++_shared.so")
        }
    }
    signingConfigs {
        create("release") {
            // 你的密钥配置...
            enableV1Signing = true
            enableV2Signing = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true

    }
//    repositories {
//        flatDir {
//            dirs("libs") // 声明本地 libs 目录
//        }
//    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmToolchain(21)
        // You can add other compiler options here if needed
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
//    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    //implementation(libs.vlc.android.mini)
    implementation(files("libs/lib-decoder-ffmpeg-release.aar"))
   // implementation(files("libs/libvlc-release.aar"))
    // 弹幕相关
    implementation(files("libs/akdanmaku.aar"))
    implementation(libs.ashley)
    // Source: https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx-backend-android
    implementation(libs.gdx.backend.android)
    // Source: https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx
    implementation(libs.gdx)
    implementation(libs.androidx.palette.ktx)
    //implementation(libs.akdanmaku)
    implementation(libs.accompanist.permissions)
    implementation(libs.smbj)
    implementation(libs.logback.android)
    // implementation(libs.androidx.media3.ui.compose)
    implementation(libs.gson)
    implementation(libs.coil3.coil.compose)
    implementation(libs.coil.network.okhttp)

    //implementation("io.coil-kt:coil-transformations:3.3.0")
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // 分页
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    //implementation("org.videolan:libbluray:1.3.4")
    // 暂时不需要原生ass显示
    //implementation(libs.ass.media)
    // 👇 修改这一行：排除 xpp3 和 stax
    implementation(libs.thegrizzlylabs.sardine.android) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "stax", module = "stax-api")
        exclude(group = "stax", module = "stax")
        exclude(group = "xmlpull", module = "xmlpull")
    }
    // 二维码
    implementation(libs.core)
    // 轻量级 HTTP 服务器
    implementation(libs.nanohttpd)
    // Gson 用于解析 JSON (如果你的项目里还没有的话)

    implementation(libs.jaudiotagger)
    implementation(libs.commons.net)
    //implementation("androidx.media3:media3-exoplayer-ffmpeg:1.9.0-alpha01")
    //implementation(libs.jcifs)
//    // https://mvnrepository.com/artifact/org.videolan.android/libvlc-all
    implementation(libs.libvlc.all)
// 请检查最新版本
    //implementation(libs.ass.kt)
    //implementation(libs.ass.media.v030beta02)
    //implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    // https://mvnrepository.com/artifact/com.emc.ecs/nfs-client
    implementation(libs.nfs.client)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    implementation(libs.androidx.ui.tooling)
    coreLibraryDesugaring(libs.desugarJdkLibs)
    debugImplementation(libs.androidx.ui.test.manifest)
}