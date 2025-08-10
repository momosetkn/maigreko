plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    implementation(project(":core"))

    // main
    implementation(libs.jdbcPostgresql)

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    api(platform(libs.testcontainersBom))
    testImplementation(libs.testcontainersPostgresql)
    runtimeOnly(libs.jdbcPostgresql)
    testImplementation(libs.bundles.kotest)
}
