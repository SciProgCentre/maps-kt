plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("space.kscience:kmath-trajectory:$kmathVersion")
                api(compose.foundation)
            }
        }
    }
}

kscience{
    useSerialization{
        json()
    }

    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST){
        protobuf()
    }
}