plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra

kscience{
    jvm()
    js()
    useSerialization{
        json()
    }

    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST){
        protobuf()
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.trajectoryKt)
                api(compose.foundation)
                api("io.github.oshai:kotlin-logging:5.1.0")
            }
        }
    }
}