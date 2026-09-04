package com.miyako.compose.bubble

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.miyako.core.bubble.BubbleSpec

/**
 * Compose 版气泡配置：字段与 core-ui 的 [BubbleSpec] 完全一致，
 * 仅 [content] 由 View 内容改为 Compose composable。
 *
 * [showBubble] 会通过 [toBubbleSpec] 把它转成 core-ui 的 [BubbleSpec]，
 * 用 [ComposeView] 承载 composable 内容，堆叠/动画/手势/生命周期等
 * 逻辑完全复用 core-ui 的 [com.miyako.core.bubble.BubbleManager]，
 * 本模块**不重复实现任何气泡逻辑**。
 */
class ComposeBubbleSpec {

  /** 出现位置 */
  var position: BubblePosition = BubblePosition.TOP

  /**
   * 自动消失时长（毫秒），默认 [BubbleSpec.DEFAULT_DURATION_MS]。
   * 为 null 时气泡常驻，直到手动 dismiss 或 [dismissAllBubbles]。
   */
  var durationMs: Long? = BubbleSpec.DEFAULT_DURATION_MS

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
   * 自定义 Compose 内容。返回的 composable 会作为气泡内容填充。
   * 必须提供，否则 [showBubble] 会抛异常。
   */
  var content: (@Composable () -> Unit)? = null

  /** 转换为 core-ui 的 [BubbleSpec]，内容用 [ComposeView] 承载 */
  internal fun toBubbleSpec(): BubbleSpec = BubbleSpec().apply {
    // apply 的隐式接收者是 BubbleSpec，外层 ComposeBubbleSpec 属性需显式限定
    position = this@ComposeBubbleSpec.position
    durationMs = this@ComposeBubbleSpec.durationMs
    margin = this@ComposeBubbleSpec.margin
    onClick = this@ComposeBubbleSpec.onClick
    onDismiss = this@ComposeBubbleSpec.onDismiss
    val composableContent: @Composable () -> Unit = this@ComposeBubbleSpec.content
      ?: throw IllegalArgumentException("气泡内容不能为空，请设置 content")
    this.content = { parent ->
      ComposeView(parent.context).apply {
        setContent { composableContent() }
      }
    }
  }
}
