plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.komapper)
}

dependencies {
    // komapper
    implementation(libs.komapperStarterJdbc)
    platform(libs.komapperPlatform).let {
        implementation(it)
        ksp(it)
    }
    ksp(libs.komapperProcessor)
    implementation(libs.komapperDialectPostgresqlJdbc)

    // test
    testImplementation(libs.bundles.kotest)
}
