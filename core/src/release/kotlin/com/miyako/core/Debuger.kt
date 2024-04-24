package com.miyako.core

inline fun <T> T?.debugLog(tag: String = "miyako") = this

inline fun <T> T?.debug(crossinline action: T?.() -> Unit) = this

inline fun <R> measureExecuteMillis(tag: String, crossinline block: () -> R): R = block()

inline fun <R> measureExecuteNano(tag: String, crossinline block: () -> R): R = block()

suspend inline fun <R> measureSuspendMillis(tag: String, crossinline block: suspend () -> R): R = block()

suspend inline fun <R> measureSuspendNano(tag: String, crossinline block: suspend () -> R): R = block()
