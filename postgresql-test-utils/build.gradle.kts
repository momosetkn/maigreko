dependencies {
    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)

    // test
    api(platform(libs.testcontainersBom))
    implementation(libs.testcontainersPostgresql)
    testImplementation(project(":postgresql-test-utils"))
}
