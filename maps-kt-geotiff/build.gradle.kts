plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

repositories {
    maven("https://repo.osgeo.org/repository/release/")
}

dependencies {
    api("org.geotools:gt-geotiff:27.2") {
        exclude(group = "javax.media", module = "jai_core")
    }

    api(projects.mapsKtCore)
    api(projects.mapsKtFeatures)
}
