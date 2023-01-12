plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
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