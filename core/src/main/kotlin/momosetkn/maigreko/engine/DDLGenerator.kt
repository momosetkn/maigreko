package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable

interface DDLGenerator {
    fun createTable(createTable: CreateTable): String
    fun addForeignKey(addForeignKey: AddForeignKey): String
    fun addColumn(addColumn: AddColumn): String
    fun renameTable(renameTable: RenameTable): String
    fun renameColumn(renameColumn: RenameColumn): String
}
