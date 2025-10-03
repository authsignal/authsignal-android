import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlinx-serialization")
    id("maven-publish")
    id("org.jreleaser") version "1.19.0"
}

configurations.all {
    resolutionStrategy {
        force("org.apache.commons:commons-compress:1.26.2")
    }
}

android {
    namespace = "com.authsignal"

    compileSdk = 33

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

fun getProperty(name: String): String {
    return System.getenv(name) ?: gradleLocalProperties(rootDir).getProperty(name) ?: ""
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val pomGroup: String by project
val pomArtifactId: String by project
val versionName: String by project

project.version = versionName

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = pomGroup
            artifactId = pomArtifactId

            pom {
                name.set("authsignal-android")
                description.set("The official Authsignal SDK for Android.")
                url.set("https://github.com/authsignal/authsignal-android")
            
                scm {
                    url.set("https://github.com/authsignal/authsignal-android/tree/main")
                    connection.set("scm:git:github.com/authsignal/authsignal-android.git")
                    developerConnection.set("scm:git:ssh://github.com/authsignal/authsignal-android.git")
                }

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/authsignal/authsignal-android/blob/main/LICENSE.md")
                    }
                }

                developers {
                    developer {
                        id.set("Authsignal")
                        name.set("Authsignal")
                    }
                }

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }

    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        java {
            groupId.set("com.authsignal")
            artifactId.set("authsignal-android")
            description.set("Authsignal SDK for Android")
        }
    }
    gitRootSearch = true
    signing {
        setActive("ALWAYS")
        verify = false
        secretKey = getProperty("SIGNING_SECRET_KEY")
        passphrase = getProperty("SIGNING_PASSPHRASE")
    }
    release {
        github {
            skipRelease = true
        }
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    stagingRepository("build/staging-deploy")
                    setActive("ALWAYS")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    username = getProperty("MAVENCENTRAL_USERNAME")
                    password = getProperty("MAVENCENTRAL_PASSWORD")
                    applyMavenCentralRules = false // See: https://github.com/kordamp/pomchecker/issues/21
                    sourceJar = true
                    javadocJar = true
                    sign = true
                }
            }
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-client-android:2.1.3")
    implementation("io.ktor:ktor-client-core:2.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.3")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}