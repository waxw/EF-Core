plugins {
  id("java-library")
  alias(libs.plugins.jetbrainsKotlinJvm)
  id("com.google.devtools.ksp")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

ksp {
  // arg("InflateViewBinding", "com.miyako.core.viewbinding.ksp.InflateProcessor")
}

dependencies {
  implementation(libs.ksp.symbol)
}
