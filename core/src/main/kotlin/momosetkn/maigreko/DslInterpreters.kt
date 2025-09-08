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
    private var results = emptyList<DryRunResult>()

    override fun interpret(migrationClassName: String, changeSet: ChangeSet) {
        val ddls = changeSet.changes.map { change ->
            migrateEngine.forwardDdl(change)
        }
        appendDryRunResult(migrationClassName = migrationClassName, changeSet = changeSet, ddls = ddls)
    }

    private fun appendDryRunResult(migrationClassName: String, changeSet: ChangeSet, ddls: List<String>,) {
        results += DryRunResult(
            migrationClassName = migrationClassName,
            changeSet = changeSet,
            ddls = ddls,
            direction = DryRunResult.Direction.FORWARD,
        )
    }

    fun getResults(): List<DryRunResult> = results
}

class DryRunRollbackDslInterpreter(
    private val migrateEngine: MigrateEngine,
) : DslInterpreter {
    private var results = emptyList<DryRunResult>()

    override fun interpret(migrationClassName: String, changeSet: ChangeSet) {
        val ddls = changeSet.changes.map { change ->
            migrateEngine.rollbackDdl(change)
        }
        appendDryRunResult(migrationClassName = migrationClassName, changeSet = changeSet, ddls = ddls)
    }

    private fun appendDryRunResult(migrationClassName: String, changeSet: ChangeSet, ddls: List<String>,) {
        results += DryRunResult(
            migrationClassName = migrationClassName,
            changeSet = changeSet,
            ddls = ddls,
            direction = DryRunResult.Direction.BACKWARD,
        )
    }

    fun getResults(): List<DryRunResult> = results
}
