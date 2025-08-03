package momosetkn.maigreko.engine

import momosetkn.maigreko.core.Change
import momosetkn.maigreko.core.CreateTable

interface MigrateEngine {
    fun createTable(createTable: CreateTable)
    fun execute(change: Change)
}
