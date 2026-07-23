package com.miyako.core.task

import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
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
    val executionJob: Job?,
    var totalTimeoutJob: Job? = null,
    var executionCount: Int = 1,
  )

  private sealed interface AttemptResult<out T> {
    data object Continue : AttemptResult<Nothing>

    data class Complete<T>(
      val result: TaskResult<T>,
    ) : AttemptResult<T>
  }

  private suspend fun handleFailure(
    state: ExecutionState,
    error: Throwable,
    result: ExecutionResult<Throwable>,
  ): Boolean {
    if (error is CancellationException && !canHandleCancellationAsFailure(state, error)) throw error

    spec.throwable?.invoke(result)
    val matchedFailWhenExecutions = spec.failWhenExecutions.filter { it.accepts(error) }
    if (matchedFailWhenExecutions.isNotEmpty()) {
      return matchedFailWhenExecutions.any { it.matches(error, result) }
    }

    return spec.failWhenFallback?.invoke(result) ?: false
  }

  private fun canHandleCancellationAsFailure(
    state: ExecutionState,
    error: CancellationException,
  ): Boolean {
    if (error !is TimeoutCancellationException) return false

    val ownerJob = error.ownerJob() ?: return false
    // Supplier-owned withTimeout has its own timeout job; caller timeout and runner timeout target boundary jobs.
    val isBoundaryTimeout = ownerJob == state.executionJob || ownerJob == state.totalTimeoutJob
    return !isBoundaryTimeout && state.executionJob?.isActive == true
  }

  private fun TimeoutCancellationException.ownerJob(): Job? {
    return runCatching {
      javaClass.getDeclaredField("coroutine")
        .apply { isAccessible = true }
        .get(this) as? Job
    }.getOrNull()
  }

  suspend fun execute(): TaskResult<T> {
    val state = ExecutionState(
      startTime = System.currentTimeMillis(),
      startNano = nanoMillis,
      executionJob = currentCoroutineContext()[Job],
    )

    return try {
      runWithTotalTimeout(state)
    } catch (t: Throwable) {
      if (t is CancellationException) {
        invokeCancel(state)
      }
      throw t
    } finally {
      invokeFinally(state)
    }
  }

  private suspend fun runWithTotalTimeout(state: ExecutionState): TaskResult<T> {
    return try {
      if (spec.config.totalTimeoutMs > 0) {
        withTimeout(spec.config.totalTimeoutMs) {
          state.totalTimeoutJob = currentCoroutineContext()[Job]
          runLoop(state)
        }
      } else {
        runLoop(state)
      }
    } catch (t: TimeoutCancellationException) {
      invokeTimeout(state)
    }
  }

  private suspend fun runLoop(state: ExecutionState): TaskResult<T> {
    delay(spec.config.initialDelayMs)

    while (true) {
      val attemptStart = nanoMillis
      when (val attemptResult = runAttempt(state, attemptStart)) {
        is AttemptResult.Complete -> return attemptResult.result
        AttemptResult.Continue -> Unit
      }

      if (state.executionCount >= spec.config.maxAttempts) {
        return invokeExhausted(state, attemptStart)
      }

      state.executionCount++
      delay(spec.config.retryIntervalMs)
    }
  }

  private suspend fun runAttempt(
    state: ExecutionState,
    attemptStart: Long,
  ): AttemptResult<T> {
    var shouldRunSupplier = true

    if (state.executionCount > 1) {
      try {
        spec.beforeRetry?.invoke(ExecutionResult(state.metrics(attemptStart, attemptStart), Unit))
      } catch (t: Throwable) {
        val result = ExecutionResult(state.metrics(attemptStart, nanoMillis), t)
        if (handleFailure(state, t, result)) return AttemptResult.Complete(TaskResult.Failure(result))
        shouldRunSupplier = false
      }
    }

    if (!shouldRunSupplier) return AttemptResult.Continue

    try {
      val processData = spec.supplier()
      val info = ExecutionResult(state.metrics(attemptStart, nanoMillis), processData)
      spec.result?.invoke(info)

      if (shouldStop(processData, info)) {
        return AttemptResult.Complete(TaskResult.Success(info))
      }
    } catch (t: Throwable) {
      val result = ExecutionResult(state.metrics(attemptStart, nanoMillis), t)
      if (handleFailure(state, t, result)) return AttemptResult.Complete(TaskResult.Failure(result))
    }

    return AttemptResult.Continue
  }

  private suspend fun shouldStop(data: T, result: ExecutionResult<T>): Boolean {
    if (spec.stopWhenExecutions.isEmpty()) return true

    return spec.stopWhenExecutions.any { it.matches(data as Any, result) }
  }

  private suspend fun invokeExhausted(
    state: ExecutionState,
    attemptStart: Long,
  ): TaskResult<Nothing> {
    val metrics = state.metrics(attemptStart, nanoMillis)
    try {
      spec.exhausted?.invoke(ExecutionResult(metrics, Unit))
    } catch (t: Throwable) {
      val result = ExecutionResult(metrics, t)
      if (handleFailure(state, t, result)) return TaskResult.Failure(result)
    }
    return TaskResult.Exhausted(metrics)
  }

  private suspend fun invokeTimeout(state: ExecutionState): TaskResult.Timeout {
    val nanoEnd = nanoMillis
    val metrics = state.metrics(nanoEnd, nanoEnd)
    spec.timeout?.invoke(ExecutionResult(metrics, Unit))
    return TaskResult.Timeout(metrics)
  }

  private suspend fun invokeCancel(state: ExecutionState) {
    val nanoEnd = nanoMillis
    spec.cancel?.invoke(ExecutionResult(state.metrics(nanoEnd, nanoEnd), Unit))
  }

  private suspend fun invokeFinally(state: ExecutionState) {
    withContext(NonCancellable) {
      // 这里的挂起代码即使协程被取消，也会执行完成
      val nanoEnd = nanoMillis
      spec.finally?.invoke(ExecutionResult(state.metrics(nanoEnd, nanoEnd), Unit))
    }
  }

  private fun ExecutionState.metrics(start: Long, end: Long): ExecutionMetrics {
    return ExecutionMetrics(
      executionCount,
      startTime + (start - startNano),
      end - start,
      end - startNano,
    )
  }
}
