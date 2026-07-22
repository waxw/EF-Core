package com.miyako.core.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class TaskRunner<T>(
  delayMs: Long = 0,
  intervalMs: Long = 0,
  maxAttempts: Int = 1,
  timeoutMs: Long = 0,
  private var supplier: (suspend () -> T)?,
) {
  private val config: ExecutionConfig = ExecutionConfig(delayMs, intervalMs, maxAttempts, timeoutMs)
  private val launched = AtomicBoolean(false)
  private var job: Job? = null

  // 是否终止流程
  private val stopWhenExecutions = mutableListOf<StopWhenCondition<*>>()
  private val failWhenExecutions = mutableListOf<FailWhenCondition<out Throwable>>()

  private var result: (suspend (ExecutionResult<T>) -> Unit)? = null
  private var throwable: (suspend (ExecutionResult<Throwable>) -> Unit)? = null
  private var failWhenFallback: (suspend (ExecutionResult<Throwable>) -> Boolean)? = null
  private var beforeRetry: (suspend (ExecutionResult<Unit>) -> Unit)? = null
  private var exhausted: (suspend (ExecutionResult<Unit>) -> Unit)? = null
  private var timeout: (suspend (ExecutionResult<Unit>) -> Unit)? = null
  private var cancel: (suspend (ExecutionResult<Unit>) -> Unit)? = null
  private var finally: (suspend (ExecutionResult<Unit>) -> Unit)? = null

  private fun checkExecution(block: () -> Unit) = apply {
    check(!launched.get()) { "Cannot modify after launch" }
    block()
  }

  inline fun <reified E : Any> stopWhen(
    noinline execution: suspend (ExecutionResult<E>) -> Boolean
  ) = apply {
    addStopWhenExecution(StopWhenCondition(E::class, execution))
  }

  inline fun <reified E : Throwable> failWhen(
    noinline execution: suspend (ExecutionResult<E>) -> Boolean
  ) = apply {
    addFailExecution(FailWhenCondition(E::class, execution))
  }

  inline fun <reified E : Any> TaskRunner<T>.retryWhen(
    noinline execution: suspend (ExecutionResult<E>) -> Boolean
  ) = apply {
    stopWhen<E> { execution(it).not() }
  }

  fun exhausted(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    exhausted = block
  }

  fun result(block: suspend (ExecutionResult<T>) -> Unit) = checkExecution {
    result = block
  }

  fun throwable(block: suspend (ExecutionResult<Throwable>) -> Unit) = checkExecution {
    throwable = block
  }

  fun failWhenFallback(block: suspend (ExecutionResult<Throwable>) -> Boolean) = checkExecution {
    failWhenFallback = block
  }

  fun beforeRetry(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    beforeRetry = block
  }

  fun timeout(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    timeout = block
  }

  fun cancel(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    cancel = block
  }


  fun finally(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    finally = block
  }

  fun <E : Any> addStopWhenExecution(execution: StopWhenCondition<E>) = checkExecution {
    stopWhenExecutions.add(execution)
  }

  fun <E : Throwable> addFailExecution(execution: FailWhenCondition<E>) = checkExecution {
    failWhenExecutions.add(execution)
  }

  private val nanoMillis: Long get() = System.nanoTime() / 1_000_000

  private suspend fun handleFailure(
    error: Throwable,
    result: ExecutionResult<Throwable>,
  ): Boolean {
    if (error is CancellationException) throw error

    throwable?.invoke(result)
    val matchedFailWhenExecutions = failWhenExecutions.filter { it.accepts(error) }
    if (matchedFailWhenExecutions.isNotEmpty()) {
      return matchedFailWhenExecutions.any { it.matches(error, result) }
    }

    return failWhenFallback?.invoke(result) ?: false
  }

  private suspend fun execute() {
    val realSupplier = supplier ?: throw IllegalStateException("TaskRunner must have supplier")
    val startTime = System.currentTimeMillis()
    val startNano = nanoMillis
    var executionCount = 1
    val maxExecutions = config.maxAttempts

    delay(config.initialDelayMs)

    val executionMetrics = { start: Long, end: Long ->
      ExecutionMetrics(executionCount, startTime + start, end - start, end - startNano)
    }

    try {
      while (true) {
        val attemptStart = nanoMillis
        // 是否超时
        if (config.totalTimeoutMs > 0 && attemptStart - startNano > config.totalTimeoutMs) {
          try {
            timeout?.invoke(ExecutionResult(executionMetrics(attemptStart, attemptStart), Unit))
          } catch (t: Throwable) {
            handleFailure(t, ExecutionResult(executionMetrics(attemptStart, nanoMillis), t))
          }
          break
        }

        var shouldRunSupplier = true

        // 进行重试回调
        if (executionCount > 1) {
          try {
            beforeRetry?.invoke(ExecutionResult(executionMetrics(attemptStart, attemptStart), Unit))
          } catch (t: Throwable) {
            if (handleFailure(t, ExecutionResult(executionMetrics(attemptStart, nanoMillis), t))) {
              break
            }
            shouldRunSupplier = false
          }
        }

        if (shouldRunSupplier) {
          try {
            val processData = realSupplier()
            val info = ExecutionResult(executionMetrics(attemptStart, nanoMillis), processData)
            result?.invoke(info)

            // 如果 result 为 Unit，说明执行函数没有返回值，剩余执行次数或超时自动停止
            if (processData !is Unit) {
              val shouldStop = stopWhenExecutions.any { it.matches(processData as Any, info) }
              if (shouldStop) break
            }
          } catch (t: Throwable) {
            val result = ExecutionResult(executionMetrics(attemptStart, nanoMillis), t)
            if (handleFailure(t, result)) break
          }
        }

        if (executionCount >= maxExecutions) {
          try {
            exhausted?.invoke(ExecutionResult(executionMetrics(attemptStart, nanoMillis), Unit))
          } catch (t: Throwable) {
            handleFailure(t, ExecutionResult(executionMetrics(attemptStart, nanoMillis), t))
          }
          break
        }

        executionCount++
        delay(config.retryIntervalMs)
      }
    } catch (t: Throwable) {
      if (t is CancellationException) {
        val nanoEnd = nanoMillis
        try {
          cancel?.invoke(ExecutionResult(executionMetrics(nanoEnd, nanoEnd), Unit))
        } catch (cancelError: Throwable) {
          handleFailure(cancelError, ExecutionResult(executionMetrics(nanoEnd, nanoMillis), cancelError))
        }
      }
      throw t
    } finally {
      withContext(NonCancellable) {
        // 这里的挂起代码即使协程被取消，也会执行完成
        val nanoEnd = nanoMillis
        try {
          finally?.invoke(ExecutionResult(executionMetrics(nanoEnd, nanoEnd), Unit))
        } catch (t: Throwable) {
          handleFailure(t, ExecutionResult(executionMetrics(nanoEnd, nanoMillis), t))
        }
      }
      cleanUp()
    }
  }

  /**
   * 清理所有外部变量引用，避免内存泄露
   */
  private fun cleanUp() {
    supplier = null
    result = null
    throwable = null
    failWhenFallback = null
    beforeRetry = null
    exhausted = null
    timeout = null
    finally = null
    stopWhenExecutions.clear()
    failWhenExecutions.clear()
  }

  /**
   * 启动任务执行流程，并返回对应的协程 Job。
   *
   * 注意事项：
   * 1. 本方法可能会同步抛出异常（如任务已启动时抛出 IllegalStateException），
   *    调用方应使用 try/catch 捕获此类异常以避免程序崩溃。
   *
   * 2. 任务执行过程中可能出现异步异常，这些异常不会通过本方法抛出，
   *    而是在返回的 Job 上通过 [Job.invokeOnCompletion] 以 Throwable 形式通知。
   *    调用方应主动监听该回调，否则异步异常可能导致程序崩溃或异常丢失。
   *
   * 3. 本 TaskRunner 只能启动一次，重复调用会抛异常。
   *
   * 4. 返回的 Job 可用于取消任务执行或监听任务完成状态。
   *
   * 示例：
   * ```
   * try {
   *   val job = executor.launchIn(scope)
   *   job.invokeOnCompletion { throwable ->
   *     if (throwable != null) {
   *       // 处理异步异常
   *     }
   *   }
   * } catch (e: IllegalStateException) {
   *   // 处理重复启动等同步异常
   * }
   * ```
   *
   * @param scope 用于启动协程的 CoroutineScope。
   * @param dispatcher 调度器。
   * @return 启动的协程 Job。
   * @throws IllegalStateException 如果任务已启动过，再次启动会抛出。
   */
  fun launchIn(scope: CoroutineScope, dispatcher: CoroutineContext): Job {
    check(launched.compareAndSet(false, true)) {
      "TaskRunner can only be launched once"
    }
    check(supplier != null) {
      "TaskRunner must have supplier"
    }
    return scope.launch(dispatcher) {
      job = this.coroutineContext[Job]
      execute()
    }.also {
      job = it
      it.invokeOnCompletion {
        cleanUp()
      }
    }
  }

  fun cancel() {
    job?.cancel()
  }
}
