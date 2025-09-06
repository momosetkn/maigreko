package momosetkn.maigreko.dsl

import momosetkn.maigreko.change.ChangeSet

interface DslInterpreter {
    fun interpret(migrationClassName: String, changeSet: ChangeSet)
}
