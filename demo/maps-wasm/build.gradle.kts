import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

//val ktorVersion: String by rootProject.extra

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(projects.mapsKtScheme)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class) api(compose.components.resources)
            }
        }
    }
}

compose {
    experimental.web{
        application{}
    }
}