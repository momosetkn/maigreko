package momosetkn.maigreko.introspector.infras

import momosetkn.maigreko.jdbc.JdbcExecutor
import java.sql.ResultSet
import javax.sql.DataSource

internal class MysqlInfoRepository(
    private val dataSource: DataSource
) {
    private val jdbcExecutor = JdbcExecutor(dataSource)

    /**
     * Get constraint details for a table
     *
     * @return List of constraint details for the specified table
     */
    fun getConstraintDetails(
        excludeTable: String,
    ): List<MysqlConstraintDetail> {
        val sql = """
            SELECT
                kcu.CONSTRAINT_NAME as constraint_name,
                kcu.TABLE_NAME as table_name,
                kcu.COLUMN_NAME as column_name,
                kcu.REFERENCED_TABLE_NAME as foreign_table_name,
                kcu.REFERENCED_COLUMN_NAME as foreign_column_name,
                rc.UPDATE_RULE as on_update,
                rc.DELETE_RULE as on_delete
            FROM
                INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
            JOIN
                INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
                ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
                AND kcu.TABLE_SCHEMA = rc.CONSTRAINT_SCHEMA
            WHERE
                kcu.REFERENCED_TABLE_NAME IS NOT NULL
                AND kcu.TABLE_NAME <> ?
                AND kcu.TABLE_SCHEMA = DATABASE()
            ORDER BY
                kcu.TABLE_NAME,
                kcu.CONSTRAINT_NAME,
                kcu.ORDINAL_POSITION
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<MysqlConstraintDetail>()
            while (resultSet.next()) {
                results.add(mapConstraintResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Get column details for a table
     */
    @Suppress("LongMethod")
    fun getColumnDetails(
        excludeTable: String,
    ): List<MysqlColumnDetail> {
        val sql = """
            SELECT
                c.TABLE_NAME as table_name,
                c.COLUMN_NAME as column_name,
                c.COLUMN_TYPE as type,
                CASE c.IS_NULLABLE WHEN 'NO' THEN 'YES' ELSE 'NO' END as not_null,
                c.COLUMN_DEFAULT as column_default,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 'YES' ELSE 'NO' END as primary_key,
                CASE WHEN uk.COLUMN_NAME IS NOT NULL THEN 'YES' ELSE 'NO' END as `unique`,
                fk.REFERENCED_TABLE_NAME as foreign_table,
                fk.REFERENCED_COLUMN_NAME as foreign_column,
                CASE 
                    WHEN c.EXTRA LIKE '%auto_increment%' THEN 'AUTO_INCREMENT'
                    ELSE NULL
                END as generated_kind,
                NULL as identity_generation,
                NULL as owned_sequence,
                NULL as sequence_data_type,
                NULL as start_value,
                NULL as increment_by,
                NULL as min_value,
                NULL as max_value,
                NULL as cache_size,
                NULL as cycle
            FROM
                INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN (
                SELECT
                    k.TABLE_NAME,
                    k.COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN
                    INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                    ON tc.CONSTRAINT_NAME = k.CONSTRAINT_NAME
                    AND tc.TABLE_SCHEMA = k.TABLE_SCHEMA
                    AND tc.TABLE_NAME = k.TABLE_NAME
                WHERE
                    tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
                    AND tc.TABLE_SCHEMA = DATABASE()
            ) pk ON c.TABLE_NAME = pk.TABLE_NAME AND c.COLUMN_NAME = pk.COLUMN_NAME
            LEFT JOIN (
                SELECT
                    k.TABLE_NAME,
                    k.COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN
                    INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                    ON tc.CONSTRAINT_NAME = k.CONSTRAINT_NAME
                    AND tc.TABLE_SCHEMA = k.TABLE_SCHEMA
                    AND tc.TABLE_NAME = k.TABLE_NAME
                WHERE
                    tc.CONSTRAINT_TYPE = 'UNIQUE'
                    AND tc.TABLE_SCHEMA = DATABASE()
            ) uk ON c.TABLE_NAME = uk.TABLE_NAME AND c.COLUMN_NAME = uk.COLUMN_NAME
            LEFT JOIN (
                SELECT
                    k.TABLE_NAME,
                    k.COLUMN_NAME,
                    k.REFERENCED_TABLE_NAME,
                    k.REFERENCED_COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                WHERE
                    k.REFERENCED_TABLE_NAME IS NOT NULL
                    AND k.TABLE_SCHEMA = DATABASE()
            ) fk ON c.TABLE_NAME = fk.TABLE_NAME AND c.COLUMN_NAME = fk.COLUMN_NAME
            WHERE
                c.TABLE_NAME <> ?
                AND c.TABLE_SCHEMA = DATABASE()
            ORDER BY
                c.TABLE_NAME,
                c.ORDINAL_POSITION
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<MysqlColumnDetail>()
            while (resultSet.next()) {
                results.add(mapColumnResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a MysqlConstraintDetail entity
     */
    private fun mapConstraintResultSetToEntity(resultSet: ResultSet): MysqlConstraintDetail {
        return MysqlConstraintDetail(
            constraintName = resultSet.getString("constraint_name"),
            tableName = resultSet.getString("table_name"),
            columnName = resultSet.getString("column_name"),
            foreignTableName = resultSet.getString("foreign_table_name"),
            foreignColumnName = resultSet.getString("foreign_column_name"),
            onUpdate = resultSet.getString("on_update"),
            onDelete = resultSet.getString("on_delete")
        )
    }

    /**
     * Map a result set to a MysqlColumnDetail entity
     */
    private fun mapColumnResultSetToEntity(resultSet: ResultSet): MysqlColumnDetail {
        return MysqlColumnDetail(
            tableName = resultSet.getString("table_name"),
            columnName = resultSet.getString("column_name"),
            type = resultSet.getString("type"),
            notNull = resultSet.getString("not_null"),
            columnDefault = resultSet.getString("column_default"),
            primaryKey = resultSet.getString("primary_key"),
            unique = resultSet.getString("unique"),
            foreignTable = resultSet.getString("foreign_table"),
            foreignColumn = resultSet.getString("foreign_column"),
            generatedKind = resultSet.getString("generated_kind"),
            identityGeneration = resultSet.getString("identity_generation"),
            ownedSequence = resultSet.getString("owned_sequence"),
            sequenceDataType = resultSet.getString("sequence_data_type"),
            startValue = resultSet.getLong("start_value").takeIf { !resultSet.wasNull() },
            incrementBy = resultSet.getLong("increment_by").takeIf { !resultSet.wasNull() },
            minValue = resultSet.getLong("min_value").takeIf { !resultSet.wasNull() },
            maxValue = resultSet.getLong("max_value").takeIf { !resultSet.wasNull() },
            cacheSize = resultSet.getLong("cache_size").takeIf { !resultSet.wasNull() },
            cycle = resultSet.getBoolean("cycle").takeIf { !resultSet.wasNull() }
        )
    }

    /**
     * Get a list of all tables in the current schema
     *
     * @return List of table names
     */
    fun getTableList(
        excludeTable: String,
    ): List<String> {
        val sql = """
            SELECT
                TABLE_NAME as table_name
            FROM
                INFORMATION_SCHEMA.TABLES
            WHERE
                TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME <> ?
                AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY
                TABLE_NAME
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<String>()
            while (resultSet.next()) {
                results.add(resultSet.getString("table_name"))
            }
            results
        }
    }
}
