import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    `maven-publish`
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    sourceSets {
        commonMain{
            dependencies{
                api(projects.mapsKtCore)
                api(compose.foundation)
                api("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        val jvmMain by getting
        val jvmTest by getting
    }
}