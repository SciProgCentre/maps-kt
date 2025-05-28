plugins {
    id("space.kscience.gradle.mpp")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
    `maven-publish`
}

repositories {
    maven("https://repo.osgeo.org/repository/release/")
    exclusiveContent {
        forRepository {
            maven("https://repo.osgeo.org/repository/release/")
        }
        filter {
            includeGroup("javax.media")
        }
    }
}

kscience {
    jvm()
    useSerialization()
    commonMain {
        api(projects.mapsKtCore)
        api(projects.mapsKtFeatures)
    }
    jvmMain {
        api(libs.gt.geotiff)
        api(libs.gt.epsg.hsql)
        api(libs.gt.shapefile)
    }
}
