dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)

    api(platform(libs.testcontainersBom))
    implementation(libs.testcontainersJdbc)
}

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/kotlin")
    val packageName = "momosetkn"
    val className = "BuildConfig"

    // Tell Gradle where to output
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().asFile.resolve("${packageName.replace('.', '/')}/$className.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package $packageName

            object $className {
                object ContainerImage {
                    const val MYSQL = "${libs.versions.containerImage.mysql.get()}"
                    const val POSTGRESQL = "${libs.versions.containerImage.postgres.get()}"
                    // TODO: Add more container images as needed
                }
            }
            """.trimIndent()
        )
    }
}

sourceSets {
    named("main") {
        kotlin.srcDir(generateBuildConfig.map { it.outputs.files })
    }
}
