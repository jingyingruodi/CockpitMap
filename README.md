# CockpitMap - 智能车载导航系统 🚗📍

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Android Studio](https://img.shields.io/badge/Android%20Studio-Ladybug-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)

## 📌 项目简介
CockpitMap 是一款专为 Android 车机系统设计的**现代导航地图程序**。本项目采用**现代 Android 模块化架构**开发，深度适配车载 HMI（人机交互）标准，旨在为开发者提供一个可扩展、高性能的车载导航基础框架。

## 🛠 核心功能 (已实现)
- **全屏沉浸地图**: 极致瘦身的 UI 布局，最大化保留驾驶视野。
- **智能路径规划**: 自动规划从当前位置到目的地的最优路径，支持路线预览。
- **智能镜头控制**: 导航模式下支持**自动回归机制**——手动操作后 5 秒自动恢复 3D 跟随。
- **常用地址管理**: 集成“家”、“公司”及自定义地点的收藏、状态感应与取消确认。
- **车载 HMI 适配**: 精修版大点击区域组件、13sp 标准字号、高对比度导航地图样式。
- **多视图模式**: 支持标准、卫星、夜间、导航四种地图显示模式。

## 🏗 技术架构说明
本项目严格遵循 **Clean Architecture** 架构：
- **UI 层**: Jetpack Compose (Material 3)
- **架构模式**: MVI / MVVM
- **持久化**: DataStore (常用地址) + Local Persistence
- **模块化**: 详细说明请参见 [MODULES.md](./MODULES.md) 👈

## 🛠 开发者调试信息 (Amap SDK)
在申请或更换高德地图 SDK Key 时，请使用以下信息：
- **包名 (Package Name)**: `com.example.cockpitmap`
- **调试版 SHA1**: `12:E1:6F:BC:56:FE:62:8F:41:68:12:F7:4C:11:93:12:7D:F0:F8:7A`
- **当前已配置 Key**: `4eeab95822b6a0e53f2835fcc4d87fc1`

## 🤝 协作开发
1. **查阅文档**: 开发前请务必阅读 [模块化开发指南](./MODULES.md)。
2. **AI 使用规范**: 本项目对 AI 辅助开发有严格的质量要求，详见 [AI_RULES.md](./AI_RULES.md)。

---
**开发者**: [jingyingruodi]  
**许可证**: [MIT](./LICENSE)
