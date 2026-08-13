package com.miyako.core.task

data class ExecutionAttempt<T>(
  val metrics: ExecutionMetrics,
  val data: T,
)
