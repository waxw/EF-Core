# Progress Log

## Session: 2026-08-12

### Phase 1: 规格与现状确认
- **Status:** complete
- Actions taken:
  - 读取 HANDOFF、Issue #36、当前实现、调用点、共享风格与架构参考。
  - 确认分支为 `task_runner`，仅有两个用户拥有的 Gradle 版本改动。
  - 确认项目内没有作用于 EF-Core 的更具体 AGENTS.md。
- Files created/modified:
  - `task_plan.md`（本次执行记录）
  - `findings.md`（规格与发现记录）
  - `progress.md`（进度记录）

### Phase 2: API 与状态机映射
- **Status:** complete
- Actions taken:
  - 已整理 Issue #36 行为清单。
  - 补读交接中指定的历史设计对话，确认入口、一次性冻结和职责边界。
  - 确定新回调、metrics 阶段规则和异常传播边界。

### Phase 3: 实现
- **Status:** complete
- Actions taken:
  - 实现 retry/poll、ExecutionAttempt/ExecutionResult、异常映射和配置校验。
  - 实现共享 TaskExecution 状态机、五阶段 metrics、取消/总超时/attempt timeout 区分。
  - 实现同步观察器错误隔离。
  - 按用户决定删除 0.0.5 废弃兼容 API、字段及运行时分支。

### Phase 4: 公开行为测试
- **Status:** complete
- Actions taken:
  - 通过 TaskRunnerUnitTest 覆盖 retry/poll、四种终态、beforeRetry、取消、timeout phase 和观察顺序。

### Phase 5: 验证与审查
- **Status:** complete
- Actions taken:
  - 已启动 standards/spec 两轴只读审查。
  - standards 轴的公共类型文件与无用参数问题已修正。
  - spec 轴的配置冻结竞态、legacy beforeRetry 通知、控制步骤 timeout 分类与终态顺序测试已修正。
  - 已明确不承诺旧 `ExecutionResult` 或 0.0.5 API 的源码兼容。
  - 已向用户请求运行 Gradle 单测的确认。
  - 用户批准后运行 `./gradlew :core:testDebugUnitTest`，构建成功。
  - 最终测试结果：TaskRunnerUnitTest 32/32、core 全部 41/41，无 failure/error/skipped。
  - `git diff --check` 通过；原有两个 Gradle 版本改动保持不变。

## Test Results
| Test | Expected | Actual | Status |
|------|----------|--------|--------|
| core unit tests | `./gradlew :core:testDebugUnitTest` | 全部通过 | BUILD SUCCESSFUL；41 tests | pass |

## Error Log
| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 2026-08-12 | `gh: command not found` | 1 | 改用 GitHub REST API 成功获取 Issue |
| 2026-08-12 | `read_thread turnLimit` 超出最大值 10 | 1 | 调整为 10 后成功读取 |
| 2026-08-12 | standards review：公共模型文件职责过多、旧条件参数未使用 | 1 | 拆分文件并移除参数 |
| 2026-08-12 | spec review：冻结竞态、legacy beforeRetry 通知缺失、step timeout 误分类 | 1 | 原子冻结、补通知、修正 timeout 分类并加测试 |
| 2026-08-12 | 新增 cancel suppressed 测试 1/41 失败 | 1 | 确认 suppressed 存在但 withContext 复制取消异常，修正过强的 identity 断言 |

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| Where am I? | 完成 |
| Where am I going? | 向用户交付实现与验证结果 |
| What's the goal? | 完成 EF-Core Issue #36 |
| What have I learned? | 见 findings.md |
| What have I done? | 见上方日志 |

## Session: 2026-08-13

### Phase 6: API 命名与重试策略完善
- **Status:** implementation complete; verification pending
- Actions taken:
  - 将 `TaskRunner.retry`/`poll` 的 `delayMs` 参数改为 `initialDelayMs`。
  - 新增 `retryOn<E>` 白名单策略；多个条件使用 OR 语义，`abortOn` 优先。
  - 将 `stopWhen`、内部条件模型和错误消息统一改为 `completeWhen`。
  - 将 `ExecutionPhase.STOP_CONDITION` 改为 `COMPLETION_CONDITION`。
  - 新增 `retryOn` 匹配、未匹配、predicate 控制、与 `abortOn` 冲突及 predicate 异常测试。
  - 提交 `6ee88e0 Add TaskRunner retry conditions`。
  - 提交 `e58c682 Rename TaskRunner completion condition`。

