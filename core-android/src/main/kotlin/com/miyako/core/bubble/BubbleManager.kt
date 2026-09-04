package com.miyako.core.bubble

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.ref.WeakReference

/**
 * 单个 Activity 的气泡管理器：承载 overlay 容器、计算堆叠偏移并负责生命周期清理。
 * 同一个 Activity 通过 [android.app.Activity.bubbleManager] 共享同一个实例，
 * 堆叠状态（多气泡上下排列）跨多次 [show] 调用保持一致。
 *
 * 通过 [Application.ActivityLifecycleCallbacks] 监听 Activity 销毁，
 * 因此对任意 [Activity]（不要求 ComponentActivity）都可用。
 *
 * 不处理 SystemUI：状态栏/导航栏避让由调用方自行通过 [BubbleSpec.margin] 处理。
 */
class BubbleManager internal constructor(activity: Activity) {

  private val activityRef = WeakReference(activity)
  private var overlay: FrameLayout? = null

  /** 当前展示中的气泡，index 0 为最新（栈顶） */
  private val bubbles = mutableListOf<BubbleView>()

  private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
    override fun onActivityDestroyed(destroyed: Activity) {
      if (destroyed === activityRef.get()) {
        cleanup()
        destroyed.application.unregisterActivityLifecycleCallbacks(this)
      }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
  }

  init {
    activity.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
  }

  /** 当前展示中的气泡数量 */
  val activeCount: Int get() = bubbles.size

  /** DSL 入口，等价于 [show] 配合 [BubbleSpec] 构建器 */
  fun show(build: BubbleSpec.() -> Unit): BubbleView = show(BubbleSpec().apply(build))

  /**
   * 展示一个气泡。新气泡位于栈顶，已有气泡平滑让位。
   * 返回的 [BubbleView] 可用于手动 [BubbleView.dismiss]。
   */
  fun show(spec: BubbleSpec): BubbleView {
    checkMainThread()
    val activity = activityRef.get() ?: throw IllegalStateException("Activity 已被回收")
    if (activity.isDestroyed || activity.isFinishing) {
      throw IllegalStateException("Activity 已销毁或正在结束，无法展示气泡")
    }
    val host = overlay(activity)
    val bubble = BubbleView(activity).apply { bind(spec) }
    host.addView(
      bubble,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = when (spec.position) {
          BubblePosition.TOP -> Gravity.TOP
          BubblePosition.BOTTOM -> Gravity.BOTTOM
          BubblePosition.CENTER -> Gravity.CENTER_VERTICAL
        }
        // SystemUI 避让交由外部处理：边距完全由调用方通过 spec.margin 传入
        if (spec.position == BubblePosition.TOP) {
          topMargin = spec.margin ?: 0
        } else if (spec.position == BubblePosition.BOTTOM) {
          bottomMargin = spec.margin ?: 0
        }
      }
    )
    bubble.onDismissFinished = { onBubbleDismissed(it) }
    bubbles.add(0, bubble)
    bubble.alpha = 0f
    // 等宿主完成布局后按实测高度入场；高度未就绪时等首次布局后再排，避免按 0 高度堆叠导致重叠
    host.post { if (bubble.isAttachedToWindow) placeWhenMeasured(bubble) }
    return bubble
  }

  /** 关闭当前 Activity 的全部气泡 */
  fun dismissAll() {
    val list = bubbles.toList()
    bubbles.clear()
    list.forEach { it.dismiss() }
  }

  private fun cleanup() {
    overlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
    overlay = null
    bubbles.clear()
  }

  private fun overlay(activity: Activity): FrameLayout {
    overlay?.let { return it }
    val decor = activity.window.decorView as? ViewGroup
      ?: throw IllegalStateException("无法获取窗口根视图")
    val host = FrameLayout(activity).apply {
      // 透明且不可点击：容器外的触摸事件会穿透到下方内容
      isClickable = false
      isFocusable = false
    }
    decor.addView(
      host,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    )
    overlay = host
    return host
  }

  /** 新气泡入场：只重排同 position 的气泡，其余位置互不干扰 */
  private fun placeBubble(newBubble: BubbleView) {
    val host = overlay ?: return
    val position = newBubble.spec?.position ?: BubblePosition.TOP
    val (offsets, blockHeight) = computeOffsets()[position] ?: return
    for ((bubble, offset) in offsets) {
      val target = actualTranslationY(bubble, offset, blockHeight)
      if (bubble === newBubble) {
        // TOP/CENTER 从上方滑入，BOTTOM 从下方滑入
        val from = if (position == BubblePosition.BOTTOM) {
          host.height.toFloat()
        } else {
          -host.height.toFloat()
        }
        bubble.animateShow(from, target)
      } else {
        bubble.animateTo(target)
      }
    }
  }

  /** 高度未就绪时等待首次布局，确保按实测高度堆叠（不重叠） */
  private fun placeWhenMeasured(bubble: BubbleView) {
    if (!bubble.isAttachedToWindow) return
    if (bubble.height > 0) {
      placeBubble(bubble)
      return
    }
    bubble.addOnLayoutChangeListener(
      object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
          v: View,
          left: Int,
          top: Int,
          right: Int,
          bottom: Int,
          oldLeft: Int,
          oldTop: Int,
          oldRight: Int,
          oldBottom: Int
        ) {
          if (bubble.height > 0) {
            bubble.removeOnLayoutChangeListener(this)
            placeBubble(bubble)
          }
        }
      }
    )
  }

  /** 气泡消失完成后：移除视图并重新对齐同 position 的剩余气泡 */
  private fun onBubbleDismissed(bubble: BubbleView) {
    bubbles.remove(bubble)
    val host = overlay
    if (host != null && bubble.parent === host) {
      host.removeView(bubble)
    }
    val position = bubble.spec?.position ?: BubblePosition.TOP
    val (offsets, blockHeight) = computeOffsets()[position] ?: return
    for ((b, offset) in offsets) {
      b.animateTo(actualTranslationY(b, offset, blockHeight))
    }
  }

  /**
   * 按 [BubblePosition] 分组计算堆叠偏移，**不同位置单独累计，互不干扰**：
   * offset_i = 同位置上方所有 BubbleView 实测高度之和，严格按高度偏移、互不重叠。
   * 返回 Map：位置 -> (每个气泡的累积偏移, 气泡块总高（最底气泡底部）)。
   */
  private fun computeOffsets(): Map<BubblePosition, Pair<List<Pair<BubbleView, Float>>, Float>> {
    return bubbles.groupBy { it.spec?.position ?: BubblePosition.TOP }
      .mapValues { (_, group) ->
        var acc = 0f
        val offsets = group.map { bubble ->
          val offset = acc
          acc += bubble.height.toFloat()
          bubble to offset
        }
        val blockHeight = if (offsets.isEmpty()) 0f else {
          val last = offsets.last()
          last.first.height + last.second
        }
        offsets to blockHeight
      }
  }

  /**
   * 实际 translationY：
   * TOP 从顶部向下为正；BOTTOM 从底部向上为负；CENTER 气泡块整体垂直居中。
   */
  private fun actualTranslationY(bubble: BubbleView, offset: Float, blockHeight: Float): Float {
    return when (bubble.spec?.position ?: BubblePosition.TOP) {
      BubblePosition.TOP -> offset
      BubblePosition.BOTTOM -> -offset
      // 每个气泡中心 = 块顶 + 偏移 + 自身半高，块顶 = 容器中心 - 块半高
      BubblePosition.CENTER -> offset + bubble.height / 2f - blockHeight / 2f
    }
  }

  private fun checkMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) { "showBubble 必须在主线程调用" }
  }
}
