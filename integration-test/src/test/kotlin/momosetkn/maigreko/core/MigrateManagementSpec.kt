package momosetkn.maigreko.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.PostgresqlColumnDetail
import momosetkn.PostgresqlInfoDao
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.engine.PostgreMigrateEngine
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialects

class MigrateManagementSpec : FunSpec({
    lateinit var migrateManagement: MigrateManagement
    lateinit var dataSource: javax.sql.DataSource
    lateinit var db: JdbcDatabase
    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        db = JdbcDatabase(dataSource = dataSource, dialect = JdbcDialects.get("postgresql"))
        migrateManagement = MigrateManagement(db, PostgreMigrateEngine())
    }
    beforeEach {
        PostgresqlDatabase.clear()
    }
    context("double forward") {
        test("can migrate") {
            val createTable = CreateTable(
                tableName = "migrations",
                columns = listOf(
                    Column(
                        name = "version",
                        type = "character varying(255)",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    )
                )
            )

            val changeSet = ChangeSet(
                filename = "filename",
                author = "author",
                changeSetId = "changeSetId",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(changeSet)
            migrateManagement.forwardWithManagement(changeSet)
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("migrations")
            columnDetails.size shouldBe 1
            columnDetails[0] shouldBe PostgresqlColumnDetail(
                columnName = "version",
                type = "character varying(255)",
                notNull = "YES",
                columnDefault = null,
                primaryKey = "YES",
                unique = "NO",
                foreignTable = null,
                foreignColumn = null,
            )
            val constraintDetails = postgresqlInfoDao.getConstraintDetails("migrations")
            constraintDetails.size shouldBe 0
        }
    }
    context("rollback") {
        test("can migrate") {
            val createTable = CreateTable(
                tableName = "migrations",
                columns = listOf(
                    Column(
                        name = "version",
                        type = "character varying(255)",
                        columnConstraint = ColumnConstraint(primaryKey = true, nullable = false)
                    )
                )
            )

            val changeSet = ChangeSet(
                filename = "filename",
                author = "author",
                changeSetId = "changeSetId",
                changes = listOf(createTable),
            )

            migrateManagement.forwardWithManagement(changeSet)
            migrateManagement.rollbackWithManagement(changeSet)
            val postgresqlInfoDao = PostgresqlInfoDao(db)
            val columnDetails = postgresqlInfoDao.getColumnDetails("migrations")
            columnDetails.size shouldBe 0
            val constraintDetails = postgresqlInfoDao.getConstraintDetails("migrations")
            constraintDetails.size shouldBe 0
        }
    }
})
