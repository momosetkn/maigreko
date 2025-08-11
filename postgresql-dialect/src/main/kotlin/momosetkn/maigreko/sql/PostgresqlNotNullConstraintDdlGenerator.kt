package momosetkn.maigreko.sql

import momosetkn.maigreko.change.AddNotNullConstraint
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface PostgresqlNotNullConstraintDdlGenerator : DDLGenerator {
    override fun addNotNullConstraint(addNotNullConstraint: AddNotNullConstraint): String {
        val alterColumn = "ALTER COLUMN ${addNotNullConstraint.columnName}"
        val defaultClause = addNotNullConstraint.defaultValue?.let {
            "$alterColumn SET DEFAULT $it"
        }
        val notNullClause = "$alterColumn SET NOT NULL"

        return """
            |ALTER TABLE ${addNotNullConstraint.tableName}
            |${listOfNotNull(defaultClause, notNullClause).joinToString(",\n")}
            """.trimMargin().normalizeText()
    }

    override fun dropNotNullConstraint(addNotNullConstraint: AddNotNullConstraint): String {
        return """
            |ALTER TABLE ${addNotNullConstraint.tableName}
            |ALTER COLUMN ${addNotNullConstraint.columnName} DROP NOT NULL
            """.trimMargin().normalizeText()
    }
}
