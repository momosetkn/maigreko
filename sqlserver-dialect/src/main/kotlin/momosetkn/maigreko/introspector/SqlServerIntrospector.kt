package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * SQL Server implementation of the Introspector interface.
 * Uses SqlServerInfoService to fetch database information and
 * SqlServerChangeGenerator to generate Change objects.
 */
class SqlServerIntrospector(
    private val dataSource: DataSource
) : Introspector {
    private val infoService = SqlServerInfoService(dataSource)
    private val changeGenerator = SqlServerChangeGenerator()

    /**
     * Introspects the SQL Server database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    override fun introspect(): List<Change> {
        val (tableInfos, sequenceDetails) = infoService.fetchAll()
        return changeGenerator.generateChanges(tableInfos, sequenceDetails)
    }
}
