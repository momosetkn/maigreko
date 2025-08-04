package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.AddIndex
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable

@Suppress("TooManyFunctions")
interface DDLGenerator {
    fun createTable(createTable: CreateTable): String

    fun addForeignKey(addForeignKey: AddForeignKey): String

    fun addColumn(addColumn: AddColumn): String

    fun renameTable(renameTable: RenameTable): String

    fun renameColumn(renameColumn: RenameColumn): String

    fun dropTable(createTable: CreateTable): String

    fun dropForeignKey(addForeignKey: AddForeignKey): String

    fun dropColumn(addColumn: AddColumn): String

    fun addIndex(addIndex: AddIndex): String

    fun dropIndex(addIndex: AddIndex): String

    fun reverseRenameTable(renameTable: RenameTable): String {
        val reverseRenameTable = renameTable.copy(
            oldTableName = renameTable.newTableName,
            newTableName = renameTable.oldTableName,
        )
        return renameTable(reverseRenameTable)
    }

    fun reverseRenameColumn(renameColumn: RenameColumn): String {
        val reverseRenameColumn = renameColumn.copy(
            oldColumnName = renameColumn.newColumnName,
            newColumnName = renameColumn.oldColumnName,
        )
        return renameColumn(reverseRenameColumn)
    }
}
