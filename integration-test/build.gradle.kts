plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    implementation(project(":core"))

    // main
    implementation(libs.jdbcPostgresql)

    // komapper
    implementation(libs.komapperCore)
    implementation(libs.komapperJdbc)
    implementation(libs.komapperAnnotation)
    implementation(libs.komapperTemplate)
    platform(libs.komapperPlatform).let {
        implementation(it)
        ksp(it)
    }
    ksp(libs.komapperProcessor)
    implementation(libs.komapperDialectPostgresqlJdbc)

    // test
    api(platform(libs.testcontainersBom))
    testImplementation(libs.testcontainersPostgresql)
    runtimeOnly(libs.jdbcPostgresql)
    testImplementation(libs.bundles.kotest)
}
