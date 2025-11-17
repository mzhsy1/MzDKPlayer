import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.mz.mzdkplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.mz.mzdkplayer"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf( "armeabi-v7a","x86")
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

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // You can add other compiler options here if needed
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.exoplayer)
//    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.akdanmaku)
    implementation(libs.accompanist.permissions)
    implementation(libs.smbj)
    implementation(libs.logback.android)
   // implementation(libs.androidx.media3.ui.compose)
    implementation(libs.gson)
    implementation(libs.coil3.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // 暂时不需要原生ass显示
    //implementation(libs.ass.media)
    // 👇 修改这一行：排除 xpp3 和 stax
    implementation(libs.thegrizzlylabs.sardine.android) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "stax", module = "stax-api")
        exclude(group = "stax", module = "stax")
        exclude(group = "xmlpull", module = "xmlpull")
    }

    implementation(libs.jaudiotagger)
    implementation(libs.commons.net)
    //implementation(libs.jcifs)

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