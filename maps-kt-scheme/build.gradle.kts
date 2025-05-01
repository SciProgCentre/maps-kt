plugins {
    id("space.kscience.gradle.mpp")
    alias(spclibs.plugins.compose.compiler)
    alias(spclibs.plugins.compose.jb)
    `maven-publish`
}

kscience {
    jvm()
//    js()
    wasm()

    commonMain {
        api(projects.mapsKtFeatures)
    }
    jvmMain {
        api(compose.desktop.currentOs)
    }
}


readme{
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}

//java {
//    targetCompatibility = JVM_TARGET
//}