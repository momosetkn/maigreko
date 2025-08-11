package momosetkn.maigreko.sql

import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface MysqlNotNullConstraintDdlGenerator : DDLGenerator {
    override fun addNotNullConstraint(addNotNullConstraint: AddNotNullConstraint): String {
        val defaultClause = addNotNullConstraint.defaultValue?.let {
            "DEFAULT $it"
        } ?: ""

        return """
            |ALTER TABLE ${addNotNullConstraint.tableName}
            |MODIFY COLUMN ${addNotNullConstraint.columnName} ${addNotNullConstraint.columnDataType} NOT NULL $defaultClause
            """.trimMargin().normalizeText()
    }

    override fun dropNotNullConstraint(addNotNullConstraint: AddNotNullConstraint): String {
        val defaultClause = addNotNullConstraint.defaultValue?.let {
            "DEFAULT $it"
        } ?: ""

        return """
            |ALTER TABLE ${addNotNullConstraint.tableName}
            |MODIFY COLUMN ${addNotNullConstraint.columnName} ${addNotNullConstraint.columnDataType} NULL $defaultClause
            """.trimMargin().normalizeText()
    }
}
