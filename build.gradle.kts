import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

buildscript {
  repositories {
    mavenCentral()
    google()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
  }

  dependencies {
    classpath("com.android.tools.build:gradle:8.1.4")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
  }
}

nexusPublishing {
  repositories {
    sonatype {
      val properties = gradleLocalProperties(rootDir)

      username.set(properties.getProperty("ossrhUsername"))
      password.set(properties.getProperty("ossrhPassword"))
      stagingProfileId.set(properties.getProperty("sonatypeStagingProfileId"))
      nexusUrl.set(uri(properties.getProperty("nexusUrl")))
      snapshotRepositoryUrl.set(uri(properties.getProperty("snapshotRepositoryUrl")))
    }
  }
}
