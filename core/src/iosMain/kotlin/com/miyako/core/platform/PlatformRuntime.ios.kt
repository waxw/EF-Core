package com.miyako.core.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970() * 1_000).toLong()

internal actual fun platformClassSimpleName(value: Any): String = value::class.simpleName ?: "Unknown"
