plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// for can reference in allprojects scope
val catalog = libs

allprojects {
    apply(plugin = catalog.plugins.kotlinJvm.get().pluginId)
    apply(plugin = catalog.plugins.detekt.get().pluginId)
    apply(plugin = catalog.plugins.ktlint.get().pluginId)

    repositories {
        mavenCentral()
    }
    tasks.test {
        useJUnitPlatform()
    }
}
