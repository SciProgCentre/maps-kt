plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra

kscience{
    jvm()
    js()
    native()
    wasm()

    useSerialization()

    dependencies{
        api(projects.trajectoryKt)
    }
}

readme {
    description = "Core cartography, UI-agnostic"
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))

    feature(
        id = "angles and distances",
    ) { "Type-safe angle and distance measurements." }

    feature(
        id = "ellipsoid",
    ) { "Ellipsoid geometry and distances" }

    feature(
        id = "mercator",
    ) { "Mercator and web-mercator projections" }
}