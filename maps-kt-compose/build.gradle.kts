import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    `maven-publish`
}


kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Warning
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = space.kscience.gradle.KScienceVersions.JVM_TARGET.toString()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(projects.mapsKtCore)
                api(compose.foundation)
                api(project.dependencies.platform(spclibs.ktor.bom))
                api("io.ktor:ktor-client-core")
                api("io.github.microutils:kotlin-logging:2.1.23")
            }
        }
        val jvmMain by getting {
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio")
                implementation(compose.desktop.currentOs)
                implementation(spclibs.kotlinx.coroutines.test)

                implementation("ch.qos.logback:logback-classic:1.2.11")

                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.8.2")
            }
        }
    }
}

java {
    targetCompatibility = space.kscience.gradle.KScienceVersions.JVM_TARGET
}

tasks.withType<Test> {
    useJUnitPlatform()
}