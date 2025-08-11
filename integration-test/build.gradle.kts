plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":postgresql-dialect"))
    implementation(project(":mysql-dialect"))

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)
    testApi(platform(libs.testcontainersBom))

    // PostgreSQL dependencies
    testImplementation(libs.testcontainersPostgresql)
    testRuntimeOnly(libs.jdbcPostgresql)
    testImplementation(project(":postgresql-test-utils"))

    // MySQL dependencies
    testImplementation(libs.testcontainersMysql)
    testRuntimeOnly(libs.jdbcMysql)
    testImplementation(project(":mysql-test-utils"))

    testImplementation(project(":test-utils"))
}
