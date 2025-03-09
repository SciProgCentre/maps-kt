import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvmToolchain(11)
    jvm()
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.mapsKtCompose)
                implementation(projects.mapsKtGeojson)
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-cio")
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
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "maps-compose-demo"
                packageVersion = "1.0.0"
            }
        }
    }
}
