// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.jetbrainsKotlinAndroid) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.jetbrainsKotlinJvm) apply false
}

buildscript {
  dependencies {
    classpath(libs.ksp.gradle.plugin)
    classpath(libs.ksp.symbol)
  }
}
