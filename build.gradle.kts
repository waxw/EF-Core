import org.jmailen.gradle.kotlinter.tasks.FormatTask
import java.io.FileInputStream
import java.io.InputStreamReader

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.jetbrainsKotlinAndroid) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.jetbrainsKotlinJvm) apply false
}

buildscript {
  dependencies {
    classpath(libs.kotlinter)
  }
}

allprojects {
  apply(plugin = "org.jmailen.kotlinter")
}

tasks.register<FormatTask>("ktFormat") {
  val files = project.properties["formatFile"] as? String
  files?.split(",")?.filter { it.isEmpty().not() }?.let {
    source(it)
  }
  (project.properties["file"] as? String)?.let {
    val file = file(it)
    println("file: $it")
    if (file.exists()) {
      InputStreamReader(FileInputStream(file)).readLines()
        .filter { it.isEmpty().not() }
        .map { it.replace("${rootProject.name}/", "") }
        .let {
          println("format: $it")
          source(it)
        }
    }
  }
}

tasks.create("setGitHooksPath") {
  description = "Set custom Git hooks path"
  group = "git"
  // 要设置的自定义Git hooks路径
  val hooksPath = "$rootDir/config/githooks"


  doLast {
    // 修改Git配置，设置自定义Git hooks路径
    exec {
      commandLine = listOf("git", "config", "--local", "core.hooksPath", hooksPath)
    }
  }
}
