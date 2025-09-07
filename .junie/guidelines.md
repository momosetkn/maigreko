# Maigreko – Advanced Developer Guidelines

Last verified: 2025-09-07 00:30

This document collects project-specific knowledge to accelerate development, testing, and debugging of Maigreko.

--------------------------------------------------------------------------------

## 1. Build and Configuration

- Toolchain
  - Java language level: 21 (Kotlin/JVM target: 17)
  - Gradle: 8.14.3+ (wrapper included)
- Top-level tasks
  - Build all modules: `./gradlew build`
  - Run all tests: `./gradlew test`
  - Code quality: `./gradlew detekt ktlintCheck`
- Module-scoped tasks (example for core)
  - Build: `./gradlew :core:build`
  - Test: `./gradlew :core:test`
- Configuration on demand is enabled; configuration cache can be enabled if needed (validate locally before committing).

Project modules:
- core: base APIs (DSL, engines, versioning) and common tests.
- {dialect}-dialect: DB-specific implementations (PostgreSQL, MySQL, Oracle, H2, MariaDB, SQL Server). Each registers services via ServiceLoader.
- integration-test: cross-dialect scenarios using Testcontainers and real DBs.
- *-test-utils and test-utils: shared test infra (DummyDataSource, container helpers).

Logging: Log4j2 via SLF4J. Default configs at core/src/main/resources/log4j2.xml and integration-test/src/test/resources/log4j2.xml.

Gradle/Kotlin considerations:
- KSP is in use via Komapper in dialect modules. Ensure your IDE has KSP enabled if you add or modify mapper entities.
- JVM target remains 17 even though language is 21—mind API usage.

--------------------------------------------------------------------------------

## 2. Testing

Framework: Kotest (FunSpec). Group with `context("...")` and define tests with `test("...")`.

Common commands
- All tests: `./gradlew test`
- Per-module: `./gradlew :postgresql-dialect:test`, `./gradlew :mysql-dialect:test`, etc.
- Detailed Gradle output (useful when investigating CI issues): `./gradlew test --info` or `--stacktrace`.

Integration tests
- Module: `integration-test`
- Use Testcontainers and real DBs (primarily PostgreSQL and MySQL).
- Requires Docker running locally. If Docker is unavailable, skip by running non-integration modules only: `./gradlew :core:test`.

Dialect discovery in tests
- ServiceLoader pattern is used for runtime discovery:
  - Example: `MigrateEngineFactory.create("postgresql")` loads from META-INF/services provided by dialect modules.
  - Ensure the relevant *-dialect module is on the test runtime classpath when writing tests relying on ServiceLoader.

2.1 Add a new unit test (example pattern)
- Place under the appropriate module, e.g., `core/src/test/kotlin/.../*Spec.kt`.
- Prefer dry-run paths for unit tests to avoid DB dependency:
  - Use DummyDataSource from test-utils
  - Create an engine via MigrateEngineFactory
  - Exercise the DSL to produce DDL, assert on generated SQL text

Example (already present and verified): integration-test/src/test/kotlin/momosetkn/maigreko/MaigrekoSpec.kt demonstrates dry-run forward and rollback using the DSL and a migration class.

2.2 Minimal reproducible test (unit, no containers)
If you need a pure unit example, mirror MaigrekoSpec patterns but keep it in a non‑integration module and assert only string content of DDLs.

Run and verify
- Example run (verified): `./gradlew test` — all modules passed locally with Testcontainers starting PostgreSQL 17.5.
- For single module quick loop: `./gradlew :core:test`.

--------------------------------------------------------------------------------

## 3. Development Notes and Conventions

3.1 ServiceLoader registration
- IntrospectorBuilder implementations must be registered at: `META-INF/services/momosetkn.maigreko.introspector.IntrospectorBuilder`.
- For engines, register at: `META-INF/services/momosetkn.maigreko.sql.MigrateEngine` (see postgres/mysql modules).
- Missing or incorrect service registrations are the most common cause of "engine not found" or factory failures.

3.2 DSL and dry-run paths
- The DSL is defined in core (ChangeSetDsl, TableDsl, ColumnDsl). DDL generation occurs via the dialect-specific DDLGenerator wired by the MigrateEngine.
- For tests and tooling, prefer using Maigreko.dryRunForward / dryRunRollback to validate migrations without a live DB:
  - Instantiate: `Maigreko(DummyDataSource, MigrateEngineFactory.create("postgresql"))`.
  - Provide DSL block or MaigrekoMigration subclass.

3.3 Versioning
- core/versioning provides ChangeSetHistory and Versioning APIs. Integration tests cover bootstrap and forward/rollback flows using real containers. When modifying versioning behavior, add both unit tests (logic) and integration tests (DB behavior).

3.4 Code quality
- ktlint: configured via .editorconfig and per-module baselines: `*/config/ktlint/baseline.xml`.
- detekt: rules at `config/detekt/detekt.yml`, baselines per module: `*/detekt-baseline.xml`.
- Before committing code: `./gradlew ktlintCheck detekt`.

**IMPORTANT**: Before committing your code, continue making fixes until both `gradle ktlintCheck` and `gradle detekt` complete successfully. If these checks fail, fix any issues they report and run them again until they pass.

3.5 Troubleshooting
- Service not found / engine null:
  - Verify META-INF/services registrations in the relevant dialect module.
  - Ensure the module is included in settings.gradle.kts and is a dependency of the module running tests.
- Testcontainers failures:
  - Ensure Docker is running.
  - Use `--info` to get container startup logs.
  - Temporarily restrict to unit tests: run only non-container modules.
- SQL assertion instability:
  - The exact SQL formatting differs per generator. Prefer contains() checks on key tokens and identifiers (see MaigrekoSpec).

--------------------------------------------------------------------------------

## 4. Verified Example – Create and Run a Simple Test

Goal: demonstrate adding and executing a project-specific test.

Overview
- We rely on the existing MaigrekoSpec (integration-test module) as a working pattern that uses DummyDataSource + MigrateEngineFactory("postgresql") and asserts on DDL strings.
- Verified command: `./gradlew test` completed successfully locally (PostgreSQL Testcontainers started automatically).

Steps (re-usable pattern)
1) Author a test using Kotest FunSpec and the DSL dry-run path (no live DB logic required):
   - Create a file under your target module, e.g., `core/src/test/kotlin/mypkg/ExampleDryRunSpec.kt`.
   - In the test, construct `Maigreko(DummyDataSource, MigrateEngineFactory.create("postgresql"))`.
   - Call `dryRunForward { changeSet { createTable("t") { column("id", "bigint") { constraint(primaryKey = true, nullable = false) } } } }`.
   - Assert: `ddl.lowercase().contains("create table") && ddl.contains("t")`.
2) Run just that module: `./gradlew :core:test`.
3) If the test requires dialect-specific behavior (e.g., MySQL differences), ensure the corresponding *-dialect module is in the test runtime classpath.

Note: If you add a file only for experimental verification, consider removing it before committing if it isn’t providing long-term value.

--------------------------------------------------------------------------------

## 5. Commands Cheat Sheet
- All: `./gradlew build` | `./gradlew test`
- Module tests: `./gradlew :core:test`, `./gradlew :integration-test:test`
- PostgreSQL-only dialect tests: `./gradlew :postgresql-dialect:test`
- Lint/Static analysis: `./gradlew ktlintCheck detekt`
- Verbose logs: `./gradlew test --info --stacktrace`

--------------------------------------------------------------------------------

This document is intended to be kept brief and specific. Update it whenever you discover new project-specific behaviors (e.g., a new dialect’s special registration, additional integration prerequisites, or recurring pitfalls).
