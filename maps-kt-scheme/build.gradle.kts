plugins {
    id("space.kscience.gradle.mpp")
    id("org.jetbrains.compose")
    `maven-publish`
}

kscience{
    jvm()
//    js()
    wasm()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.mapsKtFeatures)
            }
        }
        getByName("jvmMain"){
            dependencies {
                implementation("org.jfree:org.jfree.svg:5.0.4")
                api(compose.desktop.currentOs)
            }
        }
    }
}

//java {
//    targetCompatibility = JVM_TARGET
//}