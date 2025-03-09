plugins {
    kotlin("multiplatform")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvm()
    jvmToolchain(11)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.mapsKtScheme)
                implementation(projects.trajectoryKt)
                implementation(compose.desktop.currentOs)
                implementation(spclibs.logback.classic)
            }
        }
        val jvmTest by getting
    }
}

compose {
    desktop {
        application {
            mainClass = "MainKt"
        }
    }
}
