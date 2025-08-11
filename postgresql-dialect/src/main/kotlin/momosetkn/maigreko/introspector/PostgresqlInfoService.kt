package momosetkn.maigreko.introspector

import momosetkn.maigreko.introspector.infras.PostgresqlColumnDetail
import momosetkn.maigreko.introspector.infras.PostgresqlConstraintDetail
import momosetkn.maigreko.introspector.infras.PostgresqlInfoRepository
import momosetkn.maigreko.introspector.infras.PostgresqlSequenceDetail
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class PostgresqlInfoService(
    private val dataSource: DataSource
) {
    private val postgresqlInfoRepository = PostgresqlInfoRepository(dataSource)

    fun fetchPostgresqlTableInfo(): List<PostgresqlTableInfo> {
        val excludeTable = Versioning.VERSIONING_TABLE_NAME
        val tableNames = postgresqlInfoRepository.getTableList(excludeTable)

        val columnDetails = postgresqlInfoRepository.getColumnDetails(excludeTable)
        val columnDetailsMap = columnDetails.groupBy { it.tableName }

        val constraintDetails = postgresqlInfoRepository.getConstraintDetails(excludeTable)
        val constraintDetailsMap = constraintDetails.groupBy { it.tableName }

        return tableNames.map { tableName ->
            PostgresqlTableInfo(
                tableName = tableName,
                columnDetails = requireNotNull(columnDetailsMap[tableName]),
                columnConstraints = constraintDetailsMap[tableName] ?: emptyList(),
            )
        }
    }

    fun fetchAll(): Pair<List<PostgresqlTableInfo>, List<PostgresqlSequenceDetail>> {
        val exclude = Versioning.VERSIONING_SEQUENCE_NAME

        val sequenceDetails = postgresqlInfoRepository.getSequenceDetails(exclude)
        return Pair(fetchPostgresqlTableInfo(), sequenceDetails)
    }
}

data class PostgresqlTableInfo(
    val tableName: String,
    val columnDetails: List<PostgresqlColumnDetail>,
    val columnConstraints: List<PostgresqlConstraintDetail>,
)
