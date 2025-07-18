package com.miyako.core


inline fun <T> T.debugLog(tag: String = "miyako"): T {
  println("$tag -> ${this?.toString() ?: "null"}")
  return this
}
