pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "maigreko"

include(":utils")
include(":core")
include(":integration-test")
include(":postgresql-dialect")
include(":h2-dialect")
include(":mariadb-dialect")
include(":sqlserver-dialect")
include(":mysql-dialect")
include(":oracle-dialect")
