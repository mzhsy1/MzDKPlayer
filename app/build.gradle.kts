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
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.akdanmaku)
    implementation(libs.accompanist.permissions)
    implementation(libs.smbj)
    implementation(libs.logback.android)
    implementation(libs.gson)
    implementation(libs.ass.media)
    implementation(libs.thegrizzlylabs.sardine.android)
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

    debugImplementation(libs.androidx.ui.test.manifest)
}