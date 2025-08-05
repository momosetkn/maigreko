package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.AddIndex
import momosetkn.maigreko.core.ForeignKeyAction
import momosetkn.maigreko.engine.StringUtils.normalizeText

interface PostgresqlAddKeyDdlGenerator : DDLGenerator {
    override fun addForeignKey(addForeignKey: AddForeignKey): String {
        val columnNames = addForeignKey.columnNames.joinToString(", ")
        val referencedColumnNames = addForeignKey.referencedColumnNames.joinToString(", ")

        val onDelete = addForeignKey.onDelete?.let {
            "on delete ${formatForeignKeyAction(it)}"
        } ?: ""

        val onUpdate = addForeignKey.onUpdate?.let {
            "on update ${formatForeignKeyAction(it)}"
        } ?: ""

        val deferrable = if (addForeignKey.deferrable) {
            "deferrable" + if (addForeignKey.initiallyDeferred) " initially deferred" else ""
        } else {
            ""
        }

        return """
            |alter table ${addForeignKey.tableName}
            |add constraint ${addForeignKey.constraintName} 
            |foreign key ($columnNames) 
            |references ${addForeignKey.referencedTableName} ($referencedColumnNames)
            |$onDelete $onUpdate $deferrable
            """.trimMargin().normalizeText()
    }

    private fun formatForeignKeyAction(action: ForeignKeyAction): String {
        return when (action) {
            ForeignKeyAction.CASCADE -> "cascade"
            ForeignKeyAction.SET_NULL -> "set null"
            ForeignKeyAction.SET_DEFAULT -> "set default"
            ForeignKeyAction.RESTRICT -> "restrict"
            ForeignKeyAction.NO_ACTION -> "no action"
        }
    }

    override fun addIndex(addIndex: AddIndex): String {
        val columnNames = addIndex.columnNames.joinToString(", ")
        val uniqueKeyword = if (addIndex.unique) "unique " else ""

        return """
            |create ${uniqueKeyword}index ${addIndex.indexName}
            |on ${addIndex.tableName} ($columnNames)
            """.trimMargin().normalizeText()
    }
}
