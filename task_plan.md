# Task Plan: TaskRunner 与 KMP 模块迁移

## Goal
完成 EF-Core TaskRunner 契约，并将 `core` 迁移为 KMP、`core-ui` 重命名为 `core-android`，通过 Android、Desktop 与 iOS 编译和单元测试验证。

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
- [x] 映射公开 API、运行阶段与回调顺序
- [x] 确认配置冲突的校验时机
- **Status:** complete

### Phase 3: 实现
- [x] 实现公开模型、构建入口和异常
- [x] 实现共享运行状态机
- [x] 实现观察错误隔离和回调顺序
- **Status:** complete

### Phase 4: 公开行为测试
- [x] 重写/扩充 TaskRunnerUnitTest
- [x] 覆盖 Issue #36 指定的结果、取消、阶段与顺序
- **Status:** complete

### Phase 5: 验证与审查
- [x] 经用户确认后运行 `./gradlew :core:testDebugUnitTest`
- [x] 对照 Issue #36 与仓库规范审查最终 diff
- [x] 确认未触碰用户 Gradle 版本改动与范围外文件
- **Status:** complete

### Phase 6: API 命名与重试策略完善
- [x] 将公开参数 `delayMs` 统一改为 `initialDelayMs`
- [x] 新增类型化 `retryOn<E>` 重试白名单
- [x] 将 `stopWhen` 及相关内部模型统一改为 `completeWhen`
- [x] 将 `ExecutionPhase.STOP_CONDITION` 改为 `COMPLETION_CONDITION`
- [x] 补充 `retryOn` 匹配、未匹配、predicate、优先级和规则异常测试
- [x] 经用户确认后运行 `./gradlew :core:testDebugUnitTest --tests com.miyako.core.TaskRunnerUnitTest`
- **Status:** complete

### Phase 7: 健壮性修复与最终收口
- [x] 保证 buildSpec 失败时仍清理一次性 runner 引用
- [x] 防止 abortOn/retryOn predicate 重抛原异常时发生 self-suppression
- [x] 补充 self-suppression 与多 retryOn OR/短路测试
- [x] 运行 TaskRunnerUnitTest 并记录最新结果
- [x] 更新规格与进度文档
- **Status:** complete

### Phase 8: core KMP 与 core-android 模块迁移
- [x] 从已合并 0.0.5 的 main 创建 `feature_kmp`，将预发布版本提升为 `0.1.0-alpha-01`
- [x] 确认 KMP target、发布配置和 JVM 专属 API 边界
- [x] 将 `:core` 转为 Kotlin Multiplatform，并迁移 source set
- [x] 将 `:core-ui` 重命名为 `:core-android`，同步依赖与文档
- [x] 静态检查模块引用、源码平台依赖和 diff
- [x] 经用户确认后运行最小 Gradle 构建与测试
- **Status:** complete

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| 保留 Builder/Spec/Execution 分层 | Issue #36 明确要求，新旧入口共用一个 runtime |
| 所有行为测试只走 TaskRunnerUnitTest | 避免测试耦合内部执行类 |
| 不修改 EF-Chat，不提交 | 交接明确限定 |
| `ExecutionAttempt`/终态对象统一使用 `metrics` | 不保留旧字段别名，调用方直接迁移到新契约 |
| 删除 0.0.5 构造器、旧 DSL、旧回调和兼容执行分支 | 用户决定不做旧 API 兼容，避免维护双套语义 |
| `delayMs` 改为 `initialDelayMs` | 明确它是首次执行前延迟，避免与 `intervalMs` 混淆 |
| 增加 `retryOn<E>` | 支持仅重试指定异常类型或子集；多个条件使用 OR 语义 |
| `abortOn` 优先于 `retryOn` | 同一异常同时匹配时优先终止，避免策略冲突 |
| `stopWhen` 改为 `completeWhen` | 条件命中产生 Success，名称直接表达完成语义 |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| `gh` 命令不存在 | 1 | 使用 GitHub REST API 只读获取 Issue 正文 |
| read_thread 的 turnLimit 不能超过 10 | 1 | 改为允许的 10 后成功读取 |
| standards review 发现公共模型集中在 Execution.kt、旧条件有无用参数 | 1 | 公共模型按职责拆文件并删除无用参数 |
| spec review 发现配置冻结竞态、legacy beforeRetry 通知缺失、控制步骤自有 timeout 误分类 | 1 | 原子冻结快照、补 legacy 通知，并将步骤自有 TimeoutCancellationException 映射 Failure |
| 新增 cancel suppressed 测试误用对象身份断言 | 1 | 保留外部取消对象 identity 断言；suppressed 改断言类型与消息，因为 withContext 可复制取消异常 |
| Phase 7 状态补丁混用了 task_plan/progress 上下文 | 1 | 读取两个局部段落后改用分文件精确补丁 |
| KMP 首次编译中 `AtomicBoolean.get()` 不存在 | 1 | 改用 AtomicFU 公共属性 `executed.value`，保持 `compareAndSet` 语义不变 |
| 组合构建在 Desktop 失败后 iOS worker 长时间未退出 | 1 | 终止已失败构建，后续按 Desktop、Android、iOS 分组验证 |
| KMP core 与示例 app 使用相同 Android namespace | 1 | 将 core 的 Android namespace 改为 `com.miyako.core.shared`，公开 Kotlin 包名保持不变 |
| iOS commonized Foundation 无法解析 `NSDate.timeIntervalSince1970` 属性 | 1 | 根据 Kotlin/Native 2.0 Foundation 元数据改用明确生成的 `timeIntervalSince1970()` 方法 |
| 改成 `timeIntervalSince1970()` 后仍未解析 | 2 | 确认该 Category API 是包级扩展，显式导入 `platform.Foundation.timeIntervalSince1970` |
| iOS 的 `hex` 测试硬编码 JVM 类名 `Integer` | 1 | 公共测试改为验证非空类名前缀和稳定的十六进制哈希格式 |

## Notes
- 实施期间保留了 `core/build.gradle.kts`、`ksp/build.gradle.kts` 当时已有的用户版本改动。
- 构建前必须向用户说明影响并获得确认。
- 当前 TaskRunner 只提供 `retry`/`poll` 构建入口及新观察 API，不承诺 0.0.5 源码兼容。
- 2026-08-14 运行 TaskRunnerUnitTest 35/35 通过，无 failure/error/skipped；本轮未运行 core 其他单元测试。
