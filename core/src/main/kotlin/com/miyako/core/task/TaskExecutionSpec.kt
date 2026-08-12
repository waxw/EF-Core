package com.miyako.core.task

internal enum class ExecutionMode {
  RETRY,
  POLL,
}

internal data class TaskExecutionSpec<T>(
  val mode: ExecutionMode,
  val config: ExecutionConfig,
  val supplier: suspend () -> T,
  val stopConditions: List<StopWhenCondition<*>>,
  val abortConditions: List<AbortCondition<out Throwable>>,
  val legacyFailConditions: List<LegacyFailCondition<out Throwable>>,
  val legacyFailFallback: (suspend (ExecutionAttempt<Throwable>) -> Boolean)?,
  val beforeRetry: (suspend (RetryContext) -> Unit)?,
  val onAttemptSuccess: ((ExecutionAttempt<T>) -> Unit)?,
  val onAttemptFailure: ((ExecutionAttempt<Throwable>) -> Unit)?,
  val onSuccess: ((ExecutionResult.Success<T>) -> Unit)?,
  val onFailure: ((ExecutionResult.Failure) -> Unit)?,
  val onExhausted: ((ExecutionResult.Exhausted) -> Unit)?,
  val onTimeout: ((ExecutionResult.Timeout) -> Unit)?,
  val onCancel: ((ExecutionMetrics) -> Unit)?,
  val onFinished: ((ExecutionMetrics) -> Unit)?,
  val onObserverError: ((ObserverFailure) -> Unit)?,
  val legacyAttemptSuccess: (suspend (ExecutionAttempt<T>) -> Unit)?,
  val legacyAttemptFailure: (suspend (ExecutionAttempt<Throwable>) -> Unit)?,
  val legacyExhausted: (suspend (ExecutionAttempt<Unit>) -> Unit)?,
  val legacyTimeout: (suspend (ExecutionAttempt<Unit>) -> Unit)?,
  val legacyCancel: (suspend (ExecutionAttempt<Unit>) -> Unit)?,
  val legacyFinished: (suspend (ExecutionAttempt<Unit>) -> Unit)?,
)