### Verification
- `git diff --check` 已通过。
- 已扫描 `core`、`app`、`ksp`，当前代码与测试无 `delayMs`、`stopWhen`、`STOP_CONDITION` 残留。
- 尚未重新运行 `./gradlew :core:testDebugUnitTest`；2026-08-12 的 41/41 结果仅作为变更前基线。

## Session: 2026-08-14

### Phase 7: 健壮性修复与最终收口
- **Status:** complete
- Planned actions:
  - 修复规格构建失败后的引用清理。
  - 修复策略 predicate 重抛原异常时的 self-suppression。
  - 补充多 `retryOn` OR/短路契约测试。
  - 运行 TaskRunnerUnitTest 并更新最终状态。
- Completed implementation:
  - `executeResult()` 通过 `try/finally` 保证 `buildSpec()` 失败后仍执行 `cleanUp()`。
  - `abortOn`/`retryOn` 仅在规则异常与 attempt 异常不是同一对象时添加 suppressed。
  - TaskRunnerUnitTest 从 32 个增加到 35 个。
- Verification:
  - 运行 `./gradlew :core:testDebugUnitTest --tests com.miyako.core.TaskRunnerUnitTest`。
  - `BUILD SUCCESSFUL in 16s`；35 tests，0 skipped，0 failures，0 errors。
  - `git diff --check` 通过。
- Error log:
  - Phase 7 状态补丁首次混用了两个文档的上下文，未产生文件修改；读取局部内容后使用分文件补丁修正。

## Session: 2026-08-18

### core/ksp gav 动态版本（分支 0.0.5，独立脚本 gav.gradle.kts）
- **Status:** 实现与静态审查完成；Gradle 验证按用户要求跳过
- Actions taken:
  - **gav 坐标与版本策略全部收敛到独立脚本 `gav.gradle.kts`**（用户要求不放在根 build.gradle.kts）：`gavArtifactIds`（按项目名映射 core->core / ksp->core-ksp）、`resolvedGavVersion`（只解析一次），经 `subprojects {}` 注入各子项目 extra（gavGroupId / gavArtifactId / gavBaseVersion / gavVersion）；`resolveGavVersion()`（发布判定 + 版本拼接）、`gitCommitShort()`（`git rev-parse --short HEAD`，失败回退 "unknown"）、`gitTagsAtHead()`（`git tag --points-at HEAD`）。
  - 根 `build.gradle.kts` 仅保留一行 `apply(from = "gav.gradle.kts")` 加载脚本。
  - **core / ksp 用类型化 delegate 读取**（用户要求，替代 rootProject.extra cast）：
    ```kotlin
    val gavGroupId: String by extra
    val gavArtifactId: String by extra
    val gavBaseVersion: String by extra
    val gavVersion: String by extra
    ```
  - core 的 debug 变体 `"$gavArtifactId-debug"` 自动派生。
  - 版本策略：开发阶段 `0.0.5-<commit 短哈希>-<yyyyMMddHHmmss>`（如 `0.0.5-ce65d4e-20260818100507`）；正式发布使用纯版本号 `0.0.5`。
  - 正式发布触发条件（任一）：`-Prelease=true`、环境变量 `RELEASE=true`、当前 commit 命中 `0.0.5`/`v0.0.5` tag。
  - `gavVersion` 同时作用于 core 的 release 发布与 debug 变体（`core-debug`），以及 ksp 的 `core-ksp`。
- Verification:
  - git helper 命令行实测：`rev-parse --short HEAD` → ce65d4e；`tag --points-at HEAD` → 空（开发模式生效）。
  - 模拟开发版本：`0.0.5-ce65d4e-20260818100507`；`git diff --check` 通过。
  - 静态检查：根脚本无 gav 逻辑残留（仅 apply 行）；无 `0.0.5-SNAPSHOT` / 重复 helper 残留；未编译（用户要求）。

## 2026-08-18 测试发布（mavenLocal，用户要求）

- **Status:** 通过
- Actions taken:
  - `./gradlew :core:publishToMavenLocal`（开发模式）→ 发布至 `~/.m2/io/github/waxw/core/0.0.5-ce65d4e-20260818110448/`，含 aar/pom/sources/javadoc + `.asc` 签名，`core-debug` 同步发布。
  - `./gradlew :ksp:publishToMavenLocal` → `io.github.waxw:core-ksp:0.0.5-ce65d4e-20260818110524`。
  - `./gradlew :core:publishToMavenLocal -Prelease=true` → 发布至 `0.0.5/`（纯版本号），pom `<version>0.0.5</version>` 确认。
