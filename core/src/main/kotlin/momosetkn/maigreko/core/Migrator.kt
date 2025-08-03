package momosetkn.maigreko.core

class Migrator(
    private val dataSource: javax.sql.DataSource
) {
    fun createTable(
        createTable: CreateTable,
    ) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            val statement = conn.prepareStatement(
                """
                create table ${createTable.tableName} (
                ${createTable.columns.joinToString {
                    listOfNotNull(
                        it.name,
                        it.type,
                        if (it.constraint.primaryKey) "primary key" else null,
                        if (it.constraint.unique) "unique" else null,
                        if (it.constraint.nullable) "not null" else null,
                    ).joinToString(" ")
                }}
                )
                """.trimIndent()
            )
            statement.execute()
            conn.commit()
        }
    }
}
