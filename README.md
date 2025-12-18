# CockpitMap - 智能车载导航系统

## 📌 项目简介
CockpitMap 是一款专为 Android 车机系统设计的导航地图程序。本项目采用模块化架构开发，旨在提供高性能、高可靠性且符合车载 HMI 标准的导航体验。

## 🛠 核心功能规划
- [ ] **地图集成**: 支持多源地图数据渲染（矢量/卫星）。
- [ ] **路径规划**: 实时避堵、多路径策略选择。
- [ ] **语音交互**: 深度集成 TTS 与 ASR，支持全语音控制导航。
- [ ] **车机适配**: 针对车载屏幕大尺寸、高对比度、驾驶安全交互进行专项优化。

## 🏗 技术架构
项目采用 **Clean Architecture** 结合 **MVI/MVVM** 设计模式，并进行多模块化拆分：

### 模块说明
- `app`: 宿主程序。
- `feature:*`: 独立业务模块。
- `core:designsystem`: 车机标准组件库。
- `core:network/data`: 数据通信与持久化。

## 👥 协作指南
1. **代码规范**: 请遵循 Kotlin 官方代码风格。
2. **分支管理**: 采用 Git Flow。所有 Feature 开发请从 `develop` 分支拉取，通过 PR 合入。
3. **提交规范**: 建议使用 `feat:`, `fix:`, `docs:`, `refactor:` 等前缀。

## 🚀 快速开始
1. 克隆项目：`git clone https://github.com/your-repo/CockpitMap.git`
2. 使用 Android Studio Ladybug 或更高版本打开。
3. 同步 Gradle 并运行 `app` 模块。

---
**开发者**: [jingyingruodi]  
**许可证**: MIT
