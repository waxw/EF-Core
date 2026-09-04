# TaskRunner 设计与跨语言移植规范

> 本文是行为规范与移植蓝图，不是 Kotlin 源码解说。目标是在任意支持异步任务、取消和超时的语言中重建等价框架。Dart、Swift 仅为示例，不是指定目标。

## 1. 模块定位

TaskRunner 是一次性、调用方所有权的异步任务执行模块。它把重试、轮询、终态建模、取消传播、总超时、阶段指标和观察通知集中在一个小接口之后。

设计目标：

- 调用方只描述任务、次数、时间和规则，不自行编写循环。
- `retry` 与 `poll` 共享执行引擎，但成功判定不同。
- 业务失败、耗尽、总超时和外部取消保持不同语义。
- 不创建 scope、job、线程池或调度器，继承调用方执行上下文。
- 观察器不能改变任务控制流或终态。
- 配置在执行开始时冻结，每个 runner 只能执行一次。

## 2. 外部接口与契约

语言移植不必复刻 Kotlin fluent DSL，但必须保留以下能力。

### 2.1 构建模式

| 模式 | 成功定义 |
|---|---|
| `retry(task, config)` | task 第一次正常返回即成功；异常按中止和重试规则处理 |
| `poll(task, config)` | task 正常返回后，还需至少一个完成条件为 true |

配置字段：

| 字段 | 约束 | 语义 |
|---|---:|---|
| `initialDelay` | >= 0 | 第一次执行前等待 |
| `retryInterval` | >= 0 | 后续尝试前等待 |
| `maxAttempts` | >= 1 | task 最大实际调用次数 |
| `totalTimeout` | >= 0 | 覆盖所有阶段；0 表示禁用 |

`poll` 必须显式提供 `maxAttempts`，执行前至少有一个完成条件。`retry` 禁止配置完成条件。

### 2.2 规则

| 规则 | 输入 | 组合方式 | 用途 |
|---|---|---|---|
| completion condition | 成功数据与本次指标 | 有序 OR、短路 | 仅 poll，决定成功或继续 |
| abort condition | task 异常 | 有序 OR、短路 | 匹配后立即 Failure |
| retry condition | task 异常 | 有序 OR、短路 | 配置后至少一项匹配才允许继续 |
| before retry | 下次序号、上次失败、指标 | 单个异步回调 | 每次非首次尝试前执行 |

失败决策优先级：

1. 外部取消直接传播，不进入业务规则。
2. 记录并通知本次 task 失败。
3. 先计算 abort；匹配则终止。
4. 再计算 retry；未配置时默认允许，配置后仅匹配时允许。
5. 允许继续但达到次数上限时返回 Exhausted。

规则谓词抛错时，谓词错误成为 Failure 的主错误；原 task 错误作为 cause、suppressed error 或组合错误保留。

### 2.3 执行入口

建议提供两层接口：

- `executeResult()` 返回结构化终态，适合框架代码。
- `execute()` 成功时返回数据，其他终态映射为异常，适合业务代码。

| 终态 | `execute()` 行为 |
|---|---|
| Success | 返回 data |
| Failure | 抛出原始错误 |
| Exhausted | 抛出 AttemptsExhausted，携带 lastError 与 metrics |
| Timeout | 抛出 TaskTimeout，携带 metrics |
| 外部取消 | 原样继续传播，不转换为终态 |

## 3. 语言无关领域模型

```text
ExecutionResult<T> =
  Success(data: T, metrics)
  | Failure(error, metrics)
  | Exhausted(lastError?: Error, metrics)
  | Timeout(metrics)

ExecutionAttempt<T> { metrics, data }
RetryContext { nextAttempt, previousFailure?, metrics }

ExecutionMetrics {
  executionCount,
  attemptStartTime?,
  attemptDuration?,
  totalDuration,
  phase
}

ExecutionPhase = INITIAL_DELAY | ATTEMPT | RETRY_DELAY |
                 BEFORE_RETRY | COMPLETION_CONDITION
```

四种终态互斥。取消不是 ExecutionResult，而是运行时控制信号。`Exhausted.lastError` 可以为空：poll 可能每次都正常返回，但始终未满足完成条件。

指标要求：

- `executionCount` 是已开始的 task 次数，不是循环次数。
- duration 与 deadline 使用单调时钟。
- 展示用 attemptStartTime 可由起始墙上时间加单调时钟偏移得到。
- 进入阶段前先更新 phase，使超时和取消指标能定位中断位置。
- retry 准备阶段清空上一 attempt 的开始与结束时间。

