import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvm()
    jvmToolchain(11)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.mapsKtScheme)
                implementation(compose.desktop.currentOs)
                implementation("ch.qos.logback:logback-classic:1.2.11")
            }
        }
        val jvmTest by getting
    }
}

compose{
    desktop {
        application {
            mainClass = "MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "scheme-compose-demo"
                packageVersion = "1.0.0"
            }
        }
    }
}
