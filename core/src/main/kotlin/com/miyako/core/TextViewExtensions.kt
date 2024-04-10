package com.miyako.core

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.text.set

fun TextView.spannable(): SpannableString = this.text as? SpannableString ?: SpannableString.valueOf(this.text)

fun SpannableString.background(target: String, @ColorInt color: Int): SpannableString {
  setSpan(target, BackgroundColorSpan(color))
  return this
}

fun SpannableString.highlight(target: String, @ColorInt color: Int): SpannableString {
  setSpan(target, ForegroundColorSpan(color))
  return this
}

fun SpannableString.clickable(
  target: String,
  @ColorInt color: Int,
  onClick: (View) -> Unit
): SpannableString {
  val click = object : ClickableSpan() {
    override fun onClick(view: View) {
      onClick.invoke(view)
    }

    override fun updateDrawState(ds: TextPaint) {
      ds.color = color
    }
  }
  setSpan(target, click)
  return this
}

fun SpannableString.underline(target: String): SpannableString {
  setSpan(target, UnderlineSpan())
  return this
}

fun SpannableString.normal(target: String): SpannableString {
  setSpan(target, StyleSpan(Typeface.NORMAL))
  return this
}

fun SpannableString.bold(target: String): SpannableString {
  setSpan(target, StyleSpan(Typeface.BOLD))
  return this
}

fun SpannableString.italic(target: String): SpannableString {
  setSpan(target, StyleSpan(Typeface.ITALIC))
  return this
}

fun SpannableString.boldItalic(target: String): SpannableString {
  setSpan(target, StyleSpan(Typeface.BOLD_ITALIC))
  return this
}

@RequiresApi(Build.VERSION_CODES.P)
fun SpannableString.typeface(target: String, typeface: Typeface): SpannableString {
  setSpan(target, TypefaceSpan(typeface))
  return this
}

fun SpannableString.relateSize(target: String, proportion: Float): SpannableString {
  setSpan(target, RelativeSizeSpan(proportion))
  return this
}

fun SpannableString.strikethrough(target: String): SpannableString {
  setSpan(target, StrikethroughSpan())
  return this
}

fun SpannableString.setSpan(target: String, span: Any) {
  val start = this.indexOf(target)
  if (target.isNotEmpty() && start != -1) {
    this[start, start + target.length] = span
  }
}
