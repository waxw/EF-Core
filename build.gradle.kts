// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.jetbrainsKotlinAndroid) apply false
  alias(libs.plugins.androidLibrary) apply false
}

buildscript {
  dependencies {
    classpath(libs.kotlinter)
  }
}

allprojects {
  apply(plugin = "org.jmailen.kotlinter")
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
