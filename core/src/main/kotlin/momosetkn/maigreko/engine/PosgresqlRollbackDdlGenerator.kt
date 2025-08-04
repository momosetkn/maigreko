package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable
import momosetkn.maigreko.engine.StringUtils.collapseSpaces

class PosgresqlRollbackDdlGenerator : DDLGenerator {
    override fun createTable(createTable: CreateTable): String {
        return """
                drop table ${createTable.tableName}
            """.trimIndent().collapseSpaces()
    }

    override fun addForeignKey(addForeignKey: AddForeignKey): String {
        return """
                alter table ${addForeignKey.tableName}
                drop constraint ${addForeignKey.constraintName}
            """.trimIndent().collapseSpaces()
    }

    override fun addColumn(addColumn: AddColumn): String {
        return """
                alter table ${addColumn.tableName}
                drop column ${addColumn.column.name}
            """.trimIndent().collapseSpaces()
    }

    override fun renameTable(renameTable: RenameTable): String {
        return """
                alter table ${renameTable.newTableName}
                rename to ${renameTable.oldTableName}
            """.trimIndent().collapseSpaces()
    }

    override fun renameColumn(renameColumn: RenameColumn): String {
        return """
                alter table ${renameColumn.tableName}
                rename column ${renameColumn.newColumnName} to ${renameColumn.oldColumnName}
            """.trimIndent().collapseSpaces()
    }
}
