package momosetkn.maigreko.introspector.infras

import momosetkn.maigreko.jdbc.JdbcExecutor
import java.sql.ResultSet
import javax.sql.DataSource

internal class SqlServerInfoRepository(
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
    ): List<SqlServerConstraintDetail> {
        val sql = """
            SELECT
                fk.name AS constraint_name,
                OBJECT_NAME(fk.parent_object_id) AS table_name,
                COL_NAME(fkc.parent_object_id, fkc.parent_column_id) AS column_name,
                OBJECT_NAME(fk.referenced_object_id) AS foreign_table_name,
                COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id) AS foreign_column_name,
                CASE fk.update_referential_action
                    WHEN 0 THEN 'NO ACTION'
                    WHEN 1 THEN 'CASCADE'
                    WHEN 2 THEN 'SET NULL'
                    WHEN 3 THEN 'SET DEFAULT'
                END AS on_update,
                CASE fk.delete_referential_action
                    WHEN 0 THEN 'NO ACTION'
                    WHEN 1 THEN 'CASCADE'
                    WHEN 2 THEN 'SET NULL'
                    WHEN 3 THEN 'SET DEFAULT'
                END AS on_delete
            FROM
                sys.foreign_keys fk
            INNER JOIN
                sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
            WHERE
                OBJECT_NAME(fk.parent_object_id) <> ?
            ORDER BY
                table_name,
                constraint_name,
                column_name
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<SqlServerConstraintDetail>()
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
    ): List<SqlServerColumnDetail> {
        val sql = """
            SELECT
                t.name AS table_name,
                c.name AS column_name,
                CASE 
                    WHEN ty.name IN ('varchar', 'nvarchar', 'char', 'nchar') AND c.max_length != -1 THEN 
                        ty.name + '(' + CAST(c.max_length / CASE WHEN ty.name LIKE 'n%' THEN 2 ELSE 1 END AS VARCHAR) + ')'
                    WHEN ty.name IN ('decimal', 'numeric') THEN 
                        ty.name + '(' + CAST(c.precision AS VARCHAR) + ',' + CAST(c.scale AS VARCHAR) + ')'
                    ELSE ty.name
                END AS type,
                CASE WHEN c.is_nullable = 0 THEN 'YES' ELSE 'NO' END AS not_null,
                OBJECT_DEFINITION(c.default_object_id) AS column_default,
                CASE WHEN pk.column_id IS NOT NULL THEN 'YES' ELSE 'NO' END AS primary_key,
                CASE WHEN uk.column_id IS NOT NULL THEN 'YES' ELSE 'NO' END AS unique,
                OBJECT_NAME(fk.referenced_object_id) AS foreign_table,
                COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id) AS foreign_column,
                CASE 
                    WHEN c.is_identity = 1 THEN 'IDENTITY'
                    ELSE NULL
                END AS generated_kind,
                CASE 
                    WHEN c.is_identity = 1 THEN 'BY DEFAULT'
                    ELSE NULL
                END AS identity_generation,
                NULL AS owned_sequence,
                NULL AS sequence_data_type,
                CAST(ISNULL(IDENT_SEED(OBJECT_NAME(c.object_id)), 0) AS BIGINT) AS start_value,
                CAST(ISNULL(IDENT_INCR(OBJECT_NAME(c.object_id)), 0) AS BIGINT) AS increment_by,
                NULL AS min_value,
                NULL AS max_value,
                NULL AS cache_size,
                NULL AS cycle
            FROM
                sys.columns c
            INNER JOIN
                sys.tables t ON c.object_id = t.object_id
            INNER JOIN
                sys.types ty ON c.user_type_id = ty.user_type_id
            LEFT JOIN (
                SELECT
                    ic.object_id,
                    ic.column_id
                FROM
                    sys.index_columns ic
                INNER JOIN
                    sys.indexes i ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                WHERE
                    i.is_primary_key = 1
            ) pk ON c.object_id = pk.object_id AND c.column_id = pk.column_id
            LEFT JOIN (
                SELECT
                    ic.object_id,
                    ic.column_id
                FROM
                    sys.index_columns ic
                INNER JOIN
                    sys.indexes i ON ic.object_id = i.object_id AND ic.index_id = i.index_id
                WHERE
                    i.is_unique = 1 AND i.is_primary_key = 0
            ) uk ON c.object_id = uk.object_id AND c.column_id = uk.column_id
            LEFT JOIN (
                SELECT
                    fkc.parent_object_id,
                    fkc.parent_column_id,
                    fk.referenced_object_id
                FROM
                    sys.foreign_key_columns fkc
                INNER JOIN
                    sys.foreign_keys fk ON fkc.constraint_object_id = fk.object_id
            ) fk ON c.object_id = fk.parent_object_id AND c.column_id = fk.parent_column_id
            LEFT JOIN
                sys.foreign_key_columns fkc ON fk.parent_object_id = fkc.parent_object_id 
                AND fk.parent_column_id = fkc.parent_column_id
            WHERE
                t.name <> ?
            ORDER BY
                t.name,
                c.column_id
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTable) { resultSet ->
            val results = mutableListOf<SqlServerColumnDetail>()
            while (resultSet.next()) {
                results.add(mapColumnResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a SqlServerConstraintDetail entity
     */
    private fun mapConstraintResultSetToEntity(resultSet: ResultSet): SqlServerConstraintDetail {
        return SqlServerConstraintDetail(
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
     * Map a result set to a SqlServerColumnDetail entity
     */
    private fun mapColumnResultSetToEntity(resultSet: ResultSet): SqlServerColumnDetail {
        return SqlServerColumnDetail(
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
    fun getSequenceDetails(exclude: String): List<SqlServerSequenceDetail> {
        val sql = """
            SELECT
                name AS sequenceName,
                NULL AS sequenceOwner,
                CASE 
                    WHEN system_type_id = 56 THEN 'int'
                    WHEN system_type_id = 127 THEN 'bigint'
                    WHEN system_type_id = 52 THEN 'smallint'
                    WHEN system_type_id = 48 THEN 'tinyint'
                    ELSE 'bigint'
                END AS data_type,
                CAST(start_value AS BIGINT) AS start_value,
                CAST(minimum_value AS BIGINT) AS min_value,
                CAST(maximum_value AS BIGINT) AS max_value,
                CAST(increment AS BIGINT) AS increment_by,
                CASE is_cycling WHEN 1 THEN 1 ELSE 0 END AS cycle,
                NULL AS cache_size,
                CAST(current_value AS BIGINT) AS last_value
            FROM
                sys.sequences
            WHERE
                name <> ?
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, exclude) { resultSet ->
            val results = mutableListOf<SqlServerSequenceDetail>()
            while (resultSet.next()) {
                results.add(mapSequenceResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a SqlServerSequenceDetail entity
     */
    private fun mapSequenceResultSetToEntity(resultSet: ResultSet): SqlServerSequenceDetail {
        return SqlServerSequenceDetail(
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
                name AS table_name
            FROM
                sys.tables
            WHERE
                name <> ?
                AND type = 'U'
            ORDER BY
                name
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
