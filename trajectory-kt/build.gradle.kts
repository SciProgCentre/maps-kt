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

    useContextReceivers()
    useSerialization()
    dependencies {
        api("space.kscience:kmath-geometry:$kmathVersion")
    }
}

readme {
    description = "Path and trajectory optimization (to be moved to a separate project)"
    maturity = space.kscience.gradle.Maturity.DEPRECATED
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))
}
