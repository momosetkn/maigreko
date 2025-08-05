package momosetkn

import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.bind

@Suppress("LongMethod")
class PostgresqlInfoDao(
    private val db: org.komapper.jdbc.JdbcDatabase,
) {
    fun getConstraintDetails(tableName: String): List<PostgresqlConstraintDetail> {
        val query =
            QueryDsl.fromTemplate(
                """
                    SELECT
                        con.conname AS constraint_name,
                        tbl.relname AS table_name,
                        col.attname AS column_name,
                        f_tbl.relname AS foreign_table_name,
                        f_col.attname AS foreign_column_name,
                        con.confupdtype AS on_update,
                        con.confdeltype AS on_delete
                    FROM
                        pg_constraint con
                    JOIN pg_class tbl ON tbl.oid = con.conrelid
                    JOIN pg_attribute col ON col.attnum = ANY(con.conkey) AND col.attrelid = con.conrelid
                    JOIN pg_class f_tbl ON f_tbl.oid = con.confrelid
                    JOIN pg_attribute f_col ON f_col.attnum = ANY(con.confkey) AND f_col.attrelid = con.confrelid
                    WHERE
                        tbl.relname = /*tableName*/'tableName'
                        AND con.contype = 'f';
                """.trimIndent()
            )
                .bind("tableName", tableName)
                .selectAsEntity(Meta.postgresqlConstraintDetail)
        return db.runQuery(query)
    }

    fun getColumnDetails(tableName: String): List<PostgresqlColumnDetail> {
        val query =
            QueryDsl.fromTemplate(
                """
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
                        WHERE c.table_schema = 'public' AND c.table_name = /*tableName*/'tableName'
                    ),
                    pk_cols AS (
                        SELECT
                            kcu.table_name,
                            kcu.column_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                        WHERE tc.constraint_type = 'PRIMARY KEY'
                          AND tc.table_name = /*tableName*/'tableName'
                    ),
                    unique_cols AS (
                        SELECT DISTINCT
                            kcu.column_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                        WHERE tc.constraint_type = 'UNIQUE'
                          AND tc.table_name = /*tableName*/'tableName'
                    ),
                    fk_cols AS (
                        SELECT
                            kcu.column_name,
                            ccu.table_name AS foreign_table,
                            ccu.column_name AS foreign_column
                        FROM information_schema.table_constraints AS tc
                        JOIN information_schema.key_column_usage AS kcu
                          ON tc.constraint_name = kcu.constraint_name
                        JOIN information_schema.constraint_column_usage AS ccu
                          ON ccu.constraint_name = tc.constraint_name
                        WHERE tc.constraint_type = 'FOREIGN KEY'
                          AND tc.table_name = /*tableName*/'tableName'
                    )
                    SELECT
                        t.column_name,
                        t.data_type ||
                            CASE
                              WHEN t.data_type = 'character varying' THEN
                                '(' || t.character_maximum_length || ')'
                              WHEN t.data_type = 'numeric' THEN
                                '(' || t.numeric_precision || ',' || COALESCE(t.numeric_scale, 0) || ')'
                              ELSE ''
                            END
                            AS type,
                        CASE t.is_nullable WHEN 'NO' THEN 'YES' ELSE 'NO' END AS not_null,
                        t.column_default,
                        CASE WHEN p.column_name IS NOT NULL THEN 'YES' ELSE 'NO' END AS primary_key,
                        CASE WHEN u.column_name IS NOT NULL THEN 'YES' ELSE 'NO' END AS unique,
                        f.foreign_table,
                        f.foreign_column
                    FROM table_cols t
                    LEFT JOIN pk_cols p ON t.column_name = p.column_name
                    LEFT JOIN unique_cols u ON t.column_name = u.column_name
                    LEFT JOIN fk_cols f ON t.column_name = f.column_name
                    ORDER BY t.ordinal_position;
                """.trimIndent()
            )
                .bind("tableName", tableName)
                .selectAsEntity(Meta.postgresqlColumnDetail)

        return db.runQuery(query)
    }
}
