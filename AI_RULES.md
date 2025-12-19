# 🤖 CockpitMap AI 开发与协作规范

为了提高代码正确率，AI 在执行任务时必须严格遵守以下规则：

## 1. 语法纯净度红线 (Zero Artifacts)
- **禁止写入占位符**：绝对禁止在代码中写入 `<caret>`、`TODO` 或任何 IDE 标记字符。
- **操作闭环**：执行 `write_file` 后，必须立即执行 `analyze_current_file`。

## 2. 强制自审闭环 (Self-Analysis Check) 🔴 [新增加固]
- **零红字原则**：在向用户报告任务完成前，必须确保 `analyze_current_file` 返回结果中 ERROR 为 0。
- **API 稳健性调用**：在调用第三方 SDK（如高德）时，优先使用显式的 **Setter/Getter 方法** (如 `setXxx()`)，禁止使用不稳定的 Kotlin 属性映射赋值，以规避版本兼容性引发的 `val cannot be reassigned` 错误。

## 3. 模块边界与解耦 (Dependency Integrity)
- **协议下沉原则**：跨模块通信接口、枚举、通用模型必须定义在 `:core:model` 中。

## 4. 重构安全守则 (Refactoring Safety)
- **移动即同步**：类或文件迁移时，必须同步更新全项目所有调用处的 `import`。

## 5. HMI 统一性 (UI Standards)
- **禁止私造 UI**：所有组件必须符合车载“极致瘦身”规范，字号以 13sp-14sp 为准，优先复用 `:core:designsystem`。

## 6. 强制注释规范 (Documentation)
- **KDoc 覆盖**：所有新增公共类和 Repository 方法必须具备包含 `[职责描述]` 标签的详尽注释。
