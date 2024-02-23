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
    wasm()

    useContextReceivers()
    useSerialization{
        json()
    }
    dependencies {
        api("space.kscience:kmath-geometry:$kmathVersion")
    }
}

readme {
    description = "Path and trajectory optimization"
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))
}
