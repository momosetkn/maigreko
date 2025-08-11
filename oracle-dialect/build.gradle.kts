dependencies {
    // Core module dependency
    api(project(":core"))

    // Oracle database driver
    implementation(libs.jdbcOracle)

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    testImplementation(libs.bundles.kotest)
}
