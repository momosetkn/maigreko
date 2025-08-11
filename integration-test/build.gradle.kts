plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":postgresql-dialect"))

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)
    testApi(platform(libs.testcontainersBom))
    testImplementation(libs.testcontainersPostgresql)
    testRuntimeOnly(libs.jdbcPostgresql)
    testImplementation(project(":postgresql-test-utils"))
    testImplementation(project(":test-utils"))
}
