package momosetkn.maigreko.engine

import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.engine.StringUtils.collapseSpaces

class PosgresqlRollbackDdlGenerator : DDLGenerator {
    override fun createTable(createTable: CreateTable): String {
        return """
                drop table ${createTable.tableName}
            """.trimIndent().collapseSpaces()
    }
}
