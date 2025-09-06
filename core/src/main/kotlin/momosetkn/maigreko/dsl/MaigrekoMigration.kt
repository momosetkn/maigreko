package momosetkn.maigreko.dsl

abstract class MaigrekoMigration(
    internal val body: ChangeSetGroupDsl.() -> Unit
)
