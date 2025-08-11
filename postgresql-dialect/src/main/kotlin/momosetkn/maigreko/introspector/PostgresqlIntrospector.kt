package momosetkn.maigreko.introspector

import momosetkn.maigreko.change.Change
import javax.sql.DataSource

/**
 * PostgreSQL implementation of the Introspector interface.
 * Uses PostgresqlInfoService to fetch database information and
 * PostgresqlChangeGenerator to generate Change objects.
 */
class PostgresqlIntrospector(
    private val dataSource: DataSource
) : Introspector {
    private val infoService = PostgresqlInfoService(dataSource)
    private val changeGenerator = PostgresqlChangeGenerator()

    /**
     * Introspects the PostgreSQL database and generates a list of Change objects
     * representing the database schema.
     *
     * @return List of Change objects
     */
    override fun introspect(): List<Change> {
        val (tableInfos, sequenceDetails) = infoService.fetchAll()
        return changeGenerator.generateChanges(tableInfos, sequenceDetails)
    }
}
