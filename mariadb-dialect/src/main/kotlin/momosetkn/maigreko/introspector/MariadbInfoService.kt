package momosetkn.maigreko.introspector

import momosetkn.maigreko.introspector.infras.MariadbColumnDetail
import momosetkn.maigreko.introspector.infras.MariadbConstraintDetail
import momosetkn.maigreko.introspector.infras.MariadbInfoRepository
import momosetkn.maigreko.introspector.infras.MariadbSequenceDetail
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class MariadbInfoService(
    private val dataSource: DataSource
) {
    private val mariadbInfoRepository = MariadbInfoRepository(dataSource)

    fun fetchMariadbTableInfo(): List<MariadbTableInfo> {
        val excludeTable = Versioning.VERSIONING_TABLE_NAME
        val tableNames = mariadbInfoRepository.getTableList(excludeTable)

        val columnDetails = mariadbInfoRepository.getColumnDetails(excludeTable)
        val columnDetailsMap = columnDetails.groupBy { it.tableName }

        val constraintDetails = mariadbInfoRepository.getConstraintDetails(excludeTable)
        val constraintDetailsMap = constraintDetails.groupBy { it.tableName }

        return tableNames.map { tableName ->
            val columnDetails = requireNotNull(columnDetailsMap[tableName]) {
                "No column details found for table $tableName"
            }
            MariadbTableInfo(
                tableName = tableName,
                columnDetails = columnDetails,
                columnConstraints = constraintDetailsMap[tableName] ?: emptyList(),
            )
        }
    }

    fun fetchAll(): Pair<List<MariadbTableInfo>, List<MariadbSequenceDetail>> {
        // Note: MariaDB doesn't have sequences like PostgreSQL, but we use an empty list for compatibility
        val sequenceDetails = emptyList<MariadbSequenceDetail>()
        return Pair(fetchMariadbTableInfo(), sequenceDetails)
    }
}

data class MariadbTableInfo(
    val tableName: String,
    val columnDetails: List<MariadbColumnDetail>,
    val columnConstraints: List<MariadbConstraintDetail>,
)
