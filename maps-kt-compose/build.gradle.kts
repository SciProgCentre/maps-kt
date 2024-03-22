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
                api(projects.mapsKtCore)
                api(projects.mapsKtFeatures)
                api(compose.foundation)
                api(project.dependencies.platform(spclibs.ktor.bom))
                api("io.ktor:ktor-client-core")
                api("io.github.microutils:kotlin-logging:2.1.23")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio")
                implementation(compose.desktop.currentOs)
                implementation(spclibs.kotlinx.coroutines.test)

                implementation(spclibs.logback.classic)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio")
                implementation("androidx.activity:activity-compose:1.8.2")
            }
        }
    }
}

readme {
    description = "Compose-multiplaform implementation for web-mercator tiled maps"
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))

    feature(
        id = "osm",
    ) { "OpenStreetMap tile provider." }
}

android {
    namespace = "center.sciprog.maps.compose"
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
