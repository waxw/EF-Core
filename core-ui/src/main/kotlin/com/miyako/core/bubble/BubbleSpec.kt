package com.miyako.core.bubble

import android.view.View
import android.view.ViewGroup

/**
 * 气泡配置，通过 [showBubble] 的 DSL 构建。
 *
 * 气泡**不提供任何默认 UI**（无背景、圆角、边距、阴影、波纹），
 * 容器只负责 AddView，外观完全由调用方在内容里自绘。
 * 内容必须通过 [content] 提供，否则 [android.app.Activity.showBubble] 会抛异常。
 */
class BubbleSpec {

  /** 出现位置 */
  var position: BubblePosition = BubblePosition.TOP

  /**
   * 自动消失时长（毫秒），默认 [DEFAULT_DURATION_MS]。
   * 为 null 时气泡常驻，直到手动 dismiss 或 [dismissAllBubbles]。
   */
  var durationMs: Long? = DEFAULT_DURATION_MS

  /**
   * 距容器边缘的额外边距（px），null 表示不追加。
   * 仅对 [BubblePosition.TOP]（topMargin）/ [BubblePosition.BOTTOM]（bottomMargin）生效，
   * [BubblePosition.CENTER] 忽略。
   *
   * 气泡**不处理 SystemUI**（状态栏/导航栏避让交由外部处理）：
   * 需要避开系统栏时，调用方自行获取系统栏高度并计入本边距。
   */
  var margin: Int? = null

  /** 点击回调，点击后气泡先回调再消失 */
  var onClick: (() -> Unit)? = null

  /** 消失回调，点击/自动/滑动/手动关闭后都会触发 */
  var onDismiss: (() -> Unit)? = null

  /**
   * 自定义内容视图，返回的 View 会作为气泡内容填充，边距由调用方自行控制。
   */
  var content: ((ViewGroup) -> View)? = null

  companion object {
    /** 默认自动消失时长：3 秒 */
    const val DEFAULT_DURATION_MS = 3000L
  }
}
