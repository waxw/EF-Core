package com.miyako.core.task

class TaskTimeoutException(
  val metrics: ExecutionMetrics,
) : Exception("Task timed out during ${metrics.phase}")
