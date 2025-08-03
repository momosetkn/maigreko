package momosetkn.maigreko.core

sealed interface Change

data class CreateTable(
    val ifNotExists: Boolean = false,
    val tableName: String,
    val columns: List<Column>,
) : Change

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
