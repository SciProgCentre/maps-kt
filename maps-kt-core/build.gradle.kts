plugins {
    id("space.kscience.gradle.mpp")
    id("com.android.library")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra

kscience {
    jvm()
    js()
    useSerialization()

    dependencies {
        api(projects.trajectoryKt)
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

readme {
    description = "Core cartography, UI-agnostic"
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))

    feature(
        id = "angles and distances",
    ) { "Type-safe angle and distance measurements." }

    feature(
        id = "ellipsoid",
    ) { "Ellipsoid geometry and distances" }

    feature(
        id = "mercator",
    ) { "Mercator and web-mercator projections" }
}

android {
    namespace = "center.sciprog.maps.core"
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

