package momosetkn.maigreko.introspector.infras

import momosetkn.maigreko.jdbc.JdbcExecutor
import java.sql.ResultSet
import javax.sql.DataSource

internal class OracleInfoRepository(
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
    ): List<OracleConstraintDetail> {
        val sql = """
            SELECT
                c.constraint_name,
                c.table_name,
                cc.column_name,
                r.table_name as foreign_table_name,
                rc.column_name as foreign_column_name,
                c.delete_rule as on_delete,
                'NO ACTION' as on_update  -- Oracle doesn't support ON UPDATE actions
            FROM
                user_constraints c
            JOIN
                user_cons_columns cc ON c.constraint_name = cc.constraint_name
            JOIN
                user_constraints r ON c.r_constraint_name = r.constraint_name
            JOIN
                user_cons_columns rc ON r.constraint_name = rc.constraint_name
            WHERE
                c.constraint_type = 'R'
                AND c.table_name <> ?
            ORDER BY
                c.table_name,
                c.constraint_name,
                cc.position
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<OracleConstraintDetail>()
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
    ): List<OracleColumnDetail> {
        val sql = """
            SELECT
                c.table_name,
                c.column_name,
                c.data_type || 
                    CASE 
                        WHEN c.data_type IN ('VARCHAR2', 'CHAR', 'NVARCHAR2', 'NCHAR') THEN '(' || c.char_length || ')'
                        WHEN c.data_type = 'NUMBER' AND c.data_precision IS NOT NULL THEN 
                            '(' || c.data_precision || CASE WHEN c.data_scale IS NOT NULL THEN ',' || c.data_scale ELSE '' END || ')'
                        ELSE ''
                    END as type,
                CASE c.nullable WHEN 'N' THEN 'YES' ELSE 'NO' END as not_null,
                c.data_default as column_default,
                CASE WHEN pk.column_name IS NOT NULL THEN 'YES' ELSE 'NO' END as primary_key,
                CASE WHEN uk.column_name IS NOT NULL THEN 'YES' ELSE 'NO' END as unique,
                fk.foreign_table_name as foreign_table,
                fk.foreign_column_name as foreign_column,
                CASE 
                    WHEN i.column_name IS NOT NULL THEN 'IDENTITY'
                    ELSE NULL
                END as generated_kind,
                CASE 
                    WHEN i.column_name IS NOT NULL THEN 'BY DEFAULT'
                    ELSE NULL
                END as identity_generation,
                s.sequence_name as owned_sequence,
                CASE 
                    WHEN s.sequence_name IS NOT NULL THEN 'NUMBER'
                    ELSE NULL
                END as sequence_data_type,
                s.min_value as start_value,
                s.increment_by,
                s.min_value,
                s.max_value,
                s.cache_size,
                CASE s.cycle_flag WHEN 'Y' THEN 1 ELSE 0 END as cycle
            FROM
                user_tab_columns c
            LEFT JOIN (
                SELECT
                    cc.table_name,
                    cc.column_name
                FROM
                    user_constraints con
                JOIN
                    user_cons_columns cc ON con.constraint_name = cc.constraint_name
                WHERE
                    con.constraint_type = 'P'
            ) pk ON c.table_name = pk.table_name AND c.column_name = pk.column_name
            LEFT JOIN (
                SELECT
                    cc.table_name,
                    cc.column_name
                FROM
                    user_constraints con
                JOIN
                    user_cons_columns cc ON con.constraint_name = cc.constraint_name
                WHERE
                    con.constraint_type = 'U'
            ) uk ON c.table_name = uk.table_name AND c.column_name = uk.column_name
            LEFT JOIN (
                SELECT
                    cc.table_name,
                    cc.column_name,
                    r.table_name as foreign_table_name,
                    rc.column_name as foreign_column_name
                FROM
                    user_constraints con
                JOIN
                    user_cons_columns cc ON con.constraint_name = cc.constraint_name
                JOIN
                    user_constraints r ON con.r_constraint_name = r.constraint_name
                JOIN
                    user_cons_columns rc ON r.constraint_name = rc.constraint_name
                WHERE
                    con.constraint_type = 'R'
            ) fk ON c.table_name = fk.table_name AND c.column_name = fk.column_name
            LEFT JOIN (
                SELECT
                    table_name,
                    column_name
                FROM
                    user_tab_identity_cols
            ) i ON c.table_name = i.table_name AND c.column_name = i.column_name
            LEFT JOIN (
                SELECT
                    sequence_name,
                    min_value,
                    max_value,
                    increment_by,
                    cycle_flag,
                    cache_size,
                    last_number
                FROM
                    user_sequences
            ) s ON c.data_default LIKE '%' || s.sequence_name || '%'
            WHERE
                c.table_name <> ?
            ORDER BY
                c.table_name,
                c.column_id
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<OracleColumnDetail>()
            while (resultSet.next()) {
                results.add(mapColumnResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a OracleConstraintDetail entity
     */
    private fun mapConstraintResultSetToEntity(resultSet: ResultSet): OracleConstraintDetail {
        return OracleConstraintDetail(
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
     * Map a result set to a OracleColumnDetail entity
     */
    private fun mapColumnResultSetToEntity(resultSet: ResultSet): OracleColumnDetail {
        return OracleColumnDetail(
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
    fun getSequenceDetails(exclude: String): List<OracleSequenceDetail> {
        val sql = """
            SELECT
                sequence_name as sequenceName,
                NULL as sequenceOwner,
                'NUMBER' as data_type,
                min_value as start_value,
                min_value,
                max_value,
                increment_by,
                CASE cycle_flag WHEN 'Y' THEN 1 ELSE 0 END as cycle,
                cache_size,
                last_number as last_value
            FROM
                user_sequences
            WHERE
                sequence_name <> ?
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, exclude) { resultSet ->
            val results = mutableListOf<OracleSequenceDetail>()
            while (resultSet.next()) {
                results.add(mapSequenceResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a OracleSequenceDetail entity
     */
    private fun mapSequenceResultSetToEntity(resultSet: ResultSet): OracleSequenceDetail {
        return OracleSequenceDetail(
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
                table_name
            FROM
                user_tables
            WHERE
                table_name <> ?
            ORDER BY
                table_name
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
