plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)
}
