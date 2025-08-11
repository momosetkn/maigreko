package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change
import javax.sql.DataSource

/**
 * H2 implementation of the Introspector interface.
 * Uses H2InfoService to fetch database information and
 * H2ChangeGenerator to generate Change objects.
 */
class H2Introspector(
    private val dataSource: DataSource
) : Introspector {
    private val infoService = H2InfoService(dataSource)
    private val changeGenerator = H2ChangeGenerator()

    /**
     * Introspects the H2 database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    override fun introspect(): List<Change> {
        val (tableInfos, sequenceDetails) = infoService.fetchAll()
        return changeGenerator.generateChanges(tableInfos, sequenceDetails)
    }
}
