plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

kscience{
    jvm()
}

kotlin {
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