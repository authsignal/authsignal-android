plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")

  `maven-publish`
}

val baseURL: String by project

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

val pomGroup: String by project
val pomArtifactId: String by project
val versionName: String by project

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = pomGroup
      artifactId = pomArtifactId
      version = versionName

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

val ktorVersion: String by project

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.20")
  implementation("io.ktor:ktor-client-android:$ktorVersion")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}