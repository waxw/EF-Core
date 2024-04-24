package com.miyako.core

import android.util.Log

inline fun <T> T?.debugLog(tag: String = "miyako"): T? {
  Log.d(tag, this?.toString() ?: "null")
  return this
}

inline fun <T> T?.debug(crossinline action: T?.() -> Unit): T? {
  this?.action()
  return this
}

inline fun <R> measureExecuteMillis(tag: String, crossinline block: () -> R): R {
  val start = System.currentTimeMillis()
  return block().apply {
    "execute $tag: ${System.currentTimeMillis() - start}ms".debugLog()
  }
}

inline fun <R> measureExecuteNano(tag: String, crossinline block: () -> R): R {
  val start = System.nanoTime()
  return block().apply {
    "execute $tag: ${System.nanoTime() - start}ns".debugLog()
  }
}


suspend inline fun <R> measureSuspendMillis(tag: String, crossinline block: suspend () -> R): R {
  val start = System.currentTimeMillis()
  return block().apply {
    "suspend $tag: ${System.currentTimeMillis() - start}ms".debugLog()
  }
}

suspend inline fun <R> measureSuspendNano(tag: String, crossinline block: suspend () -> R): R {
  val start = System.nanoTime()
  return block().apply {
    "suspend $tag: ${System.nanoTime() - start}ns".debugLog()
  }
}
