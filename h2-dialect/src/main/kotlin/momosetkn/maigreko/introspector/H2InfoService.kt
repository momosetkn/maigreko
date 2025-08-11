package momosetkn.maigreko.introspector

import momosetkn.maigreko.introspector.infras.H2ColumnDetail
import momosetkn.maigreko.introspector.infras.H2ConstraintDetail
import momosetkn.maigreko.introspector.infras.H2InfoRepository
import momosetkn.maigreko.introspector.infras.H2SequenceDetail
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class H2InfoService(
    private val dataSource: DataSource
) {
    private val h2InfoRepository = H2InfoRepository(dataSource)

    fun fetchH2TableInfo(): List<H2TableInfo> {
        val excludeTable = Versioning.VERSIONING_TABLE_NAME
        val tableNames = h2InfoRepository.getTableList(excludeTable)

        val columnDetails = h2InfoRepository.getColumnDetails(excludeTable)
        val columnDetailsMap = columnDetails.groupBy { it.tableName }

        val constraintDetails = h2InfoRepository.getConstraintDetails(excludeTable)
        val constraintDetailsMap = constraintDetails.groupBy { it.tableName }

        return tableNames.map { tableName ->
            val columnDetails = requireNotNull(columnDetailsMap[tableName]) {
                "No column details found for table $tableName"
            }
            H2TableInfo(
                tableName = tableName,
                columnDetails = columnDetails,
                columnConstraints = constraintDetailsMap[tableName] ?: emptyList(),
            )
        }
    }

    fun fetchAll(): Pair<List<H2TableInfo>, List<H2SequenceDetail>> {
        val exclude = Versioning.VERSIONING_SEQUENCE_NAME

        val sequenceDetails = h2InfoRepository.getSequenceDetails(exclude)
        return Pair(fetchH2TableInfo(), sequenceDetails)
    }
}

data class H2TableInfo(
    val tableName: String,
    val columnDetails: List<H2ColumnDetail>,
    val columnConstraints: List<H2ConstraintDetail>,
)
