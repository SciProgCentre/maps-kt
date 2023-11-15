plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

kscience{
    js {
        binaries.executable()
    }
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(projects.mapsKtCompose)
                implementation(compose.runtime)
                implementation(compose.html.core)
                implementation(npm("@types/leaflet", "1.9.6"))
            }
        }
    }
}

compose {
    experimental.web{
        application{}
    }
//    web{}
}