## 4. 状态机

```text
start
  -> initial delay
  -> attempt
       task throws -> attempt failure -> abort? -> retry allowed?
       retry mode returns -> success
       poll mode returns -> completion conditions
                              true -> success
                              false -> retry preparation / exhausted
                              throws -> failure
  -> retry delay
  -> before retry -> next attempt
```

所有活动阶段均受总期限约束。语言无关执行伪代码：

```text
run(spec):
  atomically assert one-shot
  validate mode invariants
  snapshot immutable spec and release mutable builder references
  state = new State(monotonicNow, wallClockNow)

  try:
    result = with optional total deadline:
      await initial delay if configured

      while executionCount < maxAttempts:
        if this is not the first attempt:
          clear attempt timing
          await retry interval
          phase = BEFORE_RETRY
          run beforeRetry; its error becomes Failure

        executionCount += 1
        phase = ATTEMPT
        run task and notify attempt success/failure

        on task error:
          if abort matches: return Failure
          if configured retry rules do not match: return Failure
          otherwise continue

        on task data:
          if retry mode: return Success
          phase = COMPLETION_CONDITION
          if any completion rule matches: return Success
          otherwise continue

      return Exhausted(lastError, metrics)

    notify matching terminal observer
    return result

  catch external cancellation:
    notify cancel in cancellation-insulated context
    rethrow cancellation

  finally:
    notify finished in cancellation-insulated context
```

### 超时归属

必须区分两类超时：

- TaskRunner 自己设置的总期限到期：返回 `Timeout`。
- task、beforeRetry 或完成条件内部主动设置的局部超时：属于对应步骤的错误；task 内超时按 attempt failure 走规则，另外两处直接 Failure。

目标平台可通过 timeout token 身份、子任务身份或专用错误类型区分两者。不能把所有 timeout error 无差别映射为 TaskRunner Timeout。

## 5. 通知协议

观察器是遥测接口，不是控制接口。观察器异常必须隔离并交给 `onObserverError`；错误报告器自身再抛错时吞掉，禁止递归。

| 路径 | 通知顺序 |
|---|---|
| 成功 | onAttemptSuccess -> onSuccess -> onFinished |
| 立即失败 | onAttemptFailure -> onFailure -> onFinished |
| 耗尽 | 各 attempt 通知 -> onExhausted -> onFinished |
| 总超时 | onTimeout -> onFinished |
| 外部取消 | onCancel -> onFinished -> 继续传播取消 |

poll 中 task 正常返回但未完成，仍触发 onAttemptSuccess。完成条件错误和 beforeRetry 错误不触发 onAttemptFailure，因为它们不是 task attempt 失败。

onCancel 与 onFinished 必须在不受当前取消阻断的上下文中运行。若平台没有 NonCancellable 等价能力，使用同步不可挂起通知或独立受控清理上下文。

## 6. 配置冻结与生命周期

TaskRunner 是有状态 builder，执行引擎只接收不可变 spec：

1. 构建阶段添加规则和观察器。
2. 执行入口通过原子操作抢占 one-shot 状态。
3. 在锁、actor 或等价隔离下校验并复制为不可变 ExecutionSpec。
4. 清空 builder 内 task、闭包和集合，减少长期引用。
5. 执行阶段只读取 spec。

不变量：

- 同一实例最多调用一次 execute 或 executeResult。
- 执行开始后不可修改配置。
- 并发执行竞争中只有一个调用能进入。
- 数值参数立即校验，模式约束在最早可判定时校验。
- TaskRunner 不拥有调用方线程、事件循环、scope、job 或 executor。

目标语言也可采用不可变 builder，但 one-shot 与调用方所有权必须明确。

## 7. 目标语言适配原则

### 必须保持一致

- 两种模式及成功判定。
- 四种结构化终态，取消独立传播。
- abort 优先于 retry；规则按声明顺序 OR 短路。
- 总超时覆盖所有阶段，局部超时归属于步骤。
- 通知顺序、观察器错误隔离、finished 必达。
- one-shot、配置冻结和指标含义。

### 可以语言本地化

- fluent builder、构造器、顶层函数或配置对象。
- 错误类型匹配方式。
- Duration、TimeInterval 或整数毫秒等时间表示。
- sealed class、关联值 enum、union type 等终态表示。
- suppressed error 的替代表示。
- atomic、lock、actor、isolate 或单线程事件循环等并发实现。

