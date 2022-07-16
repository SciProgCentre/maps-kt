plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR){
        browser()
    }
    sourceSets {
        commonMain{
            dependencies{
                api("io.github.microutils:kotlin-logging:2.1.23")
            }
        }
        val jvmMain by getting
        val jvmTest by getting
    }
}