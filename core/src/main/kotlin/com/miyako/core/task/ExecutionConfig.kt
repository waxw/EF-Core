package com.miyako.core.task

data class ExecutionConfig(
  val initialDelayMs: Long = 0L,
  val retryIntervalMs: Long = 0L,
  val maxAttempts: Int = 1,
  val totalTimeoutMs: Long = 0L,
) {
  init {
    require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    require(initialDelayMs >= 0) { "initialDelayMs must be >= 0" }
    require(retryIntervalMs >= 0) { "retryIntervalMs must be >= 0" }
    require(totalTimeoutMs >= 0) { "totalTimeoutMs must be >= 0" }
  }
}
