dependencies {
    // Core module dependency
    api(project(":core"))

    // MariaDB database driver
    implementation(libs.jdbcMariadb)

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)
}
