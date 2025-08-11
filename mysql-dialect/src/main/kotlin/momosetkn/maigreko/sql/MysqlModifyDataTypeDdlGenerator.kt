package momosetkn.maigreko.sql

import momosetkn.maigreko.change.ModifyDataType
import momosetkn.maigreko.sql.StringUtils.collapseSpaces

interface MysqlModifyDataTypeDdlGenerator : DDLGenerator {
    override fun modifyDataType(modifyDataType: ModifyDataType): String {
        return """
            alter table ${modifyDataType.tableName}
            modify column ${modifyDataType.columnName} ${modifyDataType.newDataType}
            """.trimIndent().collapseSpaces()
    }
}
