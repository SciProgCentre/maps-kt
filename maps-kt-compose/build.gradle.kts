plugins {
    id("space.kscience.gradle.mpp")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
//    id("com.android.library")
    `maven-publish`
}

kscience {
    jvm()
    wasm()

    useCoroutines()

    commonMain{
        api(projects.mapsKtCore)
        api(projects.mapsKtFeatures)
        api(dependencies.platform(spclibs.ktor.bom))
        api(compose.foundation)
    }
    jvmMain{
        api("io.ktor:ktor-client-cio")
    }
    jvmTest{
        implementation(spclibs.kotlinx.coroutines.test)

        implementation(spclibs.logback.classic)

        implementation(compose.desktop.currentOs)
    }
}

readme {
    description = "Compose-multiplaform implementation for web-mercator tiled maps"
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))

    feature(
        id = "osm",
    ) { "OpenStreetMap tile provider." }
}
