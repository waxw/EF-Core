# Findings & Decisions

## Requirements
- 新增 `TaskRunner.retry` 和 `TaskRunner.poll` 构建入口；poll 显式指定 maxAttempts 且执行前必须有 stopWhen。
- `execute(): T`；`executeResult(): ExecutionResult<T>`，后者为 Success/Failure/Exhausted/Timeout 终态。
- 外部 CancellationException 原样上抛；supplier 自有 withTimeout 作为 attempt failure。
- 新控制规则为同步 `abortOn`；未命中异常默认重试；stopWhen/beforeRetry 异常立即 Failure。
- 同步 onXxx 观察回调不影响控制流，错误交给 onObserverError，且不递归。
- 不保留 0.0.5 构造器、DSL、回调和字段别名；调用方统一迁移到新 API。
- 扩展 metrics，标识 INITIAL_DELAY/ATTEMPT/RETRY_DELAY/BEFORE_RETRY/STOP_CONDITION。

## Research Findings
- 当前实现已有一次性冻结、结构化 `execute`、总超时、supplier 自有超时区分和 Builder/Spec/Execution 分层。
- 当前 `ExecutionResult<T>` 是单轮数据包装，`TaskResult` 才是终态；Issue 要求分别替换为 `ExecutionAttempt<T>` 和 sealed `ExecutionResult<T>`。
- 仓库内 TaskRunner 仅被 `TaskRunnerUnitTest` 使用，因此 core 范围内迁移面集中。
- 当前用户未提交改动仅为两个 Gradle 文件版本号。

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| 先确认历史设计中的精确签名再编辑 | Issue 正文对若干回调参数类型没有逐项给出，公开 API 不宜猜测 |
| 使用单一 TaskExecution 状态机 | 防止 retry/poll 行为分叉，符合规格 |
| `abortOn<E>` 接收同步 `(E) -> Boolean`，默认 predicate 为 true | 同时覆盖按类型终止与 4xx 子集分类 |
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
| spec review 指出冻结竞态与控制步骤 timeout 分类问题 | 用 configurationLock 原子冻结/快照；beforeRetry/stopWhen 自有 withTimeout 产出 Failure |
| 旧 attempt ExecutionResult 与新 sealed ExecutionResult 同包同名冲突 | 用户决定不做旧 API 兼容，删除全部 deprecated 兼容入口和字段 |

## Resources
- https://github.com/waxw/EF-Core/issues/36
- `/tmp/ef-core-taskrunner-handoff.c4qkpH/HANDOFF.md`
- `core/src/main/kotlin/com/miyako/core/task/`
- `core/src/test/java/com/miyako/core/TaskRunnerUnitTest.kt`
