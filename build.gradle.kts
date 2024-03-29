import space.kscience.gradle.isInDevelopment
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

val kmathVersion: String by extra("0.3.1-dev-RC")

allprojects {
    group = "center.sciprog"
    version = "0.2.2"

    repositories {
        mavenLocal()
        maven("https://repo.kotlin.link")
        maven("https://maven.pkg.jetbrains.space/spc/p/sci/dev")
    }
}

ksciencePublish {
    pom("https://github.com/SciProgCentre/maps-kt") {
        useApache2Licence()
        useSPCTeam()
    }
    github("SciProgCentre", "maps-kt")
    space(
        if (isInDevelopment) {
            "https://maven.pkg.jetbrains.space/spc/p/sci/dev"
        } else {
            "https://maven.pkg.jetbrains.space/spc/p/sci/maven"
        }
    )
    sonatype()
}

subprojects {
    repositories {
        maven("https://maven.pkg.jetbrains.space/mipt-npm/p/sci/dev")
        google()
        mavenCentral()
        maven("https://repo.kotlin.link")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

readme.readmeTemplate = file("docs/templates/README-TEMPLATE.md")



