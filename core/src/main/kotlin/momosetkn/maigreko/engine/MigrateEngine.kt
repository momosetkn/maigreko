package momosetkn.maigreko.engine

import momosetkn.maigreko.core.Change
import momosetkn.maigreko.core.CreateTable

open class MigrateEngine(
    open val forwardMigrateEngine: DDLGenerator,
    open val rollbackMigrateEngine: DDLGenerator,
) {
    fun forwardDdl(change: Change): String {
        return when (change) {
            is CreateTable -> forwardMigrateEngine.createTable(change)
        }
    }

    fun rollbackDdl(change: Change): String {
        return when (change) {
            is CreateTable -> rollbackMigrateEngine.createTable(change)
        }
    }
}

class PostgreMigrateEngine(
    override val forwardMigrateEngine: DDLGenerator = PosgresqlForwardDdlGenerator(),
    override val rollbackMigrateEngine: DDLGenerator = PosgresqlRollbackDdlGenerator(),
) : MigrateEngine(forwardMigrateEngine, rollbackMigrateEngine)
