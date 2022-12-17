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
                api(compose.foundation)
            }
        }
        val jvmTest by getting {
            dependencies {
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