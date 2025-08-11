package momosetkn.maigreko.sql

import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.MysqlColumnIndividualObject
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface MysqlCreateTableDdlGenerator : DDLGenerator {
    override fun createTable(
        createTable: CreateTable,
    ): String {
        val ifNotExists = if (createTable.ifNotExists) "if not exists" else ""
        val columns = createTable.columns.joinToString(",\n") {
            listOfNotNull(
                nameWithType(it),
                defaultValueWithAutoIncrement(it),
                it.columnConstraint?.let(::constraint),
            ).joinToString(" ")
        }
        return """
                |create table $ifNotExists ${createTable.tableName} (
                |$columns
                |)
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

    private fun nameWithType(column: Column): String {
        return listOf(
            column.name,
            column.type,
        ).joinToString(" ")
    }

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
