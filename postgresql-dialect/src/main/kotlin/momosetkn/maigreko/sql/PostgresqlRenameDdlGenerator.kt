package momosetkn.maigreko.sql

import momosetkn.maigreko.change.RenameColumn
import momosetkn.maigreko.change.RenameTable
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface PostgresqlRenameDdlGenerator : DDLGenerator {
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