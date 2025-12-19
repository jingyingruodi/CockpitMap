# CockpitMap - 智能车载导航系统 🚗📍

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Android Studio](https://img.shields.io/badge/Android%20Studio-Ladybug-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)

## 📌 项目简介
CockpitMap 是一款专为 Android 车机系统设计的**现代导航地图程序**。本项目采用**现代 Android 模块化架构**开发，深度适配车载 HMI（人机交互）标准，旨在为开发者提供一个可扩展、高性能的车载导航基础框架。

## 🛠 核心功能
- **全屏沉浸地图**: 采用 Compose 打造的沉浸式地图底座。
- **车载 HMI 适配**: 大尺寸交互组件、高对比度视觉设计，确保驾驶安全。
- **模块化架构**: 清晰的业务逻辑拆分，支持多人高效协作。
- **语音交互预设**: 内置语音助手交互 UI 及状态管理。

## 🏗 技术架构说明
本项目严格遵循 **Clean Architecture** 架构：
- **UI 层**: Jetpack Compose (Material 3)
- **架构模式**: MVI / MVVM
- **依赖管理**: Version Catalog (libs.versions.toml)
- **模块化**: 详细说明请参见 [MODULES.md](./MODULES.md) 👈

## 📁 模块速览
- `:app` - 应用入口与模块集成。
- `:feature:map` - 地图渲染与交互核心。
- `:feature:routing` - 路径规划与导航逻辑。
- `:core:designsystem` - 车机标准 UI 组件库。
- `:core:data` - 数据持久化与 Repository 实现。

## 🛠 开发者调试信息 (Amap SDK)
在申请或更换高德地图 SDK Key 时，请使用以下信息：
- **包名 (Package Name)**: `com.example.cockpitmap`
- **调试版 SHA1**: `12:E1:6F:BC:56:FE:62:8F:41:68:12:F7:4C:11:93:12:7D:F0:F8:7A`
- **当前已配置 Key**: `4eeab95822b6a0e53f2835fcc4d87fc1` (已写入 AndroidManifest.xml)

## 🤝 协作开发
1. **查阅文档**: 开发前请务必阅读 [模块化开发指南](./MODULES.md)。
2. **分支管理**: 采用 Git Flow，Feature 开发请基于 `develop` 分支。
3. **提交规范**: 遵循 Angular Commit Message 规范。
4. **AI使用规范**: AI 开发与协作规范请参见 [AI_RULES.md](./AI_RULES.md)。

## 🚀 快速开始
1. `git clone https://github.com/your-repo/CockpitMap.git`
2. 使用 Android Studio Ladybug 或更高版本打开。
3. 点击 `Sync Project with Gradle Files`。
4. 运行 `app` 模块。

---
**开发者**: [jingyingruodi]  
**许可证**: [MIT](./LICENSE)
