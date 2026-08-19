# ComposeBubble — `com.miyako.compose.bubble`

应用内气泡的 Jetpack Compose 适配层。**只做 Compose 适配，不重复实现气泡逻辑**：
`@Composable` 内容经 `ComposeView` 承载后，堆叠/动画/手势/生命周期全部复用
core-ui 的 [BubbleManager](bubble.md)。

## 依赖

```kotlin
implementation("io.github.waxw:core-compose:0.0.5")
// 或源码模块
implementation(project(":core-compose"))
```

> 调用方需要自行应用 Compose 编译器插件并声明 compose BOM 与所需依赖
> （`:core-compose` 的 Compose 依赖为 `implementation` 作用域）。
> `com.miyako.compose.bubble` 通过 typealias 导出 `BubblePosition/BubbleView/BubbleManager`，
> 单包导入即可使用。

## 基本用法

```kotlin
import com.miyako.compose.bubble.BubblePosition
import com.miyako.compose.bubble.showBubble
import com.miyako.compose.bubble.dismissAllBubbles

// 任意 ComponentActivity（含 AppCompatActivity）
showBubble {
  position = BubblePosition.TOP      // TOP / CENTER / BOTTOM
  durationMs = 3000                  // null 常驻
  margin = statusBarHeightPx         // SystemUI 避让由调用方处理
  onClick = { /* 点击回调 */ }
  onDismiss = { /* 消失回调 */ }
  content = {
    // @Composable 内容，外观完全自绘
    Card(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
      Text("这是一条应用内气泡通知", modifier = Modifier.padding(16.dp))
    }
  }
}

dismissAllBubbles()
```

## 与 View 版的差异

| 项目 | View 版 | ComposeBubble |
|------|---------|---------------|
| 内容 | `(ViewGroup) -> View` | `@Composable () -> Unit` |
| 接收者 | `Activity` | `ComponentActivity` |
| 逻辑 | core-ui 实现 | **复用 core-ui 实现**，仅包 `ComposeView` |

其余行为（位置/堆叠/滑动关闭/点击/自动消失/SystemUI 约定）与 View 版完全一致。

## 手动控制

```kotlin
val manager = bubbleManager()       // core-ui BubbleManager，同一 Activity 共享
manager.activeCount
manager.dismissAll()
```

## 演示

参考 `app` 模块 MainActivity 的「Compose 气泡 TOP / CENTER / BOTTOM」与「关闭全部气泡」按钮。
