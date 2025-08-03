package momosetkn.maigreko.core

import momosetkn.maigreko.core.infras.ChangeSetHistory
import momosetkn.maigreko.core.infras.ChangeSetHistoryRepository
import momosetkn.maigreko.engine.MigrateEngine
import org.komapper.jdbc.JdbcDatabase

class MigrateManagement(
    val db: JdbcDatabase,
    private val migrateEngine: MigrateEngine,
) {
    private val changeSetHistoryRepository = ChangeSetHistoryRepository(db)

    fun executeWithManagement(
        changeSet: ChangeSet
    ) {
        executeWithManagement(listOf(changeSet))
    }

    fun executeWithManagement(
        changeSets: List<ChangeSet>
    ) {
        db.withTransaction {
            val maigrekoCreateTable = MigrateManagementInitialize.createTable()
            migrateEngine.createTable(maigrekoCreateTable)

            changeSetHistoryRepository.acquireLock()

            changeSets.forEach { changeSet ->
                val existsHistory = changeSetHistoryRepository.fetchChangeSetHistory(changeSet.changeSetId)

                if (existsHistory == null) {
                    changeSet.changes.forEach { change ->
                        migrateEngine.execute(change)
                    }
                    val newHistory = changeSet.toMaigrekoChangelog()
                    changeSetHistoryRepository.save(newHistory)
                }
            }
        }
    }
}

private fun ChangeSet.toMaigrekoChangelog(): ChangeSetHistory {
    return ChangeSetHistory(
        filename = filename,
        author = author,
        changeSetId = changeSetId,
    )
}
