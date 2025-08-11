package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change
import javax.sql.DataSource

/**
 * MySQL implementation of the Introspector interface.
 * Uses MysqlInfoService to fetch database information and
 * MysqlChangeGenerator to generate Change objects.
 */
class MysqlIntrospector(
    private val dataSource: DataSource
) : Introspector {
    private val infoService = MysqlInfoService(dataSource)
    private val changeGenerator = MysqlChangeGenerator()

    /**
     * Introspects the MySQL database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    override fun introspect(): List<Change> {
        val tableInfos = infoService.fetchAll()
        return changeGenerator.generateChanges(tableInfos)
    }
}
