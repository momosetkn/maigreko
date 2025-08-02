package momosetkn.maigreko.core

class Migrator(
    private val dataSource: javax.sql.DataSource
) {
    fun createTable(
        tableName: String,
        vararg columnNames: String,
    ) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            val statement = conn.prepareStatement(
                """
                create table $tableName (
                ${columnNames.joinToString { "$it varchar(255) not null" }}
                )
                """.trimIndent()
            )
            statement.execute()
            conn.commit()
        }
    }
}
