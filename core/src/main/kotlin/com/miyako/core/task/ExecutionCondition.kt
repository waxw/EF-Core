package com.miyako.core.task

import kotlin.reflect.KClass

@PublishedApi
internal class StopWhenCondition<T : Any>(
  private val type: KClass<T>,
  private val predicate: suspend (ExecutionAttempt<T>) -> Boolean,
) {
  @Suppress("UNCHECKED_CAST")
  suspend fun matches(data: Any?, attempt: ExecutionAttempt<*>): Boolean {
    return type.isInstance(data) && predicate(attempt as ExecutionAttempt<T>)
  }
}

@PublishedApi
internal class AbortCondition<T : Throwable>(
  private val type: KClass<T>,
  private val predicate: (T) -> Boolean,
) {
  fun accepts(throwable: Throwable): Boolean = type.isInstance(throwable)

  @Suppress("UNCHECKED_CAST")
  fun matches(throwable: Throwable): Boolean = predicate(throwable as T)
}

@PublishedApi
internal class LegacyFailCondition<T : Throwable>(
  private val type: KClass<T>,
  private val predicate: suspend (ExecutionAttempt<T>) -> Boolean,
) {
  fun accepts(throwable: Throwable): Boolean = type.isInstance(throwable)

  @Suppress("UNCHECKED_CAST")
  suspend fun matches(attempt: ExecutionAttempt<*>): Boolean {
    return predicate(attempt as ExecutionAttempt<T>)
  }
}
