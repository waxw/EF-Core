import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.vanniktechMavenPublish)
}

// gav 坐标与版本策略统一见根目录 gav.gradle.kts（subprojects 注入，类型化读取）
val gavGroupId: String by extra
val gavArtifactId: String by extra
val gavBaseVersion: String by extra

/**
 * 版本策略见 gav.gradle.kts：
 * 开发阶段 `0.0.5-<commit 短哈希>-<时间戳>`；正式发布使用纯版本号 `0.0.5`。
 */
val gavVersion: String by extra

android {
  namespace = "com.miyako.core.ui"
  compileSdk = 34

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    viewBinding = true
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  lint {
    abortOnError = false
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

}

dependencies {
  // MVI 纯 JVM 契约（UiState/UiEffect/UiAction）来自 core，作为公开 API 暴露
  api(project(":core"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.recyclerview)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  // Coroutines 测试库
  testImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
  configure(
    AndroidSingleVariantLibrary(
      variant = "release",
      sourcesJar = true,
      publishJavadocJar = true,
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
    name.set("EF-Core UI")
    configurePomMetadata()
  }
}

fun MavenPom.configurePomMetadata() {
  description.set("Android UI utilities for EF-Core (views, viewbinding, rv, mvi).")
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
