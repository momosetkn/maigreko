package momosetkn.maigreko.sql

import momosetkn.maigreko.change.AddColumn
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddIndex
import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.change.AddUniqueConstraint
import momosetkn.maigreko.change.Change
import momosetkn.maigreko.change.CreateSequence
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.ModifyDataType
import momosetkn.maigreko.change.RenameColumn
import momosetkn.maigreko.change.RenameTable

interface MigrateEngine {
    val ddlGenerator: DDLGenerator

    fun forwardDdl(change: Change): String {
        return when (change) {
            is CreateTable -> ddlGenerator.createTable(change)
            is AddForeignKey -> ddlGenerator.addForeignKey(change)
            is AddColumn -> ddlGenerator.addColumn(change)
            is RenameTable -> ddlGenerator.renameTable(change)
            is RenameColumn -> ddlGenerator.renameColumn(change)
            is AddIndex -> ddlGenerator.addIndex(change)
            is ModifyDataType -> ddlGenerator.modifyDataType(change)
            is AddNotNullConstraint -> ddlGenerator.addNotNullConstraint(change)
            is AddUniqueConstraint -> ddlGenerator.addUniqueConstraint(change)
            is CreateSequence -> ddlGenerator.createSequence(change)
        }
    }

    fun rollbackDdl(change: Change): String {
        return when (change) {
            is CreateTable -> ddlGenerator.dropTable(change)
            is AddForeignKey -> ddlGenerator.dropForeignKey(change)
            is AddColumn -> ddlGenerator.dropColumn(change)
            is RenameTable -> ddlGenerator.reverseRenameTable(change)
            is RenameColumn -> ddlGenerator.reverseRenameColumn(change)
            is AddIndex -> ddlGenerator.dropIndex(change)
            is ModifyDataType -> ddlGenerator.reverseModifyDataType(change)
            is AddNotNullConstraint -> ddlGenerator.dropNotNullConstraint(change)
            is AddUniqueConstraint -> ddlGenerator.dropUniqueConstraint(change)
            is CreateSequence -> ddlGenerator.dropSequence(change)
        }
    }
}
