plugins {
  id("java-library")
  alias(libs.plugins.jetbrainsKotlinJvm)
  `maven-publish`
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation(libs.ksp.symbol.api)
  implementation(libs.kotlinpoet)
  implementation(kotlin("reflect"))
}

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("java") {
        from(components["java"])
        groupId = "com.miyako"
        artifactId = "ksp"
        version = "0.0.1"
      }
    }
  }
}
