package momosetkn.maigreko

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.DummyDataSource
import momosetkn.maigreko.dsl.MaigrekoMigration
import momosetkn.maigreko.sql.MigrateEngineFactory

class MaigrekoSpec : FunSpec({
    // Minimal forward dry-run test
    context("block: ChangeSetGroupDsl.() -> Unit") {
        context("DryRunForward") {
            test("should collect forward DDLs for create table") {
                val engine = MigrateEngineFactory.create("postgresql")
                val maigreko = Maigreko(DummyDataSource, engine)

                val ddls = maigreko.dryRunForward {
                    changeSet {
                        createTable("users") {
                            column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
                            column("name", "varchar(255)") { }
                        }
                    }
                }

                ddls.size shouldBe 1
                // The exact SQL differs per generator formatting; ensure it contains the table name.
                (ddls.first().lowercase().contains("create table") && ddls.first().contains("users")) shouldBe true
            }
        }

        // Minimal rollback dry-run test
        context("DryRunRollback") {
            test("should collect rollback DDLs for drop table of created table") {
                val engine = MigrateEngineFactory.create("postgresql")
                val maigreko = Maigreko(DummyDataSource, engine)

                val ddls = maigreko.dryRunRollback {
                    changeSet {
                        createTable("users") {
                            column("id", "bigint") { constraint(primaryKey = true, nullable = false) }
                        }
                    }
                }

                ddls.size shouldBe 1
                ddls.first().lowercase().contains("drop table") shouldBe true
                ddls.first().contains("users") shouldBe true
            }
        }
    }

    context("MaigrekoMigration") {
        // Simple migration class for testing
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

                val ddls = maigreko.dryRunForward(CreateUsersTableMigration())

                ddls.size shouldBe 1
                val ddl = ddls.first()
                (ddl.lowercase().contains("create table") && ddl.contains("users_from_class")) shouldBe true
            }
        }

        context("DryRunRollback") {
            test("should collect rollback DDLs from migration class") {
                val engine = MigrateEngineFactory.create("postgresql")
                val maigreko = Maigreko(DummyDataSource, engine)

                val ddls = maigreko.dryRunRollback(CreateUsersTableMigration())

                ddls.size shouldBe 1
                val ddl = ddls.first()
                ddl.lowercase().contains("drop table") shouldBe true
                ddl.contains("users_from_class") shouldBe true
            }
        }
    }
})
