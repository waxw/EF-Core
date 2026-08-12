# Task Plan: TaskRunner Issue #36

## Goal
在 EF-Core `core` 模块实现 Issue #36 已确认的 TaskRunner retry/poll、终态结果、异常映射、观察回调与 0.0.5 兼容契约，并通过公开 API 单元测试验证。

## Current Phase
Complete

## Phases

### Phase 1: 规格与现状确认
- [x] 读取交接文档、Issue #36、当前实现与调用点
- [x] 读取 Kotlin 风格与架构参考
- [x] 确认无项目内额外 AGENTS.md 约束
- **Status:** complete

### Phase 2: API 与状态机映射
- [x] 从既有设计记录确认公开签名方向
- [x] 映射新 API、兼容 API、运行阶段与回调顺序
- [x] 确认配置冲突的校验时机
- **Status:** complete

### Phase 3: 实现
- [x] 实现公开模型、构建入口和异常
- [x] 实现共享运行状态机与兼容行为
- [x] 实现观察错误隔离和回调顺序
- **Status:** complete

### Phase 4: 公开行为测试
- [x] 重写/扩充 TaskRunnerUnitTest
- [x] 覆盖 Issue #36 指定的结果、取消、阶段、顺序与兼容行为
- **Status:** complete

### Phase 5: 验证与审查
- [x] 经用户确认后运行 `./gradlew :core:testDebugUnitTest`
- [x] 对照 Issue #36 与仓库规范审查最终 diff
- [x] 确认未触碰用户 Gradle 版本改动与范围外文件
- **Status:** complete

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| 保留 Builder/Spec/Execution 分层 | Issue #36 明确要求，新旧入口共用一个 runtime |
| 所有行为测试只走 TaskRunnerUnitTest | 避免测试耦合内部执行类 |
| 不修改 EF-Chat，不提交 | 交接明确限定 |
| `ExecutionAttempt`/终态对象统一使用 `metrics`，并保留 `executionMetrics` 兼容别名 | 新契约清晰，同时降低旧调用迁移成本 |
| callback 对应关系为 result/attemptSuccess、throwable/attemptFailure、exhausted/onExhausted、timeout/onTimeout、cancel/onCancel、finally/onFinished | 用于立即拒绝同一通知的新旧 API 混用 |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| `gh` 命令不存在 | 1 | 使用 GitHub REST API 只读获取 Issue 正文 |
| read_thread 的 turnLimit 不能超过 10 | 1 | 改为允许的 10 后成功读取 |
| standards review 发现公共模型集中在 Execution.kt、旧条件有无用参数 | 1 | 公共模型按职责拆文件并删除无用参数 |
| spec review 发现配置冻结竞态、legacy beforeRetry 通知缺失、控制步骤自有 timeout 误分类 | 1 | 原子冻结快照、补 legacy 通知，并将步骤自有 TimeoutCancellationException 映射 Failure |
| 新增 cancel suppressed 测试误用对象身份断言 | 1 | 保留外部取消对象 identity 断言；suppressed 改断言类型与消息，因为 withContext 可复制取消异常 |

## Notes
- 保留 `core/build.gradle.kts`、`ksp/build.gradle.kts` 的用户版本改动。
- 构建前必须向用户说明影响并获得确认。
- 规格同时要求新 sealed `ExecutionResult` 取代旧 attempt `ExecutionResult`，又要求旧显式类型调用完全源码兼容；同包同名无法兼得。本实现保留旧方法和常用属性访问，不保留显式旧类型注解兼容。
