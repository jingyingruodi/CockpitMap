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
  - 职责描述：集成高德 3D 地图渲染，提供 `MapController` 实现，支持打点、画线、缩放控制。
  - 依赖：`:core:model`, `:core:designsystem`。
- **`:feature:routing`**:
  - 职责描述：提供搜索联想、目的地确认、路径规划预览 UI。
  - 依赖：`:core:data`, `:core:model`, `:core:designsystem`。

### 2. Core 模块 (基础支撑)
- **`:core:designsystem`**:
  - 职责描述：**车机专属 UI 组件库**。定义全局提示 (`HintLayer`)、加载反馈 (`LoadingHint`) 及标准悬浮组件。
- **`:core:data`**:
  - 职责描述：Repository 模式。管理 **DataStore** (常用地址存储)、**RouteSearch** (驾车路径规划) 及搜索建议。
- **`:core:model`**:
  - 职责描述：**协议下沉中心**。存放 `RouteInfo`, `GeoLocation`, `SavedLocation` 及跨模块控制接口 `MapController`。
- **`:core:common`**:
  - 职责描述：提供权限管理 (`PermissionManager`) 与协程工具。

---

## 🤝 协作开发守则
1. **单向依赖**：Feature 模块之间禁止相互依赖，必须通过 `app` 模块组合或 `core:model` 协议解耦。
2. **UI 统一样式**：禁止私造 UI，必须引用 `:core:designsystem`，确保极致瘦身的 HMI 风格一致。
3. **数据流向**：遵循 UDF (Unidirectional Data Flow) 模式，UI 通过 ViewModel 与 Repository 交互。
