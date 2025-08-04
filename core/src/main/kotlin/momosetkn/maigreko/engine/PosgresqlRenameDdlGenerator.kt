package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.Column
import momosetkn.maigreko.core.ColumnConstraint
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.ForeignKeyAction
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable
import momosetkn.maigreko.engine.StringUtils.collapseSpaces
import momosetkn.maigreko.engine.StringUtils.normalizeText

interface PosgresqlRenameDdlGenerator : DDLGenerator {
    override fun renameTable(renameTable: RenameTable): String {
        return """
            |alter table ${renameTable.oldTableName}
            |rename to ${renameTable.newTableName}
            """.trimMargin().normalizeText()
    }

    override fun renameColumn(renameColumn: RenameColumn): String {
        return """
            |alter table ${renameColumn.tableName}
            |rename column ${renameColumn.oldColumnName} to ${renameColumn.newColumnName}
            """.trimMargin().normalizeText()
    }
}
