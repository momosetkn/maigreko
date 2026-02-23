# maigreko

[![check](https://github.com/momosetkn/maigreko/actions/workflows/check.yml/badge.svg)](https://github.com/momosetkn/maigreko/actions/workflows/check.yml)
[![license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

A Kotlin-first database migration library.

`maigreko` lets you define schema changes with a Kotlin DSL, then executes dialect-specific DDL for both forward and rollback migrations.

## Features

- Kotlin DSL for schema changes
- Forward and rollback execution
- Dry-run support (preview SQL without execution)
- Migration history tracking via `change_set_history`
- Dialect auto-detection from `DataSource`
- Pluggable dialect modules via Java `ServiceLoader`

# Major Features

## 1. Kotlin DSL-based Migration Definition

Define schema migrations directly in Kotlin using a type-safe DSL.
This is not a DDL wrapper — migrations themselves are expressed declaratively in Kotlin.

* Table operations: create, rename, drop
* Column operations: add, rename, modify type
* Constraints and keys: primary key, foreign key, unique, index, not-null
* Sequence operations
* Reversible and irreversible operations are explicitly modeled

The DSL is type-safe and dialect-aware, allowing database-specific column types to be expressed safely at compile time.

---

## 2. Forward and Rollback Execution from a Single Definition

A single migration definition can generate both forward and rollback operations.

* `migrate(...)` executes forward changes
* `rollback(...)` executes reverse changes
* Block-style migrations and `MaigrekoMigration` class-based migrations are supported
* Package scanning enables batch execution of migration classes

Irreversible operations must be explicitly declared to prevent unsafe rollback assumptions.

---

## 3. Dry-run SQL Generation

Generated SQL can be previewed without executing it.

* `dryRunForward(...)`
* `dryRunRollback(...)`
* `dryRunMigrate(...)` for package-wide preview

Dry-run includes full execution order and is intended for CI validation and release review.

---

## 4. Versioning and Execution Safety

Migration history is tracked automatically.

* `change_set_history` table is bootstrapped if missing
* Stores migration class, change set identifier, checksum, and execution metadata
* Transactional execution
* Lock-based coordination to prevent concurrent runs
* Rollback removes corresponding history entries

Designed to handle multi-branch development safely.

---

## 5. Dialect-aware Schema Model and SQL Generation

Migrations are first translated into an intermediate schema model,
which is then rendered into SQL by dialect-specific engines.

* Dialect provides `Dialect`, `MigrateEngine`, and `DDLGenerator`
* Vendor-specific syntax differences are encapsulated
* Supports PostgreSQL, MySQL, MariaDB, SQL Server, Oracle, and H2

---

## 6. Dialect Discovery and Extension Model

Dialect resolution is automatic.

* `Maigreko(dataSource)` detects dialect from JDBC metadata
* Dialects are discovered via Java `ServiceLoader`
* New dialects can be added by implementing `Dialect`

## Supported Databases

- PostgreSQL
- MySQL

## Quick Start

```kotlin
package com.example.migrations

import momosetkn.maigreko.Maigreko
import momosetkn.maigreko.dsl.MaigrekoMigration
import javax.sql.DataSource

object Migration20260623CreateUser : MaigrekoMigration({
    changeSet {
        createTable("users") {
            column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
            column("name", "varchar(255)") { }
        }
    }
})

fun main() {
    val maigreko = Maigreko(dataSource)
    // Run all MaigrekoMigration classes in a package
    maigreko.migrate("com.example.migrations")
}
```

### Other Usage Examples

```kotlin
// Preview generated SQL without executing migration
val dryRunResults = maigreko.dryRunForward(Migration20260623CreateUser)
dryRunResults.forEach { println(it.ddls.joinToString(";\n")) }
```

```kotlin
// Preview generated SQL without executing rollback
val dryRunResults = maigreko.dryRunRollback(Migration20260623CreateUser)
dryRunResults.forEach { println(it.ddls.joinToString(";\n")) }
```

```kotlin
// Rollback all MaigrekoMigration classes in a package
maigreko.rollback("com.example.migrations")
```

```kotlin
// Run Migration20260623CreateUser
maigreko.migrate(Migration20260623CreateUser)
```

## Module Structure

- `:core`
- `:postgresql-dialect`
- `:mysql-dialect`
- `:mariadb-dialect`
- `:sqlserver-dialect`
- `:oracle-dialect`
- `:h2-dialect`
- `:integration-test`
- `:test-utils`
- `:postgresql-test-utils`
- `:mysql-test-utils`

## Notes

- Gradle JVM toolchain is configured with Java 21 and Kotlin/JVM target 17.
- Dialects are registered under `META-INF/services/momosetkn.maigreko.dialect.Dialect`.
