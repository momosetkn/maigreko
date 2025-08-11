package momosetkn.maigreko.introspector

import momosetkn.maigreko.introspector.infras.SqlServerColumnDetail
import momosetkn.maigreko.introspector.infras.SqlServerConstraintDetail
import momosetkn.maigreko.introspector.infras.SqlServerInfoRepository
import momosetkn.maigreko.introspector.infras.SqlServerSequenceDetail
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class SqlServerInfoService(
    private val dataSource: DataSource
) {
    private val sqlServerInfoRepository = SqlServerInfoRepository(dataSource)

    fun fetchSqlServerTableInfo(): List<SqlServerTableInfo> {
        val excludeTable = Versioning.VERSIONING_TABLE_NAME
        val tableNames = sqlServerInfoRepository.getTableList(excludeTable)

        val columnDetails = sqlServerInfoRepository.getColumnDetails(excludeTable)
        val columnDetailsMap = columnDetails.groupBy { it.tableName }

        val constraintDetails = sqlServerInfoRepository.getConstraintDetails(excludeTable)
        val constraintDetailsMap = constraintDetails.groupBy { it.tableName }

        return tableNames.map { tableName ->
            val columnDetails = requireNotNull(columnDetailsMap[tableName]) {
                "No column details found for table $tableName"
            }
            SqlServerTableInfo(
                tableName = tableName,
                columnDetails = columnDetails,
                columnConstraints = constraintDetailsMap[tableName] ?: emptyList(),
            )
        }
    }

    fun fetchAll(): Pair<List<SqlServerTableInfo>, List<SqlServerSequenceDetail>> {
        val exclude = Versioning.VERSIONING_SEQUENCE_NAME

        val sequenceDetails = sqlServerInfoRepository.getSequenceDetails(exclude)
        return Pair(fetchSqlServerTableInfo(), sequenceDetails)
    }
}

data class SqlServerTableInfo(
    val tableName: String,
    val columnDetails: List<SqlServerColumnDetail>,
    val columnConstraints: List<SqlServerConstraintDetail>,
)
