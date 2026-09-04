@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.jetbrainsKotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.vanniktechMavenPublish)
}

// gav 坐标与版本策略统一见根目录 gav.gradle.kts（subprojects 注入，类型化读取）
val gavGroupId: String by extra
val gavArtifactId: String by extra
val gavBaseVersion: String by extra

/**
 * 版本策略见 gav.gradle.kts：
 * 开发阶段 `0.1.0-alpha-01-<commit 短哈希>-<时间戳>`；发布使用纯版本号 `0.1.0-alpha-01`。
 */
val gavVersion: String by extra

kotlin {
  androidTarget("androidCore") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.atomicfu)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

android {
  namespace = "com.miyako.core.shared"
  compileSdk = 34

  defaultConfig {
    minSdk = 24
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

mavenPublishing {
  configure(
    KotlinMultiplatform(
      javadocJar = JavadocJar.Empty(),
      sourcesJar = true,
      androidVariantsToPublish = listOf("release"),
    ),
  )

  publishToMavenCentral()
  signAllPublications()

  coordinates(
    groupId = gavGroupId,
    artifactId = gavArtifactId,
    version = gavVersion,
  )

  pom {
    name.set("EF-Core")
    configurePomMetadata()
  }
}

fun MavenPom.configurePomMetadata() {
  description.set("Kotlin Multiplatform core utilities for EF-Core.")
  inceptionYear.set("2024")
  url.set("https://github.com/waxw/EF-Core")

  licenses {
    license {
      name.set("The Apache License, Version 2.0")
      url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
      distribution.set("repo")
    }
  }

  developers {
    developer {
      id.set("waxw")
      name.set("waxw")
      url.set("https://github.com/waxw")
    }
  }

  scm {
    url.set("https://github.com/waxw/EF-Core")
    connection.set("scm:git:git://github.com/waxw/EF-Core.git")
    developerConnection.set("scm:git:ssh://git@github.com/waxw/EF-Core.git")
  }
}
