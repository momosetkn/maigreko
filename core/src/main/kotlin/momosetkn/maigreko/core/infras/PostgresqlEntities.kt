package momosetkn.maigreko.core.infras

data class PostgresqlColumnDetail(
    val columnName: String,
    val type: String,
    val notNull: String,
    val columnDefault: String?,
    val primaryKey: String,
    val unique: String,
    val foreignTable: String?,
    val foreignColumn: String?,
)

data class PostgresqlConstraintDetail(
    val constraintName: String,
    val tableName: String,
    val columnName: String,
    val foreignTableName: String,
    val foreignColumnName: String,
    val onUpdate: String,
    val onDelete: String,
)
