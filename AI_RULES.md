# 🤖 CockpitMap AI 开发与协作规范

为了提高代码正确率，AI 在执行任务时必须严格遵守以下规则：

## 1. 语法纯净度红线 (Zero Artifacts)
- **禁止写入占位符**：绝对禁止在代码中写入 `<caret>`、`TODO` 或任何 IDE 标记字符。
- **操作闭环**：执行 `write_file` 后，必须立即执行 `analyze_current_file`。

## 2. 强制自审闭环 (Self-Analysis Check) 🔴 [新增]
- **零红字原则**：在向用户报告任务完成前，必须确保 `analyze_current_file` 返回结果中 ERROR 为 0。
- **依赖先行**：若报错提示 `Unresolved reference`，必须先检查模块依赖关系，严禁盲目修改。

## 3. 模块边界与解耦 (Dependency Integrity)
- **协议下沉原则**：跨模块通信接口、枚举、通用模型必须定义在 `:core:model` 中。

## 4. 重构安全守则 (Refactoring Safety)
- **移动即同步**：类或文件迁移时，必须同步更新全项目所有调用处的 `import`。

## 5. HMI 统一性 (UI Standards)
- **禁止私造 UI**：所有通用悬浮窗、按钮必须引用 `:core:designsystem` 中的组件。

## 6. 强制注释规范 (Documentation)
- **KDoc 覆盖**：所有新增公共类和 Repository 方法必须具备详尽的 KDoc 注释。
