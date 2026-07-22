package com.miyako.core.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * 一次性协程任务执行器 DSL。
 *
 * [timeoutMs] 表示 TaskRunner 从首次延迟到重试流程结束的总硬超时。supplier 内部自行使用
 * withTimeout 抛出的 TimeoutCancellationException 会作为一次执行失败进入 [throwable] /
 * [failWhen] / [failWhenFallback]，不会触发 [timeout]。
 *
 * @param delayMs 首次执行前延迟，单位毫秒。
 * @param intervalMs 两次执行之间的重试间隔，单位毫秒。
 * @param maxAttempts 最大执行次数，包含首次执行。
 * @param timeoutMs TaskRunner 总硬超时，0 表示不限制。
 * @param supplier 每次执行的任务体。
 */
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

  /**
   * 每次 supplier 成功返回后调用。
   */
  fun result(block: suspend (ExecutionResult<T>) -> Unit) = checkExecution {
    result = block
  }

  /**
   * 处理一次正常流程中的失败。
   *
   * supplier 内部抛出的异常会进入这里，包括 supplier 自己使用 withTimeout 产生的
   * TimeoutCancellationException。TaskRunner 的总超时不会进入这里，而是触发 [timeout]。
   */
  fun throwable(block: suspend (ExecutionResult<Throwable>) -> Unit) = checkExecution {
    throwable = block
  }

  /**
   * 当没有任何 [failWhen] 类型匹配当前异常时，决定是否提前终止。
   *
   * 返回 true 表示终止 TaskRunner；返回 false 表示继续重试，直到达到最大执行次数后触发 [exhausted]。
   */
  fun failWhenFallback(block: suspend (ExecutionResult<Throwable>) -> Boolean) = checkExecution {
    failWhenFallback = block
  }

  /**
   * 每次重试前调用。
   *
   * 这里抛出的异常属于正常流程失败，会进入 [throwable] / [failWhen] / [failWhenFallback]。
   */
  fun beforeRetry(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    beforeRetry = block
  }

  /**
   * 达到最大执行次数后调用。
   *
   * 这里抛出的异常属于正常流程失败，会进入 [throwable] / [failWhen] / [failWhenFallback]。
   */
  fun exhausted(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    exhausted = block
  }

  /**
   * TaskRunner 总超时时调用。
   *
   * 该回调只表示 TaskRunner 自己的 [ExecutionConfig.totalTimeoutMs] 到达。supplier 内部的
   * withTimeout 超时会作为一次失败进入 [throwable]，不会触发这里。
   */
  fun timeout(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    timeout = block
  }

  /**
   * TaskRunner 所在协程被外部取消时调用。
   *
   * 这里属于生命周期收尾阶段；回调自身抛出的异常不会进入 [throwable]，而是暴露给返回的 Job。
   */
  fun cancel(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    cancel = block
  }

  /**
   * TaskRunner 结束时调用，取消场景下也会尽量执行完成。
   *
   * 这里属于生命周期收尾阶段；回调自身抛出的异常不会进入 [throwable]，而是暴露给返回的 Job。
   */
  fun finally(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    finally = block
  }

  @PublishedApi
  internal fun <E : Any> addStopWhenExecution(execution: StopWhenCondition<E>) = checkExecution {
    stopWhenExecutions.add(execution)
  }

  @PublishedApi
  internal fun <E : Throwable> addFailExecution(execution: FailWhenCondition<E>) = checkExecution {
    failWhenExecutions.add(execution)
  }

  private fun buildSpec(): TaskExecutionSpec<T> {
    return TaskExecutionSpec(
      config = config,
      supplier = supplier ?: throw IllegalStateException("TaskRunner must have supplier"),
      stopWhenExecutions = stopWhenExecutions.toList(),
      failWhenExecutions = failWhenExecutions.toList(),
      result = result,
      throwable = throwable,
      failWhenFallback = failWhenFallback,
      beforeRetry = beforeRetry,
      exhausted = exhausted,
      timeout = timeout,
      cancel = cancel,
      finally = finally,
    )
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
    cancel = null
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
    val execution = TaskExecution(buildSpec())
    return execution.launchIn(scope, dispatcher).also {
      job = it
      cleanUp()
    }
  }

  fun cancel() {
    job?.cancel()
  }
}
