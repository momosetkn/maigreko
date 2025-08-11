package momosetkn.maigreko.introspector.infras

import momosetkn.maigreko.jdbc.JdbcExecutor
import java.sql.ResultSet
import javax.sql.DataSource

internal class H2InfoRepository(
    private val dataSource: DataSource
) {
    private val jdbcExecutor = JdbcExecutor(dataSource)

    /**
     * Get constraint details for a table
     *
     * @return List of constraint details for the specified table
     */
    fun getConstraintDetails(
        excludeTableName: String,
    ): List<H2ConstraintDetail> {
        val sql = """
            SELECT
                c.CONSTRAINT_NAME as constraint_name,
                c.TABLE_NAME as table_name,
                k.COLUMN_NAME as column_name,
                c.REFERENCED_TABLE_NAME as foreign_table_name,
                k.REFERENCED_COLUMN_NAME as foreign_column_name,
                c.UPDATE_RULE as on_update,
                c.DELETE_RULE as on_delete
            FROM
                INFORMATION_SCHEMA.CONSTRAINTS c
            JOIN
                INFORMATION_SCHEMA.CROSS_REFERENCES k
                ON c.CONSTRAINT_NAME = k.CONSTRAINT_NAME
            WHERE
                c.CONSTRAINT_TYPE = 'REFERENTIAL'
                AND c.TABLE_NAME <> ?
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTableName) { resultSet ->
            val results = mutableListOf<H2ConstraintDetail>()
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
        excludeTableName: String,
    ): List<H2ColumnDetail> {
        val sql = """
            SELECT
                c.TABLE_NAME as table_name,
                c.COLUMN_NAME as column_name,
                c.TYPE_NAME || 
                    CASE 
                        WHEN c.TYPE_NAME IN ('VARCHAR', 'CHAR') THEN '(' || c.CHARACTER_MAXIMUM_LENGTH || ')'
                        WHEN c.TYPE_NAME = 'DECIMAL' THEN '(' || c.NUMERIC_PRECISION || ',' || COALESCE(c.NUMERIC_SCALE, 0) || ')'
                        ELSE ''
                    END as type,
                CASE c.IS_NULLABLE WHEN 'NO' THEN 'YES' ELSE 'NO' END as not_null,
                c.COLUMN_DEFAULT as column_default,
                CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 'YES' ELSE 'NO' END as primary_key,
                CASE WHEN uk.COLUMN_NAME IS NOT NULL THEN 'YES' ELSE 'NO' END as unique,
                fk.REFERENCED_TABLE_NAME as foreign_table,
                fk.REFERENCED_COLUMN_NAME as foreign_column,
                CASE 
                    WHEN c.SEQUENCE_NAME IS NOT NULL THEN 'IDENTITY'
                    ELSE NULL
                END as generated_kind,
                CASE 
                    WHEN c.IS_IDENTITY = 'YES' THEN 'BY DEFAULT'
                    ELSE NULL
                END as identity_generation,
                c.SEQUENCE_NAME as owned_sequence,
                NULL as sequence_data_type,
                c.IDENTITY_START as start_value,
                c.IDENTITY_INCREMENT as increment_by,
                NULL as min_value,
                NULL as max_value,
                NULL as cache_size,
                NULL as cycle
            FROM
                INFORMATION_SCHEMA.COLUMNS c
            LEFT JOIN (
                SELECT
                    tc.TABLE_NAME,
                    ccu.COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN
                    INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu
                    ON tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME
                WHERE
                    tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
            ) pk ON c.TABLE_NAME = pk.TABLE_NAME AND c.COLUMN_NAME = pk.COLUMN_NAME
            LEFT JOIN (
                SELECT
                    tc.TABLE_NAME,
                    ccu.COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN
                    INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu
                    ON tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME
                WHERE
                    tc.CONSTRAINT_TYPE = 'UNIQUE'
            ) uk ON c.TABLE_NAME = uk.TABLE_NAME AND c.COLUMN_NAME = uk.COLUMN_NAME
            LEFT JOIN (
                SELECT
                    cr.TABLE_NAME,
                    cr.COLUMN_NAME,
                    cr.REFERENCED_TABLE_NAME,
                    cr.REFERENCED_COLUMN_NAME
                FROM
                    INFORMATION_SCHEMA.CROSS_REFERENCES cr
            ) fk ON c.TABLE_NAME = fk.TABLE_NAME AND c.COLUMN_NAME = fk.COLUMN_NAME
            WHERE
                c.TABLE_NAME <> ?
            ORDER BY
                c.TABLE_NAME,
                c.ORDINAL_POSITION
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTableName) { resultSet ->
            val results = mutableListOf<H2ColumnDetail>()
            while (resultSet.next()) {
                results.add(mapColumnResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a H2ConstraintDetail entity
     */
    private fun mapConstraintResultSetToEntity(resultSet: ResultSet): H2ConstraintDetail {
        return H2ConstraintDetail(
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
     * Map a result set to a H2ColumnDetail entity
     */
    private fun mapColumnResultSetToEntity(resultSet: ResultSet): H2ColumnDetail {
        return H2ColumnDetail(
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
     * Get sequence details from the database
     */
    fun getSequenceDetails(exclude: String): List<H2SequenceDetail> {
        val sql = """
            SELECT
                SEQUENCE_NAME as sequenceName,
                NULL as sequenceOwner,
                NULL as data_type,
                START_WITH as start_value,
                MIN_VALUE as min_value,
                MAX_VALUE as max_value,
                INCREMENT as increment_by,
                CYCLE_OPTION as cycle,
                CACHE as cache_size,
                CURRENT_VALUE as last_value
            FROM
                INFORMATION_SCHEMA.SEQUENCES
            WHERE
                SEQUENCE_NAME != ?
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, exclude) { resultSet ->
            val results = mutableListOf<H2SequenceDetail>()
            while (resultSet.next()) {
                results.add(mapSequenceResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a H2SequenceDetail entity
     */
    private fun mapSequenceResultSetToEntity(resultSet: ResultSet): H2SequenceDetail {
        return H2SequenceDetail(
            sequenceName = resultSet.getString("sequenceName"),
            sequenceOwner = resultSet.getString("sequenceOwner"),
            dataType = resultSet.getString("data_type"),
            startValue = resultSet.getLong("start_value"),
            minValue = resultSet.getLong("min_value"),
            maxValue = resultSet.getLong("max_value"),
            incrementBy = resultSet.getLong("increment_by"),
            cycle = resultSet.getBoolean("cycle"),
            cacheSize = resultSet.getLong("cache_size").takeIf { !resultSet.wasNull() },
            lastValue = resultSet.getLong("last_value").takeIf { !resultSet.wasNull() }
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
                TABLE_SCHEMA = 'PUBLIC'
                AND TABLE_NAME <> ?
                AND TABLE_TYPE = 'TABLE'
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
