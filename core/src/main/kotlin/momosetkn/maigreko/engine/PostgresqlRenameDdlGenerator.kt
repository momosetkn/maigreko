package momosetkn.maigreko.engine

import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable
import momosetkn.maigreko.engine.StringUtils.normalizeText

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
