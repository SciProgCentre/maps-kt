plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

kscience {
    jvm()
    js()
    wasmJs()

    commonMain {
        api(spclibs.kotlinx.datetime)
    }
}
