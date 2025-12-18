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
        google()
        mavenCentral()
    }
}

rootProject.name = "CockpitMap"

// 应用程序入口
include(":app")

// 特能模块 (Features)
include(":feature:map")      // 地图显示与基本交互
include(":feature:routing")  // 路径规划与搜索
include(":feature:voice")    // 语音交互功能

// 核心通用模块 (Core)
include(":core:common")      // 通用工具类、基类
include(":core:designsystem") // 车机 UI 组件库
include(":core:data")        // 数据层 (Repository, API, DB)
include(":core:model")       // 核心业务模型
include(":core:network")     // 网络请求封装
