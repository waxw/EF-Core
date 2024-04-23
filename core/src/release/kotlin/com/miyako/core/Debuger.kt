package com.miyako.core

inline fun <T> T?.debugLog(tag: String = "miyako") = this

inline fun <T> T?.debug(crossinline action: T?.() -> Unit) = this

inline fun <R> measureExecuteMillis(tag: String, crossinline block: () -> R): R = block()

inline fun <R> measureExecuteNano(tag: String, crossinline block: () -> R): R = block()
