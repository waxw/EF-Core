package com.miyako.core.task

data class ExecutionAttempt<T>(
  val metrics: ExecutionMetrics,
  val data: T,
) {
  @Deprecated("Use metrics", ReplaceWith("metrics"))
  val executionMetrics: ExecutionMetrics get() = metrics
}
