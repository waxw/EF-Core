import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import org.gradle.api.publish.maven.MavenPublication

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.ksp)
  alias(libs.plugins.vanniktechMavenPublish)
}

val gavGroupId = "io.github.waxw"
val gavArtifactId = "core"
val gavVersion = "0.0.5-SNAPSHOT"

android {
  namespace = "com.miyako.core"
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }

  publishing {
    singleVariant("debug") {
      withSourcesJar()
      withJavadocJar()
    }
  }

}

dependencies {

  ksp(project(":ksp"))

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
    name.set("EF-Core")
    configurePomMetadata()
  }
}

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("debug") {
        from(components["debug"])
        groupId = gavGroupId
        artifactId = "$gavArtifactId-debug"
        version = gavVersion

        pom {
          name.set("EF-Core Debug")
        }
      }
    }
  }
}

fun MavenPom.configurePomMetadata() {
  description.set("Android Kotlin core utilities for projects.")
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
