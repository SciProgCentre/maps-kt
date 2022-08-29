plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val ktorVersion: String by rootProject.extra

kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Warning
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR) {
        browser()
    }

    sourceSets{
        commonTest{
            dependencies{
                implementation(kotlin("test"))
            }
        }
    }
}