dependencies {
    implementation(project(":test-utils"))

    // logging
    api(libs.bundles.log4j)
    api(libs.slf4j)
    implementation(project(":mysql-dialect"))

    // test
    api(platform(libs.testcontainersBom))
    implementation(libs.testcontainersMysql)
}
