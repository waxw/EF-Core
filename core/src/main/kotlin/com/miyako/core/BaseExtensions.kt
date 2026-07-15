package com.miyako.core

import java.util.Locale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> T?.init(block: () -> T): T {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }
  return this ?: block()
}

@OptIn(ExperimentalContracts::class)
inline fun Boolean?.ifTrue(block: () -> Unit): Boolean? {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }
  if (this == true) block()
  return this
}

@OptIn(ExperimentalContracts::class)
inline fun Boolean?.ifFalse(block: () -> Unit): Boolean? {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }
  if (this == false) block()
  return this
}

inline fun <T> T.withNull(block: () -> Unit): Boolean {
  if (this == null) block()
  return this == null
}

inline fun <T> T.withNotNull(block: (T) -> Unit): Boolean {
  if (this != null) block(this)
  return this != null
}

inline fun <reified T> Any?.instance(onNotMatch: (Any?) -> Unit = {}, onMatch: (T) -> Unit) {
  if (this is T) onMatch(this) else onNotMatch(this)
}

inline fun <reified T, R> Any?.instance(onNotMatch: (Any?) -> R, onMatch: (T) -> R): R {
  return if (this is T) onMatch(this) else onNotMatch(this)
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

val Any.hex: String
  get() = "${javaClass.simpleName}@0x${this.hashCode().toString(16).uppercase(Locale.ENGLISH).padStart(8, '0')}"
