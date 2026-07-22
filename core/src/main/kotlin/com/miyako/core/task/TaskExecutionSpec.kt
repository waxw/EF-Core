package com.miyako.core.task

internal data class TaskExecutionSpec<T>(
  val config: ExecutionConfig,
  val supplier: suspend () -> T,
  val stopWhenExecutions: List<StopWhenCondition<*>>,
  val failWhenExecutions: List<FailWhenCondition<out Throwable>>,
  val result: (suspend (ExecutionResult<T>) -> Unit)?,
  val throwable: (suspend (ExecutionResult<Throwable>) -> Unit)?,
  val failWhenFallback: (suspend (ExecutionResult<Throwable>) -> Boolean)?,
  val beforeRetry: (suspend (ExecutionResult<Unit>) -> Unit)?,
  val exhausted: (suspend (ExecutionResult<Unit>) -> Unit)?,
  val timeout: (suspend (ExecutionResult<Unit>) -> Unit)?,
  val cancel: (suspend (ExecutionResult<Unit>) -> Unit)?,
  val finally: (suspend (ExecutionResult<Unit>) -> Unit)?,
)
