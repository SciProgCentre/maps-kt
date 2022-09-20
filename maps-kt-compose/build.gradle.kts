import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    `maven-publish`
}

val ktorVersion: String by rootProject.extra

kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Warning
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = space.kscience.gradle.KScienceVersions.JVM_TARGET.toString()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(projects.mapsKtCore)
                api(compose.foundation)
                api("io.ktor:ktor-client-core:$ktorVersion")
                api("io.github.microutils:kotlin-logging:2.1.23")
            }
        }
        val jvmMain by getting{

        }
        val jvmTest by getting{
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation(compose.desktop.currentOs)
                implementation(spclibs.kotlinx.coroutines.test)

                implementation("ch.qos.logback:logback-classic:1.2.11")

                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.8.2")
            }
        }
    }
}

java{
    targetCompatibility = space.kscience.gradle.KScienceVersions.JVM_TARGET
}

tasks.withType<Test> {
    useJUnitPlatform()
}