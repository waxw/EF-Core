package com.miyako.core.bubble

import android.app.Activity
import java.util.WeakHashMap

private val managers = WeakHashMap<Activity, BubbleManager>()

/**
 * 获取当前 Activity 的气泡管理器，同一 Activity 共享同一实例，
 * 多气泡堆叠状态由管理器统一维护。
 */
fun Activity.bubbleManager(): BubbleManager = synchronized(managers) {
  managers.getOrPut(this) { BubbleManager(this) }
}

/**
 * 在当前 Activity 中弹出一个应用内气泡，用法：
 *
 * ```kotlin
 * showBubble {
 *   title = "新消息"
 *   message = "这是一条应用内气泡通知"
 *   onClick = { ... }
 * }
 * ```
 */
fun Activity.showBubble(build: BubbleSpec.() -> Unit): BubbleView = bubbleManager().show(build)

/** 使用已有 [BubbleSpec] 弹出一个应用内气泡 */
fun Activity.showBubble(spec: BubbleSpec): BubbleView = bubbleManager().show(spec)

/** 关闭当前 Activity 的全部气泡 */
fun Activity.dismissAllBubbles() = bubbleManager().dismissAll()
