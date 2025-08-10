package momosetkn.maigreko.sql

import momosetkn.maigreko.change.Column
import momosetkn.maigreko.change.ColumnConstraint
import momosetkn.maigreko.change.CreateTable
import momosetkn.maigreko.change.PostgresqlColumnIndividualObject
import momosetkn.maigreko.sql.StringUtils.normalizeText

interface PostgresqlCreateTableDdlGenerator : DDLGenerator {
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
        if (column.autoIncrement && isLegacySerial(column)) {
            return column.name + " ${typeToSerialType(column.type)}"
        }
        return listOf(
            column.name,
            column.type,
        ).joinToString(" ")
    }

    private fun defaultValueWithAutoIncrement(column: Column): String {
        return if (column.autoIncrement) {
            if (!isLegacySerial(column)) {
                val identityGeneration = column.identityGeneration ?: Column.IdentityGeneration.default
                val baseIdentity = "generated ${identityGeneration.sql} as identity"

                // Collect identity sequence parameters using immutable operations
                val params = listOfNotNull(
                    column.startValue?.let { "START WITH $it" },
                    column.incrementBy?.let { "INCREMENT BY $it" },
                    column.cycle?.let { if (it) "CYCLE" else "NO CYCLE" }
                )

                if (params.isEmpty()) {
                    baseIdentity
                } else {
                    "$baseIdentity (${params.joinToString(" ")})"
                }
            } else {
                ""
            }
        } else {
            column.defaultValue?.let {
                "default $it"
            } ?: ""
        }
    }

    private fun isLegacySerial(column: Column): Boolean =
        (column.individualObject as? PostgresqlColumnIndividualObject)?.generatedKind?.isSerial == true

    companion object {
        private val typeToSerialType = mapOf(
            "smallint" to "smallserial",
            "integer" to "serial",
            "bigint" to "bigserial",
        )
        private fun typeToSerialType(s: String) = requireNotNull(typeToSerialType[s]) { "Unsupported type: $s" }
    }
}