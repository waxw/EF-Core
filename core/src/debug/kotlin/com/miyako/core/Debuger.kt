package com.miyako.core

import android.util.Log
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <T> T?.debugLog(tag: String = "miyako"): T? {
  Log.d(tag, this?.toString() ?: "null")
  return this
}

inline fun <T> T?.debug(crossinline action: T?.() -> Unit): T? {
  this?.action()
  return this
}

@OptIn(ExperimentalContracts::class)
inline fun <R> measureExecuteMillis(tag: String, crossinline block: () -> R): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val start = System.currentTimeMillis()
  return block().apply {
    "execute $tag: ${System.currentTimeMillis() - start}ms".debugLog()
  }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> measureExecuteNano(tag: String, crossinline block: () -> R): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val start = System.nanoTime()
  return block().apply {
    "execute $tag: ${System.nanoTime() - start}ns".debugLog()
  }
}


@OptIn(ExperimentalContracts::class)
suspend inline fun <R> measureSuspendMillis(tag: String, crossinline block: suspend () -> R): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val start = System.currentTimeMillis()
  return block().apply {
    "suspend $tag: ${System.currentTimeMillis() - start}ms".debugLog()
  }
}

@OptIn(ExperimentalContracts::class)
suspend inline fun <R> measureSuspendNano(tag: String, crossinline block: suspend () -> R): R {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val start = System.nanoTime()
  return block().apply {
    "suspend $tag: ${System.nanoTime() - start}ns".debugLog()
  }
}
