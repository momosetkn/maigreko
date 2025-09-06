package momosetkn.maigreko

import momosetkn.maigreko.change.ChangeSet
import momosetkn.maigreko.dsl.DslInterpreter
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.versioning.Versioning

class ForwardDslInterpreter(
    private val versioning: Versioning,
) : DslInterpreter {
    override fun interpret(migrationClassName: String, changeSet: ChangeSet) {
        versioning.forward(changeSet)
    }
}

class RollbackDslInterpreter(
    private val versioning: Versioning,
) : DslInterpreter {
    override fun interpret(migrationClassName: String, changeSet: ChangeSet) {
        versioning.rollback(changeSet)
    }
}

class DryRunForwardDslInterpreter(
    private val migrateEngine: MigrateEngine,
) : DslInterpreter {
    private var ddls = emptyList<String>()

    override fun interpret(migrationClassName: String, changeSet: ChangeSet) {
        ddls = changeSet.changes.map { change ->
            migrateEngine.forwardDdl(change)
        }
    }

    fun getDdls(): List<String> = ddls
}

class DryRunRollbackDslInterpreter(
    private val migrateEngine: MigrateEngine,
) : DslInterpreter {
    private var ddls = emptyList<String>()

    override fun interpret(migrationClassName: String, changeSet: ChangeSet) {
        ddls = changeSet.changes.map { change ->
            migrateEngine.rollbackDdl(change)
        }
    }

    fun getDdls(): List<String> = ddls
}
