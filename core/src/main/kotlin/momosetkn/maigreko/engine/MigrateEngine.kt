package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.AddIndex
import momosetkn.maigreko.core.Change
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable

open class MigrateEngine(
    open val forwardMigrateEngine: DDLGenerator,
) {
    fun forwardDdl(change: Change): String {
        return when (change) {
            is CreateTable -> forwardMigrateEngine.createTable(change)
            is AddForeignKey -> forwardMigrateEngine.addForeignKey(change)
            is AddColumn -> forwardMigrateEngine.addColumn(change)
            is RenameTable -> forwardMigrateEngine.renameTable(change)
            is RenameColumn -> forwardMigrateEngine.renameColumn(change)
            is AddIndex -> forwardMigrateEngine.addIndex(change)
        }
    }

    fun rollbackDdl(change: Change): String {
        return when (change) {
            is CreateTable -> forwardMigrateEngine.dropTable(change)
            is AddForeignKey -> forwardMigrateEngine.dropForeignKey(change)
            is AddColumn -> forwardMigrateEngine.dropColumn(change)
            is RenameTable -> forwardMigrateEngine.reverseRenameTable(change)
            is RenameColumn -> forwardMigrateEngine.reverseRenameColumn(change)
            is AddIndex -> forwardMigrateEngine.dropIndex(change)
        }
    }
}

class PostgreMigrateEngine(
    override val forwardMigrateEngine: DDLGenerator = PosgresqlDdlGenerator(),
) : MigrateEngine(forwardMigrateEngine)
