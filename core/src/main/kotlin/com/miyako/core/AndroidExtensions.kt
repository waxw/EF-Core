package com.miyako.core

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import com.miyako.core.ksp.InflateViewBinding
import java.util.Locale

fun View.roundCorners(radius: Int) {
  this.outlineProvider =
    object : ViewOutlineProvider() {
      override fun getOutline(
        view: View,
        outline: Outline
      ) {
        outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
      }
    }
  this.clipToOutline = true
}

fun View.roundCorners(radiusX: Int, radiusY: Int) {
  clipPath { view, path ->
    path.addRoundRect(
      RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
      radiusX.toFloat(), radiusY.toFloat(),
      Path.Direction.CW
    )
  }
}

fun View.roundCorners(radiusArr: FloatArray) {
  if (radiusArr.size < 8) return
  clipPath { view, path ->
    path.addRoundRect(
      RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
      radiusArr,
      Path.Direction.CW
    )
  }
}

/**
 * 只有 API 33 才生效
 */
fun View.clipPath(action: (View, Path) -> Unit) {
  this.outlineProvider = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
      val path = Path()
      action(view, path)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        outline.setPath(path)
      } else {
        outline.setConvexPath(path)
      }
    }
  }
  this.clipToOutline = true
}

fun View.roundBorder(
  radius: Int,
  width: Int,
  borderColor: Int
) {
  background =
    GradientDrawable().apply {
      setColor(Color.TRANSPARENT)
      setStroke(width, borderColor)
      cornerRadius = radius.toFloat()
    }
}

inline fun View.runWhenReady(crossinline action: (View) -> Unit) {
  val view = this
  this.viewTreeObserver.addOnGlobalLayoutListener(
    object : ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        action.invoke(view)
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
      }
    }
  )
}

/**
 * Number 类型 dp 换 px，按照系统的逻辑，四舍五入和非法值。
 *
 * 参考 [android.util.TypedValue.complexToDimensionPixelSize]
 */
val Number.dp: Int
  get() {
    val value = this.toFloat()
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value,
      Resources.getSystem().displayMetrics
    ).let {
      val res: Int = (if (it >= 0f) it + 0.5f else it - 0.5f).toInt()
      return when {
        res != 0 -> res
        value == 0f -> 0
        value > 0f -> 1
        else -> -1
      }
    }
  }

val Number.sp: Int
  get() {
    val value = this.toFloat()
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_SP,
      value,
      Resources.getSystem().displayMetrics
    ).let {
      val res: Int = (if (it >= 0f) it + 0.5f else it - 0.5f).toInt()
      return when {
        res != 0 -> res
        value == 0f -> 0
        value > 0f -> 1
        else -> -1
      }
    }
  }

fun Context.changeLanguage(code: String?): Context? {
  return try {
    val locale = getLocaleByCode(code)
    "change local: $locale".debugLog()
    if (locale == null) return null
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    // api 17 废弃，仍可以生效
    resources.updateConfiguration(configuration, resources.displayMetrics)
    createConfigurationContext(configuration)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

private fun getLocaleByCode(code: String?): Locale? {
  // 跟随系统。
  if (code.isNullOrEmpty()) return Locale.getDefault()
  val list = code.split(",")
  if (list.size < 2) return null
  return if (list[1].trim().isEmpty()) {
    Locale(list[0])
  } else {
    Locale(list[0], list[1])
  }
}
