package momosetkn.maigreko.introspector

import momosetkn.maigreko.introspector.infras.OracleColumnDetail
import momosetkn.maigreko.introspector.infras.OracleConstraintDetail
import momosetkn.maigreko.introspector.infras.OracleInfoRepository
import momosetkn.maigreko.introspector.infras.OracleSequenceDetail
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class OracleInfoService(
    private val dataSource: DataSource
) {
    private val oracleInfoRepository = OracleInfoRepository(dataSource)

    fun fetchOracleTableInfo(): List<OracleTableInfo> {
        val excludeTable = Versioning.VERSIONING_TABLE_NAME
        val tableNames = oracleInfoRepository.getTableList(excludeTable)

        val columnDetails = oracleInfoRepository.getColumnDetails(excludeTable)
        val columnDetailsMap = columnDetails.groupBy { it.tableName }

        val constraintDetails = oracleInfoRepository.getConstraintDetails(excludeTable)
        val constraintDetailsMap = constraintDetails.groupBy { it.tableName }

        return tableNames.map { tableName ->
            val columnDetails = requireNotNull(columnDetailsMap[tableName]) {
                "No column details found for table $tableName"
            }
            OracleTableInfo(
                tableName = tableName,
                columnDetails = columnDetails,
                columnConstraints = constraintDetailsMap[tableName] ?: emptyList(),
            )
        }
    }

    fun fetchAll(): Pair<List<OracleTableInfo>, List<OracleSequenceDetail>> {
        val exclude = Versioning.VERSIONING_SEQUENCE_NAME

        val sequenceDetails = oracleInfoRepository.getSequenceDetails(exclude)
        return Pair(fetchOracleTableInfo(), sequenceDetails)
    }
}

data class OracleTableInfo(
    val tableName: String,
    val columnDetails: List<OracleColumnDetail>,
    val columnConstraints: List<OracleConstraintDetail>,
)