能力映射示例仅供参考：

| 所需能力 | Kotlin | Dart 示例 | Swift 示例 | 通用选择 |
|---|---|---|---|---|
| 异步函数 | suspend | Future | async/await | Promise/Future/Task |
| 封闭终态 | sealed interface | sealed class | associated-value enum | algebraic data type |
| 外部取消 | Job cancellation | CancellationToken | Task cancellation | runtime token |
| 单调时钟 | System.nanoTime | Stopwatch | ContinuousClock | monotonic clock |
| 配置互斥 | synchronized + atomic | isolate/lock | actor/lock | CAS + lock |

选定任何新语言后，应先写一页运行时语义适配说明，再编码。Dart、Swift 在表中只是示例。

## 8. 推荐内部模块

```text
TaskRunner / Builder       外部接口、one-shot、配置冻结
ExecutionSpec              不可变执行描述
ExecutionEngine            状态机、时间、取消、超时、决策顺序
Conditions                 类型筛选与谓词适配
ObserverDispatcher         通知顺序与错误隔离
Models                     Result、Metrics、Attempt、Context、Errors
```

ExecutionEngine 是主要深模块。不要将每个阶段公开为由调用方任意驱动的方法，否则状态机不变量会泄漏。时钟和 delay/deadline 原语可作为内部 seam 注入测试 adapter，生产接口无需暴露。

## 9. 跨语言一致性测试清单

1. retry 首次成功；失败后按间隔重试并成功。
2. abort 匹配立即停止，且优先于 retry。
3. 无 retry 规则时默认重试；配置后未匹配立即 Failure。
4. 多个规则按声明顺序 OR 短路。
5. 规则谓词错误成为主错误，原 task 错误被关联保留。
6. poll 无完成条件在 task 前失败；retry 拒绝完成条件。
7. poll 正常返回但始终未完成时 Exhausted，lastError 为空。
8. 最后一次 task 失败后 Exhausted 携带 lastError。
9. beforeRetry 位于 retry delay 后、下一 attempt 前，nextAttempt 正确。
10. beforeRetry 或 completion condition 错误直接 Failure。
11. task 内局部超时按 attempt failure；其他步骤局部超时按步骤 Failure。
12. 总超时在五个阶段均产生 Timeout，metrics.phase 准确。
13. 外部取消继续传播，并按 onCancel -> onFinished 通知。
14. 四种终态的观察顺序准确。
15. 观察器错误不改变终态，报告 source 正确且不递归。
16. 第二次执行失败；执行后修改失败；并发抢占只有一个赢家。
17. 非法时间和次数参数在构建时失败。
18. 次数、attempt 时长、总时长和阶段指标符合定义。

优先使用虚拟时钟与可控取消 token，另保留少量运行时集成测试验证平台的真实取消和 deadline 行为。

## 10. 移植实施流程

1. 填写目标语言能力映射：异步、取消、deadline、单调时钟、封闭联合类型、原子状态。
2. 实现公开模型和不可变 ExecutionSpec。
3. 按第 4 节实现无观察器状态机。
4. 加入取消与总超时，验证局部和总超时可区分。
5. 加入 ObserverDispatcher。
6. 加入 builder 或惯用配置接口，只负责配置、校验、冻结和 one-shot。
7. 移植第 9 节契约测试，再做目标语言惯用包装。

验收标准不是代码结构与 Kotlin 相似，而是相同输入、时间推进、错误和取消下，产生等价终态、指标与通知序列。

## 11. Kotlin 参考实现索引

| 职责 | 文件 |
|---|---|
| 外部接口、配置冻结、one-shot | `core/src/commonMain/kotlin/com/miyako/core/task/TaskRunner.kt` |
| 执行状态机 | `core/src/commonMain/kotlin/com/miyako/core/task/TaskExecution.kt` |
| 不可变执行描述 | `core/src/commonMain/kotlin/com/miyako/core/task/TaskExecutionSpec.kt` |
| 条件封装 | `core/src/commonMain/kotlin/com/miyako/core/task/ExecutionCondition.kt` |
| 终态与指标 | `ExecutionResult.kt`、`ExecutionMetrics.kt` |
| 行为规范测试 | `core/src/commonTest/kotlin/com/miyako/core/TaskRunnerUnitTest.kt` |
