dependencies {
    // Core module dependency
    api(project(":core"))

    // H2 database driver
    implementation(libs.jdbcH2)

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)
}
