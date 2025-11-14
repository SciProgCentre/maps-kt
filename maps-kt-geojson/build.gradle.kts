plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

description = "GeoJson format support"

kscience{
    jvm()
//    js()
    wasmJs()

    useSerialization {
        json()
    }
    dependencies{
        api(projects.mapsKtCore)
        api(projects.mapsKtFeatures)
        api(spclibs.kotlinx.serialization.json)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}