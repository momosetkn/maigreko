package momosetkn.maigreko.core

object MigrateManagementInitialize {
    fun createTable() =
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
                    name = "filename",
                    type = "varchar(255)",
                    columnConstraint = ColumnConstraint(nullable = false)
                ),
                Column(
                    name = "author",
                    type = "varchar(255)",
                    columnConstraint = ColumnConstraint(nullable = false)
                ),
                Column(
                    name = "change_set_id",
                    type = "varchar(255)",
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
