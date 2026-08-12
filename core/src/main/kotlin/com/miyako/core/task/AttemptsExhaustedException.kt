package com.miyako.core.task

class AttemptsExhaustedException(
  val lastThrowable: Throwable?,
  val metrics: ExecutionMetrics,
) : Exception("Task attempts exhausted after ${metrics.executionCount} attempts", lastThrowable)
