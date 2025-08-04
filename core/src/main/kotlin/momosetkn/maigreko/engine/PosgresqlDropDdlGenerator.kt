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

interface PosgresqlDropDdlGenerator : DDLGenerator {
    override fun dropTable(createTable: CreateTable): String {
        return """
                drop table ${createTable.tableName}
            """.trimIndent().collapseSpaces()
    }

    override fun dropForeignKey(addForeignKey: AddForeignKey): String {
        return """
                alter table ${addForeignKey.tableName}
                drop constraint ${addForeignKey.constraintName}
            """.trimIndent().collapseSpaces()
    }

    override fun dropColumn(addColumn: AddColumn): String {
        return """
                alter table ${addColumn.tableName}
                drop column ${addColumn.column.name}
            """.trimIndent().collapseSpaces()
    }
}
