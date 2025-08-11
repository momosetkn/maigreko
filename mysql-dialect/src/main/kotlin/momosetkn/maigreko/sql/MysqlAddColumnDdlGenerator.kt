package momosetkn.maigreko.sql

import momosetkn.maigreko.change.AddColumn
import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.MysqlColumnIndividualObject
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface MysqlAddColumnDdlGenerator : DDLGenerator {
    override fun addColumn(addColumn: AddColumn): String {
        val column = addColumn.column
        val columnDefinition = listOfNotNull(
            nameWithType(column),
            defaultValueWithAutoIncrement(column),
            column.columnConstraint?.let(::constraint),
        ).joinToString(" ")

        val positionClause = when {
            addColumn.afterColumn != null -> "after ${addColumn.afterColumn}"
            addColumn.beforeColumn != null -> "first" // MySQL doesn't have BEFORE, use FIRST if it's the first column
            else -> ""
        }

        return """
            |alter table ${addColumn.tableName}
            |add column $columnDefinition $positionClause
            """.trimMargin().normalizeText()
    }

    private fun constraint(columnConstraint: ColumnConstraint): String {
        return when {
            columnConstraint.primaryKey -> "primary key"
            else -> {
                listOfNotNull(
                    if (columnConstraint.unique) "unique" else null,
                    if (columnConstraint.nullable) null else "not null",
                ).joinToString(" ")
            }
        }
    }

    private fun nameWithType(column: Column): String = listOf(
        column.name,
        column.type,
    ).joinToString(" ")

    private fun defaultValueWithAutoIncrement(column: Column): String {
        return if (column.autoIncrement) {
            val individualObject = column.individualObject as? MysqlColumnIndividualObject
            if (individualObject?.generatedKind?.isAutoIncrement == true) {
                "auto_increment"
            } else {
                ""
            }
        } else {
            column.defaultValue?.let {
                "default $it"
            } ?: ""
        }
    }
}
