package com.miyako.core.task

internal enum class ExecutionMode {
  RETRY,
  POLL,
}

internal data class TaskExecutionSpec<T>(
  val mode: ExecutionMode,
  val config: ExecutionConfig,
  val supplier: suspend () -> T,
  val completionConditions: List<CompletionCondition<*>>,
  val abortConditions: List<AbortCondition<out Throwable>>,
  val retryConditions: List<RetryCondition<out Throwable>>,
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
)
