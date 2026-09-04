# Findings & Decisions

## Requirements
- 提供 `TaskRunner.retry` 和 `TaskRunner.poll` 构建入口；poll 显式指定 maxAttempts，且执行前必须配置 `completeWhen`。
- `execute(): T`；`executeResult(): ExecutionResult<T>`，后者为 Success/Failure/Exhausted/Timeout 终态。
- 外部 CancellationException 原样上抛；supplier 自有 withTimeout 作为 attempt failure。
- `abortOn<E>` 与 `retryOn<E>` 均为同步异常策略；`abortOn` 优先于 `retryOn`。
- 未配置 `retryOn` 时，未被 `abortOn` 匹配的 supplier 异常默认重试；配置后，仅命中至少一个 `retryOn` 的异常重试。
- 多个 `retryOn` 使用 OR 语义；未命中时立即 Failure；predicate 抛错时保留原 attempt 异常为 suppressed。
- `completeWhen`/beforeRetry 异常立即 Failure，不进入异常重试策略。
- 同步 onXxx 观察回调不影响控制流，错误交给 onObserverError，且不递归。
- 不保留 0.0.5 构造器、DSL、回调和字段别名；调用方统一迁移到新 API。
- 延迟参数统一命名为 `initialDelayMs`，与重试间隔 `intervalMs` 区分。
- metrics 阶段为 INITIAL_DELAY/ATTEMPT/RETRY_DELAY/BEFORE_RETRY/COMPLETION_CONDITION。

## Research Findings
- 当前实现已有一次性冻结、结构化 `execute`、总超时、supplier 自有超时区分和 Builder/Spec/Execution 分层。
- 当前 `ExecutionResult<T>` 是单轮数据包装，`TaskResult` 才是终态；Issue 要求分别替换为 `ExecutionAttempt<T>` 和 sealed `ExecutionResult<T>`。
- 仓库内 TaskRunner 仅被 `TaskRunnerUnitTest` 使用，因此 core 范围内迁移面集中。
- 实施初期的用户未提交改动仅为两个 Gradle 文件版本号，TaskRunner 改造未覆盖这些改动。

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| 先确认历史设计中的精确签名再编辑 | Issue 正文对若干回调参数类型没有逐项给出，公开 API 不宜猜测 |
| 使用单一 TaskExecution 状态机 | 防止 retry/poll 行为分叉，符合规格 |
| `abortOn<E>` 接收同步 `(E) -> Boolean`，默认 predicate 为 true | 同时覆盖按类型终止与 4xx 子集分类 |
| `retryOn<E>` 接收同步 `(E) -> Boolean`，默认 predicate 为 true | 同时覆盖按类型重试与异常子集重试 |
| 未配置 `retryOn` 时保持默认重试；配置后切换为白名单 | 保持现有主路径行为，同时允许调用方收窄重试范围 |
| `abortOn` 在 `retryOn` 之前求值 | 明确冲突策略，终止规则拥有更高优先级 |
| 多个 `retryOn` 条件按顺序 OR 短路 | 与多个完成条件的组合方式一致，并保持调用顺序可预测 |
| 使用 `completeWhen` 而非 `stopWhen` | 条件命中对应 Success，完成语义比笼统停止更准确 |
| `RetryContext.previousFailure` 可空 | poll 的上一次成功但未完成也会进入 beforeRetry |
| 非 attempt 阶段的 attemptStartTime/attemptDuration 为空 | 使 metrics 不伪造当前阶段不存在的 attempt 时间 |
| 新观察器异常（含 CancellationException）一律隔离 | 观察器不改变 TaskRunner 控制流 |
| 外部/总超时取消通过当前 coroutine context active 状态区分 supplier 自有 withTimeout | 避免继续依赖反射读取协程内部字段 |
| 公共模型按职责拆分文件 | 符合 code-styles.md 一个主要对外声明的默认规则 |
| 不为 observer 字段增加聚合配置对象 | 属判断性 smell，现有显式 Builder→Spec 映射更便于核对且没有实际遗漏证据 |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| 本机无 GitHub CLI | REST API 成功读取 Issue #36 |
| 历史对话记录较长且分页 | 只读取足以确认 Builder/Spec/Execution、retry/poll 与兼容方向的相关摘要，最终语义以 Issue #36 为准 |
| standards review 指出一个硬风格问题和无用参数 | 已分别拆分公开模型文件、删除 LegacyFailCondition.matches 的无用 throwable 参数 |
| spec review 指出冻结竞态与控制步骤 timeout 分类问题 | 用 configurationLock 原子冻结/快照；beforeRetry/completeWhen 自有 withTimeout 产出 Failure |
| 旧 attempt ExecutionResult 与新 sealed ExecutionResult 同包同名冲突 | 用户决定不做旧 API 兼容，删除全部 deprecated 兼容入口和字段 |

