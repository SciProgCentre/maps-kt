import space.kscience.gradle.isInDevelopment
import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

val ktorVersion by extra("2.0.3")

allprojects {
    group = "center.sciprog"
    version = "0.1.0-dev-2"
}

ksciencePublish{
    pom("https://github.com/SciProgCentre/maps-kt") {
        useApache2Licence()
        useSPCTeam()
    }
    github("maps-kt", "SciProgCentre")
    space(
        if (isInDevelopment) {
            "https://maven.pkg.jetbrains.space/mipt-npm/p/sci/dev"
        } else {
            "https://maven.pkg.jetbrains.space/mipt-npm/p/sci/release"
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



