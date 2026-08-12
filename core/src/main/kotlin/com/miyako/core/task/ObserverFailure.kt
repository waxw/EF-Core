package com.miyako.core.task

enum class ObserverSource {
  ON_ATTEMPT_SUCCESS,
  ON_ATTEMPT_FAILURE,
  ON_SUCCESS,
  ON_FAILURE,
  ON_EXHAUSTED,
  ON_TIMEOUT,
  ON_CANCEL,
  ON_FINISHED,
}

data class ObserverFailure(
  val source: ObserverSource,
  val throwable: Throwable,
  val metrics: ExecutionMetrics,
)
