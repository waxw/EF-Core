package com.miyako.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchMain(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = launch(Dispatchers.Main + context, start, block)

fun CoroutineScope.launchIO(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = launch(Dispatchers.IO + context, start, block)

fun CoroutineScope.launchDefault(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = launch(Dispatchers.Default + context, start, block)

fun CoroutineScope.launchUnconfined(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = launch(Dispatchers.Unconfined + context, start, block)

fun CoroutineScope.asyncMain(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = async(Dispatchers.Main + context, start, block)

fun CoroutineScope.asyncIO(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = async(Dispatchers.IO + context, start, block)

fun CoroutineScope.asyncDefault(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = async(Dispatchers.Default + context, start, block)

fun CoroutineScope.asyncUnconfined(
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
) = async(Dispatchers.Unconfined + context, start, block)

suspend inline fun <T> CoroutineScope.withMain(
  context: CoroutineContext = EmptyCoroutineContext,
  noinline block: suspend CoroutineScope.() -> T
) = withContext(Dispatchers.Main + context, block)

suspend inline fun <T> CoroutineScope.withIO(
  context: CoroutineContext = EmptyCoroutineContext,
  noinline block: suspend CoroutineScope.() -> T
) = withContext(Dispatchers.IO + context, block)

suspend inline fun <T> CoroutineScope.withDefault(
  context: CoroutineContext = EmptyCoroutineContext,
  noinline block: suspend CoroutineScope.() -> T
) = withContext(Dispatchers.Default + context, block)

suspend inline fun <T> CoroutineScope.withUnconfined(
  context: CoroutineContext = EmptyCoroutineContext,
  noinline block: suspend CoroutineScope.() -> T
) = withContext(Dispatchers.Unconfined + context, block)
