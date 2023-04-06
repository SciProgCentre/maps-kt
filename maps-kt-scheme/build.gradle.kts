plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    `maven-publish`
}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                api(projects.mapsKtFeatures)
                api("io.github.microutils:kotlin-logging:2.1.23")
                api(compose.foundation)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jfree:org.jfree.svg:5.0.4")
                api(compose.desktop.currentOs)
            }
        }
    }
}

//java {
//    targetCompatibility = JVM_TARGET
//}