- Verification:
  - 开发模式版本 `0.0.5-<sha7>-<yyyyMMddHHmmss>` ✅；正式发布 `-Prelease=true` → `0.0.5` ✅。
  - 签名（signAllPublications）在 mavenLocal 发布链路正常（.asc 产物生成）。
  - 沙箱升级说明：Gradle 写 `~/.gradle`/`~/.m2` 需 danger-full-access，本次测试发布在用户授权下完成。

## Session: 2026-09-03

### Phase 8: core KMP 与 core-android 模块迁移
- **Status:** complete
- Actions taken:
  - 读取项目结构、Gradle 配置、平台 import、Kotlin 风格和架构参考。
  - 确认 `:core` 的 JVM 专属 API 边界及 `:core-compose` 的 Android-only 属性。
  - 确认保留用户已有的 README、base extensions 和 TaskRunner 移植规范改动。
  - 确认 KMP 发布插件支持 `KotlinMultiplatform`，并识别默认 Android target 与 `core-android` UI 制品的坐标冲突。
  - 决定将 KMP Android target 命名为 `androidCore`，公共根坐标仍保留 `io.github.waxw:core`。
  - 将 `:core` 改为 KMP，配置 Android、Desktop JVM、iOS x64/arm64/simulatorArm64 targets。
  - 将公共源码/测试迁入 `commonMain`/`commonTest`，用公共单调时钟与平台 runtime actual 替换 JVM API。
  - 使用 `kotlinx.atomicfu` 保留 TaskRunner 的 one-shot 与配置冻结并发保证。
  - 将 `core-ui/` 重命名为 `core-android/`，同步项目依赖、GAV 映射和文档。
  - 静态检查通过：产品代码与文档无旧模块引用，common source set 无 Android/JVM import，`git diff --check` 通过。
  - 尚未运行 Gradle；按项目规则需先向用户说明影响并获得确认。
  - 用户已确认执行三平台编译和单元测试。
  - `:core:tasks --all` 配置成功，确认 Android、Desktop、iOS 编译与测试任务均已生成。
  - 首次组合验证在 `compileKotlinDesktop` 发现 `AtomicBoolean.get()` 不属于 AtomicFU 公共 API；iOS worker 在已失败状态下长时间未退出，因此终止该次构建。
  - 已将读取改为 `executed.value`；后续按平台分组重新验证。
  - `./gradlew :core:desktopTest` 通过，Desktop 公共源码与 commonTest 编译、执行成功。
  - Android 组验证通过：`:core:testDebugUnitTest`、`:core-android:testDebugUnitTest`、`:core-compose:testDebugUnitTest`、`:app:testDebugUnitTest`，86 个 Gradle task 成功。
  - Android manifest 合并报告 `:core` 与 `:app` namespace 同为 `com.miyako.core`；将 `:core` Android namespace 改为 `com.miyako.core.shared`，不影响源码包名。
  - iOS 首次构建完成 Kotlin/Native LLVM/sysroot 下载，随后在 `NSDate.timeIntervalSince1970` 属性解析失败。
  - 检查 Kotlin/Native 2.0 Foundation metadata，确认存在 `NSDate.timeIntervalSince1970()`，已改用方法形式。
  - 第二次 iOS 编译仍未解析同名方法；确认它是 Foundation category 的包级扩展，补充显式 import。
  - 第三次 iOS 构建已完成源码/测试编译和链接；43 个测试仅 `test_hex` 因硬编码 JVM 类名失败。
  - iOS 实际类名为 `Int`、JVM 为 `Integer`；公共测试改为验证平台无关的 identity 字符串结构与哈希格式。
  - `:core:iosSimulatorArm64Test` 最终通过：43 tests，0 skipped/failure/error。
  - `:core:compileKotlinIosArm64` 与 `:core:compileKotlinIosX64` 通过，三个已配置 iOS targets 均完成编译验证。
  - 最终 Desktop + Android 回归通过：`:core:desktopTest`、`:core:testDebugUnitTest`、`:core-android:testDebugUnitTest`、`:core-compose:testDebugUnitTest`、`:app:testDebugUnitTest`。
  - 测试报告合计 131 tests：core 每个平台 43，core-android 1，app 1；全部 0 skipped/failure/error，core-compose 无测试源码。
  - 将 KMP 生成的根目录 `.kotlin/` 加入 `.gitignore`。
  - Kotlin 2.0 对本机 Xcode 26.6 输出兼容性范围警告，但 iOS 编译、链接和测试均成功；未添加 suppress 配置。
