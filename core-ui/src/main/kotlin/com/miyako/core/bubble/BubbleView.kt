package com.miyako.core.bubble

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 单个气泡容器：只负责承载调用方提供的内容（View）并处理交互与动画，
 * **不负责任何 UI 外观设置**（无背景、圆角、边距、阴影、波纹），样式完全由调用方在内容里自绘。
 *
 * 由 [BubbleManager] 创建并注入 [BubbleSpec] 与消失完成回调，
 * 堆叠偏移由 manager 计算后通过 [animateShow]/[animateTo] 驱动。
 */
class BubbleView(context: Context) : FrameLayout(context) {

  internal var spec: BubbleSpec? = null

  /** 消失动画结束后回调（由 BubbleManager 注入，用于重新堆叠剩余气泡） */
  internal var onDismissFinished: ((BubbleView) -> Unit)? = null

  /** 静止时的 translationY（堆叠偏移），水平滑动时不改变 */
  private var baseTranslationY = 0f

  private var downX = 0f
  private var downY = 0f
  private var dragging = false
  private var moved = false
  private var dismissing = false

  private val autoDismissRunnable = Runnable { dismiss() }

  init {
    // 仅用于接收触摸事件（滑动/点击交互），非外观设置
    isClickable = true
  }

  /** 根据 [spec] 构建并添加内容 */
  fun bind(spec: BubbleSpec) {
    this.spec = spec
    removeAllViews()
    val content = spec.content
    require(content != null) { "气泡内容不能为空，请设置 content" }
    addView(
      content(this),
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    )
  }

  /**
   * 入场动画：从 [fromOffset] 滑入到 [toOffset]（均为实际 translationY）。
   */
  fun animateShow(fromOffset: Float, toOffset: Float) {
    if (dismissing) return
    baseTranslationY = toOffset
    translationY = fromOffset
    alpha = 0f
    visibility = View.VISIBLE
    animate()
      .translationY(toOffset)
      .alpha(1f)
      .setDuration(SHOW_DURATION_MS)
      .setInterpolator(DecelerateInterpolator())
      .start()
    scheduleAutoDismiss()
  }

  /** 平滑移动到新的堆叠偏移 */
  fun animateTo(toOffset: Float) {
    baseTranslationY = toOffset
    if (translationY == toOffset) return
    animate().translationY(toOffset).setDuration(REPOSITION_DURATION_MS).start()
  }

  /**
   * 关闭气泡。自动消失、点击、滑动、手动关闭统一走这里，
   * 动画结束后触发 [BubbleSpec.onDismiss] 并回调 [onDismissFinished] 由 manager 重新对齐剩余气泡。
   */
  fun dismiss() {
    if (dismissing) return
    dismissing = true
    stopAutoDismiss()
    if (!isAttachedToWindow) {
      finishDismiss()
      return
    }
    animate().cancel()
    // TOP/CENTER 向上滑出，BOTTOM 向下滑出
    val direction = if ((spec?.position ?: BubblePosition.TOP) == BubblePosition.BOTTOM) 1f else -1f
    val exitY = direction * height
    animate()
      .translationX(0f)
      .translationY(baseTranslationY + exitY)
      .alpha(0f)
      .setDuration(DISMISS_DURATION_MS)
      .setInterpolator(AccelerateInterpolator())
      .setListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            finishDismiss()
          }
        }
      )
      .start()
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        if (dismissing) return true
        downX = event.rawX
        downY = event.rawY
        dragging = false
        moved = false
        stopAutoDismiss()
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        if (dismissing) return true
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        val dx = event.rawX - downX
        val dy = event.rawY - downY
        // 只能左右滑动：仅水平位移超过阈值进入拖拽，垂直方向不跟随
        if (!dragging && abs(dx) > slop) {
          dragging = true
          parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (abs(dx) > slop || abs(dy) > slop) moved = true
        if (dragging) {
          translationX = dx
          return true
        }
      }
      MotionEvent.ACTION_UP -> {
        if (dismissing) return true
        parent?.requestDisallowInterceptTouchEvent(false)
        if (dragging) {
          dragging = false
          if (abs(translationX) > width * SWIPE_DISMISS_RATIO) {
            dismissWithSwipe(translationX)
          } else {
            animate().translationX(0f).setDuration(REPOSITION_DURATION_MS).start()
            scheduleAutoDismiss()
          }
        } else if (moved) {
          // 非水平拖拽（如垂直滑动）：不算点击，也不移动气泡
          scheduleAutoDismiss()
        } else {
          handleTap()
        }
        return true
      }
      MotionEvent.ACTION_CANCEL -> {
        if (dragging) {
          dragging = false
          parent?.requestDisallowInterceptTouchEvent(false)
          animate().translationX(0f).setDuration(REPOSITION_DURATION_MS).start()
        }
        scheduleAutoDismiss()
        return true
      }
    }
    return super.onTouchEvent(event)
  }

  private fun handleTap() {
    performClick()
    val onClick = spec?.onClick
    try {
      onClick?.invoke()
    } finally {
      dismiss()
    }
  }

  /** 水平滑动关闭：沿滑动方向飞出，垂直位置保持不变 */
  private fun dismissWithSwipe(dx: Float) {
    dismissing = true
    stopAutoDismiss()
    animate().cancel()
    animate()
      .translationX(dx * SWIPE_EXIT_SCALE)
      .translationY(baseTranslationY)
      .alpha(0f)
      .setDuration(DISMISS_DURATION_MS)
      .setInterpolator(AccelerateInterpolator())
      .setListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            finishDismiss()
          }
        }
      )
      .start()
  }

  /** 统一收尾：触发 [BubbleSpec.onDismiss]，再交由 manager 移除视图并重新堆叠 */
  private fun finishDismiss() {
    try {
      spec?.onDismiss?.invoke()
    } finally {
      onDismissFinished?.invoke(this)
        ?: run { (parent as? ViewGroup)?.removeView(this) }
    }
  }

  private fun scheduleAutoDismiss() {
    stopAutoDismiss()
    val duration = spec?.durationMs ?: return
    postDelayed(autoDismissRunnable, duration)
  }

  private fun stopAutoDismiss() {
    removeCallbacks(autoDismissRunnable)
  }

  private companion object {
    const val SHOW_DURATION_MS = 300L
    const val DISMISS_DURATION_MS = 200L
    const val REPOSITION_DURATION_MS = 200L
    const val SWIPE_DISMISS_RATIO = 0.35f
    const val SWIPE_EXIT_SCALE = 3f
  }
}
