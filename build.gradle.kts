import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.6.21"
}

buildscript {
  repositories {
    mavenCentral()
    google()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.1.4")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
  }
}

fun getProperty(propertyName: String, gradleLocalPropertyName: String = propertyName): String {
  return System.getenv(propertyName) ?: gradleLocalProperties(rootDir).getProperty(gradleLocalPropertyName) ?: ""
}

nexusPublishing {
  repositories {
    sonatype {
      username.set(getProperty("OSSRH_USERNAME", "ossrhUsername"))
      password.set(getProperty("OSSRH_PASSWORD", "ossrhPassword"))
      stagingProfileId.set(getProperty("SONATYPE_STAGING_PROFILE_ID", "sonatypeStagingProfileId"))
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
}