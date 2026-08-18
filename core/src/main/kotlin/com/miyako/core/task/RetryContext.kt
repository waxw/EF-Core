package com.miyako.core.task

data class RetryContext(
  val nextAttempt: Int,
  val previousFailure: Throwable?,
  val metrics: ExecutionMetrics
)
