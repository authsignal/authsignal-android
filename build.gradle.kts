plugins {
  id("org.jetbrains.kotlin.plugin.serialization") version "1.6.21"
}

buildscript {
  repositories {
    mavenCentral()
    google()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.2.2")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
  }
}