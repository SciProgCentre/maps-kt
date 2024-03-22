plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    id("com.android.library")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra

kscience {
    jvm()
    js()

    useSerialization {
        json()
    }

    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST) {
        protobuf()
    }
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(projects.trajectoryKt)
                api(compose.foundation)
            }
        }
    }
}

android {
    namespace = "center.sciprog.maps.feature"
    compileSdk = 34
    compileSdkVersion = "android-34"
    defaultConfig {
        minSdk = 19
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
