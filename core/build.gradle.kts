plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)
    api(libs.classgraph)

    // test
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}
