package momosetkn.maigreko.versioning

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.maigreko.change.ChangeSet
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.introspector.infras.PostgresqlColumnDetail
import momosetkn.maigreko.introspector.infras.PostgresqlInfoRepository
import momosetkn.maigreko.sql.PostgreMigrateEngine
import javax.sql.DataSource

class VersioningIntegrationSpec : FunSpec({
    lateinit var versioning: Versioning
    lateinit var dataSource: DataSource
    lateinit var jdbcInfoDao: PostgresqlInfoRepository

    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        jdbcInfoDao = PostgresqlInfoRepository(dataSource)
        versioning = Versioning(dataSource, PostgreMigrateEngine)
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

            versioning.forward(changeSet)
            versioning.forward(changeSet)

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

            versioning.forward(changeSet)
            versioning.rollback(changeSet)

            val columnDetails = jdbcInfoDao.getColumnDetails("migrations")
            columnDetails.size shouldBe 0

            val constraintDetails = jdbcInfoDao.getConstraintDetails("migrations")
            constraintDetails.size shouldBe 0
        }
    }
})
