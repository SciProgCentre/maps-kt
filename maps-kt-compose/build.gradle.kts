plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
//    id("com.android.library")
    `maven-publish`
}

kscience {
    jvm()
    wasm()

    useCoroutines()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.mapsKtCore)
                api(projects.mapsKtFeatures)
                api(compose.foundation)
                api(project.dependencies.platform(spclibs.ktor.bom))
            }
        }

        getByName("jvmMain"){
            dependencies {
                api("io.ktor:ktor-client-cio")
            }
        }
//
//        getByName("jsMain"){
//            dependencies {
//                api("io.ktor:ktor-client-js")
//            }
//        }

        getByName("jvmTest") {
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

//tasks.getByName<Copy>("downloadWix"){
//    duplicatesStrategy = DuplicatesStrategy.WARN
//}
