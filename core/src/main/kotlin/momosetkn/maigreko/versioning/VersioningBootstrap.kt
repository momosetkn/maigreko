package momosetkn.maigreko.versioning

import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.jdbc.JdbcDatabase
import momosetkn.maigreko.jdbc.JdbcQueryDsl
import momosetkn.maigreko.sql.MigrateEngine

class VersioningBootstrap(
    private val db: JdbcDatabase,
    private val migrateEngine: MigrateEngine,
) {
    fun bootstrap() {
        val maigrekoCreateTable = createTable()
        val ddl = migrateEngine.forwardDdl(maigrekoCreateTable)
        db.runQuery(JdbcQueryDsl.executeScript(ddl))
    }

    private fun createTable() =
        CreateTable(
            ifNotExists = true,
            tableName = "change_set_history",
            columns = listOf(
                Column(
                    name = "id",
                    type = "bigint",
                    autoIncrement = true,
                    columnConstraint = ColumnConstraint(primaryKey = true, nullable = false,)
                ),
                Column(
                    name = "migration_class",
                    type = "varchar(255)",
                    columnConstraint = ColumnConstraint(nullable = false)
                ),
                Column(
                    name = "change_set_id",
                    type = "integer",
                    columnConstraint = ColumnConstraint(nullable = false,)
                ),
                Column(
                    name = "tag",
                    type = "varchar(255)",
                ),
                Column(
                    name = "applied_at",
                    type = "timestamp",
                    defaultValue = "CURRENT_TIMESTAMP",
                    columnConstraint = ColumnConstraint(nullable = false)
                ),
            )
        )
}
