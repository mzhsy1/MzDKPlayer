
import java.io.File
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
        // 手机端分支：抬到 Android 8.0（自适应图标 / 通知渠道 / 更少的兼容分支），
        // 电视端 D-pad 与 Media3 内核在 26+ 上行为不变。
        minSdk = 26
        targetSdk = 37
        versionCode = 114
        versionName = "1.18.0"
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

    // 两种形态各出一个 App：源码目录、依赖、清单、资源、applicationId 全部隔离。
    // 这是「两套设计系统零交叉」的落地方式 —— 电视端编译在 androidx.tv.material3 + compose 1.12.1 上，
    // 手机端编译在 androidx.compose.material3 1.5.0-alpha29（连带 compose 1.13.0-alpha01）上，
    // 依赖按 variant 解析，互不污染。
    flavorDimensions += "form"
    productFlavors {
        create("tv") {
            dimension = "form"
            // 沿用历史包名：老用户可以直接升级，Release 资产名保持同一套口径
            applicationId = "org.mz.mzdkplayer"
        }
        create("phone") {
            dimension = "form"
            // 与电视端不同包名 → 两个 App 可以同时安装，不存在桌面图标冲突
            applicationId = "org.mz.mzdkplayer.phone"
        }
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
    // 签名材料由环境变量提供（CI 里来自 GitHub Secrets），本地不配就维持「不签名」的现状。
    // 相关变量：MZDK_KEYSTORE_FILE / MZDK_KEYSTORE_PASSWORD / MZDK_KEY_ALIAS / MZDK_KEY_PASSWORD
    val releaseSigning = signingConfigs.create("release") {
        val keystorePath = System.getenv("MZDK_KEYSTORE_FILE")
        if (!keystorePath.isNullOrBlank() && File(keystorePath).exists()) {
            storeFile = File(keystorePath)
            storePassword = System.getenv("MZDK_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("MZDK_KEY_ALIAS")
            keyPassword = System.getenv("MZDK_KEY_PASSWORD")
        }
        enableV1Signing = true
        enableV2Signing = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 拿到签名材料才挂签名配置，否则照旧产出 app-*-release-unsigned.apk
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
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

    // 业务层：data / player / tool / ViewModel（不含任何设计系统）
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
//    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.logback.android)
    implementation(libs.gson)
    implementation(libs.coil3.coil.compose)
    implementation(libs.coil.network.okhttp)
    // 二维码（电视端「手机扫码遥控」面板）
    implementation(libs.core)

    // ---- 电视端独有的依赖 ----
    // tv-material3 会带进 compose 1.12.x 的稳定 core；这里刻意**不**引 androidx.compose.material3，
    // 所以 tvDebug 解析到的仍是 1.12.1（也就是改动前的版本）。
    "tvImplementation"(libs.androidx.tv.foundation)
    "tvImplementation"(libs.androidx.tv.material)
    // 电视端本地文件列表的存储权限申请（手机端走系统相册/SAF，用不到）
    "tvImplementation"(libs.accompanist.permissions)

    // ---- 手机端独有的依赖 ----
    // Material 3 Expressive（1.5.0-alpha29）。它会连带把 compose ui/foundation/runtime 抬到
    // 1.13.0-alpha01 —— 因为现在按 variant 解析，电视端不再受这个副作用影响。
    "phoneImplementation"(libs.androidx.material3)
    "phoneImplementation"(libs.androidx.material.icons.core)

    debugImplementation(libs.androidx.ui.tooling.preview)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // JVM 单元测试：业务层测试都随 :core 走，app 这两条仅为保留原有配置
    testImplementation(libs.junit)
    implementation(libs.androidx.ui.tooling)
    coreLibraryDesugaring(libs.desugarJdkLibs)
    debugImplementation(libs.androidx.ui.test.manifest)
}
