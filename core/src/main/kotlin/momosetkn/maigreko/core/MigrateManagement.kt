package momosetkn.maigreko.core

import momosetkn.maigreko.core.infras.ChangeSetHistory
import momosetkn.maigreko.core.infras.ChangeSetHistoryRepository
import momosetkn.maigreko.core.infras.jdbc.JdbcDatabase
import momosetkn.maigreko.core.infras.jdbc.JdbcQueryDsl
import momosetkn.maigreko.engine.MigrateEngine
import javax.sql.DataSource

class MigrateManagement(
    private val dataSource: DataSource,
    private val migrateEngine: MigrateEngine,
) {
    private val db = JdbcDatabase(dataSource)
    private val changeSetHistoryRepository = ChangeSetHistoryRepository(dataSource)
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
                        db.runQuery(JdbcQueryDsl.executeScript(ddl))
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
                        db.runQuery(JdbcQueryDsl.executeScript(ddl))
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
