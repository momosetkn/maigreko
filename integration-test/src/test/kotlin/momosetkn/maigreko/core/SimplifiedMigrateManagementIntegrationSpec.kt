package momosetkn.maigreko.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.core.infras.PostgresqlColumnDetail
import momosetkn.maigreko.core.infras.PostgresqlInfoRepository
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.engine.PostgreMigrateEngine
import javax.sql.DataSource

/**
 * Simplified version of MigrateManagementIntegrationSpec that uses the new JDBC implementations
 */
class SimplifiedMigrateManagementIntegrationSpec : FunSpec({
    lateinit var migrateManagement: MigrateManagement
    lateinit var dataSource: DataSource
    lateinit var jdbcInfoDao: PostgresqlInfoRepository

    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        jdbcInfoDao = PostgresqlInfoRepository(dataSource)
        migrateManagement = MigrateManagement(dataSource, PostgreMigrateEngine())
    }

    beforeEach {
        PostgresqlDatabase.clear()
    }

    afterTest {
        println(PostgresqlDatabase.generateDdl())
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

            val columnDetails = jdbcInfoDao.getColumnDetails("migrations")
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

            val constraintDetails = jdbcInfoDao.getConstraintDetails("migrations")
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

            val columnDetails = jdbcInfoDao.getColumnDetails("migrations")
            columnDetails.size shouldBe 0

            val constraintDetails = jdbcInfoDao.getConstraintDetails("migrations")
            constraintDetails.size shouldBe 0
        }
    }
})
