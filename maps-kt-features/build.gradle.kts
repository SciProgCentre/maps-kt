plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra

kscience {
    jvm()
//    js()
    wasm{
        browser {
            testTask {
                enabled = false
            }
        }
    }

    useCoroutines()

    useSerialization {
        json()
    }

    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST) {
        protobuf()
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.trajectoryKt)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.ui)
                api("io.github.oshai:kotlin-logging:6.0.3")
            }
        }
    }
}