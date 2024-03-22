plugins {
    id("space.kscience.gradle.mpp")
    id("com.android.library")
    `maven-publish`
}

group = "space.kscience"

val kmathVersion: String by rootProject.extra

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}

kscience {
    jvm()
    js()
    native()

    useContextReceivers()
    useSerialization()
    dependencies {
        api("space.kscience:kmath-geometry:$kmathVersion")
    }
}

readme {
    description = "Path and trajectory optimization"
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))
}

android {
    namespace = "center.sciprog.maps.trajectory"
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


