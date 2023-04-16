import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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
                implementation("ch.qos.logback:logback-classic:1.2.11")
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
