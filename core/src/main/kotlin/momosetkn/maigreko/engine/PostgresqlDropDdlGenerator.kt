package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.AddIndex
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.engine.StringUtils.collapseSpaces

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
