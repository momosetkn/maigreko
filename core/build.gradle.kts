plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    // test
    testImplementation(libs.bundles.kotest)
}
