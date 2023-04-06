import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
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
                packageName = "polygon-editor-demo"
                packageVersion = "1.0.0"
            }
        }
    }
}
