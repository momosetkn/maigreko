package momosetkn.maigreko.jdbc

import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class JdbcExecutor(
    private val dataSource: DataSource
) {
    /**
     * Execute a SQL script (multiple statements separated by semicolons)
     */
    fun executeScript(
        @Language("sql") vararg sql: String
    ) {
        val statements = sql.filter { it.trim().isNotEmpty() }
        withConnection { connection ->
            statements.forEach { statement ->
                connection.createStatement().use { stmt ->
                    stmt.execute(statement)
                }
            }
        }
    }

    /**
     * Execute a transaction with the given block
     */
    @Suppress("TooGenericExceptionCaught")
    fun <T> withTransaction(block: (Connection) -> T): T {
        return withConnection { connection ->
            val originalAutoCommit = connection.autoCommit
            try {
                connection.autoCommit = false
                val result = block(connection)
                connection.commit()
                return@withConnection result
            } catch (e: Exception) {
                try {
                    connection.rollback()
                } catch (rollbackEx: SQLException) {
                    e.addSuppressed(rollbackEx)
                }
                throw e
            } finally {
                try {
                    connection.autoCommit = originalAutoCommit
                } catch (_: SQLException) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Execute a block with a connection
     */
    fun <T> withConnection(block: (Connection) -> T): T {
        dataSource.connection.use { connection ->
            return block(connection)
        }
    }

    /**
     * Execute a query and map the results
     */
    fun <T> executeQuery(
        @Language("sql") sql: String,
        vararg parameters: Any?,
        mapper: (ResultSet) -> T,
    ): T {
        return withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                bindParameters(statement, parameters.toList())
                statement.executeQuery().use { resultSet ->
                    return@withConnection mapper(resultSet)
                }
            }
        }
    }

    /**
     * Execute a query and map the results
     */
    fun executeQuery(
        @Language("sql") sql: String,
        vararg parameters: Any? = emptyArray(),
    ) {
        return withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                bindParameters(statement, parameters.toList())
                statement.executeQuery()
            }
        }
    }

    /**
     * Execute an update statement
     */
    fun executeUpdate(
        @Language("sql") sql: String,
        parameters: List<Any?> = emptyList()
    ): Int {
        return withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                bindParameters(statement, parameters)
                return@withConnection statement.executeUpdate()
            }
        }
    }

    /**
     * Execute an insert statement and return generated keys
     */
    fun executeInsert(
        @Language("sql") sql: String,
        parameters: List<Any?> = emptyList(),
        keyMapper: (ResultSet) -> Any
    ): Any {
        return withConnection { connection ->
            connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS).use { statement ->
                bindParameters(statement, parameters)
                statement.executeUpdate()
                statement.generatedKeys.use { resultSet ->
                    if (resultSet.next()) {
                        return@withConnection keyMapper(resultSet)
                    } else {
                        throw SQLException("No generated keys returned")
                    }
                }
            }
        }
    }

    private fun bindParameters(statement: PreparedStatement, parameters: List<Any?>) {
        parameters.forEachIndexed { index, param ->
            when (param) {
                null -> statement.setNull(index + 1, Types.NULL)
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                is Boolean -> statement.setBoolean(index + 1, param)
                is LocalDateTime -> statement.setTimestamp(index + 1, Timestamp.valueOf(param))
                is LocalDate -> statement.setDate(index + 1, Date.valueOf(param))
//                is List<*> -> statement.setArray(index + 1, connection.createArrayOf("text", param.toTypedArray()))
                else -> statement.setObject(index + 1, param)
            }
        }
    }
}
