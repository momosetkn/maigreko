package momosetkn.maigreko.dsl

import momosetkn.maigreko.change.ChangeSet

@MaigrekoDslAnnotation
class ChangeSetGroupDsl(
    private val migrationClassName: String,
    private val dslInterpreter: DslInterpreter,
) {
    private val usedIds = mutableSetOf<Int>()

    private var currentChangeSetId = 1

    @Synchronized
    fun changeSet(changeSetId: Int? = null, block: ChangeSetDsl.() -> Unit) {
        currentChangeSetId = changeSetId ?: currentChangeSetId
        if (usedIds.contains(currentChangeSetId)) {
            requireNotNull(changeSetId) {
                "Change set ID $currentChangeSetId is already used"
            }
            currentChangeSetId = usedIds.max() + 1
        }
        usedIds += currentChangeSetId
        val changeSetDsl = ChangeSetDsl()
        changeSetDsl.block()
        val changeSet = ChangeSet(
            migrationClass = migrationClassName,
            changeSetId = currentChangeSetId,
            changes = changeSetDsl.getChanges()
        )
        dslInterpreter.interpret(migrationClassName, changeSet)
        currentChangeSetId++
    }
}
