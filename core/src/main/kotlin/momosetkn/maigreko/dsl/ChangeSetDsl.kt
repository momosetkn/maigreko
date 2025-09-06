package momosetkn.maigreko.dsl

import momosetkn.maigreko.change.AddColumn
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddIndex
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ForeignKeyAction
import momosetkn.maigreko.change.ModifyDataType
import momosetkn.maigreko.change.RenameColumn
import momosetkn.maigreko.change.RenameTable

@MaigrekoDslAnnotation
class ChangeSetDsl {
    private val changes = mutableListOf<Change>()

    fun createTable(
        tableName: String,
        ifNotExists: Boolean = false,
        block: TableDsl.() -> Unit
    ) {
        val tb = TableDsl(tableName)
        tb.block()
        changes += CreateTable(
            ifNotExists = ifNotExists,
            tableName = tableName,
            columns = tb.columns.toList(),
        )
    }

    fun addColumn(
        tableName: String,
        name: String,
        type: String,
        afterColumn: String? = null,
        beforeColumn: String? = null,
        block: (ColumnDsl.() -> Unit)? = null,
    ) {
        val cb = ColumnDsl(name, type)
        block?.let { cb.it() }
        changes += AddColumn(
            tableName = tableName,
            column = cb.build(),
            afterColumn = afterColumn,
            beforeColumn = beforeColumn,
        )
    }

    fun addForeignKey(
        constraintName: String,
        tableName: String,
        columnNames: List<String>,
        referencedTableName: String,
        referencedColumnNames: List<String>,
        onDelete: ForeignKeyAction? = null,
        onUpdate: ForeignKeyAction? = null,
        deferrable: Boolean = false,
        initiallyDeferred: Boolean = false,
    ) {
        changes += AddForeignKey(
            constraintName,
            tableName,
            columnNames,
            referencedTableName,
            referencedColumnNames,
            onDelete,
            onUpdate,
            deferrable,
            initiallyDeferred,
        )
    }

    fun renameTable(oldName: String, newName: String) {
        changes += RenameTable(oldName, newName)
    }

    fun renameColumn(tableName: String, oldName: String, newName: String) {
        changes += RenameColumn(tableName, oldName, newName)
    }

    fun addIndex(indexName: String, tableName: String, vararg columns: String, unique: Boolean = false) {
        changes += AddIndex(indexName, tableName, columns.toList(), unique)
    }

    fun modifyDataType(tableName: String, columnName: String, newDataType: String, oldDataType: String? = null) {
        changes += ModifyDataType(tableName, columnName, newDataType, oldDataType)
    }

    fun addNotNullConstraint(
        tableName: String,
        columnName: String,
        columnDataType: String,
        defaultValue: Any? = null,
    ) {
        changes += AddNotNullConstraint(tableName, columnName, columnDataType, defaultValue)
    }

    fun addUniqueConstraint(constraintName: String, tableName: String, vararg columnNames: String) {
        changes += AddUniqueConstraint(constraintName, tableName, columnNames.toList())
    }

    fun createSequence(
        sequenceName: String,
        generatedKind: String? = null,
        identityGeneration: String? = null,
        dataType: String? = null,
        startValue: Long? = null,
        minValue: Long? = null,
        maxValue: Long? = null,
        incrementBy: Long? = null,
        cycle: Boolean = false,
        cacheSize: Long? = null,
    ) {
        changes += CreateSequence(
            sequenceName,
            generatedKind,
            identityGeneration,
            dataType,
            startValue,
            minValue,
            maxValue,
            incrementBy,
            cycle,
            cacheSize,
        )
    }

    internal fun getChanges(): List<Change> = changes.toList()
}
