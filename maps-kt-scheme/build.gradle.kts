plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    id("com.android.library")
    `maven-publish`
}

kscience {
    jvm()
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

android {
    namespace = "center.sciprog.maps.scheme"
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


//java {
//    targetCompatibility = JVM_TARGET
//}