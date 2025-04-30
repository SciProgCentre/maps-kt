@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
}

//val ktorVersion: String by rootProject.extra

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain {
            dependencies {
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.components.resources)
            }
        }

        wasmJsMain {
            dependencies {
                implementation(projects.mapsKtScheme)
            }
        }
    }
}

compose {
    web {

    }
}