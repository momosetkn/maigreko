package momosetkn.maigreko

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.JdbcDatabaseContainerDataSource
import momosetkn.PostgresqlDatabase
import momosetkn.maigreko.introspector.PostgresqlInfoService
import momosetkn.maigreko.introspector.infras.PostgresqlColumnDetail
import momosetkn.maigreko.sql.MigrateEngineFactory
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class MaigrekoPostgresqlIntegrationSpec : FunSpec({
    val logger = LoggerFactory.getLogger(MaigrekoPostgresqlIntegrationSpec::class.java)
    val engine = MigrateEngineFactory.create("postgresql")

    lateinit var dataSource: DataSource
    lateinit var postgreInfoService: PostgresqlInfoService
    lateinit var maigreko: Maigreko

    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = JdbcDatabaseContainerDataSource(container)
        postgreInfoService = PostgresqlInfoService(dataSource)
        maigreko = Maigreko(dataSource, engine)
    }

    beforeEach {
        PostgresqlDatabase.clear()
    }

    afterTest {
        logger.info(PostgresqlDatabase.generateDdl())
    }

    context("double forward") {
        test("can migrate") {
            val changeSetBlock: ChangeSetGroupDslBlock = {
                changeSet {
                    createTable("migrations") {
                        column("version", "character varying(255)") {
                            constraint(primaryKey = true, nullable = false)
                        }
                    }
                }
            }
            maigreko.migrate("migrationClass", changeSetBlock)
            maigreko.migrate("migrationClass", changeSetBlock)

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

            val changeSetBlock: ChangeSetGroupDslBlock = {
                changeSet {
                    createTable("migrations") {
                        column("version", "character varying(255)") {
                            constraint(primaryKey = true, nullable = false)
                        }
                    }
                }
            }
            maigreko.migrate("migrationClass", changeSetBlock)
            maigreko.rollback("migrationClass", changeSetBlock)

            val (tableInfos, sequenceDetail) = postgreInfoService.fetchAll()
            tableInfos.size shouldBe 0

            sequenceDetail.size shouldBe 0
        }
    }
})
