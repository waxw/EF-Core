plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  `maven-publish`
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.miyako.core"
  compileSdk = 34

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
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

  // publishing {
  //   multipleVariants {
  //     includeBuildTypeValues("debug", "release")
  //   }
  // }
}

dependencies {

  implementation(project(":ksp"))
  ksp(project(":ksp"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("release") {
        from(components["release"])
        groupId = "com.miyako"
        artifactId = "core"
        version = "0.0.1"
      }
      register<MavenPublication>("debug") {
        from(components["debug"])
        groupId = "com.miyako"
        artifactId = "core-debug"
        version = "0.0.1"
      }
    }
  }
}
