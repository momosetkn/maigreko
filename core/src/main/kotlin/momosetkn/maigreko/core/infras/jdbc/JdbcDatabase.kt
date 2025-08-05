package momosetkn.maigreko.core.infras.jdbc

import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class JdbcDatabase(
    private val dataSource: DataSource
) {
    private val jdbcExecutor = JdbcExecutor(dataSource)

    /**
     * Execute a SQL script
     */
    fun executeScript(
        @Language("sql")
        sql: String
    ) {
        jdbcExecutor.executeScript(sql)
    }

    /**
     * Execute a transaction with the given block
     */
    fun <T> withTransaction(block: () -> T): T {
        return jdbcExecutor.withTransaction { _ ->
            block()
        }
    }

    /**
     * Execute a query with the given block
     */
    fun <T> runQuery(query: JdbcQuery<T>): T {
        return query.execute(jdbcExecutor)
    }
}

/**
 * Interface for JDBC queries
 */
interface JdbcQuery<T> {
    fun execute(executor: JdbcExecutor): T
}

/**
 * Implementation of JdbcQuery for executing SQL scripts
 */
class ExecuteScriptQuery(private val sql: String) : JdbcQuery<Unit> {
    override fun execute(executor: JdbcExecutor) {
        executor.executeScript(sql)
    }
}

/**
 * Factory for creating JDBC queries
 */
object JdbcQueryDsl {
    /**
     * Create a query to execute a SQL script
     */
    fun executeScript(
        @Language("sql")
        sql: String
    ): JdbcQuery<Unit> {
        return ExecuteScriptQuery(sql)
    }
}
