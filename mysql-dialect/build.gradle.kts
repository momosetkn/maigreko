dependencies {
    // Core module dependency
    api(project(":core"))

    // MySQL database driver
    implementation(libs.jdbcMysql)

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)

    testApi(platform(libs.testcontainersBom))
    testImplementation(libs.testcontainersMysql)
    testRuntimeOnly(libs.jdbcMysql)
    testImplementation(libs.bundles.kotest)
    testImplementation(project(":mysql-test-utils"))
    testImplementation(project(":test-utils"))
}
