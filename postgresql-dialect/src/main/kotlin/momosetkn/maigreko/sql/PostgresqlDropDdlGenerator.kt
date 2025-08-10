package momosetkn.maigreko.sql

import momosetkn.maigreko.change.AddColumn
import momosetkn.maigreko.change.AddForeignKey
import momosetkn.maigreko.change.AddIndex
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.sql.StringUtils.collapseSpaces

interface PostgresqlDropDdlGenerator : DDLGenerator {
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

    override fun dropIndex(addIndex: AddIndex): String {
        return """
                drop index ${addIndex.indexName}
            """.trimIndent().collapseSpaces()
    }
}