package momosetkn.maigreko.dsl

import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.ColumnIndividualObject

@MaigrekoDslAnnotation
class ColumnDsl(
    private val name: String,
    private val type: String,
) {
    private var defaultValue: Any? = null
    private var autoIncrement: Boolean = false
    private var identityGeneration: String? = null
    private var columnConstraint: ColumnConstraint? = null
    private var startValue: Long? = null
    private var incrementBy: Long? = null
    private var cycle: Boolean? = null
    private var individualObject: ColumnIndividualObject? = null

    fun default(value: Any?) { this.defaultValue = value }
    fun autoIncrement(enabled: Boolean = true) { this.autoIncrement = enabled }
    fun identityGeneration(mode: String?) { this.identityGeneration = mode }

    fun constraint(
        nullable: Boolean = false,
        primaryKey: Boolean = false,
        unique: Boolean = false,
        uniqueConstraintName: String? = null,
        checkConstraint: String? = null,
        deleteCascade: Boolean? = null,
        foreignKeyName: String? = null,
        initiallyDeferred: Boolean? = null,
        deferrable: Boolean? = null,
        validateNullable: Boolean? = null,
        validateUnique: Boolean? = null,
        validatePrimaryKey: Boolean? = null,
        validateForeignKey: Boolean? = null,
    ) {
        columnConstraint = ColumnConstraint(
            nullable,
            primaryKey,
            unique,
            uniqueConstraintName,
            checkConstraint,
            deleteCascade,
            foreignKeyName,
            initiallyDeferred,
            deferrable,
            validateNullable,
            validateUnique,
            validatePrimaryKey,
            validateForeignKey,
        )
    }

    fun individualObject(obj: ColumnIndividualObject) {
        this.individualObject = obj
    }

    internal fun build(): Column = Column.build(
        name = name,
        type = type,
        defaultValue = defaultValue,
        autoIncrement = autoIncrement,
        identityGeneration = identityGeneration,
        columnConstraint = columnConstraint,
        startValue = startValue,
        incrementBy = incrementBy,
        cycle = cycle,
        individualObject = individualObject,
    )
}
