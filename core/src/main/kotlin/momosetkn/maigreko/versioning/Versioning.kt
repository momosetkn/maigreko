package momosetkn.maigreko.versioning

import momosetkn.maigreko.change.ChangeSet
import momosetkn.maigreko.jdbc.JdbcDatabase
import momosetkn.maigreko.jdbc.JdbcQueryDsl
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.versioning.infras.ChangeSetHistory
import momosetkn.maigreko.versioning.infras.ChangeSetHistoryRepository
import javax.sql.DataSource

class Versioning(
    private val dataSource: DataSource,
    private val migrateEngine: MigrateEngine,
) {
    private val db = JdbcDatabase(dataSource)
    private val changeSetHistoryRepository = ChangeSetHistoryRepository(dataSource)
    private val versioningBootstrap = VersioningBootstrap(db, migrateEngine)

    fun forward(
        vararg changeSet: ChangeSet
    ) {
        forward(changeSet.toList())
    }

    fun forward(
        changeSets: List<ChangeSet>
    ) {
        inMigration {
            changeSets.forEach { changeSet ->
                val existsHistory = changeSetHistoryRepository.fetchChangeSetHistory(changeSet.changeSetId)

                if (existsHistory == null) {
                    changeSet.changes.forEach { change ->
                        val ddl = migrateEngine.forwardDdl(change)
                        db.runQuery(JdbcQueryDsl.executeScript(ddl))
                    }
                    val newHistory = changeSet.toChangeSetHistory()
                    changeSetHistoryRepository.save(newHistory)
                }
            }
        }
    }

    fun rollback(
        vararg changeSet: ChangeSet
    ) {
        rollback(changeSet.toList())
    }

    fun rollback(
        changeSets: List<ChangeSet>
    ) {
        inMigration {
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

    private fun inMigration(
        block: () -> Unit
    ) {
        db.withTransaction {
            versioningBootstrap.bootstrap()

            changeSetHistoryRepository.acquireLock()

            block()
        }
    }

    companion object {
        const val VERSIONING_TABLE_NAME = "change_set_history"
        const val VERSIONING_SEQUENCE_NAME = "change_set_history_id_seq"
    }
}

private fun ChangeSet.toChangeSetHistory(): ChangeSetHistory {
    return ChangeSetHistory(
        migrationClass = migrationClass,
        changeSetId = changeSetId,
    )
}
