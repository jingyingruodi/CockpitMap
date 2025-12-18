# 📦 CockpitMap 模块化架构说明

为了实现高内聚、低耦合的多人协作开发，本项目采用了现代 Android 模块化架构。

## 📂 模块层级图
- `app`: 壳工程，负责全局配置与模块集成。
- `feature/*`: 业务功能模块（如：地图显示、搜索导航、语音交互）。
- `core/*`: 核心支撑模块（如：数据层、网络层、UI 组件库）。

---

## 🛠 核心模块职责说明

### 1. Feature 模块 (业务逻辑)
- **`:feature:map`**:
  - 职责：地图 SDK 集成、地图渲染、覆盖物管理、视角控制。
  - 依赖：`:core:model`, `:core:designsystem`。
- **`:feature:routing`**:
  - 职责：路径规划算法、实时路况、搜索建议、导航诱导。
  - 依赖：`:core:data`, `:core:model`。

### 2. Core 模块 (基础支撑)
- **`:core:designsystem`**:
  - 职责：**车机专属 UI 组件库**。定义字体、颜色、大尺寸按钮、深色模式适配。
- **`:core:data`**:
  - 职责：Repository 模式。管理 Local Database (Room) 和 Remote Data Source。
- **`:core:model`**:
  - 职责：**零依赖模块**。存放所有跨模块共享的 Data Class 和 Interface。
- **`:core:common`**:
  - 职责：工具类（Log, Date, Permission）、协程调度器封装。

---

## 🤝 协作开发守则
1. **单向依赖**：Feature 模块之间禁止相互依赖，必须通过 `app` 模块组合或 `core` 模块解耦。
2. **UI 统一样式**：所有通用 UI 组件（按钮、卡片）必须从 `:core:designsystem` 引用，确保整车 HMI 风格一致。
3. **数据流向**：必须遵循 UDF (Unidirectional Data Flow) 模式，通过 Repository 向 UI 暴露数据。
