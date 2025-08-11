package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change
import javax.sql.DataSource

/**
 * Oracle implementation of the Introspector interface.
 * Uses OracleInfoService to fetch database information and
 * OracleChangeGenerator to generate Change objects.
 */
class OracleIntrospector(
    private val dataSource: DataSource
) : Introspector {
    private val infoService = OracleInfoService(dataSource)
    private val changeGenerator = OracleChangeGenerator()

    /**
     * Introspects the Oracle database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    override fun introspect(): List<Change> {
        val (tableInfos, sequenceDetails) = infoService.fetchAll()
        return changeGenerator.generateChanges(tableInfos, sequenceDetails)
    }
}
