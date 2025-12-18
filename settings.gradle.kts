pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 1. 优先使用阿里云镜像（国内访问最稳定）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        
        // 2. Google 和 MavenCentral 备用
        google()
        mavenCentral()
        
        // 3. 高德官方仓库（如果域名解析失败会跳过）
        maven { url = uri("https://mavensync.amap.com/maven/repository/") }
    }
}

rootProject.name = "CockpitMap"

include(":app")
include(":feature:map")
include(":feature:routing")
include(":feature:voice")
include(":core:common")
include(":core:designsystem")
include(":core:data")
include(":core:model")
include(":core:network")
