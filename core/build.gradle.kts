
dependencies {
    // main
    implementation(libs.jdbcPostgresql)

    // test
    api(platform(libs.testcontainersBom))
    testImplementation(libs.testcontainersPostgresql)
    testImplementation(libs.bundles.kotest)
}
