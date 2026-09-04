# MVI 契约与 KSP 代码生成

EF-Core 提供一套轻量 MVI 模型：KMP 契约（`:core`）、Android ViewModel 基类（`:core-android`）
与 KSP 注解处理器（`:core-ksp`）三层，KSP 负责生成 Action 分发委托代码。

## 模块分工

| 模块 | 内容 |
|------|------|
| `:core`（KMP） | 契约接口：`com.miyako.mvi.UiState / UiEffect / UiAction` |
| `:core-android`（Android） | `com.miyako.mvi.MviViewModel<S, E, A>`（`uiState` / `uiEffect` StateFlow） |
| `:core-ksp`（代码生成） | 注解 `@DispatchAction` / `@DelegateDispatch` / `@Action`、`Dispatcher`、`defReturn` |

## 接入

```kotlin
// 模块 build.gradle.kts（应用了 ksp 插件时）
implementation(project(":core"))          // 或 io.github.waxw:core
implementation(project(":core-android"))  // 或 io.github.waxw:core-android
ksp(project(":core-ksp"))                 // 或 io.github.waxw:core-ksp
```

## 用法

以 `app` 模块的 `DemoViewModel` 为模板：

```kotlin
import com.miyako.core.ksp.mvi.Action
import com.miyako.core.ksp.mvi.DelegateDispatch
import com.miyako.core.ksp.mvi.DispatchAction
import com.miyako.core.ksp.mvi.Dispatcher
import com.miyako.core.ksp.mvi.defReturn
import com.miyako.mvi.MviViewModel
import com.miyako.mvi.UiAction
import com.miyako.mvi.UiEffect

@DelegateDispatch
class DemoViewModel : MviViewModel<DemoViewModel.UiState, UiEffect, UiAction>() {

  override val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)

  sealed class UiState : com.miyako.mvi.UiState { /* Loading / Error / Success */ }

  sealed class UiAction : com.miyako.mvi.UiAction {
    data object Back : UiAction()
    data class Item(val settings: Int) : UiAction()
  }

  init {
    // 未匹配到 @Action 时返回的默认值
    val unitReturn = defReturn<UiAction, Unit>(tag = "Unit") { }
    val intReturn = defReturn<UiAction, Int>(tag = "Int") { -1 }
    Dispatcher.bind(this, unitReturn, intReturn)
  }

  // 声明可分发的方法：参数类型 + 返回类型 与 @DispatchAction 参数匹配
  @DispatchAction(UiAction::class)
  fun click(action: UiAction) {
    Dispatcher.dispatch<UiAction, Unit>(this, action)
  }

  @DispatchAction(UiAction::class, Int::class)
  fun clickAndReturn(action: UiAction): Int {
    return Dispatcher.dispatch<UiAction, Int>(this, action)
  }

  // 具体 Action 的处理函数
  @Action
  fun clickGroup(action: UiAction.Group) { }

  @Action
  fun clickSettings(action: UiAction.Item) { }
}
```

## 注解说明

| 注解 | 目标 | 作用 |
|------|------|------|
| `@DelegateDispatch` | 类 | 标记需要生成分发委托的 ViewModel；生成 `<类名>_Build` 与 `<类名>_<Param><Return>Dispatcher` |
| `@DispatchAction` | 函数 | 声明分发入口；`param` 为 Action 基类，`returnType` 为返回类型（默认 `Unit`） |
| `@Action` | 函数 | 具体 Action 的处理函数，参数类型是 `param` 的子类时被分发调用 |

分发规则：`Dispatcher.dispatch` 按参数类型的 isAssignableFrom 匹配 `@Action`
处理函数；未匹配时回落到 `bind` 时注册的 `defReturn` 默认值。

> 注：KSP 处理器为实验性实现，接口可能随版本演进。
