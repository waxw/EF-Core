package com.miyako.core

inline fun <T> T?.miyako(action: T?.() -> Unit): T? {
  this?.action()
  return this
}

fun String.ef(): String {
  return "$this@ef"
}
