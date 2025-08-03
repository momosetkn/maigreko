package momosetkn.maigreko.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import momosetkn.PostgresqlColumnDetail
import momosetkn.PostgresqlInfoDao
import momosetkn.maigreko.db.PostgresDataSource
import momosetkn.maigreko.db.PostgresqlDatabase
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialects

class MigratorTest : FunSpec({
    lateinit var migrator: Migrator
    lateinit var dataSource: javax.sql.DataSource
    beforeSpec {
        PostgresqlDatabase.start()
        val container = PostgresqlDatabase.startedContainer
        dataSource = PostgresDataSource(container)
        migrator = Migrator(dataSource)
    }
    test("createTable") {
        val createTable = CreateTable(
            tableName = "migrations",
            columns = listOf(
                Column(
                    name = "version",
                    type = "character varying(255)",
                    constraint = Constraint(primaryKey = true, nullable = false)
                )
            )
        )
        migrator.createTable(createTable)

        val db = JdbcDatabase(dataSource = dataSource, dialect = JdbcDialects.get("postgresql"))
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
})
