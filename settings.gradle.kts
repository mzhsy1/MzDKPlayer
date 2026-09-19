pluginManagement {
    repositories {
        // GitHub Actions 的 runner 在境外：把官方源放最前面。
        // 国内镜像放在后面兜底，本地开发仍然镜像优先（拉取更快），行为不变。
        val onCi = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
        if (onCi) {
            mavenCentral()
            gradlePluginPortal()
        }
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
        if (!onCi) {
            mavenCentral()
            gradlePluginPortal()
        }
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
        // ✅ 在这里添加 flatDir，并且指定它位于 app 模块的 libs 目录
        flatDir {
            dirs("app/libs")
        }

    }
}

rootProject.name = "MzDKPlayer"
include(":app")
 