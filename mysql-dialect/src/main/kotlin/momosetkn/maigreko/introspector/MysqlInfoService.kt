package momosetkn.maigreko.introspector

import momosetkn.maigreko.introspector.infras.MysqlColumnDetail
import momosetkn.maigreko.introspector.infras.MysqlConstraintDetail
import momosetkn.maigreko.introspector.infras.MysqlInfoRepository
import momosetkn.maigreko.introspector.infras.MysqlSequenceDetail
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class MysqlInfoService(
    private val dataSource: DataSource
) {
    private val mysqlInfoRepository = MysqlInfoRepository(dataSource)

    fun fetchMysqlTableInfo(): List<MysqlTableInfo> {
        val excludeTable = Versioning.VERSIONING_TABLE_NAME
        val tableNames = mysqlInfoRepository.getTableList(excludeTable)

        val columnDetails = mysqlInfoRepository.getColumnDetails(excludeTable)
        val columnDetailsMap = columnDetails.groupBy { it.tableName }

        val constraintDetails = mysqlInfoRepository.getConstraintDetails(excludeTable)
        val constraintDetailsMap = constraintDetails.groupBy { it.tableName }

        return tableNames.map { tableName ->
            val columnDetails = requireNotNull(columnDetailsMap[tableName]) {
                "No column details found for table $tableName"
            }
            MysqlTableInfo(
                tableName = tableName,
                columnDetails = columnDetails,
                columnConstraints = constraintDetailsMap[tableName] ?: emptyList(),
            )
        }
    }

    fun fetchAll(): Pair<List<MysqlTableInfo>, List<MysqlSequenceDetail>> {
        // MySQL 8.0+ supports sequences, but we'll get them from the repository
        // which will handle compatibility with older versions
        val sequenceDetails = emptyList<MysqlSequenceDetail>()
        return Pair(fetchMysqlTableInfo(), sequenceDetails)
    }
}

data class MysqlTableInfo(
    val tableName: String,
    val columnDetails: List<MysqlColumnDetail>,
    val columnConstraints: List<MysqlConstraintDetail>,
)
