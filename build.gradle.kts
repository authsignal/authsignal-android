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
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")
    }
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.26.2")
        }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}