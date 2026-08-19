# core（JVM）— 基础扩展与 TaskRunner

纯 JVM 模块 `io.github.waxw:core`，不依赖任何 Android 框架，可在 JVM 环境直接使用。

## BaseExtensions（`com.miyako.core`）

| 函数 | 说明 |
|------|------|
| `T?.orInit { }` | 为空时初始化 |
| `Boolean?.ifTrue { }` / `ifFalse { }` | 条件成立时执行并返回原值 |
| `T.thenIf(cond) { }` | 条件成立时变换 |
| `T.thenNull { }` / `T.thenNotNull { }` | 可空变换 |
| `Any?.cast<T>()` / `cast<T> { }` / `cast<T, R>(onNotMatch, onMatch)` | 类型安全转换 |
| `unsafeLazy { }` | 非线程安全 Lazy |
| `Any.hex` | `类名@0x哈希` 调试串 |

## TaskRunner（`com.miyako.core.task`）

一次性结构化并发任务执行器：重试（`retry`）与轮询（`poll`）两种模式，
支持 `completeWhen` / `abortOn` / `retryOn` 条件、`beforeRetry` 回调、
各阶段 `onAttemptSuccess` / `onAttemptFailure` / `onFinished` / `onObserverError` 观察者，
以及 attempt / 总超时与取消语义，返回类型化 `ExecutionResult<T>`（Success / Failure / Exhausted / Timeout）。

```kotlin
import com.miyako.core.task.TaskRunner

val result = TaskRunner
  .retry(maxAttempts = 3, intervalMs = 1000) {
    remoteCall()
  }
  .retryOn<IOException> { it.message?.contains("retryable") == true }
  .abortOn<IllegalStateException>()
  .executeResult()   // ExecutionResult<T>

// 轮询直到满足完成条件（ExecutionAttempt<T> 持有 metrics 与 data）
val polled = TaskRunner
  .poll(maxAttempts = 10, intervalMs = 500) {
    fetchStatus()
  }
  .completeWhen { it.data.isReady }
  .execute()
```

## MVI 契约（`com.miyako.mvi`）

`UiState` / `UiEffect` / `UiAction` 三个 marker 接口，配合 core-ui 的
`MviViewModel` 与 core-ksp 的代码生成使用，见 [mvi-ksp.md](mvi-ksp.md)。
