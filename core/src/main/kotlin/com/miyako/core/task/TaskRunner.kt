package com.miyako.core.task

import java.util.concurrent.atomic.AtomicBoolean

/**
 * One-shot structured-concurrency task runner.
 *
 * Use [retry] for tasks where the first successful supplier result is terminal, or [poll] for
 * tasks that require an explicit [stopWhen] completion condition. The runner inherits the caller's
 * coroutine context and never creates or owns a scope, job, or dispatcher.
 */
class TaskRunner<T> private constructor(
  private val mode: ExecutionMode,
  delayMs: Long,
  intervalMs: Long,
  maxAttempts: Int,
  timeoutMs: Long,
  private var supplier: (suspend () -> T)?,
) {
  companion object {
    /** Builds a task that completes on the first successful supplier result. */
    fun <T> retry(
      delayMs: Long = 0,
      intervalMs: Long = 0,
      maxAttempts: Int = 1,
      timeoutMs: Long = 0,
      supplier: suspend () -> T,
    ): TaskRunner<T> {
      return TaskRunner(ExecutionMode.RETRY, delayMs, intervalMs, maxAttempts, timeoutMs, supplier)
    }

    /** Builds a polling task that repeats until a [stopWhen] condition succeeds. */
    fun <T> poll(
      maxAttempts: Int,
      delayMs: Long = 0,
      intervalMs: Long = 0,
      timeoutMs: Long = 0,
      supplier: suspend () -> T,
    ): TaskRunner<T> {
      return TaskRunner(ExecutionMode.POLL, delayMs, intervalMs, maxAttempts, timeoutMs, supplier)
    }
  }

  private val config = ExecutionConfig(delayMs, intervalMs, maxAttempts, timeoutMs)
  private val executed = AtomicBoolean(false)
  private val configurationLock = Any()
  private val stopConditions = mutableListOf<StopWhenCondition<*>>()
  private val abortConditions = mutableListOf<AbortCondition<out Throwable>>()

  private var beforeRetry: (suspend (RetryContext) -> Unit)? = null
  private var onAttemptSuccess: ((ExecutionAttempt<T>) -> Unit)? = null
  private var onAttemptFailure: ((ExecutionAttempt<Throwable>) -> Unit)? = null
  private var onSuccess: ((ExecutionResult.Success<T>) -> Unit)? = null
  private var onFailure: ((ExecutionResult.Failure) -> Unit)? = null
  private var onExhausted: ((ExecutionResult.Exhausted) -> Unit)? = null
  private var onTimeout: ((ExecutionResult.Timeout) -> Unit)? = null
  private var onCancel: ((ExecutionMetrics) -> Unit)? = null
  private var onFinished: ((ExecutionMetrics) -> Unit)? = null
  private var onObserverError: ((ObserverFailure) -> Unit)? = null

  private fun configure(block: () -> Unit): TaskRunner<T> = apply {
    synchronized(configurationLock) {
      check(!executed.get()) { "Cannot modify after execution starts" }
      block()
    }
  }

  inline fun <reified E : Any> stopWhen(
    noinline predicate: suspend (ExecutionAttempt<E>) -> Boolean,
  ): TaskRunner<T> = addStopCondition(StopWhenCondition(E::class, predicate))

  inline fun <reified E : Throwable> abortOn(
    noinline predicate: (E) -> Boolean = { true },
  ): TaskRunner<T> = addAbortCondition(AbortCondition(E::class, predicate))

  fun beforeRetry(block: suspend (RetryContext) -> Unit): TaskRunner<T> = configure {
    beforeRetry = block
  }

  fun onAttemptSuccess(observer: (ExecutionAttempt<T>) -> Unit): TaskRunner<T> = configure {
    onAttemptSuccess = observer
  }

  fun onAttemptFailure(observer: (ExecutionAttempt<Throwable>) -> Unit): TaskRunner<T> = configure {
    onAttemptFailure = observer
  }

  fun onSuccess(observer: (ExecutionResult.Success<T>) -> Unit): TaskRunner<T> = configure {
    onSuccess = observer
  }

  fun onFailure(observer: (ExecutionResult.Failure) -> Unit): TaskRunner<T> = configure {
    onFailure = observer
  }

  fun onExhausted(observer: (ExecutionResult.Exhausted) -> Unit): TaskRunner<T> = configure {
    onExhausted = observer
  }

  fun onTimeout(observer: (ExecutionResult.Timeout) -> Unit): TaskRunner<T> = configure {
    onTimeout = observer
  }

  fun onCancel(observer: (ExecutionMetrics) -> Unit): TaskRunner<T> = configure {
    onCancel = observer
  }

  fun onFinished(observer: (ExecutionMetrics) -> Unit): TaskRunner<T> = configure {
    onFinished = observer
  }

  fun onObserverError(observer: (ObserverFailure) -> Unit): TaskRunner<T> = configure {
    onObserverError = observer
  }

  @PublishedApi
  internal fun <E : Any> addStopCondition(condition: StopWhenCondition<E>): TaskRunner<T> = configure {
    check(mode != ExecutionMode.RETRY) { "stopWhen is only supported by TaskRunner.poll" }
    stopConditions.add(condition)
  }

  @PublishedApi
  internal fun <E : Throwable> addAbortCondition(condition: AbortCondition<E>): TaskRunner<T> = configure {
    abortConditions.add(condition)
  }

  /**
   * Executes this runner and returns its successful data.
   *
   * Failure rethrows its original throwable, exhaustion throws [AttemptsExhaustedException], and
   * the runner total timeout throws [TaskTimeoutException]. External cancellation is rethrown
   * unchanged after cancellation and finished notifications.
   */
  suspend fun execute(): T {
    return when (val result = executeResult()) {
      is ExecutionResult.Success -> result.data
      is ExecutionResult.Failure -> throw result.throwable
      is ExecutionResult.Exhausted -> throw AttemptsExhaustedException(result.lastThrowable, result.metrics)
      is ExecutionResult.Timeout -> throw TaskTimeoutException(result.metrics)
    }
  }

  /** Executes this runner and returns its structured terminal result. */
  suspend fun executeResult(): ExecutionResult<T> {
    val spec = synchronized(configurationLock) {
      check(executed.compareAndSet(false, true)) { "TaskRunner can only be executed once" }
      buildSpec().also { cleanUp() }
    }
    return TaskExecution(spec).execute()
  }

  private fun buildSpec(): TaskExecutionSpec<T> {
    check(mode != ExecutionMode.POLL || stopConditions.isNotEmpty()) {
      "TaskRunner.poll requires at least one stopWhen condition"
    }

    return TaskExecutionSpec(
      mode = mode,
      config = config,
      supplier = supplier ?: error("TaskRunner must have supplier"),
      stopConditions = stopConditions.toList(),
      abortConditions = abortConditions.toList(),
      beforeRetry = beforeRetry,
      onAttemptSuccess = onAttemptSuccess,
      onAttemptFailure = onAttemptFailure,
      onSuccess = onSuccess,
      onFailure = onFailure,
      onExhausted = onExhausted,
      onTimeout = onTimeout,
      onCancel = onCancel,
      onFinished = onFinished,
      onObserverError = onObserverError,
    )
  }

  private fun cleanUp() {
    supplier = null
    stopConditions.clear()
    abortConditions.clear()
    beforeRetry = null
    onAttemptSuccess = null
    onAttemptFailure = null
    onSuccess = null
    onFailure = null
    onExhausted = null
    onTimeout = null
    onCancel = null
    onFinished = null
    onObserverError = null
  }
}
