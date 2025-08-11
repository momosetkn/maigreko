package momosetkn.maigreko.introspector.infras

import momosetkn.maigreko.jdbc.JdbcExecutor
import java.sql.ResultSet
import javax.sql.DataSource

internal class PostgresqlInfoRepository(
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
    ): List<PostgresqlConstraintDetail> {
        val sql = """
            SELECT
                con.conname AS constraint_name,
                tbl.relname AS table_name,
                col.attname AS column_name,
                f_tbl.relname AS foreign_table_name,
                f_col.attname AS foreign_column_name,
                con.confupdtype AS on_update,
                con.confdeltype AS on_delete,
                col.attnum,
                col.attidentity
            FROM
                pg_constraint con
            JOIN pg_class tbl ON tbl.oid = con.conrelid
            JOIN pg_attribute col ON col.attnum = ANY(con.conkey) AND col.attrelid = con.conrelid
            JOIN pg_class f_tbl ON f_tbl.oid = con.confrelid
            JOIN pg_attribute f_col ON f_col.attnum = ANY(con.confkey) AND f_col.attrelid = con.confrelid
            WHERE
                con.contype = 'f'
                AND tbl.relname <> ?
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTableName) { resultSet ->
            val results = mutableListOf<PostgresqlConstraintDetail>()
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
    ): List<PostgresqlColumnDetail> {
        val sql = """
            WITH table_cols AS (
                SELECT
                    c.table_schema,
                    c.table_name,
                    c.column_name,
                    c.ordinal_position,
                    c.data_type,
                    c.character_maximum_length,
                    c.numeric_precision,
                    c.numeric_scale,
                    c.is_nullable,
                    c.column_default
                FROM information_schema.columns c
                WHERE c.table_schema = 'public'
            ),
            -- ★ Strictly identify the sequences owned by a column（IDENTITY/serial）
            seq_owned AS (
                SELECT
                    tn.nspname AS table_schema,
                    t.relname  AS table_name,
                    a.attname  AS column_name,
                    ns.nspname AS sequence_schema,
                    s.relname  AS sequence_name
                FROM pg_class s
                JOIN pg_namespace ns ON ns.oid = s.relnamespace
                JOIN pg_depend dep
                  ON dep.objid = s.oid
                 AND dep.classid = 'pg_class'::regclass
                 AND dep.refclassid = 'pg_class'::regclass
                 AND dep.deptype in ('a', 'i')             -- auto or internal
                JOIN pg_class t ON t.oid = dep.refobjid
                JOIN pg_namespace tn ON tn.oid = t.relnamespace
                JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = dep.refobjsubid
                WHERE s.relkind = 'S'
            ),
            -- ★ IDENTITY judgment（ALWAYS/BY DEFAULT/not IDENTITY）
            identity_cols AS (
                SELECT
                    tn.nspname AS table_schema,
                    t.relname  AS table_name,
                    a.attname  AS column_name,
                    a.attidentity              -- '' (非ID), 'a' (ALWAYS), 'd' (BY DEFAULT)
                FROM pg_class t
                JOIN pg_namespace tn ON tn.oid = t.relnamespace
                JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum > 0 AND NOT a.attisdropped
                WHERE t.relkind IN ('r','p')
            ),
            -- ★ Sequence Detailed Parameters
            seq_params AS (
                SELECT
                    schemaname   AS sequence_schema,
                    sequencename AS sequence_name,
                    data_type,
                    start_value,
                    min_value,
                    max_value,
                    increment_by,
                    cycle,
                    cache_size
                FROM pg_sequences
            ),
            pk_cols AS (
                SELECT
                    kcu.table_schema,
                    kcu.table_name,
                    kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema    = kcu.table_schema
                WHERE tc.constraint_type = 'PRIMARY KEY'
            ),
            unique_cols AS (
                SELECT DISTINCT
                    kcu.table_schema,
                    kcu.table_name,
                    kcu.column_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema    = kcu.table_schema
                WHERE tc.constraint_type = 'UNIQUE'
            ),
            fk_cols AS (
                SELECT
                    tc.table_schema,
                    tc.table_name,
                    kcu.column_name,
                    ccu.table_name  AS foreign_table,
                    ccu.column_name AS foreign_column
                FROM information_schema.table_constraints AS tc
                JOIN information_schema.key_column_usage AS kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema    = kcu.table_schema
                JOIN information_schema.constraint_column_usage AS ccu
                  ON ccu.constraint_name = tc.constraint_name
                 AND ccu.table_schema    = tc.table_schema
                WHERE tc.constraint_type = 'FOREIGN KEY'
            )
            SELECT
                t.table_schema,
                t.table_name,
                t.column_name,
                t.data_type ||
                    CASE
                      WHEN t.data_type = 'character varying' THEN
                        '(' || t.character_maximum_length || ')'
                      WHEN t.data_type = 'numeric' THEN
                        '(' || t.numeric_precision || ',' || COALESCE(t.numeric_scale, 0) || ')'
                      ELSE ''
                    END AS type,
                CASE t.is_nullable WHEN 'NO' THEN 'YES' ELSE 'NO' END AS not_null,
                t.column_default,
                CASE WHEN p.column_name IS NOT NULL THEN 'YES' ELSE 'NO' END AS primary_key,
                CASE WHEN u.column_name IS NOT NULL THEN 'YES' ELSE 'NO' END AS unique,
                f.foreign_table,
                f.foreign_column,
                -- ★ Generation type（IDENTITY / serial / other）
                CASE
                  WHEN ic.attidentity IN ('a','d') THEN 'IDENTITY'
                  WHEN t.column_default LIKE 'nextval(%' THEN 'SERIAL-LIKE'
                  ELSE NULL
                END AS generated_kind,
                -- ★ IDENTITY mode（ALWAYS/BY DEFAULT）
                CASE ic.attidentity
                  WHEN 'a' THEN 'ALWAYS'
                  WHEN 'd' THEN 'BY DEFAULT'
                  ELSE NULL
                END AS identity_generation,
                -- ★ Owning sequence（Schema name. Name）and various parameters
                CASE WHEN so.sequence_name IS NOT NULL THEN so.sequence_schema || '.' || so.sequence_name END AS owned_sequence,
                sp.data_type    AS sequence_data_type,
                sp.start_value,
                sp.increment_by,
                sp.min_value,
                sp.max_value,
                sp.cache_size,
                sp.cycle
            FROM table_cols t
            LEFT JOIN pk_cols p
              ON t.table_schema = p.table_schema AND t.table_name = p.table_name AND t.column_name = p.column_name
            LEFT JOIN unique_cols u
              ON t.table_schema = u.table_schema AND t.table_name = u.table_name AND t.column_name = u.column_name
            LEFT JOIN fk_cols f
              ON t.table_schema = f.table_schema AND t.table_name = f.table_name AND t.column_name = f.column_name
            LEFT JOIN identity_cols ic
              ON t.table_schema = ic.table_schema AND t.table_name = ic.table_name AND t.column_name = ic.column_name
            LEFT JOIN seq_owned so
              ON t.table_schema = so.table_schema AND t.table_name = so.table_name AND t.column_name = so.column_name
            LEFT JOIN seq_params sp
              ON so.sequence_schema = sp.sequence_schema AND so.sequence_name = sp.sequence_name
            WHERE t.table_name <> ?
            ORDER BY t.table_schema, t.table_name, t.ordinal_position;
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, excludeTableName) { resultSet ->
            val results = mutableListOf<PostgresqlColumnDetail>()
            while (resultSet.next()) {
                results.add(mapColumnResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a PostgresqlConstraintDetail entity
     */
    private fun mapConstraintResultSetToEntity(resultSet: ResultSet): PostgresqlConstraintDetail {
        return PostgresqlConstraintDetail(
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
     * Map a result set to a PostgresqlColumnDetail entity
     */
    private fun mapColumnResultSetToEntity(resultSet: ResultSet): PostgresqlColumnDetail {
        return PostgresqlColumnDetail(
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
    fun getSequenceDetails(exclude: String): List<PostgresqlSequenceDetail> {
        val sql = """
            SELECT sequencename,
                   sequenceowner,
                   data_type,
                   start_value,
                   min_value,
                   max_value,
                   increment_by,
                   cycle,
                   cache_size,
                   last_value
            FROM pg_sequences
            WHERE schemaname = current_schema()
            AND sequencename != ?;
        """.trimIndent()

        return jdbcExecutor.executeQuery(sql, exclude) { resultSet ->
            val results = mutableListOf<PostgresqlSequenceDetail>()
            while (resultSet.next()) {
                results.add(mapSequenceResultSetToEntity(resultSet))
            }
            results
        }
    }

    /**
     * Map a result set to a PostgresqlSequenceDetail entity
     */
    private fun mapSequenceResultSetToEntity(resultSet: ResultSet): PostgresqlSequenceDetail {
        return PostgresqlSequenceDetail(
            sequenceName = resultSet.getString("sequencename"),
            sequenceOwner = resultSet.getString("sequenceowner"),
            dataType = resultSet.getString("data_type"),
            startValue = resultSet.getLong("start_value"),
            minValue = resultSet.getLong("min_value"),
            maxValue = resultSet.getLong("max_value"),
            incrementBy = resultSet.getLong("increment_by"),
            cycle = resultSet.getBoolean("cycle"),
            cacheSize = resultSet.getLong("cache_size"),
            lastValue = if (resultSet.getObject("last_value") == null) null else resultSet.getLong("last_value")
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
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
                AND table_name <> ?
                AND table_type = 'BASE TABLE'
            ORDER BY table_name;
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
