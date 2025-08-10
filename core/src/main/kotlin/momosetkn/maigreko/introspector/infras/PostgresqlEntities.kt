package momosetkn.maigreko.introspector.infras

data class PostgresqlColumnDetail(
    val tableName: String,
    val columnName: String,
    val type: String,
    val notNull: String,
    val columnDefault: String?,
    val primaryKey: String,
    val unique: String,
    val foreignTable: String?,
    val foreignColumn: String?,
    val generatedKind: String? = null,
    val identityGeneration: String? = null,
    val ownedSequence: String? = null,
    val sequenceDataType: String? = null,
    val startValue: Long? = null,
    val incrementBy: Long? = null,
    val minValue: Long? = null,
    val maxValue: Long? = null,
    val cacheSize: Long? = null,
    val cycle: Boolean? = null,
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

data class PostgresqlSequenceDetail(
    val sequenceName: String,
    val sequenceOwner: String,
    val dataType: String,
    val startValue: Long,
    val minValue: Long,
    val maxValue: Long,
    val incrementBy: Long,
    val cycle: Boolean,
    val cacheSize: Long,
    val lastValue: Long?,
)
