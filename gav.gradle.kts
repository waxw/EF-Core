import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * gav 坐标与版本策略（唯一出处）。由根构建脚本 `apply(from = "gav.gradle.kts")` 加载，
 * 经 `subprojects {}` 注入各子模块，子模块用类型化写法读取：
 *
 * ```kotlin
 * val gavGroupId: String by extra
 * val gavArtifactId: String by extra
 * val gavBaseVersion: String by extra
 * val gavVersion: String by extra
 * ```
 *
 * 版本规则：
 * - 开发阶段：`<base>-<commit 短哈希>-<时间戳 yyyyMMddHHmmss>`，如 `0.1.0-alpha-01-a1b2c3d-20260904183000`
 * - 发布：使用纯版本号 `<base>`
 *
 * 发布版本触发条件（任一满足）：
 * 1. Gradle 属性：`./gradlew publish -Prelease=true`
 * 2. 环境变量：`RELEASE=true ./gradlew publish`
 * 3. 当前 commit 命中 `<base>` 或 `v<base>` 的 Git tag
 */

/** 各发布模块的 artifactId（按项目名） */
val gavArtifactIds = mapOf(
  "core" to "core",
  "core-android" to "core-android",
  "core-compose" to "core-compose",
  "core-ksp" to "core-ksp",
)

// 版本只解析一次，注入所有子项目
val resolvedGavVersion = resolveGavVersion("0.1.0-alpha-01")

subprojects {
  extra["gavGroupId"] = "io.github.waxw"
  extra["gavArtifactId"] = gavArtifactIds[name] ?: name
  extra["gavBaseVersion"] = "0.1.0-alpha-01"
  extra["gavVersion"] = resolvedGavVersion
}

fun resolveGavVersion(baseVersion: String): String {
  val isRelease = providers.gradleProperty("release").orNull == "true" ||
    System.getenv("RELEASE") == "true" ||
    gitTagsAtHead().any { it == baseVersion || it == "v$baseVersion" }
  return if (isRelease) {
    baseVersion
  } else {
    "$baseVersion-${gitCommitShort()}-${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}"
  }
}

private fun gitCommitShort(): String = runCatching {
  gitCommand("rev-parse", "--short", "HEAD").trim()
}.getOrDefault("unknown")

private fun gitTagsAtHead(): List<String> = runCatching {
  gitCommand("tag", "--points-at", "HEAD").lines()
}.getOrDefault(emptyList())

private fun gitCommand(vararg args: String): String {
  return ProcessBuilder("git", *args)
    .redirectErrorStream(true)
    .start()
    .inputStream.bufferedReader().use { it.readText() }
}
