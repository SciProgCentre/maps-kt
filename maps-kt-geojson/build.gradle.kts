plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.mapsKtCore)
                api(projects.mapsKtFeatures)
                api(spclibs.kotlinx.serialization.json)
            }
        }
    }
}