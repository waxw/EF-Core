package com.miyako.core.task

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

internal class TaskExecution<T>(
  private val spec: TaskExecutionSpec<T>,
) {
  private val nanoMillis: Long get() = System.nanoTime() / 1_000_000

  private class ExecutionState(
    val startTime: Long,
    val startNano: Long,
    var executionCount: Int = 0,
    var phase: ExecutionPhase = ExecutionPhase.INITIAL_DELAY,
    var attemptStartNano: Long? = null,
    var attemptEndNano: Long? = null,
    var lastThrowable: Throwable? = null,
  )

  suspend fun execute(): ExecutionResult<T> {
    val state = ExecutionState(System.currentTimeMillis(), nanoMillis)
    var primaryError: Throwable? = null

    try {
      val result = runWithTotalTimeout(state)
      notifyTerminal(result)
      return result
    } catch (throwable: Throwable) {
      primaryError = throwable
      if (throwable is CancellationException) {
        try {
          notifyCancel(state)
        } catch (callbackCancellation: CancellationException) {
          throwable.addSuppressed(callbackCancellation)
        }
      }
      throw throwable
    } finally {
      try {
        notifyFinished(state)
      } catch (finishedError: CancellationException) {
        if (primaryError is CancellationException) {
          primaryError.addSuppressed(finishedError)
        } else {
          throw finishedError
        }
      }
    }
  }

  private suspend fun runWithTotalTimeout(state: ExecutionState): ExecutionResult<T> {
    if (spec.config.totalTimeoutMs == 0L) return runLoop(state)

    return try {
      withTimeout(spec.config.totalTimeoutMs) {
        runLoop(state)
      }
    } catch (throwable: TimeoutCancellationException) {
      if (!currentCoroutineContext().isActive) throw throwable
      ExecutionResult.Timeout(state.metrics())
    }
  }

  private suspend fun runLoop(state: ExecutionState): ExecutionResult<T> {
    if (spec.config.initialDelayMs > 0) {
      state.phase = ExecutionPhase.INITIAL_DELAY
      delay(spec.config.initialDelayMs)
    }

    while (state.executionCount < spec.config.maxAttempts) {
      if (state.executionCount > 0) {
        runRetryPreparation(state)?.let { return it }
      }

      state.executionCount++
      runAttempt(state)?.let { return it }
    }

    return ExecutionResult.Exhausted(state.lastThrowable, state.metrics())
  }

  private suspend fun runRetryPreparation(state: ExecutionState): ExecutionResult.Failure? {
    state.attemptStartNano = null
    state.attemptEndNano = null
    if (spec.config.retryIntervalMs > 0) {
      state.phase = ExecutionPhase.RETRY_DELAY
      delay(spec.config.retryIntervalMs)
    }

    state.phase = ExecutionPhase.BEFORE_RETRY
    val context = RetryContext(
      nextAttempt = state.executionCount + 1,
      previousFailure = state.lastThrowable,
      metrics = state.metrics(),
    )
    return try {
      spec.beforeRetry?.invoke(context)
      null
    } catch (throwable: CancellationException) {
      if (!canHandleAsStepFailure(throwable)) throw throwable
      ExecutionResult.Failure(throwable, state.metrics())
    } catch (throwable: Throwable) {
      ExecutionResult.Failure(throwable, state.metrics())
    }
  }

  private suspend fun runAttempt(state: ExecutionState): ExecutionResult<T>? {
    state.phase = ExecutionPhase.ATTEMPT
    state.attemptStartNano = nanoMillis
    state.attemptEndNano = null

    val data = try {
      spec.supplier()
    } catch (throwable: CancellationException) {
      if (!currentCoroutineContext().isActive) throw throwable
      if (throwable !is TimeoutCancellationException) throw throwable
      return handleAttemptFailure(state, throwable)
    } catch (throwable: Throwable) {
      return handleAttemptFailure(state, throwable)
    }

    state.attemptEndNano = nanoMillis
    state.lastThrowable = null
    val attempt = ExecutionAttempt(state.metrics(), data)
    notifyAttemptSuccess(attempt)

    if (spec.mode == ExecutionMode.RETRY) {
      return ExecutionResult.Success(data, attempt.metrics)
    }

    state.phase = ExecutionPhase.STOP_CONDITION
    val stopAttempt = ExecutionAttempt(state.metrics(), data)
    return try {
      val shouldStop = spec.stopConditions.any { condition -> condition.matches(data, stopAttempt) }
      if (shouldStop) {
        ExecutionResult.Success(data, state.metrics())
      } else {
        null
      }
    } catch (throwable: CancellationException) {
      if (!canHandleAsStepFailure(throwable)) throw throwable
      ExecutionResult.Failure(throwable, state.metrics())
    } catch (throwable: Throwable) {
      ExecutionResult.Failure(throwable, state.metrics())
    }
  }

  private suspend fun handleAttemptFailure(
    state: ExecutionState,
    throwable: Throwable,
  ): ExecutionResult.Failure? {
    state.attemptEndNano = nanoMillis
    state.lastThrowable = throwable
    val attempt = ExecutionAttempt(state.metrics(), throwable)
    notifyAttemptFailure(attempt)

    val abortFailure = evaluateAbortConditions(throwable)
    return abortFailure?.let { ExecutionResult.Failure(it, state.metrics()) }
  }

  private fun evaluateAbortConditions(
    throwable: Throwable,
  ): Throwable? {
    for (condition in spec.abortConditions) {
      if (!condition.accepts(throwable)) continue
      try {
        if (condition.matches(throwable)) return throwable
      } catch (ruleError: Throwable) {
        ruleError.addSuppressed(throwable)
        return ruleError
      }
    }
    return null
  }

  private suspend fun canHandleAsStepFailure(throwable: CancellationException): Boolean {
    return throwable is TimeoutCancellationException && currentCoroutineContext().isActive
  }

  private suspend fun notifyAttemptSuccess(attempt: ExecutionAttempt<T>) {
    observe(ObserverSource.ON_ATTEMPT_SUCCESS, attempt.metrics) {
      spec.onAttemptSuccess?.invoke(attempt)
    }
  }

  private suspend fun notifyAttemptFailure(attempt: ExecutionAttempt<Throwable>) {
    observe(ObserverSource.ON_ATTEMPT_FAILURE, attempt.metrics) {
      spec.onAttemptFailure?.invoke(attempt)
    }
  }

  private suspend fun notifyTerminal(result: ExecutionResult<T>) {
    when (result) {
      is ExecutionResult.Success -> {
        observe(ObserverSource.ON_SUCCESS, result.metrics) { spec.onSuccess?.invoke(result) }
      }

      is ExecutionResult.Failure -> {
        observe(ObserverSource.ON_FAILURE, result.metrics) { spec.onFailure?.invoke(result) }
      }

      is ExecutionResult.Exhausted -> {
        observe(ObserverSource.ON_EXHAUSTED, result.metrics) { spec.onExhausted?.invoke(result) }
      }

      is ExecutionResult.Timeout -> {
        observe(ObserverSource.ON_TIMEOUT, result.metrics) { spec.onTimeout?.invoke(result) }
      }
    }
  }

  private suspend fun notifyCancel(state: ExecutionState) {
    val metrics = state.metrics()
    withContext(NonCancellable) {
      observe(ObserverSource.ON_CANCEL, metrics) { spec.onCancel?.invoke(metrics) }
    }
  }

  private suspend fun notifyFinished(state: ExecutionState) {
    val metrics = state.metrics()
    withContext(NonCancellable) {
      observe(ObserverSource.ON_FINISHED, metrics) { spec.onFinished?.invoke(metrics) }
    }
  }

  private fun observe(
    source: ObserverSource,
    metrics: ExecutionMetrics,
    block: () -> Unit,
  ) {
    try {
      block()
    } catch (throwable: Throwable) {
      reportObserverError(ObserverFailure(source, throwable, metrics))
    }
  }

  private fun reportObserverError(failure: ObserverFailure) {
    try {
      spec.onObserverError?.invoke(failure)
    } catch (_: Throwable) {
      // Observer error reporting is deliberately non-recursive.
    }
  }

  private fun ExecutionState.metrics(now: Long = nanoMillis): ExecutionMetrics {
    val attemptStart = attemptStartNano
    val attemptEnd = attemptEndNano ?: now
    return ExecutionMetrics(
      executionCount = executionCount,
      attemptStartTime = attemptStart?.let { startTime + (it - startNano) },
      attemptDuration = attemptStart?.let { attemptEnd - it },
      totalDuration = now - startNano,
      phase = phase,
    )
  }
}
