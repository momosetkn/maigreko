package momosetkn.maigreko.versioning.infras

import momosetkn.maigreko.jdbc.JdbcExecutor
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * Repository for ChangeSetHistory using JDBC
 */
class ChangeSetHistoryRepository(
    private val dataSource: DataSource
) {
    private val jdbcExecutor = JdbcExecutor(dataSource)

    /**
     * Acquire a lock on the change_set_history table
     */
    fun acquireLock() {
        val sql = "SELECT * FROM ${ChangeSetHistory.TABLE_NAME} FOR UPDATE"
        jdbcExecutor.executeQuery(sql, emptyList()) { /* Do nothing with the result */ }
    }

    /**
     * Fetch a change set history by its ID
     */
    fun fetchChangeSetHistory(changeSetId: String): ChangeSetHistory? {
        val sql = """
            SELECT * FROM ${ChangeSetHistory.TABLE_NAME}
            WHERE ${ChangeSetHistory.CHANGE_SET_ID_COLUMN} = ?
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, listOf(changeSetId)) { resultSet ->
            if (resultSet.next()) {
                mapResultSetToEntity(resultSet)
            } else {
                null
            }
        }
    }

    /**
     * Save a change set history
     */
    fun save(entity: ChangeSetHistory): ChangeSetHistory {
        val sql = """
            INSERT INTO ${ChangeSetHistory.TABLE_NAME} (
                ${ChangeSetHistory.FILENAME_COLUMN},
                ${ChangeSetHistory.AUTHOR_COLUMN},
                ${ChangeSetHistory.CHANGE_SET_ID_COLUMN},
                ${ChangeSetHistory.TAG_COLUMN},
                ${ChangeSetHistory.APPLIED_AT_COLUMN}
            ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        val parameters = listOf(
            entity.filename,
            entity.author,
            entity.changeSetId,
            entity.tag,
            entity.appliedAt
        )

        val id = jdbcExecutor.executeInsert(sql, parameters) { resultSet ->
            resultSet.getLong(1)
        } as Long

        return entity.copy(id = id)
    }

    /**
     * Remove a change set history
     */
    fun remove(entity: ChangeSetHistory) {
        val sql = """
            DELETE FROM ${ChangeSetHistory.TABLE_NAME}
            WHERE ${ChangeSetHistory.ID_COLUMN} = ?
        """.trimIndent()

        jdbcExecutor.executeUpdate(sql, listOf(entity.id))
    }

    /**
     * Map a result set to a ChangeSetHistory entity
     */
    private fun mapResultSetToEntity(resultSet: ResultSet): ChangeSetHistory {
        return ChangeSetHistory(
            id = resultSet.getLong(ChangeSetHistory.ID_COLUMN),
            filename = resultSet.getString(ChangeSetHistory.FILENAME_COLUMN),
            author = resultSet.getString(ChangeSetHistory.AUTHOR_COLUMN),
            changeSetId = resultSet.getString(ChangeSetHistory.CHANGE_SET_ID_COLUMN),
            tag = resultSet.getString(ChangeSetHistory.TAG_COLUMN),
            appliedAt = resultSet.getTimestamp(ChangeSetHistory.APPLIED_AT_COLUMN).toLocalDateTime()
        )
    }
}
