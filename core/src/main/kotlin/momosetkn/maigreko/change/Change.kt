package momosetkn.maigreko.change

sealed interface Change

data class CreateTable(
    val ifNotExists: Boolean = false,
    val tableName: String,
    val columns: List<Column>,
) : Change

data class AddForeignKey(
    val constraintName: String,
    val tableName: String,
    val columnNames: List<String>,
    val referencedTableName: String,
    val referencedColumnNames: List<String>,
    val onDelete: ForeignKeyAction? = null,
    val onUpdate: ForeignKeyAction? = null,
    val deferrable: Boolean = false,
    val initiallyDeferred: Boolean = false,
) : Change

data class AddColumn(
    val tableName: String,
    val column: Column,
    val afterColumn: String? = null,
    val beforeColumn: String? = null,
) : Change

data class RenameTable(
    val oldTableName: String,
    val newTableName: String,
) : Change

data class RenameColumn(
    val tableName: String,
    val oldColumnName: String,
    val newColumnName: String,
) : Change

data class AddIndex(
    val indexName: String,
    val tableName: String,
    val columnNames: List<String>,
    val unique: Boolean = false,
) : Change

data class ModifyDataType(
    val tableName: String,
    val columnName: String,
    val newDataType: String,
    val oldDataType: String? = null, // require to rollback
) : Change

data class AddNotNullConstraint(
    val tableName: String,
    val columnName: String,
    val columnDataType: String,
    val defaultValue: Any? = null,
) : Change

data class AddUniqueConstraint(
    val constraintName: String,
    val tableName: String,
    val columnNames: List<String>,
) : Change

data class CreateSequence(
    val sequenceName: String,
    val generatedKind: String? = null,
    val identityGeneration: String? = null,
    val dataType: String? = null,
    val startValue: Long? = null,
    val minValue: Long? = null,
    val maxValue: Long? = null,
    val incrementBy: Long? = null,
    val cycle: Boolean = false,
    val cacheSize: Long? = null,
) : Change

enum class ForeignKeyAction {
    CASCADE,
    SET_NULL,
    SET_DEFAULT,
    RESTRICT,
    NO_ACTION
}

data class Column(
    val name: String,
    val type: String,
    val defaultValue: Any? = null,
    val autoIncrement: Boolean = false,
    /** IDENTITY mode（ALWAYS/BY DEFAULT） */
    val identityGeneration: IdentityGeneration? = null,
    val columnConstraint: ColumnConstraint? = null,
    val startValue: Long? = null,
    val incrementBy: Long? = null,
    val cycle: Boolean? = null,
    val individualObject: ColumnIndividualObject? = null,
) {
    enum class IdentityGeneration(
        val sql: String,
    ) {
        ALWAYS("always"),
        BY_DEFAULT("by default"),
        ;

        companion object {
            val default = BY_DEFAULT

            fun fromSql(sql: String): IdentityGeneration? {
                val s = sql.lowercase()
                return entries.find { it.sql == s }
            }
        }
    }
    companion object {
        @Suppress("LongParameterList")
        fun build(
            name: String,
            type: String,
            defaultValue: Any? = null,
            autoIncrement: Boolean = false,
            identityGeneration: String? = null,
            columnConstraint: ColumnConstraint? = null,
            startValue: Long? = null,
            incrementBy: Long? = null,
            cycle: Boolean? = null,
            individualObject: ColumnIndividualObject? = null,
        ): Column {
            return Column(
                name = name,
                type = type,
                defaultValue = defaultValue,
                columnConstraint = columnConstraint,
                autoIncrement = autoIncrement,
                identityGeneration = identityGeneration?.let(IdentityGeneration::fromSql),
                startValue = startValue,
                incrementBy = incrementBy,
                cycle = cycle,
                individualObject = individualObject,
            )
        }
    }
}

data class ColumnConstraint(
    val nullable: Boolean = false,
    val primaryKey: Boolean = false,
    val unique: Boolean = false,
    val uniqueConstraintName: String? = null,
    val checkConstraint: String? = null,
    val deleteCascade: Boolean? = null,
    val foreignKeyName: String? = null,
    val initiallyDeferred: Boolean? = null,
    val deferrable: Boolean? = null,
    val validateNullable: Boolean? = null,
    val validateUnique: Boolean? = null,
    val validatePrimaryKey: Boolean? = null,
    val validateForeignKey: Boolean? = null,
)

interface ColumnIndividualObject
