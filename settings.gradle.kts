pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 腾讯云镜像（可选）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像
        //maven { url = uri("https://maven.aliyun.com/repository/public") }
       // maven { url = uri("https://maven.aliyun.com/repository/google") }
        //maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 腾讯云镜像（可选）
        // maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        google()
        maven { url = uri("https://jitpack.io") }
        mavenCentral()


    }
}

rootProject.name = "MzDKPlayer"
include(":app")
 