package com.miyako.compose.bubble

import androidx.activity.ComponentActivity
import com.miyako.core.bubble.bubbleManager as coreBubbleManager
import com.miyako.core.bubble.dismissAllBubbles as coreDismissAllBubbles

/**
 * 复用 core-ui 的 View 版气泡类型，让 Compose 调用方从一个包导入全部 API。
 */
typealias BubblePosition = com.miyako.core.bubble.BubblePosition

/** core-ui 的气泡容器，见 [com.miyako.core.bubble.BubbleView] */
typealias BubbleView = com.miyako.core.bubble.BubbleView

/** core-ui 的气泡管理器，见 [com.miyako.core.bubble.BubbleManager] */
typealias BubbleManager = com.miyako.core.bubble.BubbleManager

/**
 * 获取当前 Activity 的气泡管理器（core-ui 实现），同一 Activity 共享同一实例。
 */
fun ComponentActivity.bubbleManager(): BubbleManager = coreBubbleManager()

/**
 * 在当前 Activity 中弹出一个 Compose 气泡，用法：
 *
 * ```kotlin
 * showBubble {
 *   position = BubblePosition.TOP
 *   onClick = { ... }
 *   onDismiss = { ... }
 *   content = {
 *     Card(modifier = Modifier.padding(16.dp)) {
 *       Text("这是一条应用内气泡通知")
 *     }
 *   }
 * }
 * ```
 *
 * 内容通过 [ComposeView] 承载，堆叠/动画/手势/生命周期逻辑复用 core-ui 的
 * [BubbleManager]，本扩展只做 Compose 适配，不重复实现气泡逻辑。
 */
fun ComponentActivity.showBubble(build: ComposeBubbleSpec.() -> Unit): BubbleView =
  showBubble(ComposeBubbleSpec().apply(build))

/** 使用已有 [ComposeBubbleSpec] 弹出一个 Compose 气泡 */
fun ComponentActivity.showBubble(spec: ComposeBubbleSpec): BubbleView =
  bubbleManager().show(spec.toBubbleSpec())

/** 关闭当前 Activity 的全部气泡（复用 core-ui 实现） */
fun ComponentActivity.dismissAllBubbles() = coreDismissAllBubbles()
