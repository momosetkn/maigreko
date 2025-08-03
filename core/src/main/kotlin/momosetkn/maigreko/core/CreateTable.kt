package momosetkn.maigreko.core

data class CreateTable(
    val ifNotExists: Boolean = false,
    val tableName: String,
    val columns: List<Column>,
)

data class Column(
    val name: String,
    val type: String,
    val value: Any? = null,
    val autoIncrement: Boolean = false,
    val constraint: Constraint,
)

data class Constraint(
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
