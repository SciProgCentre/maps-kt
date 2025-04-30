import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
}

kotlin {
    jvm()
    jvmToolchain(17)
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.components.resources)
            }
        }

        jvmMain{
            dependencies {
                implementation(projects.mapsKtScheme)
                implementation(spclibs.logback.classic)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "MainKt"
            //mainClass = "Joker2023Kt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "scheme-compose-demo"
                packageVersion = "1.0.0"
            }
        }
    }

    resources {
        generateResClass = always
    }
}
