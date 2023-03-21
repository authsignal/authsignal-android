plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")

  `maven-publish`
}

val baseURL: String by extra

android {
  namespace = "com.authsignal"

  compileSdk = 33

  defaultConfig {
    minSdk = 23
    targetSdk = 33

    consumerProguardFiles("consumer-rules.pro")

    buildConfigField("String", "BASE_URL", baseURL)
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  publishing {
    multipleVariants {
      allVariants()
    }
  }
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = "com.authsignal"
      artifactId = "authsignal-android"
      version = "1.0"

      afterEvaluate {
        from(components["release"])
      }
    }
  }
  repositories {
    maven {
      name = "authsignal-android"
      url = uri("${project.buildDir}/repo")
    }
  }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.20")
  implementation("io.ktor:ktor-client-android:2.2.4")
  implementation("io.ktor:ktor-client-core:2.2.4")
  implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
}