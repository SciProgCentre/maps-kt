plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val ktorVersion: String by rootProject.extra

kotlin {
    js {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(projects.mapsKtCompose)
                implementation(compose.runtime)
                implementation(compose.html.core)
            }
        }
    }
}

compose {
    web {}
}