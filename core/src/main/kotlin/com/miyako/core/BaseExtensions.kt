package com.miyako.core

import java.util.Locale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> T?.orInit(block: () -> T): T {
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

inline fun <T> T.thenIf(condition: Boolean, block: T.() -> T): T {
  return if (condition) block() else this
}

inline fun <T> T.thenNull(block: () -> T): T {
  return this ?: block()
}

inline fun <T> T.thenNotNull(block: T.() -> T): T {
  return if (this != null) block() else this
}

inline fun <reified T> Any?.cast() = this as? T

inline fun <reified T> Any?.cast(onMatch: T.() -> Unit) {
  if (this is T) onMatch(this)
}

inline fun <reified T, R> Any?.cast(onNotMatch: (Any?) -> R, onMatch: T.() -> R): R {
  return if (this is T) onMatch(this) else onNotMatch(this)
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

val Any.hex: String
  get() = "${javaClass.simpleName}@0x${this.hashCode().toString(16).uppercase(Locale.ENGLISH).padStart(8, '0')}"
