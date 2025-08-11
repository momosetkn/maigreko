pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "maigreko"

include(":core")
// dialect
include(":postgresql-dialect")
include(":h2-dialect")
include(":mariadb-dialect")
include(":sqlserver-dialect")
include(":mysql-dialect")
include(":oracle-dialect")
// test
include(":integration-test")
include(":test-utils")
include("postgresql-test-utils")
include("mysql-test-utils")
