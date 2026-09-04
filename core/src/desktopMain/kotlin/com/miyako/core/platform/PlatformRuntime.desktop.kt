package com.miyako.core.platform

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun platformClassSimpleName(value: Any): String = value.javaClass.simpleName
