package momosetkn.maigreko.versioning

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.PostgresqlDatabase
import momosetkn.maigreko.change.ChangeSet
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.introspector.PostgresqlInfoService
import momosetkn.maigreko.introspector.infras.PostgresqlColumnDetail
import momosetkn.maigreko.sql.PostgresqlMigrateEngine
import javax.sql.DataSource

class VersioningIntegrationSpec : FunSpec({
    val logger = org.slf4j.LoggerFactory.getLogger(VersioningIntegrationSpec::class.java)
    val postgresqlMigrateEngine = PostgresqlMigrateEngine()

    lateinit var versioning: Versioning
    lateinit var dataSource: DataSource
    lateinit var postgreInfoService: PostgresqlInfoService

    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = JdbcDatabaseContainerDataSource(container)
        postgreInfoService = PostgresqlInfoService(dataSource)
        versioning = Versioning(dataSource, postgresqlMigrateEngine)
    }

    beforeEach {
        PostgresqlDatabase.clear()
    }

    afterTest {
        logger.info(PostgresqlDatabase.generateDdl())
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
                migrationClass = "migrationClass",
                changeSetId = 1,
                changes = listOf(createTable),
            )

            versioning.forward(changeSet)
            versioning.forward(changeSet)

            val (tableInfos, sequenceDetail) = postgreInfoService.fetchAll()
            tableInfos.size shouldBe 1

            val tableInfo = tableInfos[0]
            tableInfo.columnDetails.size shouldBe 1
            tableInfo.columnDetails[0] shouldBe PostgresqlColumnDetail(
                tableName = "migrations",
                columnName = "version",
                type = "character varying(255)",
                notNull = "YES",
                columnDefault = null,
                primaryKey = "YES",
                unique = "NO",
                foreignTable = null,
                foreignColumn = null,
                generatedKind = null,
                identityGeneration = null,
                ownedSequence = null,
                sequenceDataType = null,
                startValue = null,
                incrementBy = null,
                minValue = null,
                maxValue = null,
                cacheSize = null,
                cycle = null
            )

            tableInfo.columnConstraints.size shouldBe 0
            sequenceDetail.size shouldBe 0
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
                migrationClass = "migrationClass",
                changeSetId = 1,
                changes = listOf(createTable),
            )

            versioning.forward(changeSet)
            versioning.rollback(changeSet)

            val (tableInfos, sequenceDetail) = postgreInfoService.fetchAll()
            tableInfos.size shouldBe 0

            sequenceDetail.size shouldBe 0
        }
    }
})
