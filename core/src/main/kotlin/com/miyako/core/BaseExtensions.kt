package com.miyako.core

import java.util.Locale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> T?.init(block: () -> T): T {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return this ?: block()
}

@OptIn(ExperimentalContracts::class)
inline fun Boolean?.ifTrue(block: () -> Unit): Boolean? {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  if (this == true) block()
  return this
}

@OptIn(ExperimentalContracts::class)
inline fun Boolean?.ifFalse(block: () -> Unit): Boolean? {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  if (this == false) block()
  return this
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

val Any.hex: String
  get() = "${javaClass.simpleName}@0x${this.hashCode().toString(16).uppercase(Locale.ENGLISH).padStart(8, '0')}"
