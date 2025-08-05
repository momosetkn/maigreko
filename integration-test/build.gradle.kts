plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    implementation(project(":core"))

    // main
    implementation(libs.jdbcPostgresql)

    // test
    api(platform(libs.testcontainersBom))
    testImplementation(libs.testcontainersPostgresql)
    runtimeOnly(libs.jdbcPostgresql)
    testImplementation(libs.bundles.kotest)
}
