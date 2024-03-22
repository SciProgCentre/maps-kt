import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.application")
}

val ktorVersion: String by rootProject.extra

kotlin {
    jvmToolchain(11)
    jvm()
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
                implementation(projects.mapsKtCompose)
                implementation(projects.mapsKtGeojson)
                implementation("io.ktor:ktor-client-cio")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("ch.qos.logback:logback-classic:1.2.11")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.ui:ui-tooling-preview:1.6.2")
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.compose.material:material-icons-core:1.6.2")
            }
        }
        val jvmTest by getting
    }
}

compose {
    desktop {
        application {
            mainClass = "MainKt"
            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "maps-compose-demo"
                packageVersion = "1.0.0"
            }
        }
    }
}

android {
    namespace = "maps.compose.demo"
    compileSdkVersion = "android-34"
    compileSdk = 34

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/androidMain/resources")

    defaultConfig {
        applicationId = "maps.compose.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}