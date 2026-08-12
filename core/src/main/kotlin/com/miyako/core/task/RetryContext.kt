package com.miyako.core.task

data class RetryContext(
  val nextAttempt: Int,
  val previousFailure: Throwable?,
  val metrics: ExecutionMetrics,
) {
  @Deprecated("Use metrics", ReplaceWith("metrics"))
  val executionMetrics: ExecutionMetrics get() = metrics

  @Deprecated("beforeRetry now receives RetryContext", ReplaceWith("Unit"))
  val data: Unit get() = Unit
}
