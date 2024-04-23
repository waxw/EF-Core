package com.miyako.core

inline fun <T> T?.debugLog(tag: String = "miyako") = this

inline fun <T> T?.debug(action: T?.() -> Unit) = this

inline fun <R> measureExecuteMillis(tag: String, block: () -> R): R = block()

inline fun <R> measureExecuteNano(tag: String, block: () -> R): R = block()
