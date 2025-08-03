package momosetkn.maigreko.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.PostgresqlColumnDetail
import momosetkn.PostgresqlInfoDao
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import momosetkn.maigreko.engine.PosgresqlMigrateEngine
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialects

class MigratorSpec : FunSpec({
    lateinit var posgresqlMigrateEngine: PosgresqlMigrateEngine
    lateinit var db: JdbcDatabase
    lateinit var dataSource: javax.sql.DataSource
    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        db = JdbcDatabase(dataSource = dataSource, dialect = JdbcDialects.get("postgresql"))
        posgresqlMigrateEngine = PosgresqlMigrateEngine(db)
    }
    context("createTable") {
        context("primaryKey = true, nullable = false") {
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
                posgresqlMigrateEngine.createTable(createTable)

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
    }
})
