import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
}

repositories {
    maven("https://repo.osgeo.org/repository/release/")
    exclusiveContent {
        forRepository {
            maven("https://repo.osgeo.org/repository/release/")
        }
        filter {
            includeGroup("javax.media")
        }
    }
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.mapsKtCompose)
                implementation(projects.mapsKtGeojson)
                implementation(projects.mapsKtGeotools)
                implementation(compose.desktop.currentOs)
                implementation(compose.materialIconsExtended)

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
