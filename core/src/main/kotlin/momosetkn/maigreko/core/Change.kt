package momosetkn.maigreko.core

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
    val columnConstraint: ColumnConstraint? = null,
)

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
