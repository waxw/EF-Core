package com.miyako.core

inline fun <T> T?.debugLog(tag: String = "miyako") = this

inline fun <T> T?.debug(action: T?.() -> Unit) = this
