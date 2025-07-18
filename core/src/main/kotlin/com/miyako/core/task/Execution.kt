package com.miyako.core.task


data class ExecutionConfig(
  /**
   * 初始延迟（首次执行前的等待时间，单位：毫秒）
   */
  val initialDelayMs: Long = 0L,

  /**
   * 每次重试之间的时间间隔（单位：毫秒）
   */
  val retryIntervalMs: Long = 0L,

  /**
   * 最大执行次数（含首次执行）
   */
  val maxAttempts: Int = 1,

  /**
   * 总超时时间（单位：毫秒），为 0 表示无限制
   */
  val totalTimeoutMs: Long = 0L,
) {
  init {
    require(maxAttempts >= 1) { "maxRetryAttempts must be >= 1" }
    require(initialDelayMs >= 0) { "initialDelayMs must be >= 0" }
    require(retryIntervalMs >= 0) { "retryIntervalMs must be >= 0" }
    require(totalTimeoutMs >= 0) { "totalTimeoutMs must be >= 0" }
  }

  override fun toString(): String {
    return "Config(delay: ${initialDelayMs}ms, interval: ${retryIntervalMs}ms, max: $maxAttempts, timeout: ${totalTimeoutMs}ms)"
  }
}

data class ExecutionMetrics(
  /**
   * 当前是第几次执行（从 1 开始）
   */
  val executionCount: Int,
  /**
   * 当前轮开始时间
   */
  val attemptStartTime: Long,
  /**
   * 本轮执行耗时
   */
  val attemptDuration: Long,
  /**
   * 从流程开始至今的总耗时
   */
  val totalDuration: Long,
) {
  override fun toString(): String {
    return "Metrics(cnt: $executionCount, startTime: $attemptStartTime, duration: ${attemptDuration}ms, total: ${totalDuration}ms)"
  }
}

data class ExecutionResult<T>(
  val executionMetrics: ExecutionMetrics,
  val data: T
)
