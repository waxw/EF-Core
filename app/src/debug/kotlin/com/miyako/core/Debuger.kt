package com.miyako.core

import android.util.Log

inline fun <T> T?.debugLog(tag: String = "miyako"): T? {
  Log.d(tag, this?.toString() ?: "null")
  return this
}

inline fun <T> T?.debug(action: T?.() -> Unit): T? {
  this?.action()
  return this
}
