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
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 1. 阿里云公共代理仓库 (强烈推荐，解决高德域名无法解析问题)
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        
        google()
        mavenCentral()
        
        // 2. 高德官方 (保留作为备份)
        maven { url = uri("https://mavensync.amap.com/maven/repository") }
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
