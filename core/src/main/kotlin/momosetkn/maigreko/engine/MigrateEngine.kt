package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.AddIndex
import momosetkn.maigreko.core.AddNotNullConstraint
import momosetkn.maigreko.core.AddUniqueConstraint
import momosetkn.maigreko.core.Change
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.ModifyDataType
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable

open class MigrateEngine(
    open val ddlGenerator: DDLGenerator,
) {
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
        }
    }
}

class PostgreMigrateEngine(
    override val ddlGenerator: DDLGenerator = PostgresqlDdlGenerator(),
) : MigrateEngine(ddlGenerator)
