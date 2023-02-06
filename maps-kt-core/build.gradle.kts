plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val kmathVersion: String by rootProject.extra("0.3.1-dev-10")

kscience{
    useSerialization()

    dependencies{
        api("space.kscience:kmath-trajectory:$kmathVersion")
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