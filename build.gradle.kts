plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// for can reference in allprojects scope
val catalog = libs

val jvmTargetVersion = 17
val javaLanguageVersion = 21

allprojects {
    apply(plugin = catalog.plugins.kotlinJvm.get().pluginId)
    apply(plugin = catalog.plugins.detekt.get().pluginId)
    apply(plugin = catalog.plugins.ktlint.get().pluginId)

    repositories {
        mavenCentral()
    }
    tasks.test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
    }

//    detekt {
//        parallel = true
//        autoCorrect = true
//        config.from("$rootDir/config/detekt.yml")
//        buildUponDefaultConfig = true
//        basePath = rootDir.absolutePath
//    }

    ktlint {
        version.set(catalog.ktlintRuleEngineCore.get().version)
        filter {
            exclude { entry ->
                entry.file.toString().contains("/generated/")
            }
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
        }

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmTargetVersion.toString()))
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
        }
        targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
        sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    }
}
