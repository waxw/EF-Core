import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
  id("java-library")
  alias(libs.plugins.jetbrainsKotlinJvm)
  alias(libs.plugins.vanniktechMavenPublish)
}

val gavGroupId = "io.github.waxw"
val gavArtifactId = "ksp"
val gavVersion = "0.0.5"

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation(libs.ksp.symbol.api)
  implementation(libs.kotlinpoet)
  implementation(kotlin("reflect"))
}

mavenPublishing {
  configure(
    KotlinJvm(
      javadocJar = JavadocJar.Empty(),
      sourcesJar = true,
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
    name.set("EF-Core KSP")
    description.set("KSP processor support for EF-Core.")
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
}
