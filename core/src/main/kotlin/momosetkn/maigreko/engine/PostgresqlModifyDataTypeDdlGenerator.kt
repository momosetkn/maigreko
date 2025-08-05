package momosetkn.maigreko.engine

import momosetkn.maigreko.core.ModifyDataType
import momosetkn.maigreko.engine.StringUtils.collapseSpaces

interface PostgresqlModifyDataTypeDdlGenerator : DDLGenerator {
    override fun modifyDataType(modifyDataType: ModifyDataType): String {
        return """
            alter table ${modifyDataType.tableName}
            alter column ${modifyDataType.columnName} type ${modifyDataType.newDataType}
            """.trimIndent().collapseSpaces()
    }
}
