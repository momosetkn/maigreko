package momosetkn

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

object DummyDataSource : DataSource {
    override fun getConnection(): Connection = TODO()
    override fun getConnection(username: String, password: String): Connection = TODO()
    override fun getLogWriter(): PrintWriter = TODO()
    override fun setLogWriter(out: PrintWriter) = TODO()
    override fun setLoginTimeout(seconds: Int) = TODO()
    override fun getLoginTimeout(): Int = TODO()
    override fun getParentLogger(): Logger = TODO()
    override fun <T : Any> unwrap(iface: Class<T>): T = TODO()
    override fun isWrapperFor(iface: Class<*>): Boolean = TODO()
}
