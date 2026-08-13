package com.miyako.core.task

enum class ExecutionPhase {
  INITIAL_DELAY,
  ATTEMPT,
  RETRY_DELAY,
  BEFORE_RETRY,
  COMPLETION_CONDITION,
}

data class ExecutionMetrics(
  val executionCount: Int,
  val attemptStartTime: Long?,
  val attemptDuration: Long?,
  val totalDuration: Long,
  val phase: ExecutionPhase,
)
