package com.miyako.core.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal class TaskExecution<T>(
  private val spec: TaskExecutionSpec<T>,
) {
  private val nanoMillis: Long get() = System.nanoTime() / 1_000_000

  private class ExecutionState(
    val startTime: Long,
    val startNano: Long,
    var executionCount: Int = 1,
  )

  fun launchIn(scope: CoroutineScope, dispatcher: CoroutineContext): Job {
    return scope.launch(dispatcher) {
      execute()
    }
  }

  private suspend fun handleFailure(
    error: Throwable,
    result: ExecutionResult<Throwable>,
  ): Boolean {
    if (error is CancellationException && !canHandleCancellationAsFailure(error)) throw error

    spec.throwable?.invoke(result)
    val matchedFailWhenExecutions = spec.failWhenExecutions.filter { it.accepts(error) }
    if (matchedFailWhenExecutions.isNotEmpty()) {
      return matchedFailWhenExecutions.any { it.matches(error, result) }
    }

    return spec.failWhenFallback?.invoke(result) ?: false
  }

  private suspend fun canHandleCancellationAsFailure(error: CancellationException): Boolean {
    // Supplier-owned withTimeout keeps this coroutine active; runner timeout or external cancel does not.
    return error is TimeoutCancellationException &&
      currentCoroutineContext()[Job]?.isActive == true
  }

  private suspend fun execute() {
    val state = ExecutionState(
      startTime = System.currentTimeMillis(),
      startNano = nanoMillis,
    )

    try {
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

  private suspend fun runWithTotalTimeout(state: ExecutionState) {
    try {
      if (spec.config.totalTimeoutMs > 0) {
        withTimeout(spec.config.totalTimeoutMs) {
          runLoop(state)
        }
      } else {
        runLoop(state)
      }
    } catch (t: TimeoutCancellationException) {
      invokeTimeout(state)
    }
  }

  private suspend fun runLoop(state: ExecutionState) {
    delay(spec.config.initialDelayMs)

    while (true) {
      val attemptStart = nanoMillis
      val shouldStop = runAttempt(state, attemptStart)
      if (shouldStop) break

      if (state.executionCount >= spec.config.maxAttempts) {
        invokeExhausted(state, attemptStart)
        break
      }

      state.executionCount++
      delay(spec.config.retryIntervalMs)
    }
  }

  private suspend fun runAttempt(state: ExecutionState, attemptStart: Long): Boolean {
    var shouldRunSupplier = true

    if (state.executionCount > 1) {
      try {
        spec.beforeRetry?.invoke(ExecutionResult(state.metrics(attemptStart, attemptStart), Unit))
      } catch (t: Throwable) {
        val result = ExecutionResult(state.metrics(attemptStart, nanoMillis), t)
        if (handleFailure(t, result)) return true
        shouldRunSupplier = false
      }
    }

    if (!shouldRunSupplier) return false

    try {
      val processData = spec.supplier()
      val info = ExecutionResult(state.metrics(attemptStart, nanoMillis), processData)
      spec.result?.invoke(info)

      // 如果 result 为 Unit，说明执行函数没有返回值，剩余执行次数或超时自动停止
      if (processData !is Unit) {
        return spec.stopWhenExecutions.any { it.matches(processData as Any, info) }
      }
    } catch (t: Throwable) {
      val result = ExecutionResult(state.metrics(attemptStart, nanoMillis), t)
      if (handleFailure(t, result)) return true
    }

    return false
  }

  private suspend fun invokeExhausted(state: ExecutionState, attemptStart: Long) {
    try {
      spec.exhausted?.invoke(ExecutionResult(state.metrics(attemptStart, nanoMillis), Unit))
    } catch (t: Throwable) {
      handleFailure(t, ExecutionResult(state.metrics(attemptStart, nanoMillis), t))
    }
  }

  private suspend fun invokeTimeout(state: ExecutionState) {
    val nanoEnd = nanoMillis
    spec.timeout?.invoke(ExecutionResult(state.metrics(nanoEnd, nanoEnd), Unit))
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
