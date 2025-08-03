@file:Suppress("ktlint:standard:filename", "MatchingDeclarationName")

package momosetkn

import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId

@KomapperEntity
data class PostgresqlColumnDetail(
    @KomapperId @KomapperColumn(name = "column_name") val columnName: String,
    @KomapperColumn(name = "type") val type: String,
    @KomapperColumn(name = "not_null") val notNull: String,
    @KomapperColumn(name = "column_default") val columnDefault: String?,
    @KomapperColumn(name = "primary_key") val primaryKey: String,
    @KomapperColumn(name = "unique") val unique: String,
    @KomapperColumn(name = "foreign_table") val foreignTable: String?,
    @KomapperColumn(name = "foreign_column") val foreignColumn: String?,
)

@KomapperEntity
data class PostgresqlConstraintDetail(
    @KomapperId @KomapperColumn(name = "constraint_name") val constraintName: String,
    @KomapperColumn(name = "table_name") val tableName: String,
    @KomapperColumn(name = "column_name") val columnName: String,
    @KomapperColumn(name = "foreign_table_name") val foreignTableName: String,
    @KomapperColumn(name = "foreign_column_name") val foreignColumnName: String,
    @KomapperColumn(name = "on_update") val onUpdate: String,
    @KomapperColumn(name = "on_delete") val onDelete: String,
)
