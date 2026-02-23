package momosetkn.maigreko

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.DummyDataSource
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.PostgresqlDatabase
import momosetkn.maigreko.dsl.MaigrekoMigration
import momosetkn.maigreko.introspector.PostgresqlInfoService
import momosetkn.maigreko.sql.MigrateEngineFactory
import javax.sql.DataSource

class MaigrekoSpec : FunSpec({
    val engine = MigrateEngineFactory.create("postgresql")
    lateinit var dataSource: DataSource
    lateinit var postgresqlInfoService: PostgresqlInfoService

    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = JdbcDatabaseContainerDataSource(container)
        postgresqlInfoService = PostgresqlInfoService(dataSource)
    }

    context("each methods") {
        // Minimal forward dry-run test
        context("block: ChangeSetGroupDsl.() -> Unit") {
            context("DryRunForward") {
                test("should collect forward DDLs for create table") {
                    val engine = MigrateEngineFactory.create("postgresql")
                    val maigreko = Maigreko(DummyDataSource, engine)

                    val results = maigreko.dryRunForward("migrationClass") {
                        changeSet {
                            createTable("users") {
                                column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
                                column("name", "varchar(255)") { }
                            }
                        }
                    }

                    results.size shouldBe 1
                    val ddl = results.first().ddls.joinToString("\n")
                    ddl.lowercase().contains("create table") shouldBe true
                    ddl.contains("users") shouldBe true
                }
            }

            // Minimal rollback dry-run test
            context("DryRunRollback") {
                test("should collect rollback DDLs for drop table of created table") {
                    val engine = MigrateEngineFactory.create("postgresql")
                    val maigreko = Maigreko(DummyDataSource, engine)

                    val results = maigreko.dryRunRollback("migrationClass") {
                        changeSet {
                            createTable("users") {
                                column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
                            }
                        }
                    }

                    results.size shouldBe 1
                    val ddl = results.first().ddls.joinToString("\n")
                    ddl.lowercase().contains("drop table") shouldBe true
                    ddl.contains("users") shouldBe true
                }
            }
        }

        context("MaigrekoMigration") {
            @Suppress("unused", "ClassNaming")
            class CreateUsersTableMigration : MaigrekoMigration({
                changeSet {
                    createTable("users_from_class") {
                        column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
                        column("name", "varchar(255)") { }
                    }
                }
            })

            context("DryRunForward") {
                test("should collect forward DDLs from migration class") {
                    val engine = MigrateEngineFactory.create("postgresql")
                    val maigreko = Maigreko(DummyDataSource, engine)

                    val results = maigreko.dryRunForward(CreateUsersTableMigration())

                    results.size shouldBe 1
                    val ddl = results.first().ddls.joinToString("\n")
                    (ddl.lowercase().contains("create table") && ddl.contains("users_from_class")) shouldBe true
                }
            }

            context("DryRunRollback") {
                test("should collect rollback DDLs from migration class") {
                    val engine = MigrateEngineFactory.create("postgresql")
                    val maigreko = Maigreko(DummyDataSource, engine)

                    val results = maigreko.dryRunRollback(CreateUsersTableMigration())

                    results.size shouldBe 1
                    val ddl = results.first().ddls.joinToString("\n")
                    ddl.lowercase().contains("drop table") shouldBe true
                    ddl.contains("users_from_class") shouldBe true
                }
            }
        }
    }

    context("use real database") {
        beforeEach {
            PostgresqlDatabase.clear()
        }
        context("migrate and rollback methods") {
            test("migrate should create table and rollback should drop it") {
                val maigreko = Maigreko(dataSource, engine)
                val migrationClassName = "migrationClassForExecute"

                val changeSetBlock: ChangeSetGroupDslBlock = {
                    changeSet {
                        createTable("users_execute") {
                            column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
                        }
                    }
                }

                maigreko.migrate(migrationClassName, changeSetBlock)
                postgresqlInfoService
                    .fetchPostgresqlTableInfo().map { it.tableName } shouldBe listOf("users_execute")

                maigreko.rollback(migrationClassName, changeSetBlock)
                postgresqlInfoService
                    .fetchPostgresqlTableInfo().size shouldBe 0
            }
        }
    }

    context("sub package") {
        context("DryRunForward") {
            test("should collect forward DDLs from migration class") {
                val engine = MigrateEngineFactory.create("postgresql")
                val maigreko = Maigreko(DummyDataSource, engine)

                val results = maigreko.dryRunMigrate("momosetkn.maigreko.migrations")

                results.size shouldBe 6
                val migrationClassNames = results.map { it.migrationClassName }
                migrationClassNames shouldBe listOf(
                    "momosetkn.maigreko.migrations.Migration1",
                    "momosetkn.maigreko.migrations.Migration2",
                    "momosetkn.maigreko.migrations.sub1.Migration_sub1_1",
                    "momosetkn.maigreko.migrations.sub1.Migration_sub1_2",
                    "momosetkn.maigreko.migrations.sub1.subsub1.Migration_sub1_subsub1_1",
                    "momosetkn.maigreko.migrations.sub1.subsub2.Migration_sub1_subsub2_1",
                )
            }
        }

        context("DryRunRollback") {
            test("should collect rollback DDLs from migration class") {
                val engine = MigrateEngineFactory.create("postgresql")
                val maigreko = Maigreko(DummyDataSource, engine)

                val results = maigreko.dryRunRollback("momosetkn.maigreko.migrations")

                results.size shouldBe 6
                val migrationClassNames = results.map { it.migrationClassName }
                migrationClassNames shouldBe listOf(
                    "momosetkn.maigreko.migrations.Migration1",
                    "momosetkn.maigreko.migrations.Migration2",
                    "momosetkn.maigreko.migrations.sub1.Migration_sub1_1",
                    "momosetkn.maigreko.migrations.sub1.Migration_sub1_2",
                    "momosetkn.maigreko.migrations.sub1.subsub1.Migration_sub1_subsub1_1",
                    "momosetkn.maigreko.migrations.sub1.subsub2.Migration_sub1_subsub2_1",
                )
            }
        }
    }
})
