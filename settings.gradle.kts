rootProject.name = "maps-kt"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {

    val toolsVersion: String by extra

    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.kotlin.link")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
//        id("com.android.application").version(extra["agp.version"] as String)
//        id("com.android.library").version(extra["agp.version"] as String)
        id("space.kscience.gradle.project") version toolsVersion
        id("space.kscience.gradle.mpp") version toolsVersion
        id("space.kscience.gradle.jvm") version toolsVersion
    }
}

dependencyResolutionManagement {
    val toolsVersion: String by extra

    repositories {
        mavenLocal()
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
    }

    versionCatalogs {
        create("spclibs") {
            from("space.kscience:version-catalog:$toolsVersion")
        }
    }
}


include(
    ":trajectory-kt",
    ":maps-kt-core",
    ":maps-kt-geojson",
//    ":maps-kt-geotiff",
    ":maps-kt-features",
    ":maps-kt-compose",
    ":maps-kt-scheme",
//    ":maps-kt-leaflet",
    ":demo:maps",
    ":demo:scheme",
    ":demo:polygon-editor",
    ":demo:trajectory-playground",
    ":demo:maps-wasm"
)

