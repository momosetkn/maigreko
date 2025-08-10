package momosetkn.maigreko.sql

import momosetkn.maigreko.change.ModifyDataType
import momosetkn.maigreko.sql.StringUtils.collapseSpaces

interface PostgresqlModifyDataTypeDdlGenerator : DDLGenerator {
    override fun modifyDataType(modifyDataType: ModifyDataType): String {
        return """
            alter table ${modifyDataType.tableName}
            alter column ${modifyDataType.columnName} type ${modifyDataType.newDataType}
            """.trimIndent().collapseSpaces()
    }
}