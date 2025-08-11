dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)

    api(platform(libs.testcontainersBom))
    implementation(libs.testcontainersJdbc)
}
