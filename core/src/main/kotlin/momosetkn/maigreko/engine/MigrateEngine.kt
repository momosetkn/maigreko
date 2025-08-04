package momosetkn.maigreko.engine

import momosetkn.maigreko.core.AddColumn
import momosetkn.maigreko.core.AddForeignKey
import momosetkn.maigreko.core.Change
import momosetkn.maigreko.core.CreateTable
import momosetkn.maigreko.core.RenameColumn
import momosetkn.maigreko.core.RenameTable

open class MigrateEngine(
    open val forwardMigrateEngine: DDLGenerator,
    open val rollbackMigrateEngine: DDLGenerator,
) {
    fun forwardDdl(change: Change): String {
        return when (change) {
            is CreateTable -> forwardMigrateEngine.createTable(change)
            is AddForeignKey -> forwardMigrateEngine.addForeignKey(change)
            is AddColumn -> forwardMigrateEngine.addColumn(change)
            is RenameTable -> forwardMigrateEngine.renameTable(change)
            is RenameColumn -> forwardMigrateEngine.renameColumn(change)
        }
    }

    fun rollbackDdl(change: Change): String {
        return when (change) {
            is CreateTable -> rollbackMigrateEngine.createTable(change)
            is AddForeignKey -> rollbackMigrateEngine.addForeignKey(change)
            is AddColumn -> rollbackMigrateEngine.addColumn(change)
            is RenameTable -> rollbackMigrateEngine.renameTable(change)
            is RenameColumn -> rollbackMigrateEngine.renameColumn(change)
        }
    }
}

class PostgreMigrateEngine(
    override val forwardMigrateEngine: DDLGenerator = PosgresqlForwardDdlGenerator(),
    override val rollbackMigrateEngine: DDLGenerator = PosgresqlRollbackDdlGenerator(),
) : MigrateEngine(forwardMigrateEngine, rollbackMigrateEngine)
