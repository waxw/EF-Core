package com.miyako.core.task

sealed interface ExecutionResult<out T> {
  val metrics: ExecutionMetrics

  data class Success<T>(
    val data: T,
    override val metrics: ExecutionMetrics
  ) : ExecutionResult<T>

  data class Failure(
    val throwable: Throwable,
    override val metrics: ExecutionMetrics
  ) : ExecutionResult<Nothing>

  data class Exhausted(
    val lastThrowable: Throwable?,
    override val metrics: ExecutionMetrics
  ) : ExecutionResult<Nothing>

  data class Timeout(
    override val metrics: ExecutionMetrics
  ) : ExecutionResult<Nothing>
}
