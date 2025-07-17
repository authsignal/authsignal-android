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

    compileSdk = 34

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    return System.getenv(name) ?: gradleLocalProperties(rootDir).getProperty(name)
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

val ktorVersion: String by project

dependencies {
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
}