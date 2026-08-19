# EF-Core

Kotlin 基础工具库集合：纯 JVM 核心 + Android UI + Jetpack Compose 扩展 + KSP 代码生成。

## 模块

| 模块 | 类型 | 内容 | Maven 坐标 |
|------|------|------|-----------|
| `:core` | 纯 JVM | [BaseExtensions](docs/base-extensions.md)（orInit/ifTrue/cast/unsafeLazy/hex 等）、`task`（TaskRunner 重试/轮询任务执行器）、`mvi`（UiState/UiEffect/UiAction 契约） | `io.github.waxw:core` |
| `:core-ui` | Android Library | View 扩展（`dp`/`sp`/圆角/多语言）、协程扩展（`launchMain`/launchIO 等）、TextView 富文本扩展、`rv`（Scaffold 列表）、`viewbinding`（BaseActivity/BaseFragment/BaseDialogFragment）、`MviViewModel`、[应用内气泡（View）](docs/bubble.md)、`Debuger`（debug/release 变体） | `io.github.waxw:core-ui` |
| `:core-compose` | Android Compose | [ComposeBubble](docs/compose-bubble.md)：复用 core-ui 气泡逻辑的 Compose 适配层 | `io.github.waxw:core-compose` |
| `:core-ksp` | JVM（KSP 处理器） | [MVI 注解与代码生成](docs/mvi-ksp.md)（@DispatchAction/@DelegateDispatch） | `io.github.waxw:core-ksp` |

## 环境要求

- JDK 17
- Android：compileSdk 34，minSdk 24
- `:core-compose` 与使用 Compose 的模块需要应用 Compose 编译器插件并声明 Compose 依赖

## 接入

### 源码模块（本仓库内）

```kotlin
// settings.gradle.kts
include(":core", ":core-ui", ":core-compose", ":core-ksp")

// 模块 build.gradle.kts
implementation(project(":core"))
implementation(project(":core-ui"))
implementation(project(":core-compose"))
ksp(project(":core-ksp"))   // MVI 代码生成（需应用 ksp 插件）
```

### Maven 坐标

```kotlin
implementation("io.github.waxw:core:0.0.5")
implementation("io.github.waxw:core-ui:0.0.5")
implementation("io.github.waxw:core-compose:0.0.5")
ksp("io.github.waxw:core-ksp:0.0.5")
```

`:core-ui`、`:core-compose` 额外发布 `-debug` 变体（`io.github.waxw:core-ui-debug` 等），供调试依赖使用。

> Compose 注意：`:core-compose` 的 Compose 依赖是 `implementation` 作用域，
> 调用方需自行应用 `org.jetbrains.kotlin.plugin.compose` 并声明 compose BOM 与所需依赖。

## 文档

- [docs/bubble.md](docs/bubble.md) —— 应用内气泡（View，`com.miyako.core.bubble`）
- [docs/compose-bubble.md](docs/compose-bubble.md) —— ComposeBubble（`com.miyako.compose.bubble`）
- [docs/mvi-ksp.md](docs/mvi-ksp.md) —— MVI 契约与 KSP 代码生成
- [docs/base-extensions.md](docs/base-extensions.md) —— core（JVM）扩展与 TaskRunner

## 发布

版本策略统一在根目录 `gav.gradle.kts`（`0.0.5`）：

- **开发版**：`0.0.5-<commit 短哈希>-<时间戳>`，如 `0.0.5-f3922df-20260818123000`
- **正式版**：`0.0.5`，满足任一条件触发：
  1. `./gradlew publish -Prelease=true`
  2. 环境变量 `RELEASE=true`
  3. 当前 commit 命中 tag `0.0.5` 或 `v0.0.5`

```bash
# 发布到本地 Maven 仓库（开发版）
./gradlew :core:publishToMavenLocal :core-ui:publishToMavenLocal \
  :core-compose:publishToMavenLocal :core-ksp:publishToMavenLocal

# 发布到 Maven Central（vanniktech 插件，自动签名；需先满足正式版条件）
./gradlew publish -Prelease=true
```

## 演示

`app` 模块的 MainActivity 演示了气泡（View + Compose）与各扩展用法，可作参考。
