import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")

  `maven-publish`
  signing
}

android {
  namespace = "com.authsignal.push"

  compileSdk = 33

  defaultConfig {
    minSdk = 23
    targetSdk = 33

    consumerProguardFiles("consumer-rules.pro")
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
}

val sourcesJar by tasks.creating(Jar::class) {
  archiveClassifier.set("sources")
  from(android.sourceSets.getByName("main").java.srcDirs)
}

val pomGroup: String by project
val pomArtifactId: String by project
val versionName: String by project

publishing {
  publications {
    create<MavenPublication>("Authsignal") {
      groupId = pomGroup
      artifactId = pomArtifactId
      version = versionName

      artifact("$buildDir/outputs/aar/push-release.aar")

      pom.withXml {
        asNode().apply {
          appendNode("name", "authsignal-android")
          appendNode("description", "The official Authsignal SDK for Android.")
          appendNode("url", "https://github.com/authsignal/authsignal-android")
          appendNode("licenses").apply {
            appendNode("license").apply {
              appendNode("name", "MIT")
              appendNode("url", "https://github.com/authsignal/authsignal-android/blob/main/LICENSE.md")
            }
          }
          appendNode("developers").apply {
            appendNode("developer").apply {
              appendNode("id", "Authsignal")
              appendNode("name", "Authsignal")
            }
          }
          appendNode("scm").apply {
            appendNode("connection", "scm:git:github.com/authsignal/authsignal-android.git")
            appendNode("developerConnection", "scm:git:ssh://github.com/authsignal/authsignal-android.git")
            appendNode("url", "https://github.com/authsignal/authsignal-android/tree/main")
          }
          appendNode("dependencies").apply {
            project.configurations["releaseImplementation"].allDependencies.forEach {
              appendNode("dependency").apply {
                appendNode("groupId", it.group)
                appendNode("artifactId", it.name)
                appendNode("version", it.version)
              }
            }
          }
        }
      }
    }
  }
}

signing {
  val properties = gradleLocalProperties(rootDir)

  useInMemoryPgpKeys(
    properties.getProperty("signing.keyId"),
    properties.getProperty("signing.key"),
    properties.getProperty("signing.password"),
  )

  sign(publishing.publications)
}

val ktorVersion: String by project

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.20")
  implementation("io.ktor:ktor-client-android:$ktorVersion")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}