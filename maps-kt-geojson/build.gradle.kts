plugins {
    id("space.kscience.gradle.mpp")
    id("com.android.library")
    `maven-publish`
}


kscience {
    jvm()
    js()

    useSerialization {
        json()
    }
    dependencies {
        api(projects.mapsKtCore)
        api(projects.mapsKtFeatures)
        api(spclibs.kotlinx.serialization.json)
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
}

android {
    namespace = "center.sciprog.maps.geojson"
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

