package com.miyako.core.task

import kotlin.reflect.KClass

interface ExecutionCondition {
  suspend fun matches(data: Any, result: ExecutionResult<*>): Boolean
}

class StopWhenCondition<T : Any>(
  private val type: KClass<T>,
  val execution: suspend (ExecutionResult<T>) -> Boolean
) : ExecutionCondition {
  @Suppress("UNCHECKED_CAST")
  override suspend fun matches(data: Any, result: ExecutionResult<*>): Boolean {
    return if (type.isInstance(data)) {
      execution(result as ExecutionResult<T>)
    } else false
  }
}

class FailWhenCondition<T : Throwable>(
  private val type: KClass<T>,
  val execution: suspend (ExecutionResult<T>) -> Boolean
) : ExecutionCondition {
  @Suppress("UNCHECKED_CAST")
  override suspend fun matches(data: Any, result: ExecutionResult<*>): Boolean {
    return if (type.isInstance(data)) {
      execution(result as ExecutionResult<T>)
    } else false
  }
}
