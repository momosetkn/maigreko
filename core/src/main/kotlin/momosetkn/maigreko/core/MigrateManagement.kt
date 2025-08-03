package momosetkn.maigreko.core

import momosetkn.maigreko.core.infras.ChangeSetHistory
import momosetkn.maigreko.core.infras.ChangeSetHistoryRepository
import momosetkn.maigreko.engine.MigrateEngine
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase

class MigrateManagement(
    private val db: JdbcDatabase,
    private val migrateEngine: MigrateEngine,
) {
    private val changeSetHistoryRepository = ChangeSetHistoryRepository(db)
    private val migrateManagementBootstrap = MigrateManagementBootstrap(db, migrateEngine)

    fun forwardWithManagement(
        changeSet: ChangeSet
    ) {
        forwardWithManagement(listOf(changeSet))
    }

    fun forwardWithManagement(
        changeSets: List<ChangeSet>
    ) {
        transactionWithBootstrap {
            changeSets.forEach { changeSet ->
                val existsHistory = changeSetHistoryRepository.fetchChangeSetHistory(changeSet.changeSetId)

                if (existsHistory == null) {
                    changeSet.changes.forEach { change ->
                        val ddl = migrateEngine.forwardDdl(change)
                        db.runQuery(QueryDsl.executeScript(ddl))
                    }
                    val newHistory = changeSet.toMaigrekoChangelog()
                    changeSetHistoryRepository.save(newHistory)
                }
            }
        }
    }

    fun rollbackWithManagement(
        changeSet: ChangeSet
    ) {
        rollbackWithManagement(listOf(changeSet))
    }

    fun rollbackWithManagement(
        changeSets: List<ChangeSet>
    ) {
        transactionWithBootstrap {
            changeSets.reversed().forEach { changeSet ->
                val existsHistory = changeSetHistoryRepository.fetchChangeSetHistory(changeSet.changeSetId)

                if (existsHistory != null) {
                    changeSet.changes.forEach { change ->
                        val ddl = migrateEngine.rollbackDdl(change)
                        db.runQuery(QueryDsl.executeScript(ddl))
                    }
                    changeSetHistoryRepository.remove(existsHistory)
                }
            }
        }
    }

    fun transactionWithBootstrap(
        block: () -> Unit
    ) {
        db.withTransaction {
            migrateManagementBootstrap.bootstrap()

            changeSetHistoryRepository.acquireLock()

            block()
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
