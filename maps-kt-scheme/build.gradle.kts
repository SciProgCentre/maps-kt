import org.jetbrains.compose.compose
import space.kscience.gradle.KScienceVersions.JVM_TARGET


plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    `maven-publish`
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JVM_TARGET.toString()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api("io.github.microutils:kotlin-logging:2.1.23")
                api(compose.foundation)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jfree:org.jfree.svg:5.0.3")
                api(compose.desktop.currentOs)
            }
        }
    }
}

java{
    targetCompatibility = JVM_TARGET
}