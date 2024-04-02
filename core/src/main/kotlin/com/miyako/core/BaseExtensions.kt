package com.miyako.core

import java.util.Locale

inline fun <T> T?.init(block: () -> T): T {
  return this ?: block()
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

val Any.hex: String
  get() = "${javaClass.simpleName}@0x${this.hashCode().toString(16).uppercase(Locale.ENGLISH).padStart(8, '0')}"
