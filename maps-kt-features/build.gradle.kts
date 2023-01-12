plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(compose.foundation)
            }
        }
    }
}

kscience{
    useSerialization{
        json()
    }
}