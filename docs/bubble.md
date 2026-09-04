# 应用内气泡（View）— `com.miyako.core.bubble`

应用内气泡（overlay 通知）的 View 实现，挂在 Activity 的 decorView 之上，
支持堆叠、左右滑动关闭、点击回调与自动消失。**不提供任何默认 UI**——
背景、圆角、边距、阴影全部由调用方在内容里自绘。

## 依赖

```kotlin
implementation("io.github.waxw:core-android:0.0.5")
// 或源码模块
implementation(project(":core-android"))
```

## 基本用法

```kotlin
import com.miyako.core.bubble.BubblePosition
import com.miyako.core.bubble.showBubble
import com.miyako.core.bubble.dismissAllBubbles

showBubble {
  position = BubblePosition.TOP          // TOP / CENTER / BOTTOM
  durationMs = 3000                      // 自动消失时长；null 表示常驻
  margin = statusBarHeight               // px；TOP→topMargin，BOTTOM→bottomMargin，CENTER 忽略
  content = { parent -> yourView(parent) } // 必填：自定义内容 View
  onClick = { /* 点击后先回调再消失 */ }
  onDismiss = { /* 点击/自动/滑动/手动关闭都会触发 */ }
}
```

关闭当前 Activity 的全部气泡：

```kotlin
dismissAllBubbles()
```

## BubbleSpec 字段

| 字段 | 默认 | 说明 |
|------|------|------|
| `position` | `TOP` | 出现位置，不同位置独立堆叠 |
| `durationMs` | `3000` | 自动消失时长（ms）；`null` 常驻 |
| `margin` | `null` | 距容器边缘边距（px），仅 TOP/BOTTOM 生效 |
| `content` | 必填 | `(ViewGroup) -> View`，外观完全自绘 |
| `onClick` | `null` | 点击回调，先回调再消失 |
| `onDismiss` | `null` | 消失回调（点击/自动/滑动/手动统一触发） |

## 行为约定

- **无默认 UI**：容器只负责 AddView 与交互/动画，样式由 `content` 自绘。
- **SystemUI 避让由调用方负责**：气泡不处理状态栏/导航栏，
  需要避让时自行获取系统栏高度并计入 `margin`。
- **堆叠**：同一位置多气泡按实测高度上下排列，互不重叠；不同位置互不干扰。
- **手势**：左右滑动（超过容器宽度 35%）关闭；点击触发 `onClick` 后消失；
  自动消失期间拖拽会暂停计时。
- **生命周期**：通过 `ActivityLifecycleCallbacks` 在 Activity 销毁时自动清理。

## 手动控制

```kotlin
val manager = bubbleManager()     // 同一 Activity 共享同一实例
manager.activeCount               // 当前气泡数量
manager.dismissAll()

val bubble: BubbleView = showBubble { ... }
bubble.dismiss()                  // 手动关闭单个气泡
```

## 演示

参考 `app` 模块 MainActivity 的「气泡-顶部 / 居中 / 底部」按钮。
