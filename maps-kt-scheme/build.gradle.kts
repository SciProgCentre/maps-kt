import org.jetbrains.compose.compose


plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
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
                api(compose.desktop.currentOs)
            }
        }
    }
}
