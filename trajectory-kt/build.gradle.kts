plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

group = "space.kscience"

val kmathVersion: String by rootProject.extra

kscience{
    jvm()
    js()
    native()
    wasmJs()

    useContextParameters()
    useSerialization{
        json()
    }
    dependencies {
        api(libs.kmath.geometry)
    }
}

readme {
    description = "Path and trajectory optimization"
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
