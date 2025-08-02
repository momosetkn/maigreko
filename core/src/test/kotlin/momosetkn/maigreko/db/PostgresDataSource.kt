package momosetkn.maigreko.db

import org.testcontainers.containers.PostgreSQLContainer
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Logger

class PostgresDataSource(
    private val container: PostgreSQLContainer<*>,
) : javax.sql.DataSource {
    override fun getConnection(): Connection {
        val conn = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
        return conn
    }

    override fun getConnection(p0: String?, p1: String?): Connection? {
        return getConnection()
    }

    override fun getLogWriter(): PrintWriter? {
        TODO("Not yet implemented")
    }

    override fun setLogWriter(p0: PrintWriter?) {
        TODO("Not yet implemented")
    }

    override fun setLoginTimeout(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun getLoginTimeout(): Int {
        TODO("Not yet implemented")
    }

    override fun getParentLogger(): Logger? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> unwrap(p0: Class<T?>?): T? {
        TODO("Not yet implemented")
    }

    override fun isWrapperFor(p0: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }
}
