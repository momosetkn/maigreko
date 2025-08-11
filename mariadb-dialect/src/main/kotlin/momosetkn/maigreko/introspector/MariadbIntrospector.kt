package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change
import javax.sql.DataSource

/**
 * MariaDB implementation of the Introspector interface.
 * Uses MariadbInfoService to fetch database information and
 * MariadbChangeGenerator to generate Change objects.
 */
class MariadbIntrospector(
    private val dataSource: DataSource
) : Introspector {
    private val infoService = MariadbInfoService(dataSource)
    private val changeGenerator = MariadbChangeGenerator()

    /**
     * Introspects the MariaDB database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    override fun introspect(): List<Change> {
        val (tableInfos, sequenceDetails) = infoService.fetchAll()
        return changeGenerator.generateChanges(tableInfos, sequenceDetails)
    }
}