## Resources
- https://github.com/waxw/EF-Core/issues/36
- `/tmp/ef-core-taskrunner-handoff.c4qkpH/HANDOFF.md`
- `core/src/main/kotlin/com/miyako/core/task/`
- `core/src/test/java/com/miyako/core/TaskRunnerUnitTest.kt`

## Verification Status
- 2026-08-12 基线：`./gradlew :core:testDebugUnitTest` 通过，TaskRunnerUnitTest 32/32，core 41/41。
- 2026-08-13 更新：`initialDelayMs`、`retryOn`、`completeWhen` 已完成静态检查；Gradle 测试尚未重新运行。
- 2026-08-14 最终验证：`./gradlew :core:testDebugUnitTest --tests com.miyako.core.TaskRunnerUnitTest` 通过，35/35，无 failure/error/skipped。

## 2026-08-14 Hardening Findings
- `executeResult()` 使用 `try/finally`，规格构建失败时也会清理已冻结 runner 持有的 supplier、条件和 observer 引用。
- `abortOn`/`retryOn` predicate 原样重抛 attempt throwable 时不再执行 self-suppression，仍返回包含原异常的结构化 Failure。
- 多个 `retryOn` 的按顺序 OR 短路语义已有公开行为测试覆盖。

## 2026-09-03 KMP Migration Findings
- `0.0.5` 已通过 PR #37 合入 `main`；KMP 迁移独立放在 `feature_kmp`，预发布版本提升为 `0.1.0-alpha-01`。
- 用户决定将现有 `:core` 原地迁移为 KMP，并将 `:core-ui` 重命名为 `:core-android`。
- `:core` 的公共逻辑大部分可进入 `commonMain`；当前 JVM 专属点是 `java.util.Locale`/`javaClass`、`AtomicBoolean`/`synchronized`、`System.nanoTime`/`System.currentTimeMillis`。
- 当前 `:core-compose` 通过 `ComponentActivity`、`ComposeView` 和 `api(project(":core-ui"))` 复用 Android View 气泡，因此本轮保持 Android Compose 模块，只更新对 `:core-android` 的依赖。
- 当前 `:core-ksp` 是 JVM 构建工具，且 Dispatcher runtime 使用 JVM reflection，本轮不迁移为 KMP。
- KMP 默认 Android target 会派生 `core-android` 平台制品，与重命名后的 Android UI 模块坐标冲突；KMP target 使用 `androidCore` 名称规避冲突，公共依赖入口仍为 `io.github.waxw:core`。
- TaskRunner 使用 `kotlin.time.TimeSource.Monotonic` 计算 duration；墙上时间通过 `currentTimeMillis()` expect/actual 提供。
- 为保持现有 one-shot 与配置冻结并发契约，公共实现使用 `kotlinx.atomicfu` 的原子值和同步锁，不降级为普通 Boolean。
- Android、Desktop JVM、iOS Simulator ARM64 的 `core` 公共测试均为 43/43 通过；iOS device ARM64 与 Simulator x64 主源码也编译通过。
- Kotlin 2.0 对 Xcode 26.6 给出“高于最大已测试版本 15.3”的兼容性警告；本轮实际编译、链接和测试均成功，因此保留警告而不添加 suppress 配置。
