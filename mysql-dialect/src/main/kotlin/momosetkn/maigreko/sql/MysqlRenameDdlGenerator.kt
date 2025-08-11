package momosetkn.maigreko.sql

import momosetkn.maigreko.change.RenameColumn
import momosetkn.maigreko.change.RenameTable
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface MysqlRenameDdlGenerator : DDLGenerator {
    override fun renameTable(renameTable: RenameTable): String {
        return """
            |rename table ${renameTable.oldTableName}
            |to ${renameTable.newTableName}
            """.trimMargin().normalizeText()
    }

    override fun renameColumn(renameColumn: RenameColumn): String {
        return """
            |alter table ${renameColumn.tableName}
            |change column ${renameColumn.oldColumnName} ${renameColumn.newColumnName} 
            """.trimMargin().normalizeText()
    }
}
