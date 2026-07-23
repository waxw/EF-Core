package com.miyako.core.task

import java.util.concurrent.atomic.AtomicBoolean

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
  private val executed = AtomicBoolean(false)

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
    check(!executed.get()) { "Cannot modify after execute" }
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
   * 这里属于生命周期收尾阶段；回调自身抛出的异常不会进入 [throwable]，而是从 [execute] 暴露给调用方。
   */
  fun cancel(block: suspend (ExecutionResult<Unit>) -> Unit) = checkExecution {
    cancel = block
  }

  /**
   * TaskRunner 结束时调用，取消场景下也会尽量执行完成。
   *
   * 这里属于生命周期收尾阶段；回调自身抛出的异常不会进入 [throwable]，而是从 [execute] 暴露给调用方。
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
   * 挂起执行任务流程，并返回最终终态。
   *
   * 注意事项：
   * 1. 本方法不会额外启动子协程；调用方所在协程就是 TaskRunner 的结构化并发边界。
   *
   * 2. supplier 成功且没有配置 [stopWhen] 时，会直接返回 [TaskResult.Success]。
   *    如果配置了 [stopWhen]，只有任一条件返回 true 时才返回成功终态；否则继续执行直到失败终止、
   *    达到最大执行次数或总超时。
   *
   * 3. 正常流程中的失败会进入 [throwable] / [failWhen] / [failWhenFallback]。
   *    当失败策略要求终止时，返回 [TaskResult.Failure]；如果达到最大执行次数，返回 [TaskResult.Exhausted]。
   *
   * 4. 总超时会触发 [timeout] 并返回 [TaskResult.Timeout]。外部取消会触发 [cancel] 后继续抛出
   *    CancellationException。
   *
   * 示例：
   * ```
   * val result = executor.execute()
   * when (result) {
   *   is TaskResult.Success -> handleSuccess(result.data)
   *   is TaskResult.Failure -> handleFailure(result.throwable)
   *   is TaskResult.Exhausted -> handleExhausted()
   *   is TaskResult.Timeout -> handleTimeout()
   * }
   * ```
   *
   * @return 任务最终终态。
   * @throws IllegalStateException 如果任务已执行过，再次执行会抛出。
   */
  suspend fun execute(): TaskResult<T> {
    check(executed.compareAndSet(false, true)) {
      "TaskRunner can only be executed once"
    }
    val execution = TaskExecution(buildSpec())
    cleanUp()
    return execution.execute()
  }
}
